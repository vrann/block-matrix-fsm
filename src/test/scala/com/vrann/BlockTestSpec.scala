package com.vrann

import java.io.File

import akka.actor.testkit.typed.scaladsl.{BehaviorTestKit, TestInbox}
import com.vrann.BlockMessage.MatrixDataAvailable
//import akka.actor.typed.internal.BehaviorImpl.ReceiveMessageBehavior
import com.vrann.BlockMessage.{GetState, StateMessage}
import com.vrann.cholesky.CholeskyRoleBehavior
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec

//class TestingInterceptor[T: ClassTag] extends BehaviorInterceptor[T, T] {
//  import BehaviorInterceptor._
//
//  override def aroundReceive(ctx: TypedActorContext[T],
//                             msg: T,
//                             target: ReceiveTarget[T]): Behavior[T] = {
//
////    val t: ReceiveMessageBehavior = target(ctx, msg)
//
////    System.out.println(t)
////    t
////    t.asInstanceOf[TestableBe]
//  }
//
//  // only once to the same actor in the same behavior stack
//  override def isSame(other: BehaviorInterceptor[Any, Any]): Boolean =
//    other match {
//
//      case _ => false
//    }
//
//}

class BlockTestSpec extends AnyWordSpec with BeforeAndAfterAll with Matchers {
  "Block" must {
    "invoke subordinate behavior" in {
//      val testKit = ActorTestKit()
//      val behavior = Block(Position(0, 0), CholeskyRoleBehavior(Position(0, 0)))
//      val interceptedBehavior = Behaviors.intercept[BlockMessage, BlockMessage](
//        () => new TestingInterceptor()
//      )(behavior)
//      val block = testKit.spawn(interceptedBehavior)
//      block ! MatrixDataAvailable(Position(0, 0), new File("tmp/path"), 1)

      val testKit = BehaviorTestKit(
        Block(Position(0, 0), CholeskyRoleBehavior(Position(0, 0)))
      )
      val inbox = TestInbox[BlockMessage]()
      testKit.run(MatrixDataAvailable(Position(0, 0), new File("tmp/path"), 1))
      testKit.run(GetState(Position(0, 0), inbox.ref))
      inbox.expectMessage(StateMessage(Position(0, 0), Initialized))
    }

    "must change state after Matrix data became available" in {
      val testKit = BehaviorTestKit(
        Block(Position(0, 0), CholeskyRoleBehavior(Position(0, 0)))
      )
      testKit.run(MatrixDataAvailable(Position(0, 0), new File("tmp/path"), 1))
      val inbox = TestInbox[BlockMessage]()
      testKit.run(GetState(Position(0, 0), inbox.ref))
      inbox.expectMessage(StateMessage(Position(0, 0), Initialized))
    }
    "must not change state if Matrix data is not received" in {
      val testKit = BehaviorTestKit(
        Block(Position(0, 0), CholeskyRoleBehavior(Position(0, 0)))
      )
      val inbox = TestInbox[BlockMessage]()
      testKit.run(GetState(Position(0, 0), inbox.ref))
      inbox.expectMessage(StateMessage(Position(0, 0), Uninitialized))
    }
  }
}
