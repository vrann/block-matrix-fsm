package com.vrann.cholesky

import java.io.File

import akka.actor.typed.Behavior
import akka.actor.typed.pubsub.Topic
import akka.actor.typed.pubsub.Topic.{Command, Publish}
import akka.actor.typed.scaladsl.{Behaviors, LoggerOps, StashBuffer}
import akka.actor.typed.scaladsl.Behaviors.{receiveMessage, same, setup, stopped, unhandled, withStash}
import com.vrann.BlockMessage.DataReady
import com.vrann.cholesky.CholeskyBlockMatrixType.{aMN, L11, L21}
import com.vrann.{
  BlockBehavior,
  BlockMatrixType,
  Done,
  Initialized,
  Message,
  Position,
  SomeL21Applied,
  State,
  TopicsRegistry,
  Uninitialized
}

class BlockDiagonal(position: Position, topicsRegistry: TopicsRegistry[Message]) extends BlockBehavior {

  val matrixInterested: Map[BlockMatrixType, List[Position]] =
    Map(aMN -> List(position), L21 -> (0 until position.y).foldLeft(List.empty[Position]) { (list, y) =>
      list :+ Position(position.x, y)
    })

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

//  val topics2: Map[String, Behavior[Command[Message]]] =
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
    context.log.debug(s"L21 unstashing at $position")
    buffer.unstashAll(state(Initialized, List.empty[Position], buffer))
  //state(Initialized, List.empty[Position], buffer)
  }

  private def applyL21(message: DataReady,
                       processedL21: List[Position],
                       buffer: StashBuffer[Message]): Behavior[Message] = setup { context =>
    context.log.debug(s"L21 from ${message.pos} applied at $position")
    val newProcessed = processedL21 :+ message.pos
    if (newProcessed.size == expectedL21) {
      if (!buffer.isEmpty) {
        throw new Exception("Unexpected L21")
      }
      factorize(newProcessed, buffer, message.filePath)
    } else {
      state(SomeL21Applied, newProcessed, buffer)
    }
  }

  private def factorize(processedL21: List[Position], buffer: StashBuffer[Message], filePath: File): Behavior[Message] =
    setup { context =>
      context.log.debug(s"factorized $position")
      if (topicsRegistry.hasTopic(s"matrix-$L11-ready-$position")) {
        context.log.debug(s"Publishing matrix-$L11-ready-$position")
        topicsRegistry(s"matrix-$L11-ready-$position") ! Publish(DataReady(position, L11, filePath))
        context.log.debug(s"Done $position")
      } else {
        context.log.debug(s"Process is Done after $position factorized")
      }
      state(Done, List.empty[Position], buffer)
    //Behaviors.stopped[Message]
    //stopped
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
   * 1. first: (Unitialized, DataReady(aMN(M, N)) -> Initialized, factorize, Done
   * 2. diagonal+subdiagonal: (Unitialized, b(M, N) -> Initialized
   * 3. first+diagonal+subdiagonal: (Initialized, b(M, N) -> Initialized, skip
   * 4. first+diagonal (Unitialized, L11(M, N) -> Unitialized, skip (OutOfOrderException)
   * 5. first+diagonal: (Initialized, L11(M, N) -> Initialized, skip (OutOfOrderException)
   *
   * 6. first (Unitialized, L21(M, N) -> Uninitialized, skip (UnapplicableException)
   * 7. diagonal (Unitialized, L21(M, N) -> Uninitialized, stash if M == M && N < N
   *
   * 8. first (Initialized, L21(M, N) -> Initialized, skip (UnapplicableException)
   * 9. diagonal (Initialized, L21(M, N) -> SomeL21Applied, apply if M == M && N < N
   *
   * 11. diagonal (SomeL21Applied, L21(M, N) -> SomeL21Applied, apply if M == M && N < N, SomeL21Applied(for all but last, reduce the list of required L21s), send message AllL21Applied t self for the last
   * 12. diagonal (Initialized, AllL21Applied -> factorize, Done
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

        /** 1. first: (Unitialized, DataReady(aMN(M, N)) -> Initialized, factorize, Done */
        /** 2. diagonal+subdiagonal: (Unitialized, b(M, N) -> Initialized */
        case (Uninitialized, DataReady(pos, blockMatrixType, filePath))
            if blockMatrixType.equals(aMN) && pos.equals(position) =>
          if (pos.equals(Position(0, 0))) {
            factorize(processedL21, buffer, filePath)
//            context.log.debug("Done")
//            same //we should stop actor here
          } else {
            context.log.debug("aMN")
            val res = initialize(buffer)
            res
          }

        /**  3. first+diagonal+subdiagonal: (Initialized, b(M, N) -> Initialized, skip */
        case (Initialized, DataReady(pos, blockMatrixType, _)) if blockMatrixType.equals(aMN) && pos.equals(position) =>
          same

        /** 4. first+diagonal (Unitialized, L11(M, N) -> Unitialized, skip (OutOfOrderException)
         * 5. first+diagonal: (Initialized, L11(M, N) -> Initialized, skip (OutOfOrderException)
         */
        case (Uninitialized, DataReady(pos, blockMatrixType, _))
            if blockMatrixType.equals(L11) && pos.equals(position) =>
          throw new Exception("L11 is not expected for a Diagonal element")
        case (Initialized, DataReady(pos, blockMatrixType, _)) if blockMatrixType.equals(L11) && pos.equals(position) =>
          throw new Exception("L11 is not expected for a Diagonal element")

        /** 6. first (Unitialized, L21(M, N) -> Uninitialized, skip (UnapplicableException) */
        /** 7. diagonal (Unitialized, L21(M, N) -> Uninitialized, stash if M == M && N < N */
        case (Uninitialized, message @ DataReady(pos, blockMatrixType, _))
            if blockMatrixType.equals(L21) && matrixInterested(L21).contains(pos) =>
          context.log.debug("L21 stashed")
          buffer.stash(message)
          same

        /**
         * 8. first (Initialized, L21(M, N) -> Initialized, skip (UnapplicableException)
         * 9. diagonal (Initialized, L21(M, N) -> SomeL21Applied, apply if M == M && N < N
         */
        case (Initialized, message @ DataReady(pos, blockMatrixType, filePath))
            if blockMatrixType.equals(L21) && matrixInterested(L21).contains(pos) =>
          applyL21(message, processedL21, buffer)

        /**
         * 11. diagonal (SomeL21Applied, L21(M, N) -> SomeL21Applied, apply if M == M && N < N, SomeL21Applied(for all but last, reduce the list of required L21s), send message AllL21Applied t self for the last
         * 12. diagonal (Initialized, AllL21Applied -> factorize, Done
         */
        case (SomeL21Applied, message @ DataReady(pos, blockMatrixType, filePath))
            if blockMatrixType.equals(L21) && matrixInterested(L21).contains(pos) =>
          applyL21(message, processedL21, buffer)

        case (Done, _) => throw new Exception("Out of order message")
        case (_, _) =>
          context.log.debug2("Message {} to block in state {} is unhandled", message.getClass, stateTransition)
          unhandled
      }
    }
  }

  private def processL21(position: Position, filePath: File, sectionId: Int): File = {
    filePath
    //    try {
    //      val A11 = UnformattedMatrixReader.ofPositionAndMatrixType[Nothing](message.getPosition, message.getMatrixType).readMatrix(new Nothing)
    //      val L11 = Factorization.apply(A11)
    //      val writer = UnformattedMatrixWriter.ofFileLocator(MatrixTypePositionFileLocator.getFile, message.getPosition, BlockMatrixType.L11)
    //      writer.writeMatrix(L11)
    //      mediator.tell(new Nothing(FileTransferReady.getTopic(message.getPosition), FileTransferReady.message(message.getPosition, BlockMatrixType.L11, MatrixTypePositionFileLocator.getFile(message.getPosition, BlockMatrixType.L11).toString, sectionId)), selfReference)
    //    } catch {
    //      case exception: Nothing =>
    //        log.error("File for the matrix block is not found {} in section {}", exception.getMessage, sectionId)
    //        throw new Nothing("File for the matrix block is not found", exception)
    //    }
  }

  private def processA11(position: Position, filePath: File, sectionId: Int): File = {
    filePath
    //    try {
    //      val A11 = UnformattedMatrixReader.ofPositionAndMatrixType[Nothing](message.getPosition, message.getMatrixType).readMatrix(new Nothing)
    //      val L11 = Factorization.apply(A11)
    //      val writer = UnformattedMatrixWriter.ofFileLocator(MatrixTypePositionFileLocator.getFile, message.getPosition, BlockMatrixType.L11)
    //      writer.writeMatrix(L11)
    //      mediator.tell(new Nothing(FileTransferReady.getTopic(message.getPosition), FileTransferReady.message(message.getPosition, BlockMatrixType.L11, MatrixTypePositionFileLocator.getFile(message.getPosition, BlockMatrixType.L11).toString, sectionId)), selfReference)
    //    } catch {
    //      case exception: Nothing =>
    //        log.error("File for the matrix block is not found {} in section {}", exception.getMessage, sectionId)
    //        throw new Nothing("File for the matrix block is not found", exception)
    //    }
  }
}
