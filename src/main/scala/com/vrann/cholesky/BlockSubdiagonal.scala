package com.vrann.cholesky

import java.io.File

import akka.actor.typed.Behavior
import akka.actor.typed.pubsub.Topic
import akka.actor.typed.pubsub.Topic.{Command, Publish}
import akka.actor.typed.scaladsl.Behaviors.{receiveMessage, same, setup, stopped, unhandled, withStash}
import akka.actor.typed.scaladsl.LoggerOps
import akka.actor.typed.scaladsl.StashBuffer
import com.vrann.BlockMessage.DataReady
import com.vrann.cholesky.CholeskyBlockMatrixType.{aMN, L11, L21}
import com.vrann.{
  BlockBehavior,
  BlockMatrixType,
  BlockRole,
  Done,
  Initialized,
  L21Applied,
  Message,
  Position,
  SomeL21Applied,
  State,
  TopicsRegistry,
  Uninitialized
}

class BlockSubdiagonal(position: Position, topicsRegistry: TopicsRegistry[Message]) extends BlockBehavior {

  val matrixInterested: Map[BlockMatrixType, List[Position]] =
    Map(aMN -> List(position), L21 -> (0 until position.y).foldLeft(List.empty[Position]) { (list, y) =>
      list :+ Position(position.x, y)
    }, L11 -> List(Position(position.y, position.y)))

  val topics: Map[String, Behavior[Command[Message]]] =
    matrixInterested.foldLeft(Map.empty[String, Behavior[Command[Message]]]) {
      case (map, (matrixType, listOfPositions)) => {
        map ++ listOfPositions.foldLeft(Map.empty[String, Behavior[Command[Message]]]) {
          case (topicsMap, position) => {
            val topic = s"matrix-$matrixType-ready-$position"
            topicsMap + (topic -> Topic[Message](topic))
          }
        }
      }
    }

//  val topics: Map[String, Behavior[Command[Message]]] =
//    List(s"matrix-aMN-ready", s"matrix-A11-ready", s"matrix-L11-ready")
//      .foldLeft(Map.empty[String, Behavior[Command[Message]]])((map, topicName) => {
//        val topic = s"$topicName-$position"
//        map + (topic -> Topic[Message](topic))
//      }) ++ (0 until position.y).foldLeft(Map.empty[String, Behavior[Command[Message]]]) { (map, l21positionY) =>
//      {
//        val l21Position = Position(position.x, l21positionY)
//        val topic = s"matrix-L21-ready-$l21Position"
//        map + (topic -> Topic[Message](topic))
//      }
//    }

  val expectedL21: Int = matrixInterested(L21).size

  override def apply: Behavior[Message] = withStash(this.position.y) { buffer: StashBuffer[Message] =>
    state(Uninitialized, List.empty[Position], buffer)
  }

  private def initialize(buffer: StashBuffer[Message]): Behavior[Message] = setup { context =>
    context.log.debug(s"Initializing aMN at $position")
    context.log.debug("L21 unstashing")
    buffer.unstashAll(state(Initialized, List.empty[Position], buffer))
    if (position.y == 0) {
      state(L21Applied, List.empty[Position], buffer)
    } else { state(Initialized, List.empty[Position], buffer) }

  }

  private def applyL21(message: DataReady,
                       processedL21: List[Position],
                       buffer: StashBuffer[Message]): Behavior[Message] = setup { context =>
    context.log.debug(s"L21 from ${message.pos} applied at $position")
    val newProcessed = processedL21 :+ message.pos

    if (newProcessed.size == expectedL21) {
      //unstash L11
      buffer.unstashAll(state(L21Applied, processedL21, buffer))
//      state(L21Applied, newProcessed, buffer)
    } else {
      state(SomeL21Applied, newProcessed, buffer)
    }
  }

  private def applyL11(message: DataReady,
                       processedL21: List[Position],
                       buffer: StashBuffer[Message]): Behavior[Message] = setup { context =>
    context.log.debug(s"L11 from ${message.pos} applied at $position")
    context.log.debug(s"Publishing matrix-$L21-ready-$position")
    topicsRegistry(s"matrix-$L21-ready-$position") ! Publish(DataReady(position, L21, message.filePath))
    state(Done, processedL21, buffer)
  }

  private def factorize(processedL21: List[Position], buffer: StashBuffer[Message]): Behavior[Message] = setup {
    context =>
      context.log.debug("factorized")
      stopped
    //state(Done, processedL21, buffer)
  }

  /**
   * State machine
   *
   * blocks: first, diagonal, subdiagonal
   * states: Uninitalized, Initialized, L21Applied, L11Applied, Done
   * operations: factorize, applyL11, applyL21
   * After state is Done actor should stop
   *
   *
   * 1. (Uninitialized, b(M, N) -> Initialized
   * 2. (Initialized, b(M, N) -> Initialized, skip
   *
   * 3. (Unitialized, L21(M, N) -> Uninitialized, stash if M == M && N < N
   * 4. (Initialized, L21(M, N) -> SomeL21Applied, if M == M && N < N && l211Required.size > 0
   * 5. (Initialized, L21(M, N) -> L21Applied, if M == M && N < N && l211Required.size == 0
   *
   * 6. (Uninitialized, L11(M, N) -> Uninitialized, stash
   * 7. (Initialized, L11(M, N) -> Initialized, stash
   * 8. (SomeL21Applied, L11(M, N) -> SomeL21Applied, stash
   * 9. (L21Applied, L11(M, N) -> Done, applyL11
   *
   * @param stateTransition
   * @param processedL21
   * @param buffer
   * @return
   */
  private def state(stateTransition: State,
                    processedL21: List[Position],
                    buffer: StashBuffer[Message]): Behavior[Message] = setup { context =>
    receiveMessage[Message] { message =>
      (stateTransition, message) match {

        /** 1. subdiagonal (Unitialized, b(M, N) -> Initialized */
        case (Uninitialized, DataReady(pos, blockMatrixType, _))
            if blockMatrixType.equals(aMN) && pos.equals(position) =>
          initialize(buffer)

        /**  2. subdiagonal: (Initialized, b(M, N) -> Initialized, skip */
        case (Initialized, DataReady(pos, blockMatrixType, _)) if blockMatrixType.equals(aMN) && pos.equals(position) =>
          context.log.debug("skip initialization")
          same
        case (L21Applied, DataReady(pos, blockMatrixType, _)) if blockMatrixType.equals(aMN) && pos.equals(position) =>
          context.log.debug("skip initialization")
          same
        case (SomeL21Applied, DataReady(pos, blockMatrixType, _))
            if blockMatrixType.equals(aMN) && pos.equals(position) =>
          context.log.debug("skip initialization")
          same

        /**
         * 3. (Unitialized, L21(M, N) -> Uninitialized, stash if M == M && N < N
         */
        case (Uninitialized, message @ DataReady(pos, blockMatrixType, _))
            if blockMatrixType.equals(L21) && matrixInterested(L21).contains(pos) =>
          context.log.debug("L21 stashed")
          buffer.stash(message)

          same

        /**
         * 4. (Initialized, L21(M, N) -> SomeL21Applied, if M == M && N < N && l211Required.size > 0
         * 5. (Initialized, L21(M, N) -> L21Applied, if M == M && N < N && l211Required.size == 0
         */
        case (Initialized, message @ DataReady(pos, blockMatrixType, filePath))
            if blockMatrixType.equals(L21) && matrixInterested(L21).contains(pos) =>
          applyL21(message, processedL21, buffer)

        case (SomeL21Applied, message @ DataReady(pos, blockMatrixType, filePath))
            if blockMatrixType.equals(L21) && matrixInterested(L21).contains(pos) =>
          applyL21(message, processedL21, buffer)

        case (L21Applied, DataReady(pos, blockMatrixType, _))
            if blockMatrixType.equals(L21) && matrixInterested(L21).contains(pos) =>
          context.log.debug("skip L21")
          same

        /**
         * 6. (Uninitialized, L11(M, N) -> Uninitialized, stash
         * 7. (Initialized, L11(M, N) -> Initialized, stash
         * 8. (SomeL21Applied, L11(M, N) -> SomeL21Applied, stash
         * 9. (L21Applied, L11(M, N) -> Done, applyL11
         */
        case (Uninitialized, message @ DataReady(pos, blockMatrixType, filePath))
            if blockMatrixType.equals(L11) && pos.equals(Position(position.y, position.y)) =>
          context.log.debug("L11 stashed")
          buffer.stash(message)
          same
        case (Initialized, message @ DataReady(pos, blockMatrixType, filePath))
            if blockMatrixType.equals(L11) && pos.equals(Position(position.y, position.y)) =>
          context.log.debug("L11 stashed")
          buffer.stash(message)
          same
        case (SomeL21Applied, message @ DataReady(pos, blockMatrixType, filePath))
            if blockMatrixType.equals(L11) && pos.equals(Position(position.y, position.y)) =>
          context.log.debug("L11 stashed")
          buffer.stash(message)
          same
        case (L21Applied, message @ DataReady(pos, blockMatrixType, filePath))
            if blockMatrixType.equals(L11) && pos.equals(Position(position.y, position.y)) =>
          applyL11(message, processedL21, buffer)

        case (Done, _) => throw new Exception("Out of order message")
        case (_, _) =>
          context.log.debug2("Message {} to block in state {} is unhandled", message.getClass, stateTransition)
          unhandled
      }
    }
  }
}
