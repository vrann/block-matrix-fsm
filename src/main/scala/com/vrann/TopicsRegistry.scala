package com.vrann

import akka.actor.typed.ActorRef
import akka.actor.typed.pubsub.Topic.Command

import scala.collection.JavaConverters._

trait TopicMessage
case class TrackTopics(topicName: String) extends TopicMessage

class TopicsRegistry[T] {
  private val topics = new java.util.concurrent.ConcurrentHashMap[String, ActorRef[Command[T]]]()

  def +(topicName: String, topicActor: ActorRef[Command[T]]): TopicsRegistry[T] = {
    topics.put(topicName, topicActor)
    this
  }

  def -(topicName: String): TopicsRegistry[T] = {
    topics.remove(topicName)
    this
  }

  def apply(topicName: String): ActorRef[Command[T]] = {

    topics.get(topicName)
  }

  def apply(topicName: String, position: Position): ActorRef[Command[T]] =
    topics.get(s"$topicName-$position")

  def hasTopic(topicName: String): Boolean = topics.containsKey(topicName)
  def hasTopic(topicName: String, position: Position): Boolean = topics.containsKey(s"$topicName-$position")
  def getTopicName(topicName: String, position: Position): String = s"$topicName-$position"

  def registered: Map[String, ActorRef[Command[T]]] = topics.asScala.toMap
}
