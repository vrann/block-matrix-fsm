package com.vrann.cholesky

import java.io.File

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.pubsub.Topic
import akka.actor.typed.pubsub.Topic.Command
import akka.actor.typed.scaladsl.{Behaviors, LoggerOps}
import akka.actor.typed.scaladsl.Behaviors.{receiveMessage, same, setup, unhandled}
import com.vrann.BlockMessage.{AijData, DataReady, GetState, L11Ready, L21Data, StateMessage}
import com.vrann.{
  A11Processed,
  BlockBehavior,
  BlockMessage,
  BlockTopicRegistry,
  L21Processed,
  Message,
  Position,
  State,
  TopicsRegistry,
  Uninitialized
}

//object A11Topics {
//  val A11Data = Topic[Message]("a11-data")
//  val L11Ready = Topic[Message]("l11-ready")
//}

class DiagonalBlock(position: Position, topicsRegistry: TopicsRegistry[Message]) extends BlockBehavior {

  var topics: Map[String, Behavior[Command[Message]]] =
    List(s"matrix-A11-ready", s"matrix-L11-ready").foldLeft(Map.empty[String, Behavior[Command[Message]]])(
      (map, topicName) => map + (topicName -> Topic[Message](topicName)))
//    Map((s"matrix-A11-ready-$position" -> A11Topics.A11Data), ("l11-ready" -> A11Topics.L11Ready))

  override def apply: Behavior[Message] = {
    state(Uninitialized)
  }

  private def state(stateTransition: State): Behavior[Message] = setup { context =>
    receiveMessage[Message] { message =>
      (stateTransition, message) match {
        case (Uninitialized, AijData(position, filePath, sectionId)) =>
          context.log.debug("A11 processed")
          val l11FilePath = processA11(position, filePath, sectionId)
          topicsRegistry("matrix-L11-ready", position) ! Topic.publish(L11Ready(position, l11FilePath, sectionId))
          state(A11Processed)
        case (A11Processed, L21Data(position, filePath, sectionId)) =>
          context.log.debug("L21 processed")
          val l11FilePath = processL21(position, filePath, sectionId)
          topicsRegistry("l11-ready", position) ! Topic.publish(L11Ready(position, l11FilePath, sectionId))
          state(L21Processed)
        case (_, GetState(Position(0, 0), replyTo)) =>
          replyTo ! StateMessage(Position(0, 0), stateTransition)
          same
        case (_, DataReady(position, matrixType, filePath)) =>
          context.log
            .debug("Diagonal Block {} Received data for matrix {} in position {}", this.position, matrixType, position)
          same
        case (_, _) =>
          context.log.debug2("Message to block unhandled {} in state {}", message.getClass, stateTransition)
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
