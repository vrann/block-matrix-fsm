package com.vrann

import akka.actor.typed.{Behavior, BehaviorInterceptor, TypedActorContext}
import akka.actor.typed.scaladsl.Behaviors

import scala.annotation.tailrec

class ComposedBehavior(behaviors: Vector[BlockBehavior]) extends BlockBehavior {

  private val behaviorsVector: Vector[BlockBehavior] = behaviors

  override def ::(that: BlockBehavior): BlockBehavior = {
    new ComposedBehavior(behaviorsVector :+ that)
  }

//  override def apply(state: State,
//                     blockMessage: BlockMessage): Behavior[BlockMessage] = {}

  // this could be provided as a general purpose utility
//  def handle(state: State,
//             message: BlockMessage,
//             behaviorsList: Vector[BlockBehavior]): Behavior[BlockMessage] = {
//    PartialFunction
//    behaviorsList match {
//      case Nil          => Behaviors.unhandled
//      case head :: tail => head.applyOrElse(state, message, handle(_, tail))
//    }
//  }

  override def aroundReceive(
    ctx: TypedActorContext[BlockMessage],
    msg: BlockMessage,
    target: BehaviorInterceptor.ReceiveTarget[BlockMessage]
  ): Behavior[BlockMessage] = {

    @tailrec def handle(i: Int): Behavior[BlockMessage] = {
      if (i == behaviorsVector.size)
        target(ctx, msg)
      else {
        val next =
          Behavior.interpretMessage(behaviorsVector(i).apply, ctx, msg)
        if (Behavior.isUnhandled(next))
          handle(i + 1)
        else if (!Behavior.isAlive(next))
          next
        else {
          behaviorsVector = behaviorsVector.updated(
            i,
            Behavior.canonicalize(next, behaviorsVector(i).apply, ctx)
          )
          Behaviors.same
        }
      }
    }

    handle(0)
  }

//  Behaviors.receiveMessage(command => handle(command, pingHandlers))

}
