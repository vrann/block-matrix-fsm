package com.vrann

import akka.actor.typed.scaladsl.ActorContext
import akka.actor.typed.scaladsl.Behaviors.{receiveMessage, same, setup}
import akka.actor.typed.{ActorRef, Behavior}
import com.vrann.cholesky.CholeskyRoleBehavior

class Section(val positions: List[Position]) {

  val behavior: Behavior[Message] = setup[Message] {
    context: ActorContext[Message] =>
      if (positions.isEmpty)
        same
      else {
        val fileTransferActor: ActorRef[FileTransferMessage] =
          context.spawn(
            FileTransfer(new FileLocatorDefault())(),
            "fileTransfer"
          )
        val positionedActors =
          positions.foldLeft(Map.empty[Position, ActorRef[BlockMessage]])(
            (map, position) =>
              map + (position -> context
                .spawn(
                  Block(position, CholeskyRoleBehavior(position)),
                  "position-" + position
                ))
          )
        receiveMessage {
          case message @ FileTransferMessage => {
            fileTransferActor ! message.asInstanceOf[FileTransferMessage]
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
        }
      }
  }

}
