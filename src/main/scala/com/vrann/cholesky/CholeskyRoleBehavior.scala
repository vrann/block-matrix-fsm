package com.vrann.cholesky

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.pubsub.Topic.Command
import akka.actor.typed.scaladsl.ActorContext
import akka.actor.typed.scaladsl.Behaviors.setup
import com.vrann.{BlockBehavior, BlockMessage, Message, Position, RoleBehavior, State, TopicsRegistry}

case class CholeskyRoleBehavior(position: Position, topicsRegistry: TopicsRegistry[Message]) extends BlockBehavior {

  private val roleBehavior: BlockBehavior = Roles(position) match {
    case Test => new DiagonalBlock(Position(0, 0), topicsRegistry)
    //new A11() :: new L21()
//    case Diagonal =>
//      new A11( this) :: new L21( this)
//    case Subdiagonal =>
//      new L11( this) :: new L21( this)
    case _ => new DiagonalBlock(position, topicsRegistry) //throw new Exception
  }

  var topics = roleBehavior.topics.foldLeft(Map.empty[String, Behavior[Command[Message]]])({
    case (map, (topicName, topicBehavior)) => map + (s"$topicName-$position" -> topicBehavior)
  })

  override def apply: Behavior[Message] =
    roleBehavior.apply
}
