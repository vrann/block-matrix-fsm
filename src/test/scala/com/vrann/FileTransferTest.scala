//package com.vrann
//
//import java.nio.file.{Path, Paths}
//
//import akka.actor.testkit.typed.scaladsl.{ActorTestKit, BehaviorTestKit, TestInbox}
//import akka.actor.typed.ActorSystem
//import akka.event.slf4j.Logger
//import akka.stream.SourceRef
//import akka.stream.scaladsl.{FileIO, StreamRefs}
//import akka.util.ByteString
//import com.vrann
//
//import com.vrann.cholesky.CholeskyBlockMatrixType.L11
//import org.scalatest.BeforeAndAfterAll
//import org.scalatest.matchers.must.Matchers
//import org.scalatest.wordspec.AnyWordSpec
//
//class TestFileLocator extends FileLocator {
//  val logger = Logger("test")
//  def apply(fileName: String): Path = {
//    Paths.get(getClass.getResource("/" + fileName).getPath)
//  }
//}
//
//class FileTransferTest extends AnyWordSpec with BeforeAndAfterAll with Matchers {
//  "FileTransfer" must {
//    "must accept FileTransferReadyMessage message" in {
//      val testKit =
//        BehaviorTestKit(FileTransfer(new TestFileLocator(), List(Position(0, 0)), new TopicsRegistry[Message], 1).apply)
//      val inbox = TestInbox[FileTransferMessage]()
//      testKit.run(FileTransferReadyMessage(Position(0, 0), L11, 1, "l11.mtrx", inbox.ref))
//      inbox.expectMessage(FileTransferRequestMessage(Position(0, 0), L11, 1, "l11.mtrx", testKit.ref))
//    }
//  }
//  "FileTransferMessage" must {
//    "FileTransferReadyMessage must be received by subscribers" in {
//      val testKit = ActorTestKit()
//      val root =
//        testKit.spawn(
//          vrann.FileTransfer(new TestFileLocator(), List(Position(0, 0)), new TopicsRegistry[Message], 1).apply,
//          "default")
//      val root2 =
//        testKit.spawn(
//          vrann.FileTransfer(new TestFileLocator(), List(Position(0, 0)), new TopicsRegistry[Message], 1).apply,
//          "default2")
//      val message =
//        FileTransferReadyMessage(Position(0, 0), L11, 1, "l11.mtrx", root2)
//
//      root ! message
//    }
//
//    "FileTransferResponseMessage must be received by subscribers" in {
//      val testKit = ActorTestKit()
//      //      implicit val system: ActorSystem[Nothing] = testKit
//      val root =
//        testKit.spawn(
//          vrann.FileTransfer(new TestFileLocator(), List(Position(0, 0)), new TopicsRegistry[Message], 1).apply,
//          "default")
//      val root2 =
//        testKit.spawn(
//          vrann.FileTransfer(new TestFileLocator(), List(Position(0, 0)), new TopicsRegistry[Message], 1).apply,
//          "default2")
//      val fileName = "l11.mtrx"
//      implicit val system: ActorSystem[Nothing] = testKit.system
//      val filePath = new TestFileLocator()(fileName)
//      val fileRef: SourceRef[ByteString] =
//        FileIO
//          .fromPath(filePath)
//          .log("error logging")
//          .runWith(StreamRefs
//            .sourceRef())
//      val fileTransferMessage =
//        FileTransferResponseMessage(Position(0, 0), L11, fileName, fileRef)
//
//      root2 ! fileTransferMessage
//    }
//  }
//}
