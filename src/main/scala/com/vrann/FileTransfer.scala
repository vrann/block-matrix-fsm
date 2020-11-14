package com.vrann

import java.nio.file.{Path, Paths}

import akka.actor.typed.pubsub.Topic
import akka.actor.typed.scaladsl.Behaviors.same
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import akka.stream.scaladsl.{FileIO, StreamRefs}
import akka.stream.{SourceRef, StreamRefAttributes}
import akka.util.ByteString
import com.vrann.FileTransferMessage.{
  FileTransferReadyMessage,
  FileTransferRequestMessage,
  FileTransferResponseMessage
}

import scala.concurrent.duration._

sealed trait FileTransferMessage extends Message

object FileTransferMessage extends Message {
  final case class FileTransferReadyMessage(position: Position,
                                            matrixType: BlockMatrixType,
                                            sectionId: Int,
                                            fileName: String,
                                            ref: ActorRef[FileTransferMessage])
      extends FileTransferMessage
  final case class FileTransferRequestMessage(
    position: Position,
    matrixType: BlockMatrixType,
    sectionId: Int,
    fileName: String,
    ref: ActorRef[FileTransferMessage]
  ) extends FileTransferMessage
  final case class FileTransferResponseMessage(position: Position,
                                               matrixType: BlockMatrixType,
                                               fileName: String,
                                               fileRef: SourceRef[ByteString])
      extends FileTransferMessage
}

case class FileTransferTopicHolder(context: ActorContext[Message]) {
  val transferReadyTopic: ActorRef[Topic.Command[FileTransferReadyMessage]] =
    context.spawn(
      Topic[FileTransferReadyMessage]("data-ready-1-1"),
      "FileTransferReadyTopic"
    )
//  val transferRequestTopic
//                        : ActorRef[Topic.Command[FileTransferRequestMessage]] = context.spawn(
//    Topic[FileTransferRequestMessage]("file-transfer-request-%s-%d-%s-%s"),
//    fileName, sourceSectionId, position, matrixType
//    "FileTransferReady11"
//  )
  val transferResponseTopic
    : ActorRef[Topic.Command[FileTransferResponseMessage]] = context.spawn(
    Topic[FileTransferResponseMessage]("data-ready-1-1"),
    "FileTransferResponseTopic"
  )
}

trait BlockMatrixType

trait FileLocator {
  def apply(fileName: String): Path
}

class FileLocatorDefault extends FileLocator {
  override def equals(obj: Any): Boolean =
    super.equals(obj.getClass == this.getClass)

  def apply(fileName: String): Path = {
    val pathBuilder = (new StringBuilder)
      .append(System.getProperty("user.home"))
      .append("/.actorchoreography/")
      .append(fileName)
    Paths.get(pathBuilder.toString)
  }
}

case class FileTransfer(fileLocator: FileLocator) {
  def apply(): Behavior[FileTransferMessage] = init()

  private def init() =
    Behaviors.receivePartial[FileTransferMessage] {
      case (
          context,
          FileTransferReadyMessage(
            position,
            matrixType,
            sectionId,
            fileName,
            ref
          )
          ) =>
        context.log
          .info(
            s"message FileTransferReady $matrixType received for position $position in section $sectionId"
          )

        if (!ref.equals(context.self)) {
          val fileTransferRequest = FileTransferRequestMessage(
            position,
            matrixType,
            sectionId,
            fileName,
            context.self
          )
          context.log.info(
            s"FileTransferRequestMessage: $position, $matrixType"
          )
          ref ! fileTransferRequest
        } else {
          /*val file = fileLocator.getMatrixBlockFilePath(message.getFileName)
          if (!file.exists) {
            context.log.error(
              "File for the matrix block is not found {}",
              file.getAbsolutePath
            )
            throw new IOException("File for the matrix block is not found")
          }
          return String.format ( "section-data-loaded-%s-%d-%d", matrixType, pos.getX, pos.getY )
          val resultMessage = new BlockMatrixDataLoaded.Builder()
            .setBlockMatrixType(message.getMatrixType)
            .setFilePath(file)
            .setPosition(message.getPosition)
            .setSectionId(currentSectionId)
            .build
          context.log.info(
            "File exists. Notification about available file is sent {}",
            resultMessage.getTopic
          )
          mediator.tell(
            new DistributedPubSubMediator.Publish(
              resultMessage.getTopic,
              resultMessage
            ),
            selfReference
          )*/
        }
        //        handleBlockTransitions(a, Initialized)
        same
      case (
          context,
          FileTransferRequestMessage(
            position,
            matrixType,
            sectionId,
            fileName,
            ref
          )
          ) =>
        //        handleBlockTransitions(a, Initialized)

        context.log.info(s"Received request for file $fileName")
        val filePath = fileLocator(fileName)
        context.log.info(s"Path: $filePath")
        implicit val system: ActorSystem[Nothing] = context.system
        context.log.info(s"system: $system")
        val fileRef: SourceRef[ByteString] =
          FileIO
            .fromPath(filePath)
            .log("error logging")
            .runWith(
              StreamRefs
                .sourceRef()
                .addAttributes(
                  StreamRefAttributes.subscriptionTimeout(5.minutes)
                )
            )
        context.log.info(s"File Ref: $ref")
        val fileTransferMessage =
          FileTransferResponseMessage(position, matrixType, fileName, fileRef)
        context.log.info(s"Sending response file $fileTransferMessage")
        ref ! fileTransferMessage
        same
      case (
          context,
          FileTransferResponseMessage(position, matrixType, fileName, fileRef)
          ) =>
        context.log.info("Received file {}", fileName)
        val filePath: Path =
          fileLocator(fileName)
        implicit val system: ActorSystem[Nothing] = context.system
        fileRef.runWith(FileIO.toPath(filePath))
        context.log.info("File is written to path {}", filePath)

//        val resultMessage = new BlockMatrixDataAvailable.Builder()
//          .setBlockMatrixType(message.getMatrixType)
//          .setFilePath(file)
//          .setPosition(message.getPosition)
//          .setSectionId(currentSectionId)
//          .build
//
//        context.log.info(
//          "Matrix data is written to file. Notification about available file is sent {}",
//          resultMessage.getTopic
//        )
//        mediator.tell(
//          new DistributedPubSubMediator.Publish(
//            resultMessage.getTopic,
//            resultMessage
//          ),
//          selfReference
//        )
        //        handleBlockTransitions(a, Initialized)
        same
      case _ =>
        same
    }
}
