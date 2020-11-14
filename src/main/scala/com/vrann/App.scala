package com.vrann

import java.nio.file.{Path, Paths}

import akka.actor.typed.pubsub.Topic.{Publish, Subscribe}
import akka.actor.typed.scaladsl.ActorContext
import akka.actor.typed.scaladsl.Behaviors._
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import com.vrann.FileTransferMessage.FileTransferReadyMessage
import com.vrann.cholesky.CholeskyBlockMatrixType.L11

trait Message

case object TestMessage extends Message

case class SubscribeTestProbe(ref: ActorRef[FileTransferMessage])
    extends Message

object RootBehavior {

  class FileLocatorTmp extends FileLocator {
    def apply(fileName: String): Path = {
      val pathBuilder = (new StringBuilder)
        .append("/tmp/")
        .append(fileName)
      Paths.get(pathBuilder.toString)
    }
  }

  val behavior: Behavior[Message] = setup[Message] {
    context: ActorContext[Message] =>
      val topics = FileTransferTopicHolder(context)
      val fileTransferActor: ActorRef[FileTransferMessage] =
        context.spawn(FileTransfer(new FileLocatorTmp())(), "fileTransfer")
      val fileTransferActor2: ActorRef[FileTransferMessage] =
        context.spawn(FileTransfer(new FileLocatorDefault())(), "fileTransfer2")
      topics.transferReadyTopic ! Subscribe(fileTransferActor)
      receiveMessage {
        case SubscribeTestProbe(ref) =>
          topics.transferReadyTopic ! Subscribe(ref)
          same
        case TestMessage =>
          context.log.info("Test Message received")
          topics.transferReadyTopic ! Publish(
            FileTransferReadyMessage(
              Position(0, 0),
              L11,
              1,
              "l11.mtrx",
              fileTransferActor2
            )
          )
          same
      }
  }
}

object App {
  def main(args: Array[String]): Unit = {
    val system: ActorRef[Message] =
      ActorSystem(RootBehavior.behavior, "example")
    Thread.sleep(500)
    system ! TestMessage
  }
}
