package com.vrann.cholesky

import akka.actor.typed.pubsub.Topic
import akka.actor.typed.pubsub.Topic.Command
import akka.actor.typed.scaladsl.Behaviors._
import akka.actor.typed.scaladsl.{LoggerOps, StashBuffer}
import akka.actor.typed.{ActorRef, Behavior}
import com.vrann.BlockMessage.DataReady
import com.vrann._
import com.vrann.cholesky.CholeskyBlockMatrixType.{aMN, L11, L21}

import java.io.File

class BlockSubdiagonal(position: Position,
                       topicsRegistry: TopicsRegistry[Message],
                       sectionId: Int,
                       fileTransferActor: ActorRef[Message])
    extends BlockBehavior
    with L21Operation
    with InitializeOperation
    with L11Operation {

  val matrixInterested: Map[BlockMatrixType, List[Position]] =
    Map(aMN -> List(position), L21 -> (0 until position.y).foldLeft(List.empty[Position]) { (list, y) =>
      list :+ Position(position.x, y)
    }, L11 -> List(Position(position.y, position.y)))

  val publishTo: Map[BlockMatrixType, List[Position]] =
    Map(L21 -> List(position))

  val topics: Map[String, Behavior[Command[Message]]] =
    matrixInterested.foldLeft(Map.empty[String, Behavior[Command[Message]]]) {
      case (map, (matrixType, listOfPositions)) => {
        map ++ listOfPositions.foldLeft(Map.empty[String, Behavior[Command[Message]]]) {
          case (topicsMap, position) => {
            val topic = s"matrix-${matrixType}-ready-$position"
            topicsMap + (topic -> Topic[Message](topic))
          }
        }
      }
    }

  val topicsPublishTo: Map[String, Behavior[Command[Message]]] =
    publishTo.foldLeft(Map.empty[String, Behavior[Command[Message]]]) {
      case (map, (matrixType, listOfPositions)) => {
        map ++ listOfPositions.foldLeft(Map.empty[String, Behavior[Command[Message]]]) {
          case (topicsMap, position) => {
            val topic = s"matrix-${matrixType}-ready-$position"
            topicsMap + (topic -> Topic[Message](topic))
          }
        }
      }
    }

  val expectedL21: Int = matrixInterested(L21).size

  override def apply: Behavior[Message] = withStash(this.position.y) { buffer: StashBuffer[Message] =>
    state(new File(""), Uninitialized, List.empty[Position], buffer)
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
  private def state(file: File,
                    stateTransition: State,
                    processedL21: List[Position],
                    buffer: StashBuffer[Message]): Behavior[Message] = setup { context =>
    receiveMessage[Message] { message =>
      (stateTransition, message) match {

        /** 1. subdiagonal (Unitialized, b(M, N) -> Initialized */
        case (Uninitialized, DataReady(pos, blockMatrixType, filePath, sectionId, ref))
            if blockMatrixType.equals(aMN) && pos.equals(position) =>
          if (sectionId != this.sectionId) {
            context.log.debug(s"Remote data $message")
            ref ! FileTransferRequestMessage(pos, blockMatrixType, fileTransferActor)
            same
          } else {
            context.log.debug(s"Local data $message")
            initialize(position, buffer, filePath, state)
          }

        /**  2. subdiagonal: (Initialized, b(M, N) -> Initialized, skip */
        case (Initialized, DataReady(pos, blockMatrixType, _, _, _))
            if blockMatrixType.equals(aMN) && pos.equals(position) =>
          context.log.debug("skip initialization")
          same
        case (L21Applied, DataReady(pos, blockMatrixType, _, _, _))
            if blockMatrixType.equals(aMN) && pos.equals(position) =>
          context.log.debug("skip initialization")
          same
        case (SomeL21Applied, DataReady(pos, blockMatrixType, _, _, _))
            if blockMatrixType.equals(aMN) && pos.equals(position) =>
          context.log.debug("skip initialization")
          same

        /**
         * 3. (Unitialized, L21(M, N) -> Uninitialized, stash if M == M && N < N
         */
        case (Uninitialized, message @ DataReady(pos, blockMatrixType, _, _, _))
            if blockMatrixType.equals(L21) && matrixInterested(L21).contains(pos) =>
          context.log.debug("L21 stashed")
          buffer.stash(message)

          same

        /**
         * 4. (Initialized, L21(M, N) -> SomeL21Applied, if M == M && N < N && l211Required.size > 0
         * 5. (Initialized, L21(M, N) -> L21Applied, if M == M && N < N && l211Required.size == 0
         */
        case (Initialized, message @ DataReady(pos, blockMatrixType, _, sectionId, ref))
            if blockMatrixType.equals(L21) && matrixInterested(L21).contains(pos) =>
          if (sectionId != this.sectionId) {
            context.log.debug(s"Remote data $message")
            ref ! FileTransferRequestMessage(pos, blockMatrixType, fileTransferActor)
            same
          } else {
            applyL21(
              position,
              expectedL21,
              message,
              processedL21,
              buffer,
              file,
              topicsRegistry,
              state,
              sectionId,
              fileTransferActor)
          }

        case (SomeL21Applied, message @ DataReady(pos, blockMatrixType, _, sectionId, ref))
            if blockMatrixType.equals(L21) && matrixInterested(L21).contains(pos) =>
          if (sectionId != this.sectionId) {
            context.log.debug(s"Remote data $message")
            ref ! FileTransferRequestMessage(pos, blockMatrixType, fileTransferActor)
            same
          } else {
            applyL21(
              position: Position,
              expectedL21,
              message,
              processedL21,
              buffer,
              file,
              topicsRegistry,
              state,
              sectionId,
              fileTransferActor)
          }

        case (L21Applied, DataReady(pos, blockMatrixType, _, _, _))
            if blockMatrixType.equals(L21) && matrixInterested(L21).contains(pos) =>
          context.log.debug("skip L21")
          same

        /**
         * 6. (Uninitialized, L11(M, N) -> Uninitialized, stash
         * 7. (Initialized, L11(M, N) -> Initialized, stash
         * 8. (SomeL21Applied, L11(M, N) -> SomeL21Applied, stash
         * 9. (L21Applied, L11(M, N) -> Done, applyL11
         */
        case (Uninitialized, message @ DataReady(pos, blockMatrixType, filePath, _, _))
            if blockMatrixType.equals(L11) && pos.equals(Position(position.y, position.y)) =>
          context.log.debug(s"$message to $position")
          context.log.debug("Uninitialized L11 stashed")
          buffer.stash(message)
          same
        case (Initialized, message @ DataReady(pos, blockMatrixType, filePath, _, _))
            if blockMatrixType.equals(L11) && pos.equals(Position(position.y, position.y)) =>
          context.log.debug(s"$message to $position")
          context.log.debug(s"Initialized L11 stashed $position")
          buffer.stash(message)
          same
        case (SomeL21Applied, message @ DataReady(pos, blockMatrixType, filePath, _, _))
            if blockMatrixType.equals(L11) && pos.equals(Position(position.y, position.y)) =>
          context.log.debug(s"$message to $position")
          context.log.debug("SomeL21Applied L11 stashed")
          buffer.stash(message)
          same
        case (L21Applied, message @ DataReady(pos, blockMatrixType, _, sectionId, ref))
            if blockMatrixType.equals(L11) && pos.equals(Position(position.y, position.y)) =>
          context.log.debug(s"L21Applied applyL11 from $pos at $position")
          if (sectionId != this.sectionId) {
            context.log.debug(s"Remote data $message")
            ref ! FileTransferRequestMessage(pos, blockMatrixType, fileTransferActor)
            same
          } else {
            applyL11(position, message, processedL21, buffer, file, topicsRegistry, state, sectionId, fileTransferActor)
          }

        case (Done, _) => throw new Exception("Out of order message")
        case (_, _) =>
          context.log.debug2("Message {} to block in state {} is unhandled", message.getClass, stateTransition)
          unhandled
      }
    }
  }
}
