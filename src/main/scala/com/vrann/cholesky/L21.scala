//package com.vrann.cholesky
//
//import java.io.File
//
//import akka.actor.typed.{ActorRef, Behavior}
//import akka.actor.typed.pubsub.Topic
//import akka.actor.typed.pubsub.Topic.Command
//import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
//import akka.actor.typed.scaladsl.Behaviors.{receive, receiveMessage, receiveMessagePartial, same, unhandled}
//import com.vrann.BlockMessage.{AijData, GetState, L11Ready, L21Data, MatrixDataAvailable, StateMessage}
//import com.vrann._
//
//class L21 extends BlockBehavior {
//  def apply(topicsRegistry: Map[String, ActorRef[Command[BlockMessage]]]): Behavior[BlockMessage] =
//    state(topicsRegistry, Uninitialized)
//
//  var topics: Map[String, Behavior[Command[BlockMessage]]] =
//    Map(("a11-data" -> A11Topics.A11Data), ("l11-ready" -> A11Topics.L11Ready))
//
//  private def state(topicsRegistry: Map[String, ActorRef[Command[BlockMessage]]],
//                    stateTransition: State): Behavior[BlockMessage] =
////    Behaviors.setup(context => {
//    //    val aijDataTopic = context.spawn(Topic[AijData]("a11-data"), "AijDataTopic")
//    //    val l11ReadyTopic = context.spawn(Topic[BlockMessage]("l11-ready"), "L11ReadyTopic")
//
//    //    val A11Actor = context.spawn(
//    receiveMessage[BlockMessage] { message =>
//      (stateTransition, message) match {
//        case (A11Processed, L21Data(position, filePath, sectionId)) =>
//          val l11FilePath = process(position, filePath, sectionId)
//          topicsRegistry("l11-ready") ! Topic.publish(L11Ready(position, l11FilePath, sectionId))
//          state(topicsRegistry, L21Processed)
//        case (_, GetState(Position(0, 0), replyTo)) =>
//          replyTo ! StateMessage(Position(0, 0), stateTransition)
//          same
//        case _ =>
//          println("L21 unhandled")
//          unhandled
//      }
//
//    }
//  //      , "A11Actor")
//  //
//  //    aijDataTopic ! Topic.Subscribe(A11Actor)
//  //
//  //    receiveMessage[BlockMessage] { message =>
//  //      A11Actor ! message
//  //      same
//  //    }
////    })
//
//  private def process(position: Position, filePath: File, sectionId: Int): File = {
//    filePath
//    //    try {
//    //      val A11 = UnformattedMatrixReader.ofPositionAndMatrixType[Nothing](message.getPosition, message.getMatrixType).readMatrix(new Nothing)
//    //      val L11 = Factorization.apply(A11)
//    //      val writer = UnformattedMatrixWriter.ofFileLocator(MatrixTypePositionFileLocator.getFile, message.getPosition, BlockMatrixType.L11)
//    //      writer.writeMatrix(L11)
//    //      mediator.tell(new Nothing(FileTransferReady.getTopic(message.getPosition), FileTransferReady.message(message.getPosition, BlockMatrixType.L11, MatrixTypePositionFileLocator.getFile(message.getPosition, BlockMatrixType.L11).toString, sectionId)), selfReference)
//    //    } catch {
//    //      case exception: Nothing =>
//    //        log.error("File for the matrix block is not found {} in section {}", exception.getMessage, sectionId)
//    //        throw new Nothing("File for the matrix block is not found", exception)
//    //    }
//  }
//}
