package com.vrann.cholesky

import akka.actor.typed.Behavior
import com.vrann.{BlockBehavior, BlockMessage, Position, RoleBehavior, State}

case class CholeskyRoleBehavior(position: Position) extends RoleBehavior {

  def apply(state: State, blockMessage: BlockMessage): Behavior[BlockMessage] =
    Roles(position) match {
      case Diagonal =>
        (A11 :: L11)(state, message)
      case _ => throw new Exception
    }

}
