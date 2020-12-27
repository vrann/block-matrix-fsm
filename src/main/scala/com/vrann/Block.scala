package com.vrann

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.scaladsl.Behaviors._
import com.vrann.BlockMessage.{GetState, MatrixDataAvailable, StateMessage}

object BlockMatrixEvents {
  sealed trait BlockEvent
  final case class aMNReceived() extends BlockEvent
}

sealed trait State
case object Uninitialized extends State
case object Initialized extends State
case object L11Received extends State
case object A11Processed extends State
case object L21Processed extends State
case object SomeL21Applied extends State
case object L21Applied extends State
case object AllL21Applied extends State
case object Done extends State

object Block {

  def apply(position: Position, roleBehavior: RoleBehavior): Behavior[BlockMessage] = {
    init(Uninitialized, roleBehavior)
  }

  def init(state: State, roleBehavior: RoleBehavior): Behavior[BlockMessage] =
    setup(context => {

      receiveMessage[BlockMessage] { message =>
        (message, state) match {
          case (MatrixDataAvailable(_, _, _), Uninitialized) =>
            roleBehavior.apply
          //          init(Initialized, roleBehavior)
          case (GetState(Position(0, 0), replyTo), _) =>
            replyTo ! StateMessage(Position(0, 0), state)
            same
          case _ =>
            roleBehavior.apply
        }
      }
    })

}
