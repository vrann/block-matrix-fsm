package com.vrann.cholesky

import org.apache.spark.ml.linalg.DenseMatrix

import java.io.DataInputStream
import java.nio.{ByteBuffer, ByteOrder}

object MatrixReader {
  def readMatrix(input: DataInputStream): DenseMatrix = { // read the number of rows
    val bufLengthInt = new Array[Byte](4)
    input.read(bufLengthInt)
    val numRows = ByteBuffer.wrap(bufLengthInt).order(ByteOrder.LITTLE_ENDIAN).getInt
    //read the number of columns
    val bufWidthInt = new Array[Byte](4)
    input.read(bufWidthInt)
    val numCols = ByteBuffer.wrap(bufWidthInt).order(ByteOrder.LITTLE_ENDIAN).getInt
    //read the data
    val ibufDouble = new Array[Byte](8)
    val matrixData = new Array[Double](input.available / 8)
    var i = 0
    while ({
      input.read(ibufDouble) != -1
    }) {
      val value = ByteBuffer.wrap(ibufDouble).order(ByteOrder.LITTLE_ENDIAN).getDouble
      matrixData(i) = value
      i += 1
    }
    input.close()
    new DenseMatrix(numRows, numCols, matrixData)
  }
}
