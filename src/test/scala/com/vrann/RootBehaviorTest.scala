package com.vrann

import akka.actor.testkit.typed.scaladsl.ActorTestKit
import com.vrann.cholesky.CholeskyBlockMatrixType.L11
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec

class RootBehaviorTest extends AnyWordSpec with BeforeAndAfterAll with Matchers {

  "FileTransferMessage" must {
    "must be received by subscribers" in {
      val testKit = ActorTestKit()
      val root =
        testKit.spawn(
          RootBehavior.behavior(new Section(List(Position(0, 0)), new TopicsRegistry[Message], 1)),
          "default")
      val message =
        FileTransferReadyMessage(Position(0, 0), L11, 1, "test", root)
      val probe = testKit.createTestProbe[FileTransferMessage]()
      root ! SubscribeTestProbe(probe.ref)
      Thread.sleep(500)
      root ! TestMessage
      probe.expectMessage(message)
    }
  }
}
