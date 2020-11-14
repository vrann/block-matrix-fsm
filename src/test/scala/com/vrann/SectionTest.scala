package com.vrann

import akka.actor.testkit.typed.Effect.Spawned
import akka.actor.testkit.typed.scaladsl.BehaviorTestKit
import com.vrann.cholesky.CholeskyRoleBehavior
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec

class SectionTest extends AnyWordSpec with BeforeAndAfterAll with Matchers {

  "SectionBehavior" must {
    "must spwan file transfer and positioned block" in {
      val testKit = BehaviorTestKit(new Section(List(Position(0, 0))).behavior)

      testKit.expectEffect(
        Spawned(FileTransfer.apply(new FileLocatorDefault())(), "fileTransfer")
      )
      testKit.expectEffect(
        Spawned(
          Block(Position(0, 0), CholeskyRoleBehavior(Position(0, 0))),
          "position-0-0"
        )
      )

//      val childInbox = testKit.childInbox[String]("position-0-0")
//      childInbox.expectMessage("hello")
//
//      val fileMessage =
//        FileTransferReadyMessage(Position(0, 0), L11, 1, "test", root)
//      val blockMessage = BlockMessage.MatrixDataAvailable(
//        Position(0, 0),
//        new File("/tmp/test"),
//        1
//      )
//
//      testKit.run(fileMessage)
//      testKit.expectEffect()
//
//      root ! fileMessage
//      root ! blockMessage
//
//      val childInbox = testKit.childInbox[String]("position-0-0")
//      childInbox.expectMessage("hello")
////
//      testProbe.ex
    }
  }
}
