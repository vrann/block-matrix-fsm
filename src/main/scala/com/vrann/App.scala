package com.vrann

import java.nio.file.{Path, Paths}

import akka.actor.typed.pubsub.Topic
import akka.actor.typed.pubsub.Topic.{Publish, Subscribe}
import akka.actor.typed.scaladsl.ActorContext
import akka.actor.typed.scaladsl.Behaviors._
import akka.actor.typed.{ActorRef, ActorSystem, Behavior, DispatcherSelector}
import com.vrann.cholesky.CholeskyBlockMatrixType.L11

trait Message

case object TestMessage extends Message

case class SubscribeTestProbe(ref: ActorRef[FileTransferMessage]) extends Message

object RootBehavior {

  class FileLocatorTmp extends FileLocator {
    def apply(fileName: String): Path = {
      val pathBuilder = (new StringBuilder)
        .append("/tmp/")
        .append(fileName)
      Paths.get(pathBuilder.toString)
    }
  }

  def behavior(section: Section): Behavior[Message] = setup[Message] {
    //val topicsRegistry = topicsRegistry
    context: ActorContext[Message] =>
//      val topics = FileTransferTopicHolder(context)
//    val fileTransferActor: ActorRef[FileTransferMessage] =
//      context.spawn(FileTransfer(new FileLocatorTmp())(), "fileTransfer")
//    val fileTransferActor2: ActorRef[FileTransferMessage] =
//      context.spawn(FileTransfer(new FileLocatorDefault())(), "fileTransfer2")
//    topics.transferReadyTopic ! Subscribe(fileTransferActor2)

      val sectionActor = context.spawn(section.behavior, "Section")

      receiveMessage[Message] {
//        case SubscribeTestProbe(ref) =>
//          topics.transferReadyTopic ! Subscribe(ref)
//          same
        case message @ FileTransferRequestMessage(position, matrixType, sectionId, fileName, ref) =>
          sectionActor ! FileTransferRequestMessage(position, matrixType, sectionId, fileName, ref)
          same
//        case message @ FileTransferReadyMessage(position, matrixType, sectionId, fileName, _) =>
//          topics.transferReadyTopic ! Publish(
//            FileTransferReadyMessage(position, matrixType, sectionId, fileName, sectionActor))
//          same
//        case message @ FileTransferReadyMessage(position, matrixType, sectionId, fileName, _) =>
//          topics.transferReadyTopic ! Publish(
//            FileTransferReadyMessage(position, matrixType, sectionId, fileName, sectionActor))
//          same
//      case TestMessage =>
//        context.log.info("Test Message received")
//        topics.transferReadyTopic ! Publish(
//          FileTransferReadyMessage(Position(0, 0), L11, 1, "l11.mtrx", fileTransferActor2))
//        same
      }
  }
}

object App {
  def main(args: Array[String]): Unit = {
    val system: ActorRef[Message] =
      ActorSystem(RootBehavior.behavior(new Section(List(Position(0, 0)), new TopicsRegistry[Message])), "example")
    Thread.sleep(500)
    system ! TestMessage
  }
}
