package com.vrann.positioned

class PositionMatrixTest extends org.scalatest.FunSuite {

  test("testAddPart") {
    val assignment = new PositionMatrix(100, 100, List(1, 2, 3, 4, 5, 6, 7, 8, 9))
//    val firstSection = assignment.sections(0)
//    val secondSection = assignment.sections(1)
    print(assignment.sections)
  }

  test("testUnassign") {}

  test("testApply") {}

}
