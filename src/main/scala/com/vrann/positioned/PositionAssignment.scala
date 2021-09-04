package com.vrann.positioned

import akka.actor.typed.scaladsl.Behaviors.{receiveMessage, same, setup, withTimers}
import akka.actor.typed.scaladsl.{ActorContext, Behaviors, TimerScheduler}
import akka.actor.typed.{ActorRef, Behavior}
import com.vrann.{Message, Position, UpdatePositions}

import scala.language.postfixOps

final case class Response(positionMatrix: Map[String, List[Position]])

trait PositionCommand
case class SectionJoined(id: Int, ref: ActorRef[Message]) extends PositionCommand
case object VerifyMemberAssignment extends PositionCommand
case class GetCurrentAssignment(replyTo: ActorRef[Message]) extends PositionCommand

object PositionAssignment {

  def applyToContext(M: Int,
                     N: Int,
                     nodes: List[Int],
                     positionIds: List[Int],
                     positionMatrix: PositionMatrix,
                     sections: Map[String, ActorRef[Message]],
                     context: ActorContext[PositionCommand]): Behaviors.Receive[PositionCommand] =
    receiveMessage {
      case SectionJoined(id: Int, ref: ActorRef[Message]) =>
        context.log.info(s"New Section Joined $id")
        val newNodesList = nodes :+ id
        val newPositionIds = positionIds :+ id
        val newPositionMatrix = positionMatrix ++ List(id)
        val newSections = sections + (id.toString -> ref)

        for ((_, sectionActor) <- newSections) sectionActor ! UpdatePositions(newSections, newPositionMatrix.sections)

        applyToContext(M, N, newNodesList, newPositionIds, newPositionMatrix, newSections, context)
//      case MemberUp(member) =>
//        context.log.debug("Member up {}", member.uniqueAddress)
//        val newNodesList = nodes :+ member.uniqueAddress.address.toString
//        val newPositionIds = positionIds :+ member.uniqueAddress.address.hashCode
//        val newPositionMatrix = positionMatrix ++ List(member.uniqueAddress.address.hashCode)
//        applyToContext(M, N, newNodesList, newPositionIds, newPositionMatrix, context)
//      case MemberLeft(member) =>
//        context.log.debug("Member left {}", member.uniqueAddress)
//        val newNodesList = nodes.diff(List(member.uniqueAddress.address.toString))
//        val newPositionIds = positionIds.diff(List(member.uniqueAddress.address.hashCode))
//        val newPositionMatrix = positionMatrix -- List(member.uniqueAddress.address.hashCode)
//        applyToContext(M, N, newNodesList, newPositionIds, newPositionMatrix, context)
      case VerifyMemberAssignment =>
        context.log.info("Member nodes {}", positionMatrix.sections)
        same
      case GetCurrentAssignment(replyTo) =>
        context.log.info("Assignment asked")
        replyTo ! UpdatePositions(sections, positionMatrix.sections)
        same
      case a @ _ => context.log.info("Cluster member message {}", a); same
    }

  def apply(M: Int, N: Int): Behavior[PositionCommand] =
    apply(M, N, List.empty[Int], List.empty[Int], new PositionMatrix(M, N, List.empty[Int]))

  def apply(M: Int,
            N: Int,
            nodes: List[Int],
            positionIds: List[Int],
            positionMatrix: PositionMatrix): Behavior[PositionCommand] =
    setup[PositionCommand] { context: ActorContext[PositionCommand] =>
      withTimers { timers: TimerScheduler[PositionCommand] =>
        {
//          val cluster = Cluster(context.system)
//          cluster.subscriptions ! Subscribe(context.self, classOf[MemberEvent])

          //timers.startTimerAtFixedRate(VerifyMemberAssignment, 1 second)

          applyToContext(M, N, nodes, positionIds, positionMatrix, Map.empty, context)
        }
      }
    }
}
