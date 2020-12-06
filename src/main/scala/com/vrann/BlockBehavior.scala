package com.vrann

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.pubsub.Topic.Command

object BlockTopicRegistry extends TopicsRegistry[BlockMessage]

abstract class BlockBehavior {

  var topics: Map[String, Behavior[Command[Message]]]

//  def ::(that: BlockBehavior): BlockBehavior = {
//    new composition(Vector(this, that))
//  }

//  def init(function: Function[State, PartialFunction[BlockMessage, Behavior[BlockMessage]]]): BlockBehavior = {
//    stateFunction = function
//    this
//  }

  def apply: Behavior[Message]
}
