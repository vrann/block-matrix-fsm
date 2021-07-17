package com.vrann.positioned

import akka.actor.typed.scaladsl.Behaviors.{receiveMessage, same, setup, withTimers}
import akka.actor.typed.scaladsl.{ActorContext, Behaviors, TimerScheduler}
import akka.actor.typed.{ActorRef, Behavior}
import akka.cluster.ClusterEvent.{ClusterDomainEvent, MemberEvent, MemberLeft, MemberUp}
import akka.cluster.typed.{Cluster, Subscribe}
import com.vrann.Position

import scala.concurrent.duration._
import scala.language.postfixOps

case object VerifyMemberAssignment extends ClusterDomainEvent
case class GetCurrentAssignment(replyTo: ActorRef[Response]) extends ClusterDomainEvent
final case class Response(positionMatrix: Map[String, List[Position]])

object PositionAssignment {

  def applyToContext(M: Int,
                     N: Int,
                     nodes: List[String],
                     positionIds: List[Int],
                     positionMatrix: PositionMatrix,
                     context: ActorContext[ClusterDomainEvent]): Behaviors.Receive[ClusterDomainEvent] =
    receiveMessage {
      case MemberUp(member) =>
        context.log.debug("Member up {}", member.uniqueAddress)
        val newNodesList = nodes :+ member.uniqueAddress.address.toString
        val newPositionIds = positionIds :+ member.uniqueAddress.address.hashCode
        val newPositionMatrix = positionMatrix ++ List(member.uniqueAddress.address.hashCode)
        applyToContext(M, N, newNodesList, newPositionIds, newPositionMatrix, context)
      case MemberLeft(member) =>
        context.log.debug("Member left {}", member.uniqueAddress)
        val newNodesList = nodes.diff(List(member.uniqueAddress.address.toString))
        val newPositionIds = positionIds.diff(List(member.uniqueAddress.address.hashCode))
        val newPositionMatrix = positionMatrix -- List(member.uniqueAddress.address.hashCode)
        applyToContext(M, N, newNodesList, newPositionIds, newPositionMatrix, context)
      case VerifyMemberAssignment =>
        context.log.info("Member nodes {}", positionMatrix.sections)
        same
      case GetCurrentAssignment(replyTo) =>
        context.log.info("Assignment asked")
        replyTo ! Response(positionMatrix.sections)
        same
      case a @ _ => context.log.debug("Cluster member message {}", a); same
    }

  def apply(M: Int, N: Int): Behavior[ClusterDomainEvent] =
    apply(M, N, List.empty[String], List.empty[Int], new PositionMatrix(M, N, List.empty[Int]))

  def apply(M: Int,
            N: Int,
            nodes: List[String],
            positionIds: List[Int],
            positionMatrix: PositionMatrix): Behavior[ClusterDomainEvent] =
    setup[ClusterDomainEvent] { context: ActorContext[ClusterDomainEvent] =>
      withTimers { timers: TimerScheduler[ClusterDomainEvent] =>
        {
          val cluster = Cluster(context.system)
          cluster.subscriptions ! Subscribe(context.self, classOf[MemberEvent])

          timers.startTimerAtFixedRate(VerifyMemberAssignment, 1 second)

          applyToContext(M, N, nodes, positionIds, positionMatrix, context)
        }
      }
    }
}
