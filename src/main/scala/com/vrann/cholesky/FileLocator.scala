package com.vrann.cholesky

import com.vrann.cholesky.CholeskyBlockMatrixType.{aMN, A11, A22, L11, L21}
import com.vrann.{BlockMatrixType, Position}

import java.io.File

object FileLocator {
  def getFileLocator(position: Position, matrixType: BlockMatrixType, sectionId: Integer): File = {

    def matchMatrixType(x: BlockMatrixType): String = x match {
      case _: aMN.type => "a"
      case _: A11.type => "a11"
      case _: A22.type => "a22"
      case _: L11.type => "l11"
      case _: L21.type => "l21"
      case _           => "other"
    }
//
//    if (matrixType.getClass == aMN)
//      println(s"$matrixType => ${matchMatrixType(matrixType)}")
    val fileName = s"${matchMatrixType(matrixType)}${position.x}${position.y}.mtrx"
    getFileLocator(sectionId)(fileName)
  }

  def getFileLocator(sectionId: Integer): String => File = {
    def getFile(fileName: String): File = {
      val pathBuilder = (new StringBuilder)
        .append(System.getProperty("user.home"))
        .append(String.format("/.actorchoreography/section%d/", sectionId))
        .append(fileName)
      new File(pathBuilder.toString())
    }
    getFile
  }
}
