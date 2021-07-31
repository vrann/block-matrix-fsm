package com.vrann

import akka.actor.typed.pubsub.Topic
import akka.actor.typed.pubsub.Topic.{Command, Publish, Subscribe}
import akka.actor.typed.scaladsl.ActorContext
import akka.actor.typed.scaladsl.Behaviors.{receiveMessage, same, setup}
import akka.actor.typed.{ActorRef, Behavior}
import com.typesafe.config.ConfigFactory
import com.vrann.BlockMessage.{DataReady, GetState}
import com.vrann.cholesky.CholeskyBlockMatrixType.aMN
import com.vrann.cholesky.{CholeskyRoleBehavior, FileLocator}

import java.io.File
case class InitData(position: Position, blockMatrixType: BlockMatrixType, sectionId: Int, filePath: File)
    extends Message
case class UpdatePositions(positions: List[Position]) extends Message
case class Init(positions: List[Position]) extends Message
case object Start extends Message

object Section {

  val config = ConfigFactory.load()
  val sectionId = config.getInt("section")

  def apply(nodeName: String,
            positions: List[Position] = List.empty[Position],
            topicsRegistry: TopicsRegistry[Message] = new TopicsRegistry[Message],
  ): Behavior[Message] = {

    val fileTransfer: FileTransfer = FileTransfer(positions, List(), topicsRegistry, sectionId)

    createBehaviorForPositions(nodeName, positions, List(), topicsRegistry, fileTransfer)
  }

  def createBehaviorForPositions(sectionName: String,
                                 positions: List[Position] = List.empty[Position],
                                 removedBlockTopics: List[String],
                                 topicsRegistry: TopicsRegistry[Message] = new TopicsRegistry[Message],
                                 fileTransfer: FileTransfer): Behavior[Message] = {

    setup[Message] { context: ActorContext[Message] =>
      val fileTransferActor: ActorRef[Message] =
        context.spawn(fileTransfer.apply, "fileTransfer")

      var blockTopics = Map.empty[String, Behavior[Command[Message]]]
      val fileTransferTopics: Map[String, Behavior[Command[Message]]] = fileTransfer.topics

      val choleskyRoleBehaviors: Map[Position, BlockBehavior] =
        positions.foldLeft(Map.empty[Position, BlockBehavior])((map, position) => {
          val cholesky = CholeskyRoleBehavior(position, topicsRegistry, sectionId, fileTransferActor).roleBehavior
          //println(s"$position => ${cholesky.topics}")
          blockTopics ++= cholesky.topics
          blockTopics ++= cholesky.topicsPublishTo
          map + (position -> cholesky)
        })

      fileTransferTopics.foreach({
        case (topicName, topicBehavior) => {
          topicsRegistry + (topicName, context.spawn(topicBehavior, topicName))
        }
      })

      fileTransfer.topics.foreach({
        case (topicName, _) => {
          context.log.debug(s"Subscribe $topicName => $fileTransferActor")
          topicsRegistry(topicName) ! Subscribe(fileTransferActor)
        }
      })

      removedBlockTopics.foreach({
        case (topicName) => {
          if (!topicsRegistry.hasTopic(topicName)) {
            context.stop(topicsRegistry(topicName))
            topicsRegistry - topicName
          }
        }
      })

      blockTopics.foreach({
        case (topicName, topicBehavior) => {
          if (!topicsRegistry.hasTopic(topicName)) {
            topicsRegistry + (topicName, context.spawn(topicBehavior, topicName))
          }
        }
      })

      val positionedActors =
        choleskyRoleBehaviors.foldLeft(Map.empty[Position, ActorRef[Message]])({
          case (map, (position, behavior)) => {
            val positionActor = context
              .spawn(behavior.apply, "position-" + position)
            behavior.topics.foreach({
              case (topicName, _) => {
                context.log.info(s"Subscribe $topicName => $position actor")
                topicsRegistry(topicName) ! Subscribe(positionActor)
              }
            })
            map + (position -> positionActor)
          }
        })

      receiveMessage {
        case _ @UpdatePositions(updatedPositions) =>
          //question: how would removal of the topic from the topic registry change the remote actors subscribed to
          // the topic. How would it change if we shut down the actor for the topic

          //add the new positions
          //remove missing positions
          //keep same positions

          val toRemove: List[Position] = positions.diff(updatedPositions)
          //fileTransfer = fileTransfer -- toRemove

          val toAdd: List[Position] = updatedPositions.diff(positions)
          //fileTransfer = fileTransfer ++ toAdd

          var newBlockTopics = blockTopics
          val choleskyRoleBehaviors: Map[Position, BlockBehavior] =
            positions.foldLeft(Map.empty[Position, BlockBehavior])((map, position) => {
              val cholesky = CholeskyRoleBehavior(position, topicsRegistry, sectionId, fileTransferActor).roleBehavior
              //println(s"$position => ${cholesky.topics}")
              newBlockTopics ++= cholesky.topics
              map + (position -> cholesky)
            })

          createBehaviorForPositions(sectionName, positions, List(), topicsRegistry, fileTransfer)
        case Init(positions) =>
          val messages = positions.foldLeft(List.empty[DataReady])((list, pos) => {
            list :+ DataReady(pos, aMN, FileLocator.getFileLocator(pos, aMN, sectionId), sectionId, fileTransferActor)
          })

          positions.foreach(pos => {
            val topicName = topicsRegistry.getTopicName("matrix-aMN-ready", pos)
            if (!topicsRegistry.hasTopic("matrix-aMN-ready", pos)) {
              topicsRegistry + (topicName, context.spawn(Topic[Message](topicName), topicName))
            }
          })

          messages.foreach(message => {
            if (!message.position.equals(Position(0, 0))) {
              context.log.info(s"Publishing init message $message")
              topicsRegistry("matrix-aMN-ready", message.position) ! Publish(message)
            }
          })

          same

        case Start =>
          val pos = Position(0, 0)
          val message =
            DataReady(pos, aMN, FileLocator.getFileLocator(pos, aMN, sectionId), sectionId, fileTransferActor)
          topicsRegistry("matrix-aMN-ready", pos) ! Publish(message)
          context.log.info(s"Start ${System.currentTimeMillis()}")
          same
        case _ @InitData(position, blockMatrixType, sectionId, filePath) =>
          if (positions.contains(position)) {
            context.log.debug(s"Publishing DataReady to matrix-$blockMatrixType-ready-$position")
            topicsRegistry(s"matrix-$blockMatrixType-ready-$position") ! Publish(
              DataReady(position, blockMatrixType, filePath, sectionId, fileTransferActor))
          } else {
            context.log.debug(s"Publishing FileTransferReadyMessage to data-ready-$position")
            topicsRegistry(s"data-ready-$position") ! Publish(
              FileTransferReadyMessage(position, blockMatrixType, sectionId, filePath.toString, context.self))
          }
          same
//        case message @ FileTransferRequestMessage(_, _, _, _, _) => {
//          fileTransferActor ! message.asInstanceOf[FileTransferRequestMessage]
//          same
//        }
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
//    }
  }

}
