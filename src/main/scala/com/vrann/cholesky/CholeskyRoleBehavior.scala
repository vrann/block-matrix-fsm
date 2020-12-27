package com.vrann.cholesky

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.pubsub.Topic.Command
import akka.actor.typed.scaladsl.ActorContext
import akka.actor.typed.scaladsl.Behaviors.setup
import com.vrann.{BlockBehavior, BlockMessage, Message, Position, RoleBehavior, State, TopicsRegistry}

case class CholeskyRoleBehavior(position: Position, topicsRegistry: TopicsRegistry[Message]) extends BlockBehavior {

  val roleBehavior: BlockBehavior = Roles(position) match {
    case _ if (position.x == position.y) => new BlockDiagonal(position, topicsRegistry)
    //new A11() :: new L21()
//    case Diagonal =>
//      new A11( this) :: new L21( this)
//    case Subdiagonal =>
//      new L11( this) :: new L21( this)
    case _ => new BlockSubdiagonal(position, topicsRegistry) //throw new Exception
  }

  val topics: Map[String, Behavior[Command[Message]]] = roleBehavior.topics
//  Map[String, Behavior[Command[Message]]] =
//    roleBehavior.topics.foldLeft(Map.empty[String, Behavior[Command[Message]]])({
//      case (map, (topicName, topicBehavior)) => map + (topicName -> topicBehavior)
//    })

  override def apply: Behavior[Message] =
    roleBehavior.apply
}
