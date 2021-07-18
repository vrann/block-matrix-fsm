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
