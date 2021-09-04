package com.vrann.cholesky

import akka.actor.typed.pubsub.Topic
import akka.actor.typed.pubsub.Topic.Command
import akka.actor.typed.scaladsl.Behaviors._
import akka.actor.typed.scaladsl.StashBuffer
import akka.actor.typed.{ActorRef, Behavior}
import com.vrann.BlockMessage.DataReady
import com.vrann._
import com.vrann.cholesky.CholeskyBlockMatrixType.{aMN, L11, L21}

import java.io.File

class BlockDiagonal(position: Position,
                    //topicsRegistry: TopicsRegistry[Message],
                    sectionId: Int,
                    fileTransferActor: ActorRef[Message],
                    section: ActorRef[Message])
    extends BlockBehavior
    with L21Operation
    with InitializeOperation {

  val matrixInterested: Map[BlockMatrixType, List[Position]] =
    Map(aMN -> List(position), L21 -> (0 until position.x).foldLeft(List.empty[Position]) { (list, x) =>
      list :+ Position(x, position.y)
    })

  val publishTo: Map[BlockMatrixType, List[Position]] =
    Map(L11 -> List(position))

  val topics: Map[String, Behavior[Command[Message]]] =
    matrixInterested.foldLeft(Map.empty[String, Behavior[Command[Message]]]) {
      case (map, (matrixType, listOfPositions)) => {
        map ++ listOfPositions.foldLeft(Map.empty[String, Behavior[Command[Message]]]) {
          case (topicsMap, position) => {
            var newMap = topicsMap
            val topic = s"matrix-${matrixType}-ready-$position"
            newMap = newMap + (topic -> Topic[Message](topic))
            val topicLocal = s"matrix-${matrixType}-section${sectionId}-ready-$position"
            newMap = newMap + (topicLocal -> Topic[Message](topicLocal))
            newMap
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
  private def state(file: File,
                    stateTransition: State,
                    processedL21: List[Position],
                    buffer: StashBuffer[Message]): Behavior[Message] = setup { context =>
    receiveMessage[Message] { message =>
      (stateTransition, message) match {

        /** 1. first: (Unitialized, DataReady(aMN(M, N)) -> Initialized, factorize, Done */
        /** 2. diagonal+subdiagonal: (Unitialized, b(M, N) -> Initialized */
        case (Uninitialized, message @ DataReady(pos, blockMatrixType, filePath, sectionId, ref))
            if blockMatrixType.equals(aMN) && pos.equals(position) =>
          if (sectionId != this.sectionId) {
            //context.log.info(s"Remote data $message")
            fileTransferActor ! FileTransferRequestDelegateMessage(pos, blockMatrixType, ref)
            same
          } else {
            if (pos.equals(Position(0, 0))) {
              context.log.info("0-0 Done")
              factorize(position, processedL21, buffer, filePath, section, state, sectionId, ref)
            } else {
              initialize(position, buffer, filePath, state)
            }
          }

        /**  3. first+diagonal+subdiagonal: (Initialized, b(M, N) -> Initialized, skip */
        case (Initialized, DataReady(pos, blockMatrixType, _, _, _))
            if blockMatrixType.equals(aMN) && pos.equals(position) =>
          same

        /** 4. first+diagonal (Unitialized, L11(M, N) -> Unitialized, skip (OutOfOrderException)
         * 5. first+diagonal: (Initialized, L11(M, N) -> Initialized, skip (OutOfOrderException)
         */
        case (Uninitialized, DataReady(pos, blockMatrixType, _, _, _))
            if blockMatrixType.equals(L11) && pos.equals(position) =>
          throw new Exception("L11 is not expected for a Diagonal element")
        case (Initialized, DataReady(pos, blockMatrixType, _, _, _))
            if blockMatrixType.equals(L11) && pos.equals(position) =>
          throw new Exception("L11 is not expected for a Diagonal element")

        /** 6. first (Uninitialized, L21(M, N) -> Uninitialized, skip (UnapplicableException) */
        /** 7. diagonal (Uninitialized, L21(M, N) -> Uninitialized, stash if M == M && N < N */
        case (Uninitialized, message @ DataReady(pos, blockMatrixType, _, _, _))
            if blockMatrixType.equals(L21) && matrixInterested(L21).contains(pos) =>
          context.log.info(s"L21 stashed at Unitialized block $position")
          buffer.stash(message)
          same

        /**
         * 8. first (Initialized, L21(M, N) -> Initialized, skip (UnapplicableException)
         * 9. diagonal (Initialized, L21(M, N) -> SomeL21Applied, apply if M == M && N < N
         */
        case (Initialized, message @ DataReady(pos, blockMatrixType, _, sectionId, ref))
            if blockMatrixType.equals(L21) && matrixInterested(L21).contains(pos) =>
          if (sectionId != this.sectionId) {
            //context.log.debug(s"Remote data $message")
            fileTransferActor ! FileTransferRequestDelegateMessage(pos, blockMatrixType, ref)
            same
          } else {
            applyL21(
              position,
              expectedL21,
              message,
              processedL21,
              buffer,
              file,
              section,
              state,
              sectionId,
              fileTransferActor)
          }

        /**
         * 11. diagonal (SomeL21Applied, L21(M, N) -> SomeL21Applied, apply if M == M && N < N, SomeL21Applied(for all but last, reduce the list of required L21s), send message AllL21Applied t self for the last
         * 12. diagonal (Initialized, AllL21Applied -> factorize, Done
         */
        case (SomeL21Applied, message @ DataReady(pos, blockMatrixType, _, sectionId, ref))
            if blockMatrixType.equals(L21) && matrixInterested(L21).contains(pos) =>
          if (sectionId != this.sectionId) {
            //context.log.debug(s"Remote data $message")
            fileTransferActor ! FileTransferRequestDelegateMessage(pos, blockMatrixType, ref)
            same
          } else {
            applyL21(
              position,
              expectedL21,
              message,
              processedL21,
              buffer,
              file,
              section,
              state,
              sectionId,
              fileTransferActor)
          }

        case (Done, _) => {
          //context.log.info2("Out of order message {}, {}", message.getClass, stateTransition)
          //throw new Exception("Out of order message")
          same
        }
        case (_, _) =>
          context.log.info("Message {} to block {} in state {} is unhandled", message, position, stateTransition)
          unhandled
      }
    }
  }
}
