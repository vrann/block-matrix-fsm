/*
package com.vrann.cholesky

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors.{receive, unhandled}
import com.vrann.{Block, BlockBehavior, BlockMessage, Initialized, RoleBehavior, State, Uninitialized}

class A1j(roleBehavior: RoleBehavior) extends BlockBehavior {

  def apply: Behavior[BlockMessage] = state(Initialized)

  private def state(state: State): Behavior[BlockMessage] =
    receive[BlockMessage] { (context, message) =>
      (message, state) match {
        case (AiiData(data), Uninitialized) =>
          println("A11 handled")
          //process A11 and
          this !
            Block.init(Initialized, roleBehavior)
        case (_, _) =>
          println("A11 unhandled")
          unhandled
      }
    }
}

*/
