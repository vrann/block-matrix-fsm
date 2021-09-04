package com.vrann.cholesky

import akka.actor.typed.pubsub.Topic.Command
import akka.actor.typed.{ActorRef, Behavior}
import com.vrann.{BlockBehavior, Message, Position}

case class CholeskyRoleBehavior(position: Position,
                                //topicsRegistry: TopicsRegistry[Message],
                                sectionId: Int,
                                fileTransferActor: ActorRef[Message],
                                section: ActorRef[Message])
    extends BlockBehavior {

  val roleBehavior: BlockBehavior = Roles(position) match {
    case _ if (position.x == position.y) =>
      new BlockDiagonal(position, sectionId, fileTransferActor, section)
    //new A11() :: new L21()
//    case Diagonal =>
//      new A11( this) :: new L21( this)
//    case Subdiagonal =>
//      new L11( this) :: new L21( this)
    case _ => new BlockSubdiagonal(position, sectionId, fileTransferActor, section) //throw new Exception
  }

  val topics: Map[String, Behavior[Command[Message]]] = roleBehavior.topics
  val topicsPublishTo: Map[String, Behavior[Command[Message]]] = roleBehavior.topicsPublishTo

//  Map[String, Behavior[Command[Message]]] =
//    roleBehavior.topics.foldLeft(Map.empty[String, Behavior[Command[Message]]])({
//      case (map, (topicName, topicBehavior)) => map + (topicName -> topicBehavior)
//    })

  override def apply: Behavior[Message] =
    roleBehavior.apply

}
