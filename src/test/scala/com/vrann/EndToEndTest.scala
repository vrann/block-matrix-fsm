//package com.vrann
//
//import java.io.File
//import java.nio.file.{Path, Paths}
//
//import akka.actor.testkit.typed.Effect.Spawned
//import akka.actor.testkit.typed.scaladsl.{ActorTestKit, BehaviorTestKit}
//import akka.actor.typed.pubsub.Topic.Publish
//import akka.event.slf4j.Logger
//import com.vrann.BlockMessage.{AijData, L21Data}
//import com.vrann.cholesky.CholeskyBlockMatrixType.{aMN, L11}
//import com.vrann.cholesky.CholeskyRoleBehavior
//import org.scalatest.BeforeAndAfterAll
//import org.scalatest.matchers.must.Matchers
//import org.scalatest.wordspec.AnyWordSpec
//
//import scala.collection.immutable
//
//class EndToEndTest extends AnyWordSpec with BeforeAndAfterAll with Matchers {
//  class TestFileLocator(sectionName: String) extends FileLocator {
//    val logger = Logger("test")
//    def apply(fileName: String): Path = {
//      try {
//        val fullFilePath = getClass.getResource("/" + sectionName + "/" + fileName).getPath
//        logger.debug(s"FileReader is reading $fullFilePath")
//        Paths.get(fullFilePath)
//      } catch {
//        case e: NullPointerException =>
//          logger.error(s"File does not exists: $sectionName/$fileName")
//          throw e
//      }
//    }
//  }
//  val testKit = ActorTestKit()
//  "must work" in {
//
//    val topicRegistry1 = new TopicsRegistry[Message]
//    val positions1 = List(Position(0, 0), Position(1, 0), Position(2, 0))
//    val fileLocator1: FileLocator = new TestFileLocator("endtoend-section1")
//    val section1 = Section("node-1", positions1, topicRegistry1, fileLocator1)
//
//    val topicRegistry2 = new TopicsRegistry[Message]
//    val positions2 = List(Position(1, 1), Position(2, 1), Position(2, 2))
//    val fileLocator2: FileLocator = new TestFileLocator("endtoend-section2")
//    val section2 = Section("node-1", positions2, topicRegistry2, fileLocator2)
//
//    val root1 = testKit.spawn(RootBehavior.behavior(section1), "root")
//    val root2 = testKit.spawn(RootBehavior.behavior(section2), "root2")
//
//    //@TODO sectionId never used
//    //@TODO messages are routed to section through root. Should be topic instead
//    val messages: immutable.Seq[FileTransferReadyMessage] = List(
//      //section1
//      FileTransferReadyMessage(Position(0, 0), aMN, "1", "a00.mtrx", root1),
//      FileTransferReadyMessage(Position(1, 0), aMN, "1", "a10.mtrx", root1),
//      FileTransferReadyMessage(Position(2, 0), aMN, "1", "a20.mtrx", root1),
//      //section2
//      FileTransferReadyMessage(Position(0, 0), aMN, "2", "a11.mtrx", root2),
//      FileTransferReadyMessage(Position(1, 0), aMN, "2", "a12.mtrx", root2),
//      FileTransferReadyMessage(Position(2, 0), aMN, "2", "a22.mtrx", root2))
//    Thread.sleep(500)
//    messages.foreach(m => topicRegistry1("data-ready", m.position) ! Publish(m))
////      topicRegistry1("data-ready", Position(0, 0)) ! Publish(message)
////      Thread.sleep(500)
//  }
//}
