//package com.vrann.cholesky
//
//import akka.actor.typed.Behavior
//import akka.actor.typed.scaladsl.Behaviors.{receive, unhandled}
//import com.vrann.BlockMessage.MatrixDataAvailable
//import com.vrann._
//
//class A22(roleBehavior: RoleBehavior) extends BlockBehavior {
////  def apply(state: State, blockMessage: BlockMessage): Behavior[BlockMessage] =
//////    Behaviors.receiveMessage[BlockMessage] { message =>
////    (blockMessage, state) match {
////      case (MatrixDataAvailable(_, _, _), Uninitialized) =>
////        Block.init(A11Processed)
//////        state(A11Processed)
//////        case _ => same
////    }
//////    }
//
//  def apply: Behavior[BlockMessage] = state(Initialized)
//
//  private def state(state: State): Behavior[BlockMessage] =
//    receive[BlockMessage] { (context, message) =>
//      (message, state) match {
//        case (MatrixDataAvailable(_, _, _), Uninitialized) =>
//          println("A11 handled")
//          Block.init(Initialized, roleBehavior)
//        case (_, _) =>
//          println("A11 unhandled")
//          unhandled
//      }
//    }
//}
