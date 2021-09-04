package com.vrann

import akka.actor.typed.scaladsl.ActorContext
import akka.actor.typed.scaladsl.Behaviors.{receiveMessage, same, setup}
import akka.actor.typed.{ActorRef, Behavior}
import com.vrann.BlockMessage.{DataReady, Delegate, GetState, RemoteDelegate}
import com.vrann.cholesky.CholeskyBlockMatrixType.{aMN, L11, L21}
import com.vrann.cholesky.{CholeskyRoleBehavior, FileLocator}
import com.vrann.positioned.{PositionCommand, SectionJoined}

import java.io.File
case class InitData(position: Position, blockMatrixType: BlockMatrixType, sectionId: Int, filePath: File)
    extends Message
case class UpdatePositions(sections: Map[String, ActorRef[Message]], positions: Map[String, List[Position]])
    extends Message
case object Init extends Message
case object InitAll extends Message
case object Start extends Message

object Section {

  def apply(sectionId: Int,
            positionAssignment: ActorRef[PositionCommand]
            //positions: List[Position] = List.empty[Position],
            //topicsRegistry: TopicsRegistry[Message] = new TopicsRegistry[Message],
            //positionAssignmentTopic: ActorRef[Topic.Command[PositionCommand]]
  ): Behavior[Message] = {
    setup[Message] { context: ActorContext[Message] =>
      positionAssignment ! SectionJoined(sectionId, context.self)
      val fileTransfer: FileTransfer = FileTransfer(context.self, sectionId)

      val fileTransferActor: ActorRef[Message] =
        context.spawn(fileTransfer.apply, "fileTransfer")
      context.log.info("publishing section join ")
      //positionAssignmentTopic ! Publish(SectionJoined(sectionId))

      createBehaviorForPositions(
        sectionId,
//        List(),
//        List(),
        Map.empty,
        Map.empty,
        Map.empty,
        Map.empty,
        Map.empty,
        positionAssignment,
        fileTransferActor /*topicsRegistry, fileTransfer, positionAssignmentTopic*/ )
    }
  }

  def createBehaviorForPositions(sectionId: Int,
                                 //positions: List[Position] = List.empty[Position],
                                 //removedPositions: List[Position] = List.empty[Position],
                                 sections: Map[String, ActorRef[Message]],
                                 sectionsLookup: Map[Position, String],
                                 blocksSubscribed: Map[(Position, BlockMatrixType), List[Position]],
                                 sectionsAssignments: Map[String, List[Position]] = Map.empty,
                                 positionedActors: Map[Position, ActorRef[Message]] = Map.empty,
                                 //removedBlockTopics: List[String],
                                 positionAssignment: ActorRef[PositionCommand],
                                 fileTransferActor: ActorRef[Message]
                                 //topicsRegistry: TopicsRegistry[Message] = new TopicsRegistry[Message],
                                 //fileTransfer: FileTransfer,
                                 //positionAssignmentTopic: ActorRef[Topic.Command[PositionCommand]]
  ): Behavior[Message] = {

    setup[Message] { context: ActorContext[Message] =>
      //var blockTopics = Map.empty[String, Behavior[Command[Message]]]
      //val fileTransferTopics: Map[String, Behavior[Command[Message]]] = fileTransfer.topics

//
//      fileTransferTopics.foreach({
//        case (topicName, topicBehavior) => {
//          topicsRegistry + (topicName, context.spawn(topicBehavior, topicName))
//        }
//      })
//
//      fileTransfer.topics.foreach({
//        case (topicName, _) => {
//          context.log.debug(s"Subscribe $topicName => $fileTransferActor")
//          topicsRegistry(topicName) ! Subscribe(fileTransferActor)
//        }
//      })
//
//      removedBlockTopics.foreach({
//        case (topicName) => {
//          if (!topicsRegistry.hasTopic(topicName)) {
//            context.stop(topicsRegistry(topicName))
//            topicsRegistry - topicName
//          }
//        }
//      })
//
//      blockTopics.foreach({
//        case (topicName, topicBehavior) => {
//          if (!topicsRegistry.hasTopic(topicName)) {
//            topicsRegistry + (topicName, context.spawn(topicBehavior, topicName))
//          }
//        }
//      })

      receiveMessage {
        case UpdatePositions(sections, updatedPositions) =>
          var currentSectionPositions = List.empty[Position]
          for ((k, v) <- updatedPositions) {
            if (k.equals(sectionId.toString)) {
              currentSectionPositions = v //updatedPositions(sectionId)
            }
          }
          context.log.info(s"$currentSectionPositions")
          //question: how would removal of the topic from the topic registry change the remote actors subscribed to
          // the topic. How would it change if we shut down the actor for the topic

          //add the new positions
          //remove missing positions
          //keep same positions

          val positions: List[Position] = positionedActors.keys.toList

          val toRemove: List[Position] = positions.diff(currentSectionPositions)
          var newPositionedActors = positionedActors
          for (removed <- toRemove) {
            context.log.info(s"Stopping actor for position $removed in section $sectionId")
            context.stop(positionedActors(removed))
            newPositionedActors = newPositionedActors - removed
          }
          //fileTransfer = fileTransfer -- toRemove

          val addpositions: List[Position] = newPositionedActors.keys.toList
          val toAdd: List[Position] = currentSectionPositions.diff(addpositions)
          val choleskyRoleBehaviors: Map[Position, BlockBehavior] =
            toAdd.foldLeft(Map.empty[Position, BlockBehavior])((map, position) => {
              val cholesky = CholeskyRoleBehavior(position, sectionId, fileTransferActor, context.self).roleBehavior
              //println(s"$position => ${cholesky.topics}")
              //blockTopics ++= cholesky.topics
              //blockTopics ++= cholesky.topicsPublishTo
              map + (position -> cholesky)
            })
          newPositionedActors = choleskyRoleBehaviors.foldLeft(newPositionedActors)({
            case (map, (position, behavior)) => {
//              context.log.info(s"Starting actor for position $position in section $sectionId")
              val positionActor = context
                .spawn(behavior.apply, "position-" + position)
              //            behavior.topics.foreach({
              //              case (topicName, _) => {
              //                context.log.info(s"Subscribe $topicName => $position actor")
              //                topicsRegistry(topicName) ! Subscribe(positionActor)
              //              }
              //            })
              map + (position -> positionActor)
            }
          })

          //fileTransfer = fileTransfer ++ toAdd

//          var newBlockTopics = blockTopics
//          val choleskyRoleBehaviors: Map[Position, BlockBehavior] =
//            positions.foldLeft(Map.empty[Position, BlockBehavior])((map, position) => {
//              val cholesky = CholeskyRoleBehavior(position, topicsRegistry, sectionId, fileTransferActor).roleBehavior
//              //println(s"$position => ${cholesky.topics}")
//              newBlockTopics ++= cholesky.topics
//              map + (position -> cholesky)
//            })

          context.log.info(s"Number of position actors in section $sectionId is ${newPositionedActors.keys.size}")

          var reverseLookup: Map[Position, String] = Map.empty
          for ((secId, poss) <- updatedPositions) {
            for (p <- poss) {
              reverseLookup = reverseLookup + (p -> secId)
            }
          }

          var newBlocksSubscribed: Map[(Position, BlockMatrixType), List[Position]] = Map.empty
          for ((_, poss) <- updatedPositions) {
            for (p <- poss) {

              var matrixInterested: Map[BlockMatrixType, List[Position]] = Map.empty
              if (p.x == p.y) {
                matrixInterested = Map(aMN -> List(p), L21 -> (0 until p.x).foldLeft(List.empty[Position]) {
                  (list, x) =>
                    list :+ Position(x, p.y)
                })
              } else {
                matrixInterested = Map(aMN -> List(p), L21 -> (0 until p.x).foldLeft(List.empty[Position]) {
                  (list, x) =>
                    list :+ Position(x, p.y)
                }, L11 -> List(Position(p.x, p.x)))
              }

              for ((matrixType, positions) <- matrixInterested) {
                for (positionInterested <- positions) {
                  val updatedPos = newBlocksSubscribed get (positionInterested, matrixType) match {
                    case None    => List(p)
                    case Some(a) => if (!a.contains(p)) a :+ p else a
                  }
                  newBlocksSubscribed += ((positionInterested, matrixType) -> updatedPos)
                }
              }
              //reverseLookup = reverseLookup + ((p, aMN) -> List(secId))
            }
          }

          createBehaviorForPositions(
            sectionId,
            sections,
            reverseLookup,
            newBlocksSubscribed,
            //toAdd,
            //toRemove,
            updatedPositions,
            newPositionedActors,
            positionAssignment,
            fileTransferActor
            //topicsRegistry,
            //fileTransfer,
            //positionAssignmentTopic
          )

        case InitAll =>
          for ((_, sec) <- sections) {
            sec ! Init
          }
          same

        case Init =>
          for ((pos, posActor) <- positionedActors) {
            if (!pos.equals(Position(0, 0))) {
              posActor ! DataReady(
                pos,
                aMN,
                FileLocator.getFileLocator(pos, aMN, sectionId),
                sectionId,
                fileTransferActor)
            }
          }
          same

//          val messages = positionedActors.foldLeft(List.empty[DataReady])((list, pos) => {
//            list :+ DataReady(pos, aMN, FileLocator.getFileLocator(pos, aMN, sectionId), sectionId, fileTransferActor)
//          })

//          positions.foreach(pos => {
//            val topicName = topicsRegistry.getTopicName("matrix-aMN-ready", pos)
//            if (!topicsRegistry.hasTopic("matrix-aMN-ready", pos)) {
//              topicsRegistry + (topicName, context.spawn(Topic[Message](topicName), topicName))
//            }
//          })
//
//          messages.foreach(message => {
//            if (!message.position.equals(Position(0, 0))) {
//              context.log.info(s"Publishing init message $message")
//              topicsRegistry("matrix-aMN-ready", message.position) ! Publish(message)
//            }
//          })

          same

        case Start =>
          val pos = Position(0, 0)
          val message =
            DataReady(pos, aMN, FileLocator.getFileLocator(pos, aMN, sectionId), sectionId, fileTransferActor)

          sections(sectionsLookup(pos)) ! message
          context.log.info(s"Start ${System.currentTimeMillis()}")
          same
//        case _ @InitData(position, blockMatrixType, sectionId, filePath) =>
//          if (positions.contains(position)) {
//            context.log.debug(s"Publishing DataReady to matrix-$blockMatrixType-ready-$position")
//            topicsRegistry(s"matrix-$blockMatrixType-ready-$position") ! Publish(
//              DataReady(position, blockMatrixType, filePath, sectionId, fileTransferActor))
//          } else {
//            context.log.debug(s"Publishing FileTransferReadyMessage to data-ready-$position")
//            topicsRegistry(s"data-ready-$position") ! Publish(
//              FileTransferReadyMessage(position, blockMatrixType, sectionId, filePath.toString, context.self))
//          }
//          same
//        case message @ FileTransferRequestMessage(_, _, _, _, _) => {
//          fileTransferActor ! message.asInstanceOf[FileTransferRequestMessage]
//          same
//        }
        case message @ DataReady(_, _, _, _, _) => {
          println(s"Handled ${message}")
//          val positionedMessage: BlockMessage =
//            message.asInstanceOf[BlockMessage]
          if (!positionedActors.contains(message.position)) {
            throw new Exception
          }
          positionedActors(message.position) ! message
          same
        }
        case Delegate(position, matrixType, message) => {
          var targetSections = List.empty[Int]
          for (pos <- blocksSubscribed(position, matrixType)) {
            val blockSection = sectionsLookup(pos)
            if (blockSection.equals(sectionId.toString)) {
              positionedActors(pos) ! message
            } else {
              if (!targetSections.contains(blockSection)) {
                targetSections = targetSections :+ blockSection.toInt
              }
            }
          }
          for (sec <- targetSections) {
            context.log.info(s"Remote Delegate from $sectionId to $sec ($position, $matrixType)")
            sections(sec.toString) ! RemoteDelegate(position, matrixType, message)
          }
          same
        }
        case RemoteDelegate(position, matrixType, message) => {
          for (pos <- blocksSubscribed(position, matrixType)) {
            val blockSection = sectionsLookup(pos)
            context.log.info(s"Remote Delegate applying locally at $sectionId ($position, $matrixType)")
            if (blockSection.equals(sectionId.toString)) {
              positionedActors(pos) ! message
            } else {
              //context.log.info("Ignored")
            }
          }
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
