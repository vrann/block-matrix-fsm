package com.vrann

import akka.actor.typed.pubsub.Topic
import akka.actor.typed.pubsub.Topic.{Command, Publish}
import akka.actor.typed.scaladsl.Behaviors.same
import akka.actor.typed.scaladsl.{ActorContext, Behaviors, LoggerOps}
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import akka.stream.scaladsl.{FileIO, StreamRefs}
import akka.stream.{IOResult, SourceRef, StreamRefAttributes}
import akka.util.{ByteString, Timeout}
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.{DeserializationContext, JsonDeserializer}
import com.vrann.BlockMessage.DataReady
import com.vrann.cholesky.CholeskyBlockMatrixType._
import com.vrann.cholesky.FileLocator

import java.nio.file.{Path, Paths}
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

trait FileTransferMessage extends Message

final case class FileTransferReadyMessage(position: Position,
                                          matrixType: BlockMatrixType,
                                          sectionId: Int,
                                          fileName: String,
                                          ref: ActorRef[FileTransferMessage])
    extends FileTransferMessage
final case class FileTransferRequestMessage(position: Position,
                                            matrixType: BlockMatrixType,
                                            ref: ActorRef[FileTransferMessage])
    extends FileTransferMessage
final case class FileTransferResponseMessage(position: Position,
                                             matrixType: BlockMatrixType,
                                             fileRef: SourceRef[ByteString])
    extends FileTransferMessage

case class FileTransferTopicHolder(context: ActorContext[Message]) {
  val transferReadyTopic: ActorRef[Topic.Command[FileTransferReadyMessage]] =
    context.spawn(Topic[FileTransferReadyMessage]("data-ready-1-1"), "FileTransferReadyTopic")
  val transferResponseTopic: ActorRef[Topic.Command[FileTransferResponseMessage]] =
    context.spawn(Topic[FileTransferResponseMessage]("data-ready-1-1"), "FileTransferResponseTopic")
}

class BlockMatrixTypeDeserializer extends JsonDeserializer[BlockMatrixType] {
  override def deserialize(p: JsonParser, ctxt: DeserializationContext): BlockMatrixType = {
    p.getValueAsString match {
      case A11.jsonValue => A11
      case L11.jsonValue => L11
      case A22.jsonValue => A22
      case L21.jsonValue => L21
      case aMN.jsonValue => aMN
      case L12.jsonValue => L12
      case value         => throw new IllegalArgumentException(s"Undefined deserializer for value: $value")
    }
  }
}
@JsonDeserialize(using = classOf[BlockMatrixTypeDeserializer])
trait BlockMatrixType { val jsonValue: String }

object FileTransferTopicRegistry extends TopicsRegistry[FileTransferMessage]

case class FileTransfer(positions: List[Position],
                        positionsRemove: List[Position],
                        topicsRegistry: TopicsRegistry[Message],
                        sectionId: Int) {

  private val topicPatterns = List("data-ready")

  val topics: Map[String, Behavior[Command[Message]]] = {
    this.positions.foldLeft(Map.empty[String, Behavior[Command[Message]]])((map, position) => {
      var positionTopics = Map.empty[String, Behavior[Command[Message]]]
      topicPatterns.foreach(topicPattern => {
        val topicName = s"$topicPattern-$position"
        positionTopics = positionTopics + (topicName -> Topic[Message](topicName))
      })
      map ++ positionTopics
    })

    //proceed with removal
  }
  positionsRemove.foldLeft(Map.empty[String, Behavior[Command[Message]]])((map, position) => {
    var positionTopics = Map.empty[String, Behavior[Command[Message]]]
    topicPatterns.foreach(topicPattern => {
      val topicName = s"$topicPattern-$position"
      positionTopics = positionTopics + (topicName -> Topic[Message](topicName))
    })
    map ++ positionTopics
  })

  def apply: Behavior[Message] = Behaviors.receivePartial[Message] {
    case (context, FileTransferReadyMessage(position, matrixType, sectionId, fileName, ref)) =>
      context.log
        .debug(
          s"message FileTransferReady $matrixType received for position $position in section $sectionId in ${context.self}")

      if (!sectionId.equals(this.sectionId)) {
        val fileTransferRequest = FileTransferRequestMessage(position, matrixType, context.self)
        context.log.debug(s"FileTransferRequestMessage: $position, $matrixType")
        ref ! fileTransferRequest
      } else {
        topicsRegistry(s"matrix-$matrixType-ready-$position") ! Publish(
          DataReady(position, matrixType, FileLocator.getFileLocator(sectionId)(fileName), sectionId, context.self))
      }
      same
    case (context, FileTransferRequestMessage(position, matrixType, ref)) =>
      val filePath: Path = Paths.get(FileLocator.getFileLocator(position, matrixType, sectionId).getAbsolutePath)
      context.log.debug(s"Received request for file $filePath in ${context.self}")

      implicit val system: ActorSystem[Nothing] = context.system
      context.log.debug(s"system: $system")
      val fileRef: SourceRef[ByteString] =
        FileIO
          .fromPath(filePath)
          .log("error logging")
          .runWith(
            StreamRefs
              .sourceRef()
              .addAttributes(StreamRefAttributes.subscriptionTimeout(5.minutes)))
      context.log.debug(s"File Ref: $ref")
      val fileTransferMessage =
        FileTransferResponseMessage(position, matrixType, fileRef)
      context.log.debug(s"Sending response file $fileTransferMessage to $ref")
      ref ! fileTransferMessage
      same
    case (context, message @ FileTransferResponseMessage(position, matrixType, fileRef)) =>
      //context.log.info(s"Received file $matrixType $position")
      val filePath: Path = Paths.get(FileLocator.getFileLocator(position, matrixType, sectionId).getAbsolutePath)
      if (filePath.toFile.exists()) {
        same
      } else {
        filePath.toFile.createNewFile()
        implicit val system: ActorSystem[Nothing] = context.system
        implicit val timeout = Timeout(3600 seconds)

        val future: Future[IOResult] = fileRef
          .runWith(FileIO.toPath(filePath))
        Await.result(future, timeout.duration)

        context.log.info("File is written to path {}", filePath)
//        val realL11 = MatrixReader.readMatrix(new DataInputStream(new FileInputStream(filePath.toFile)))
//        context.log.debug(
//          s"Read write ${matrixType} @ $position - $filePath: ${realL11.numCols} ${realL11.numRows} $realL11")
        context.log.info2(
          "Sending messages to local actors {} {}",
          s"matrix-${matrixType}-section${sectionId}-ready-$position",
          DataReady(position, matrixType, filePath.toFile, sectionId, context.self))
        topicsRegistry(s"matrix-${matrixType}-section${sectionId}-ready-$position") ! Publish(
          DataReady(position, matrixType, filePath.toFile, sectionId, context.self))
        same

      }
    case _ =>
      same
  }
}
