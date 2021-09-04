//package com.vrann.positioned
//
//import akka.actor.typed.scaladsl.Behaviors
//import akka.actor.typed.scaladsl.Behaviors.setup
//import akka.actor.typed.{ActorRef, Behavior}
//import akka.cluster.ClusterEvent.ClusterDomainEvent
//import akka.cluster.ddata.typed.scaladsl.{DistributedData, Replicator}
//import akka.cluster.ddata.{GCounter, GCounterKey, LWWMap, LWWMapKey, SelfUniqueAddress}
//import com.vrann.Position
//
//import scala.language.postfixOps
//
//case object VerifyMemberAssignment extends ClusterDomainEvent
//case class GetCurrentAssignment(replyTo: ActorRef[Response]) extends ClusterDomainEvent
//final case class Response(positionMatrix: Map[String, List[Position]])
//
//object SectionSelector {
//  sealed trait Command
//  case object Increment extends Command
//  private sealed trait InternalCommand extends Command
//
//  private case class InternalSubscribeResponse(chg: Replicator.SubscribeResponse[LWWMap[Position, Int]])
//      extends InternalCommand
//
//  private case class InternalUpdateResponse(rsp: Replicator.UpdateResponse[LWWMap[Position, Int]])
//      extends InternalCommand
//
//  def apply(key: LWWMapKey[Position, Int]): Behavior[Command] =
//    setup[Command] { context =>
//      implicit val node: SelfUniqueAddress = DistributedData(context.system).selfUniqueAddress
//
//      // adapter that turns the response messages from the replicator into our own protocol
//      DistributedData.withReplicatorMessageAdapter[Command, LWWMap[Position, Int]] { replicatorAdapter =>
//        replicatorAdapter.subscribe(key, InternalSubscribeResponse.apply)
//
//        def updated(cachedValue: Int): Behavior[Command] = {
//          Behaviors.receiveMessage[Command] {
//            case Increment =>
//              replicatorAdapter.askUpdate(
//                askReplyTo =>
//                  Replicator.Update(key, LWWMap.empty[Position, Int], Replicator.WriteLocal, askReplyTo)(_ :+ 1),
//                InternalUpdateResponse.apply)
//
//              Behaviors.same
//
//            case internal: InternalCommand =>
//              internal match {
//                case InternalUpdateResponse(_) => Behaviors.same // ok
//
//                case InternalGetResponse(rsp @ Replicator.GetSuccess(`key`), replyTo) =>
//                  val value = rsp.get(key).value.toInt
//                  replyTo ! value
//                  Behaviors.same
//
//                case InternalGetResponse(_, _) =>
//                  Behaviors.unhandled // not dealing with failures
//                case InternalSubscribeResponse(chg @ Replicator.Changed(`key`)) =>
//                  val value = chg.get(key).value.intValue
//                  updated(value)
//
//                case InternalSubscribeResponse(Replicator.Deleted(_)) =>
//                  Behaviors.unhandled // no deletes
//
//                case InternalSubscribeResponse(_) => // changed but wrong key
//                  Behaviors.unhandled
//
//              }
//          }
//        }
//        updated(cachedValue = 0)
//      }
//    }
//}
