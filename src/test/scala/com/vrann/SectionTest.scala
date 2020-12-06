package com.vrann

import java.io.File
import java.nio.file.{Path, Paths}

import akka.actor.testkit.typed.Effect.Spawned
import akka.actor.testkit.typed.scaladsl.{ActorTestKit, BehaviorTestKit}
import akka.actor.typed.pubsub.Topic.Publish
import akka.event.slf4j.Logger
import com.vrann.cholesky.CholeskyBlockMatrixType.L11
import com.vrann.cholesky.CholeskyRoleBehavior
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec

class SectionTest extends AnyWordSpec with BeforeAndAfterAll with Matchers {
  val testKit = ActorTestKit()
//  override def afterAll(): Unit = {
//    Thread.sleep(10000)
//    testKit.shutdownTestKit()
//  }
  "SectionBehavior" must {
    "must spawn file transfer and positioned block" in {
      val testKit = BehaviorTestKit(new Section(List(Position(0, 0)), new TopicsRegistry[Message]).behavior)

      testKit.expectEffect(
        Spawned(
          FileTransfer.apply(new FileLocatorDefault(), List(Position(0, 0)), new TopicsRegistry[Message]).apply,
          "fileTransfer"))
      testKit.expectEffect(
        Spawned(CholeskyRoleBehavior(Position(0, 0), new TopicsRegistry[Message]).apply, "position-0-0"))

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
    "must work" in {
//      val section = new Section(List(Position(0, 0), Position(-1, 0)))
////      val behavior = section.behavior
//      val sectionActor = testKit.spawn(section.behavior, "Section")
//      val probe = testKit.createTestProbe[BlockMessage]()
//
//      section.sectionTopicsRegistry("a11-data--1-0") ! Publish(AijData(Position(-1, 0), new File("tmp/path"), 1))
//      sectionActor ! GetState(Position(0, 0), probe.ref)
//      probe.expectMessage(StateMessage(Position(0, 0), A11Processed))

      class TestFileLocator(sectionName: String) extends FileLocator {
        val logger = Logger("test")
        def apply(fileName: String): Path = {
          try {
            val fullFilePath = getClass.getResource("/" + sectionName + "/" + fileName).getPath
            logger.debug(s"FileReader is reading $fullFilePath")
            Paths.get(fullFilePath)
          } catch {
            case e: NullPointerException =>
              logger.error(s"File does not exists: $sectionName/$fileName")
              throw e
          }
        }
      }

      val topicRegistry1 = new TopicsRegistry[Message]
      val positions1 = List(Position(0, 0), Position(1, 0))
      val fileLocator1: FileLocator = new TestFileLocator("section1")
      val section1 = new Section(positions1, topicRegistry1, fileLocator1)

      val topicRegistry2 = new TopicsRegistry[Message]
      val positions2 = List(Position(1, 1), Position(2, 0))
      val fileLocator2: FileLocator = new TestFileLocator("section2")
      val section2 = new Section(positions2, topicRegistry2, fileLocator2)

      val root = testKit.spawn(RootBehavior.behavior(section1), "root")
      val root2 = testKit.spawn(RootBehavior.behavior(section2), "root2")
      val message =
        FileTransferReadyMessage(Position(0, 0), L11, 1, "l11.mtrx", root2)
      Thread.sleep(500)
      topicRegistry1("data-ready", Position(0, 0)) ! Publish(message)
      Thread.sleep(500)
    }
  }
}
