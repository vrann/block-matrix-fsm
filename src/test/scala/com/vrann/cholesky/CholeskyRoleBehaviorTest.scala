//package com.vrann.cholesky
//
//import java.io.File
//
//import akka.actor.testkit.typed.scaladsl.{BehaviorTestKit, TestInbox}
//import com.vrann.BlockMessage.{AijData, GetState, L11Ready, L21Data, MatrixDataAvailable, StateMessage}
//import com.vrann.{A11Processed, Block, BlockMessage, Initialized, L21Processed, Message, Position, TopicsRegistry}
//import org.scalatest.BeforeAndAfterAll
//import org.scalatest.matchers.must.Matchers
//import org.scalatest.wordspec.AnyWordSpec
//
//class CholeskyRoleBehaviorTest extends AnyWordSpec with BeforeAndAfterAll with Matchers {
//  "CholeskyRoleBehavior" must {
//    "invoke L11 behavior" in {
////      CholeskyRoleBehavior(Position(0, 0)) ! BlockMessage(Position(0, 0))
//      val testKit = BehaviorTestKit(CholeskyRoleBehavior(Position(-1, 0), new TopicsRegistry[Message]).apply)
//      testKit.run(AijData(Position(-1, 0), new File("tmp/path"), 1))
////      testKit.run(AijData(Position(-1, 0), new File("tmp/path"), 1))
//      val inbox = TestInbox[BlockMessage]()
//      testKit.run(GetState(Position(0, 0), inbox.ref))
//      inbox.expectMessage(StateMessage(Position(0, 0), A11Processed))
//      testKit.run(L21Data(Position(-1, 0), new File("tmp/path"), 1))
//      testKit.run(GetState(Position(0, 0), inbox.ref))
//      inbox.expectMessage(StateMessage(Position(0, 0), L21Processed))
//
////      inbox.expectMessage(L11Ready(Position(-1, 0), new File("tmp/path"), 1))
//    }
//  }
//}
