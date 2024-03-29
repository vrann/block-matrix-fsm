package com.vrann.cholesky

import akka.actor.typed.scaladsl.Behaviors.setup
import akka.actor.typed.scaladsl.{ActorContext, StashBuffer}
import akka.actor.typed.{ActorRef, Behavior}
import com.github.fommil.netlib.BLAS
import com.typesafe.config.ConfigFactory
import com.vrann.BlockMessage.{DataReady, Delegate}
import com.vrann.cholesky.CholeskyBlockMatrixType.{aMN, L11}
import com.vrann._
import org.apache.spark.ml.linalg.DenseMatrix

import java.io.{DataInputStream, File, FileInputStream}

trait L21Operation {
  def applyL21(position: Position,
               expectedL21: Int,
               message: DataReady,
               processedL21: List[Position],
               buffer: StashBuffer[Message],
               filePath: File,
               section: ActorRef[Message],
               state: (File, State, List[Position], StashBuffer[Message]) => Behavior[Message],
               sectionId: Int,
               fileTransferActor: ActorRef[Message]): Behavior[Message] =
    setup { context =>
      context.log.info(s"L21 from ${message.pos} applied at $position, file ${message.filePath}")
      val newProcessed = processedL21 :+ message.pos

      val l21 = MatrixReader.readMatrix(new DataInputStream(new FileInputStream(message.filePath)))
      val a21 = MatrixReader.readMatrix(new DataInputStream(new FileInputStream(filePath)))

      val filePathOut = FileLocator.getFileLocator(position, aMN, sectionId)
      context.log.debug(s"Writing A22 to {}", filePathOut.getAbsolutePath)
      val A22 = L21toAMN(l21, a21)
      val writer = UnformattedMatrixWriter.ofFile(filePathOut)
      writer.writeMatrix(A22)

      if (newProcessed.size == expectedL21) {
        context.log.info(s"Processed all L21s for $position: $newProcessed")
        if (position.x == position.y) {
          context.log.debug(s"Factorize next $position")
          factorize(position, newProcessed, buffer, filePath, section, state, sectionId, fileTransferActor)
        } else {
          context.log.info(s"Unstash all, L11 at $position")
          buffer.unstashAll(state(filePath, L21Applied, newProcessed, buffer))
        }
      } else {
        state(filePath, SomeL21Applied, newProcessed, buffer)
      }
    }

  def factorize(position: Position,
                processedL21: List[Position],
                buffer: StashBuffer[Message],
                filePath: File,
                section: ActorRef[Message],
                state: (File, State, List[Position], StashBuffer[Message]) => Behavior[Message],
                sectionId: Int,
                fileTransferActor: ActorRef[Message]): Behavior[Message] =
    setup { context =>
      val config = ConfigFactory.load()
      val sectionId = config.getInt("section")
      val file = FileLocator.getFileLocator(position, L11, sectionId)
      context.log.debug(s"Factorized matrix located at ${file.getAbsolutePath}")
      processA11(filePath, file, context)
      context.log.info(s"Factorized $position")
      //if (topicsRegistry.hasTopic(s"matrix-L11-ready-$position")) {
      context.log.info(s"Publishing matrix-L11-ready-$position")
      section !
        Delegate(position, L11, DataReady(position, L11, file.getAbsoluteFile, sectionId, fileTransferActor))
      context.log.info(s"Done! $position ${System.currentTimeMillis()}")
//      } else {
//        context.log.debug(s"subscriber is not found for matrix-L11-ready-$position")
//        context.log.info(s"Process is Done after $position factorized")
//      }
      state(filePath, Done, processedL21, buffer)
    }

  private def processA11(filePath: File, filePathOut: File, context: ActorContext[Message]) = {
    val a11 = MatrixReader.readMatrix(new DataInputStream(new FileInputStream(filePath)))
    val l11 = Factorization.apply(a11)
    context.log.debug(s"L11: ${filePathOut}")
    context.log.debug(s"L11: ${l11.numCols} ${l11.numRows} $l11")
    val writer = UnformattedMatrixWriter.ofFile(filePathOut)
    writer.writeMatrix(l11)
  }

  def L21toAMN(L21: DenseMatrix, A22: DenseMatrix): DenseMatrix = {
    val L22 = A22.toArray
    BLAS.getInstance.dsyrk("L", "N", A22.numRows, A22.numCols, -1, L21.values, L21.numCols, 1, L22, A22.numRows)
    new DenseMatrix(A22.numRows, A22.numCols, L22)
  }
}
