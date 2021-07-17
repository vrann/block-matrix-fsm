package com.vrann

import java.nio.file.{Path, Paths}
import akka.actor.typed.pubsub.Topic
import akka.actor.typed.pubsub.Topic.{Command, Publish}
import akka.actor.typed.scaladsl.Behaviors.same
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import akka.stream.scaladsl.{FileIO, StreamRefs}
import akka.stream.{SourceRef, StreamRefAttributes}
import akka.util.ByteString
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.{DeserializationContext, JsonDeserializer}
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.vrann.BlockMessage.DataReady
import com.vrann.cholesky.CholeskyBlockMatrixType.{aMN, A11, A22, L11, L12, L21}
import com.vrann.cholesky.FileLocator

import scala.concurrent.duration._

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

object FileTransferTopicRegistry extends TopicsRegistry[FileTransferMessage]

case class FileTransfer(fileLocator: FileLocator,
                        positions: List[Position],
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
        .info(
          s"message FileTransferReady $matrixType received for position $position in section $sectionId in ${context.self}")

      if (!sectionId.equals(this.sectionId)) {
        val fileTransferRequest = FileTransferRequestMessage(position, matrixType, context.self)
        context.log.info(s"FileTransferRequestMessage: $position, $matrixType")
        ref ! fileTransferRequest
      } else {
        topicsRegistry(s"matrix-$matrixType-ready-$position") ! Publish(
          DataReady(position, matrixType, fileLocator(fileName).toFile, sectionId, context.self))
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
    case (context, FileTransferRequestMessage(position, matrixType, ref)) =>
      val filePath: Path = Paths.get(FileLocator.getFileLocator(position, matrixType, sectionId).getAbsolutePath)
      context.log.info(s"Received request for file $filePath in ${context.self}")

      implicit val system: ActorSystem[Nothing] = context.system
      context.log.info(s"system: $system")
      val fileRef: SourceRef[ByteString] =
        FileIO
          .fromPath(filePath)
          .log("error logging")
          .runWith(
            StreamRefs
              .sourceRef()
              .addAttributes(StreamRefAttributes.subscriptionTimeout(5.minutes)))
      context.log.info(s"File Ref: $ref")
      val fileTransferMessage =
        FileTransferResponseMessage(position, matrixType, fileRef)
      context.log.info(s"Sending response file $fileTransferMessage to $ref")
      ref ! fileTransferMessage
      same
    case (context, message @ FileTransferResponseMessage(position, matrixType, fileRef)) =>
      context.log.info(s"Received file $message in ${context.self}")
      val filePath: Path = Paths.get(FileLocator.getFileLocator(position, matrixType, sectionId).getAbsolutePath)
      implicit val system: ActorSystem[Nothing] = context.system
      fileRef.runWith(FileIO.toPath(filePath))
      context.log.info("File is written to path {}", filePath)
      topicsRegistry(s"matrix-$matrixType-ready-$position") ! Publish(
        DataReady(position, matrixType, filePath.toFile, sectionId, context.self))
      same
    case _ =>
      same
  }
}
