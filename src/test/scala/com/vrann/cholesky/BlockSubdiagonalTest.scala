package com.vrann.cholesky

class BlockSubdiagonalTest extends org.scalatest.FunSuite {

//  test("testInitialize") {
//    val block = BehaviorTestKit(new BlockSubdiagonal(Position(1, 0), new TopicsRegistry[Message]).apply)
//    block.run(DataReady(Position(1, 0), CholeskyBlockMatrixType.aMN, new File("/tmp")))
//    block
//      .logEntries() shouldBe Seq(CapturedLogEvent(Level.DEBUG, "aMN"), CapturedLogEvent(Level.DEBUG, "L21 unstashing"))
//  }
//
//  test("testSkipInitialize") {
//    val block = BehaviorTestKit(new BlockSubdiagonal(Position(2, 1), new TopicsRegistry[Message]).apply)
//    block.run(DataReady(Position(2, 1), CholeskyBlockMatrixType.aMN, new File("/tmp")))
//    block.run(DataReady(Position(2, 1), CholeskyBlockMatrixType.aMN, new File("/tmp")))
//    block
//      .logEntries() shouldBe Seq(
//      CapturedLogEvent(Level.DEBUG, "aMN"),
//      CapturedLogEvent(Level.DEBUG, "L21 unstashing"),
//      CapturedLogEvent(Level.DEBUG, "skip initialization"))
//  }
//
//  test("testSkipInitializeFirstColumn") {
//    val block = BehaviorTestKit(new BlockSubdiagonal(Position(1, 0), new TopicsRegistry[Message]).apply)
//    block.run(DataReady(Position(1, 0), CholeskyBlockMatrixType.aMN, new File("/tmp")))
//    block.run(DataReady(Position(1, 0), CholeskyBlockMatrixType.aMN, new File("/tmp")))
//    block
//      .logEntries() shouldBe Seq(
//      CapturedLogEvent(Level.DEBUG, "aMN"),
//      CapturedLogEvent(Level.DEBUG, "L21 unstashing"),
//      CapturedLogEvent(Level.DEBUG, "skip initialization"))
//  }
//
//  test("testL21SentToInitializedFirstRow") {
//    val block = BehaviorTestKit(new BlockSubdiagonal(Position(1, 0), new TopicsRegistry[Message]).apply)
//    block.run(DataReady(Position(1, 0), CholeskyBlockMatrixType.aMN, new File("/tmp")))
//    block.run(DataReady(Position(1, 0), CholeskyBlockMatrixType.L21, new File("/tmp")))
//
//    block
//      .logEntries() shouldBe Seq(
//      CapturedLogEvent(Level.DEBUG, "aMN"),
//      CapturedLogEvent(Level.DEBUG, "L21 unstashing"),
//      CapturedLogEvent(Level.DEBUG, "skip L21"))
//  }
//
//  test("testL21SentToInitialized") {
//    val block = BehaviorTestKit(new BlockSubdiagonal(Position(2, 1), new TopicsRegistry[Message]).apply)
//    block.run(DataReady(Position(2, 1), CholeskyBlockMatrixType.aMN, new File("/tmp")))
//    block.run(DataReady(Position(2, 1), CholeskyBlockMatrixType.L21, new File("/tmp")))
//    block
//      .logEntries() shouldBe Seq(
//      CapturedLogEvent(Level.DEBUG, "aMN"),
//      CapturedLogEvent(Level.DEBUG, "L21 unstashing"),
//      CapturedLogEvent(Level.DEBUG, "L21 applied"))
//  }
//
//  test("testL21SentToUnInitializedAndStashed") {
//    val block = BehaviorTestKit(new BlockSubdiagonal(Position(2, 1), new TopicsRegistry[Message]).apply)
//    block.run(DataReady(Position(2, 1), CholeskyBlockMatrixType.L21, new File("/tmp")))
//    block
//      .logEntries() shouldBe Seq(CapturedLogEvent(Level.DEBUG, "L21 stashed"))
//  }
//
//  test("testL11SentToUninitializedAndStashed") {
//    val block = BehaviorTestKit(new BlockSubdiagonal(Position(3, 2), new TopicsRegistry[Message]).apply)
//    block.run(DataReady(Position(3, 2), CholeskyBlockMatrixType.L11, new File("/tmp")))
//    block
//      .logEntries() shouldBe Seq(CapturedLogEvent(Level.DEBUG, "L11 stashed"))
//  }
//
//  test("testL11SentToInitializedAndStashed") {
//    val block = BehaviorTestKit(new BlockSubdiagonal(Position(3, 2), new TopicsRegistry[Message]).apply)
//    block.run(DataReady(Position(3, 2), CholeskyBlockMatrixType.aMN, new File("/tmp")))
//    block.run(DataReady(Position(3, 2), CholeskyBlockMatrixType.L11, new File("/tmp")))
//    block
//      .logEntries() shouldBe Seq(
//      CapturedLogEvent(Level.DEBUG, "aMN"),
//      CapturedLogEvent(Level.DEBUG, "L21 unstashing"),
//      CapturedLogEvent(Level.DEBUG, "L11 stashed"))
//  }
//
//  test("testL11SentToSomeL21AndStashed") {
//    val block = BehaviorTestKit(new BlockSubdiagonal(Position(4, 3), new TopicsRegistry[Message]).apply)
//    block.run(DataReady(Position(4, 3), CholeskyBlockMatrixType.aMN, new File("/tmp")))
//    block.run(DataReady(Position(4, 3), CholeskyBlockMatrixType.L21, new File("/tmp")))
//    block.run(DataReady(Position(4, 3), CholeskyBlockMatrixType.L11, new File("/tmp")))
//    block
//      .logEntries() shouldBe Seq(
//      CapturedLogEvent(Level.DEBUG, "aMN"),
//      CapturedLogEvent(Level.DEBUG, "L21 unstashing"),
//      CapturedLogEvent(Level.DEBUG, "L21 applied"),
//      CapturedLogEvent(Level.DEBUG, "L11 stashed"))
//  }
//
//  test("testL11SentToL21Applied") {
//    val block = BehaviorTestKit(new BlockSubdiagonal(Position(3, 2), new TopicsRegistry[Message]).apply)
//    block.run(DataReady(Position(3, 2), CholeskyBlockMatrixType.aMN, new File("/tmp")))
//    block.run(DataReady(Position(3, 2), CholeskyBlockMatrixType.L21, new File("/tmp")))
//    block.run(DataReady(Position(3, 2), CholeskyBlockMatrixType.L11, new File("/tmp")))
//    block
//      .logEntries() shouldBe Seq(
//      CapturedLogEvent(Level.DEBUG, "aMN"),
//      CapturedLogEvent(Level.DEBUG, "L21 unstashing"),
//      CapturedLogEvent(Level.DEBUG, "L21 applied"),
//      CapturedLogEvent(Level.DEBUG, "L11 applied"))
//  }
//
//  test("testL11SentToFirstColumn") {
//    val block = BehaviorTestKit(new BlockSubdiagonal(Position(3, 0), new TopicsRegistry[Message]).apply)
//    block.run(DataReady(Position(3, 0), CholeskyBlockMatrixType.aMN, new File("/tmp")))
//    block.run(DataReady(Position(3, 0), CholeskyBlockMatrixType.L11, new File("/tmp")))
//    block
//      .logEntries() shouldBe Seq(
//      CapturedLogEvent(Level.DEBUG, "aMN"),
//      CapturedLogEvent(Level.DEBUG, "L21 unstashing"),
//      CapturedLogEvent(Level.DEBUG, "L11 applied"))
//  }
//
//  //stashing
//  //1. L11 stashing
//  //2. l21 stashing
//  //when we send message with L11 shoud we put coordinates of the source or the destination?
//  //there should be coordingtes of expected L21 because now we only check the number of received
//  test("testL11StashingBeforeL21Received") {
//    val block = BehaviorTestKit(new BlockSubdiagonal(Position(3, 2), new TopicsRegistry[Message]).apply)
//    block.run(DataReady(Position(3, 2), CholeskyBlockMatrixType.aMN, new File("/tmp")))
//    block.run(DataReady(Position(3, 2), CholeskyBlockMatrixType.L11, new File("/tmp")))
//    block.run(DataReady(Position(3, 2), CholeskyBlockMatrixType.L21, new File("/tmp")))
//    block
//      .logEntries() shouldBe Seq(
//      CapturedLogEvent(Level.DEBUG, "aMN"),
//      CapturedLogEvent(Level.DEBUG, "L21 unstashing"),
//      CapturedLogEvent(Level.DEBUG, "L11 stashed"),
//      CapturedLogEvent(Level.DEBUG, "L21 applied"),
//      CapturedLogEvent(Level.DEBUG, "L11 applied"))
//  }
//
//  test("testL11StashingBeforeLastL21Received") {
//    val block = BehaviorTestKit(new BlockSubdiagonal(Position(4, 3), new TopicsRegistry[Message]).apply)
//    block.run(DataReady(Position(4, 3), CholeskyBlockMatrixType.aMN, new File("/tmp")))
//    block.run(DataReady(Position(4, 3), CholeskyBlockMatrixType.L21, new File("/tmp")))
//    block.run(DataReady(Position(4, 3), CholeskyBlockMatrixType.L11, new File("/tmp")))
//    block.run(DataReady(Position(4, 3), CholeskyBlockMatrixType.L21, new File("/tmp")))
//    block
//      .logEntries() shouldBe Seq(
//      CapturedLogEvent(Level.DEBUG, "aMN"),
//      CapturedLogEvent(Level.DEBUG, "L21 unstashing"),
//      CapturedLogEvent(Level.DEBUG, "L21 applied"),
//      CapturedLogEvent(Level.DEBUG, "L11 stashed"),
//      CapturedLogEvent(Level.DEBUG, "L21 applied"),
//      CapturedLogEvent(Level.DEBUG, "L11 applied"))
//  }

//  test("testL21StashingBefore_aMN_Received") {
//    val block = BehaviorTestKit(new BlockSubdiagonal(Position(4, 3), new TopicsRegistry[Message]).apply)
//    block.run(DataReady(Position(4, 3), CholeskyBlockMatrixType.L21, new File("/tmp")))
//    block.run(DataReady(Position(4, 3), CholeskyBlockMatrixType.L11, new File("/tmp")))
//    block.run(DataReady(Position(4, 3), CholeskyBlockMatrixType.L21, new File("/tmp")))
//    block.run(DataReady(Position(4, 3), CholeskyBlockMatrixType.aMN, new File("/tmp")))
//    block
//      .logEntries() shouldBe Seq(
//      CapturedLogEvent(Level.DEBUG, "L21 stashed"),
//      CapturedLogEvent(Level.DEBUG, "L11 stashed"),
//      CapturedLogEvent(Level.DEBUG, "L21 stashed"),
//      CapturedLogEvent(Level.DEBUG, "aMN"),
//      CapturedLogEvent(Level.DEBUG, "L21 unstashing"),
//      CapturedLogEvent(Level.DEBUG, "L21 applied"),
//      CapturedLogEvent(Level.DEBUG, "L11 stashed"),
//      CapturedLogEvent(Level.DEBUG, "L21 applied"),
//      CapturedLogEvent(Level.DEBUG, "L11 applied"))
//  }

}
