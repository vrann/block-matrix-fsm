package com.vrann.cholesky

import java.io.File

import akka.actor.testkit.typed.CapturedLogEvent
import akka.actor.testkit.typed.scaladsl.{BehaviorTestKit, TestInbox}
import com.vrann.BlockMessage.{DataReady, StateMessage}
import com.vrann.{A11Processed, BlockMessage, Message, Position, TopicsRegistry}
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper
import org.slf4j.event.Level

class BlockDiagonalTest extends org.scalatest.FunSuite {

  test("testApplyAMN") {
    val block = BehaviorTestKit(new BlockDiagonal(Position(0, 0), new TopicsRegistry[Message]).apply)
    block.run(DataReady(Position(0, 0), CholeskyBlockMatrixType.aMN, new File("/tmp")))
    block
      .logEntries() shouldBe Seq(CapturedLogEvent(Level.DEBUG, "factorize"), CapturedLogEvent(Level.DEBUG, "Done"))
//    inbox.expectMessage(StateMessage(Position(0, 0), A11Processed))
  }

  test("testApplyL21First") {
    val block = BehaviorTestKit(new BlockDiagonal(Position(0, 0), new TopicsRegistry[Message]).apply)
    assertThrows[Exception] {
      block.run(DataReady(Position(0, 0), CholeskyBlockMatrixType.L21, new File("/tmp")))
    }
  }

  test("testApplyL21") {
    val block = BehaviorTestKit(new BlockDiagonal(Position(1, 1), new TopicsRegistry[Message]).apply)
    block.run(DataReady(Position(1, 1), CholeskyBlockMatrixType.L21, new File("/tmp")))
    block.logEntries() shouldBe Seq(CapturedLogEvent(Level.DEBUG, "L21 stashed"))
  }

  test("stashAndApply") {
    val block = BehaviorTestKit(new BlockDiagonal(Position(4, 4), new TopicsRegistry[Message]).apply)
    block.run(DataReady(Position(4, 4), CholeskyBlockMatrixType.L21, new File("/tmp")))
    block.run(DataReady(Position(4, 4), CholeskyBlockMatrixType.L21, new File("/tmp")))
    block.run(DataReady(Position(4, 4), CholeskyBlockMatrixType.L21, new File("/tmp")))

    block.run(DataReady(Position(4, 4), CholeskyBlockMatrixType.aMN, new File("/tmp")))

    block.logEntries() shouldBe Seq(
      CapturedLogEvent(Level.DEBUG, "L21 stashed"),
      CapturedLogEvent(Level.DEBUG, "L21 stashed"),
      CapturedLogEvent(Level.DEBUG, "L21 stashed"),
      CapturedLogEvent(Level.DEBUG, "aMN"),
      CapturedLogEvent(Level.DEBUG, "L21 unstashing"),
      CapturedLogEvent(Level.DEBUG, "L21 applied"),
      CapturedLogEvent(Level.DEBUG, "L21 applied"),
      CapturedLogEvent(Level.DEBUG, "L21 applied"),
      CapturedLogEvent(Level.DEBUG, "factorized"))
  }

  test("stashAndApplyException") {
    val block = BehaviorTestKit(new BlockDiagonal(Position(4, 4), new TopicsRegistry[Message]).apply)
    block.run(DataReady(Position(4, 4), CholeskyBlockMatrixType.L21, new File("/tmp")))
    block.run(DataReady(Position(4, 4), CholeskyBlockMatrixType.L21, new File("/tmp")))
    block.run(DataReady(Position(4, 4), CholeskyBlockMatrixType.L21, new File("/tmp")))
    block.run(DataReady(Position(4, 4), CholeskyBlockMatrixType.L21, new File("/tmp")))

    assertThrows[Exception] {
      block.run(DataReady(Position(4, 4), CholeskyBlockMatrixType.aMN, new File("/tmp")))
    }
    block.logEntries() shouldBe Seq(
      CapturedLogEvent(Level.DEBUG, "L21 stashed"),
      CapturedLogEvent(Level.DEBUG, "L21 stashed"),
      CapturedLogEvent(Level.DEBUG, "L21 stashed"),
      CapturedLogEvent(Level.DEBUG, "L21 stashed"),
      CapturedLogEvent(Level.DEBUG, "aMN"),
      CapturedLogEvent(Level.DEBUG, "L21 unstashing"),
      CapturedLogEvent(Level.DEBUG, "L21 applied"),
      CapturedLogEvent(Level.DEBUG, "L21 applied"),
      CapturedLogEvent(Level.DEBUG, "L21 applied"))
  }

  test("L21InOrder") {
    val block = BehaviorTestKit(new BlockDiagonal(Position(4, 4), new TopicsRegistry[Message]).apply)
    block.run(DataReady(Position(4, 4), CholeskyBlockMatrixType.aMN, new File("/tmp")))
    block.run(DataReady(Position(4, 4), CholeskyBlockMatrixType.L21, new File("/tmp")))
    block.run(DataReady(Position(4, 4), CholeskyBlockMatrixType.L21, new File("/tmp")))
    block.run(DataReady(Position(4, 4), CholeskyBlockMatrixType.L21, new File("/tmp")))
    block.logEntries() shouldBe Seq(
      CapturedLogEvent(Level.DEBUG, "aMN"),
      CapturedLogEvent(Level.DEBUG, "L21 unstashing"),
      CapturedLogEvent(Level.DEBUG, "L21 applied"),
      CapturedLogEvent(Level.DEBUG, "L21 applied"),
      CapturedLogEvent(Level.DEBUG, "L21 applied"),
      CapturedLogEvent(Level.DEBUG, "factorized"))
  }

  test("L21OutOfOrder") {
    val block = BehaviorTestKit(new BlockDiagonal(Position(4, 4), new TopicsRegistry[Message]).apply)
    block.run(DataReady(Position(4, 4), CholeskyBlockMatrixType.aMN, new File("/tmp")))
    block.run(DataReady(Position(4, 4), CholeskyBlockMatrixType.L21, new File("/tmp")))
    block.run(DataReady(Position(4, 4), CholeskyBlockMatrixType.L21, new File("/tmp")))
    block.run(DataReady(Position(4, 4), CholeskyBlockMatrixType.L21, new File("/tmp")))
    block.logEntries() shouldBe Seq(
      CapturedLogEvent(Level.DEBUG, "aMN"),
      CapturedLogEvent(Level.DEBUG, "L21 unstashing"),
      CapturedLogEvent(Level.DEBUG, "L21 applied"),
      CapturedLogEvent(Level.DEBUG, "L21 applied"),
      CapturedLogEvent(Level.DEBUG, "L21 applied"),
      CapturedLogEvent(Level.DEBUG, "factorized"))

    assertThrows[Exception] {
      //message to stopped actor
      block.run(DataReady(Position(4, 4), CholeskyBlockMatrixType.L21, new File("/tmp")))
    }

  }

}
