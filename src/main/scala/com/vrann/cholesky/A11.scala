package com.vrann.cholesky

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.scaladsl.Behaviors.{same, unhandled}
import com.vrann.BlockMessage.MatrixDataAvailable
import com.vrann._

object A11 extends BlockBehavior {
//  def apply(state: State, blockMessage: BlockMessage): Behavior[BlockMessage] =
////    Behaviors.receiveMessage[BlockMessage] { message =>
//    (blockMessage, state) match {
//      case (MatrixDataAvailable(_, _, _), Uninitialized) =>
//        Block.init(A11Processed)
////        state(A11Processed)
////        case _ => same
//    }
////    }

  def apply: Behavior[BlockMessage] = state(Initialized)

  private def state(state: State): Behavior[BlockMessage] =
    Behaviors.receiveMessage { message =>
      (message, state) match {
        case (MatrixDataAvailable(_, _, _), Uninitialized) =>
          same
        case _ => unhandled
      }
    }
}
