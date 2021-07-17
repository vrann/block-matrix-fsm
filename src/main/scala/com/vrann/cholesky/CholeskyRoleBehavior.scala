package com.vrann.cholesky

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.pubsub.Topic.Command
import com.vrann.{BlockBehavior, Message, Position, TopicsRegistry}

case class CholeskyRoleBehavior(position: Position,
                                topicsRegistry: TopicsRegistry[Message],
                                sectionId: Int,
                                fileTransferActor: ActorRef[Message])
    extends BlockBehavior {

  val roleBehavior: BlockBehavior = Roles(position) match {
    case _ if (position.x == position.y) =>
      new BlockDiagonal(position, topicsRegistry, sectionId, fileTransferActor: ActorRef[Message])
    //new A11() :: new L21()
//    case Diagonal =>
//      new A11( this) :: new L21( this)
//    case Subdiagonal =>
//      new L11( this) :: new L21( this)
    case _ => new BlockSubdiagonal(position, topicsRegistry, sectionId, fileTransferActor) //throw new Exception
  }

  val topics: Map[String, Behavior[Command[Message]]] = roleBehavior.topics
//  Map[String, Behavior[Command[Message]]] =
//    roleBehavior.topics.foldLeft(Map.empty[String, Behavior[Command[Message]]])({
//      case (map, (topicName, topicBehavior)) => map + (topicName -> topicBehavior)
//    })

  override def apply: Behavior[Message] =
    roleBehavior.apply

  val topicsPublishTo = roleBehavior.topicsPublishTo
}
