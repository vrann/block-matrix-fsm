package com.vrann

import java.io.File

import akka.actor.typed.pubsub.Topic
import akka.actor.typed.pubsub.Topic.{Command, Publish, Subscribe}
import akka.actor.typed.scaladsl.ActorContext
import akka.actor.typed.scaladsl.Behaviors.{receiveMessage, same, setup}
import akka.actor.typed.{ActorRef, Behavior, DispatcherSelector}
import com.vrann.BlockMessage.{AijData, DataReady, GetState}
import com.vrann.cholesky.CholeskyBlockMatrixType.L11
import com.vrann.cholesky.CholeskyRoleBehavior

class Section(val positions: List[Position],
              topicsRegistry: TopicsRegistry[Message] = new TopicsRegistry[Message],
              fileLocator: FileLocator = new FileLocatorDefault) {

  var blockTopics = Map.empty[String, Behavior[Command[Message]]]

  val choleskyRoleBehaviors: Map[Position, CholeskyRoleBehavior] =
    positions.foldLeft(Map.empty[Position, CholeskyRoleBehavior])((map, position) => {
      val cholesky = CholeskyRoleBehavior(position, topicsRegistry)
      blockTopics ++= cholesky.topics
      map + (position -> cholesky)
    })

  val fileTransfer: FileTransfer = FileTransfer(fileLocator, positions, topicsRegistry)
  val fileTransferTopics: Map[String, Behavior[Command[Message]]] = fileTransfer.topics

  val behavior: Behavior[Message] = setup[Message] { context: ActorContext[Message] =>
    if (positions.isEmpty)
      same
    else {

      //val dispatcherPath = "akka.actor.default-blocking-io-dispatcher"
      //val props = DispatcherSelector.fromConfig(dispatcherPath)
      blockTopics.foreach({
        case (topicName, topicBehavior) => {
          topicsRegistry + (topicName, context.spawn(topicBehavior, topicName))
        }
      })
      fileTransferTopics.foreach({
        case (topicName, topicBehavior) => {
          topicsRegistry + (topicName, context.spawn(topicBehavior, topicName))
        }
      })

      val positionedActors =
        choleskyRoleBehaviors.foldLeft(Map.empty[Position, ActorRef[Message]])({
          case (map, (position, behavior)) => {
            val positionActor = context
              .spawn(behavior.apply, "position-" + position)
            behavior.topics.foreach({
              case (topicName, _) => {
                topicsRegistry(topicName) ! Subscribe(positionActor)
              }
            })
            map + (position -> positionActor)
          }
        })

      val fileTransferActor: ActorRef[Message] =
        context.spawn(fileTransfer.apply, "fileTransfer")
      fileTransfer.topics.foreach({
        case (topicName, _) => {
          topicsRegistry(topicName) ! Subscribe(fileTransferActor)
        }
      })
      /*val positionedActors =
        positions.foldLeft(Map.empty[Position, ActorRef[BlockMessage]])((map, position) => {
          val choleskyPosition = CholeskyRoleBehavior(position)

          val topicsRegistry: Map[String, ActorRef[Command[BlockMessage]]] =
            choleskyPosition.topics.foldLeft(Map.empty[String, ActorRef[Command[BlockMessage]]])({
              case (topicsMap, (topicName, topicBehavior)) => {
                val topicActor = context.spawn(topicBehavior, s"$topicName-$position")
                topicsMap + (topicName -> topicActor)
              }
            })

          val positionActor = context
            .spawn(CholeskyRoleBehavior(position).apply(topicsRegistry), "position-" + position)
          topicsRegistry.foreach({ case (_, topicActor) => topicActor ! Topic.Subscribe(positionActor) })
          map + (position -> positionActor)
        })*/
      receiveMessage {
//        case message @ FileTransferReadyMessage(_, _, _, _, _) => {
//          fileTransferActor ! message.asInstanceOf[FileTransferMessage]
//          same
//        }
        case message @ DataReady(pos: Position, blockMatrixType: BlockMatrixType, filePath: File) => {
          if (!positionedActors.contains(pos)) {
            val topicName = s"data-ready-$pos"
            var topic = topicsRegistry(topicName)
            if (topicsRegistry(topicName) == null) {
              topic = context.spawn(Topic[Message](topicName), topicName)
              topicsRegistry + (topicName, topic)
            }
            topic ! Publish(FileTransferReadyMessage(pos, blockMatrixType, 1, filePath.toString, context.self))
          } else {
            positionedActors(pos) ! AijData(pos, filePath, sectionId = 1)
          }
          same
        }
        case message @ FileTransferRequestMessage(_, _, _, _, _) => {
          fileTransferActor ! message.asInstanceOf[FileTransferRequestMessage]
          same
        }
        case message @ BlockMessage => {
          val positionedMessage: BlockMessage =
            message.asInstanceOf[BlockMessage]
          if (!positionedActors.contains(positionedMessage.position)) {
            throw new Exception
          }
          positionedActors(positionedMessage.position) ! positionedMessage
          same
        }
        case GetState(position, replyTo) =>
          positionedActors(position) ! GetState(position, replyTo)
          same
        case message @ _ =>
          println(s"Unhandled ${message.getClass}")
          same

      }
    }
  }

}
