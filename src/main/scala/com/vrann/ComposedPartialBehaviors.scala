//package com.vrann
//
//import akka.actor.typed.pubsub.Topic.Command
//import akka.actor.typed.{ActorRef, Behavior}
//import akka.actor.typed.scaladsl.Behaviors
//
////https://github.com/akka/akka/blob/master/akka-actor-typed-tests/src/test/scala/akka/actor/typed/OrElseSpec.scala
//class ComposedPartialBehaviors(blockBehaviors: List[BlockBehavior]) extends BlockBehavior {
//  val handlers: List[BlockBehavior] = blockBehaviors.map(behavior => behavior.init(state)) ::: Nil
//  var behaviors: List[PartialFunction[BlockMessage, Behavior[BlockMessage]]]
//
//  var topics = blockBehaviors.foldLeft(Map.empty[String, Behavior[Command[BlockMessage]]])((map, blockBehavior) =>
//    map ++ blockBehavior.topics)
//
//  override def ::(that: BlockBehavior): BlockBehavior = {
//    topics = topics ++ that.topics
//    this
//  }
//
//  override def apply(topicsRegistry: Map[String, ActorRef[Command[BlockMessage]]]): Behavior[BlockMessage] = {
//    behaviors = blockBehaviors.map(behavior => behavior.apply(topicsRegistry))
//    state(Uninitialized)
////    Behaviors.intercept[BlockMessage, BlockMessage](() => new ComposedBehavior(behaviors))(Behaviors.empty)
//  }
//
//  def state(stateTransition: State): Behavior[BlockMessage] = {
//
//    def handle(command: BlockMessage,
//               handlers: List[PartialFunction[BlockMessage, Behavior[BlockMessage]]]): Behavior[BlockMessage] = {
//      handlers match {
//        case Nil          => Behaviors.unhandled
//        case head :: tail => head.applyOrElse(command, handle(_, tail))
//      }
//    }
//
//    Behaviors.receiveMessage(command => handle(command, handlers))
//  }
//}
//
//object CompositionWithPartialFunction {
//
//  def ping(counters: Map[String, Int]): Behavior[Ping] = {
//
//    val ping1: PartialFunction[Ping, Behavior[Ping]] = {
//      case Ping1(replyTo: ActorRef[Pong]) =>
//        val newCounters = counters.updated("ping1", counters.getOrElse("ping1", 0) + 1)
//        replyTo ! Pong(newCounters("ping1"))
//        ping(newCounters)
//    }
//
//    val ping2: PartialFunction[Ping, Behavior[Ping]] = {
//      case Ping2(replyTo: ActorRef[Pong]) =>
//        val newCounters = counters.updated("ping2", counters.getOrElse("ping2", 0) + 1)
//        replyTo ! Pong(newCounters("ping2"))
//        ping(newCounters)
//    }
//
//    val ping3: PartialFunction[Ping, Behavior[Ping]] = {
//      case Ping3(replyTo: ActorRef[Pong]) =>
//        val newCounters = counters.updated("ping3", counters.getOrElse("ping3", 0) + 1)
//        replyTo ! Pong(newCounters("ping3"))
//        ping(newCounters)
//    }
//
//    val pingHandlers: List[PartialFunction[Ping, Behavior[Ping]]] = ping1 :: ping2 :: ping3 :: Nil
//
//    // this could be provided as a general purpose utility
//    def handle(command: Ping, handlers: List[PartialFunction[Ping, Behavior[Ping]]]): Behavior[Ping] = {
//      handlers match {
//        case Nil          => Behaviors.unhandled
//        case head :: tail => head.applyOrElse(command, handle(_, tail))
//      }
//    }
//
//    Behaviors.receiveMessage(command => handle(command, pingHandlers))
//  }
//}
