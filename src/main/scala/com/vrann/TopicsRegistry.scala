package com.vrann

import akka.actor.typed.ActorRef
import akka.actor.typed.pubsub.Topic.Command

import scala.collection.JavaConverters._

class TopicsRegistry[T] {
  private val topics = new java.util.concurrent.ConcurrentHashMap[String, ActorRef[Command[T]]]()

  def +(topicName: String, topicActor: ActorRef[Command[T]]): ActorRef[Command[T]] =
    topics.put(topicName, topicActor)

  def apply(topicName: String): ActorRef[Command[T]] = topics.get(topicName)

  def apply(topicName: String, position: Position): ActorRef[Command[T]] =
    topics.get(s"$topicName-$position")

  def registered: Map[String, ActorRef[Command[T]]] = topics.asScala.toMap
}
