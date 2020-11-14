package com.vrann.cholesky

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors.same
import com.vrann.{BlockBehavior, BlockMessage, State}

object L11 extends BlockBehavior {
  def apply(state: State,
            blockMessage: BlockMessage): Behavior[BlockMessage] = {
    same
  }
}
