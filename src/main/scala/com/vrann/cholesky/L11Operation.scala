package com.vrann.cholesky

import akka.actor.typed.pubsub.Topic.Publish
import akka.actor.typed.scaladsl.Behaviors.setup
import akka.actor.typed.scaladsl.StashBuffer
import akka.actor.typed.{ActorRef, Behavior}
import com.github.fommil.netlib.BLAS
import com.vrann.BlockMessage.DataReady
import com.vrann.cholesky.CholeskyBlockMatrixType.L21
import com.vrann._
import org.apache.spark.ml.linalg.DenseMatrix

import java.io.{DataInputStream, File, FileInputStream}

trait L11Operation {
  def applyL11(position: Position,
               message: DataReady,
               processedL21: List[Position],
               buffer: StashBuffer[Message],
               filePath: File,
               topicsRegistry: TopicsRegistry[Message],
               state: (File, State, List[Position], StashBuffer[Message]) => Behavior[Message],
               sectionId: Int,
               fileTransferActor: ActorRef[Message]): Behavior[Message] = setup { context =>
    context.log.debug(s"L11 from ${message.pos} applied at $position")
    val l11 = MatrixReader.readMatrix(new DataInputStream(new FileInputStream(message.filePath)))
    context.log.debug(s"L11 filepath: ${message.filePath} \n L11: ${l11.numCols} ${l11.numRows} ${l11}")

    val a21 = MatrixReader.readMatrix(new DataInputStream(new FileInputStream(filePath)))
    val filePathOut = FileLocator.getFileLocator(position, L21, sectionId)
    context.log.debug(s"Writing to {}", filePathOut.getAbsolutePath)

    val l21 = L11toL21(l11, a21)

    val writer = UnformattedMatrixWriter.ofFile(filePathOut)
    writer.writeMatrix(l21)
    val messageBody = DataReady(position, L21, filePathOut.getAbsoluteFile, sectionId, fileTransferActor)
    context.log.debug(s"Publishing {}", message)
    topicsRegistry(s"matrix-${L21}-ready-$position") ! Publish(messageBody)
    context.log.info(s"Done $position ${System.currentTimeMillis()}")
    state(filePath, Done, processedL21, buffer)
  }

  def L11toL21(L11: DenseMatrix, A21: DenseMatrix): DenseMatrix = {
    val L21 = A21.toArray
    BLAS.getInstance.dtrsm(
      "R",
      "L",
      "T",
      "N",
      L11.numRows,
      L11.numCols,
      1.0,
      L11.toArray,
      L11.numCols,
      L21,
      A21.numRows)
    new DenseMatrix(A21.numRows, A21.numCols, L21)
  }

}
