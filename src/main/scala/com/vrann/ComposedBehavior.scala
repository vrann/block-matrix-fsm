package com.vrann

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{Behavior, BehaviorInterceptor, TypedActorContext}

import scala.annotation.tailrec

//https://github.com/akka/akka/blob/master/akka-actor-typed-tests/src/test/scala/akka/actor/typed/OrElseSpec.scala

class ComposedBehavior(behaviors: Vector[Behavior[BlockMessage]])
    extends BehaviorInterceptor[BlockMessage, BlockMessage] {

//  val composableDefaultCaseHandlers = receiveMessage[BlockMessage] {
//    case GetState(Position(0, 0), replyTo) =>
//      replyTo ! StateMessage(Position(0, 0), stateTransition)
//      same
//    case _ =>
//      println("Completely unhandled")
//      unhandled
//  }

  var composableBehaviors = behaviors //:: composableDefaultCaseHandlers

  override def aroundReceive(ctx: TypedActorContext[BlockMessage],
                             msg: BlockMessage,
                             target: BehaviorInterceptor.ReceiveTarget[BlockMessage]): Behavior[BlockMessage] = {

    @tailrec def handle(i: Int): Behavior[BlockMessage] = {
      if (i == composableBehaviors.size)
        target(ctx, msg)
      else {
        val next =
          Behavior.interpretMessage(composableBehaviors(i), ctx, msg)
        if (Behavior.isUnhandled(next))
          handle(i + 1)
        else if (!Behavior.isAlive(next))
          next
        else {
          composableBehaviors = composableBehaviors.updated(i, Behavior.canonicalize(next, composableBehaviors(i), ctx))
          Behaviors.same
        }
      }
    }

    handle(0)
  }
}

//class composition(blockBehaviors: Vector[BlockBehavior]) extends BlockBehavior {
//
//  var topics = blockBehaviors.foldLeft(Map.empty[String, Behavior[Command[Message]]])((map, blockBehavior) =>
//    map ++ blockBehavior.topics)
//
//  override def ::(that: BlockBehavior): composition = {
//    topics = topics ++ that.topics
//    new composition(blockBehaviors :+ that)
//  }
//
//  override def apply: Behavior[BlockMessage] = {
//    val behaviors = blockBehaviors.map(behavior => behavior.apply)
//    Behaviors.intercept[BlockMessage, BlockMessage](() => new ComposedBehavior(behaviors))(Behaviors.empty)
//  }
//}
