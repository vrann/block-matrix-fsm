//package com.vrann.cholesky
//
//import akka.actor.typed.Behavior
//import akka.actor.typed.scaladsl.ActorContext
//import akka.actor.typed.scaladsl.Behaviors.{receive, same, unhandled}
//import com.vrann.BlockMessage.MatrixDataAvailable
//import com.vrann.{Block, BlockBehavior, BlockMessage, Initialized, L11Received, RoleBehavior, State, Uninitialized}
//
//class L11(roleBehavior: RoleBehavior) extends BlockBehavior {
//  def apply: Behavior[BlockMessage] = {
//    init(Uninitialized)
//  }
//
//  def init(state: State): Behavior[BlockMessage] =
//    receive[BlockMessage] { (context, message) =>
//      (state, message) match {
//        case (_, MatrixDataAvailable(pos, file, sectionId)) => println("L11 handled")
//          Block.init(Initialized, roleBehavior)
//        case (_, _) =>
//          println("L11 unhandled")
//          unhandled
//      }
//    }
//}
