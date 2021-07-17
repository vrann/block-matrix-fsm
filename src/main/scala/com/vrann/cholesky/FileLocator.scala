package com.vrann.cholesky

import com.vrann.cholesky.CholeskyBlockMatrixType.L11
import com.vrann.{BlockMatrixType, Position}

import java.io.File

object FileLocator {
  def getFileLocator(position: Position, matrixType: BlockMatrixType, sectionId: Integer): File = {
    val filePart = matrixType match {
      case aMN => "a"
      case L11 => "l11"
    }
    val fileName = s"$filePart${position.x}${position.y}.mtrx"
    new File(
      classOf[App].getClassLoader
        .getResource(String.format("choreography/section%d", sectionId))
        .getPath + "/" + fileName)
  }
}
