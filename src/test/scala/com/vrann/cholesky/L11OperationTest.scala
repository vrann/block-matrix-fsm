package com.vrann.cholesky

import org.apache.spark.ml.linalg.DenseMatrix

class Mock extends L11Operation {}

class L11OperationTest extends org.scalatest.FunSuite {

  test("testL11toL21") {
    val m = new Mock()
    val L11 = new DenseMatrix(3, 3, Array[Double](2.0, 6.0, -8.0, 0.0, 1.0, 5.0, 0.0, 0.0, 3.0))
    val A21 = new DenseMatrix(3, 3, Array[Double](8.0, 22.0, -19.0, 21.0, 13.0, 9.0, -6.0, -1.0, 1.0))
    val expected = new DenseMatrix(
      3,
      3,
      Array[Double](4.0, 11.0, -9.5, -3.0, -53.0, 66.0, 13.666666666666666, 117.33333333333333, -135.0))
    val L21 = m.L11toL21(L11, A21)
    print(L21)
  }

}
