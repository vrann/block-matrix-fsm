package com.vrann

import akka.actor.typed._
import akka.actor.typed.scaladsl.ActorContext
import akka.actor.typed.scaladsl.AskPattern.Askable
import akka.actor.typed.scaladsl.Behaviors._
import akka.cluster.ClusterEvent.ClusterDomainEvent
import akka.cluster.typed.{Cluster, ClusterSingleton}
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.StatusCodes.{InternalServerError, OK}
import akka.http.scaladsl.model.headers.`Access-Control-Allow-Origin`
import akka.http.scaladsl.model.{ContentTypes, HttpCharsets, MediaTypes}
import akka.http.scaladsl.server.{Directives, Route}
import akka.management.cluster.bootstrap.ClusterBootstrap
import akka.management.scaladsl.AkkaManagement
import akka.util.Timeout
import com.typesafe.config._
import com.vrann.positioned.{GetCurrentAssignment, Response}
import spray.json.{RootJsonFormat, _}

import java.io.File
import java.nio.file.{Path, Paths}
import scala.concurrent.Future
import scala.concurrent.duration.DurationInt
import scala.util.{Failure, Success}

trait Message

case object TestMessage extends Message

case class SubscribeTestProbe(ref: ActorRef[FileTransferMessage]) extends Message

object RootBehavior {

  def behavior: Behavior[Message] = setup[Message] { context: ActorContext[Message] =>
    val cluster = Cluster(context.system)
    val singletonManager = ClusterSingleton(context.system)
    implicit val system = context.system
    implicit val scheduler = context.system.scheduler

    val config = ConfigFactory.load()
    val port = config.getConfig("web")

//      val positionAssignment: ActorRef[ClusterDomainEvent] = singletonManager.init(
//        SingletonActor(
//          supervise(PositionAssignment
//            .apply(100, 100))
//            .onFailure[Exception](SupervisorStrategy.resume),
//          "PositionAssignment"))

    val sectionId = config.getInt("section")

    val getFileLocator: Integer => String => File = (a: Integer) =>
      (fileName: String) => {
        def foo(fileName: String): File = {
          new File(
            classOf[App].getClassLoader
              .getResource(String.format("choreography/section%d", a))
              .getPath + "/" + fileName)
        }
        foo(fileName)
    }

    val file = getFileLocator(sectionId)("section.conf")
    val sectionConfig: Config =
      ConfigFactory.parseFile(file, ConfigParseOptions.defaults.setSyntax(ConfigSyntax.CONF))

    var positions = List[Position]()
    val keys = sectionConfig
      .getConfigList("actors.matrix-blocks")
      .iterator()
      .forEachRemaining(a => positions = positions :+ Position(a.getInt("x"), a.getInt("y")))
    val topicRegistry = new TopicsRegistry[Message]

    val sectionActor =
      context.spawn(
        Section(cluster.selfMember.uniqueAddress.toString, positions, topicRegistry, new FileLocatorIdentity()).behavior,
        "Section")

    val startPos = List(
      Position(0, 0),
      Position(1, 0),
      Position(1, 1),
      Position(2, 2),
      Position(2, 0),
      Position(2, 1),
      Position(3, 0),
      Position(3, 1),
      Position(3, 2),
      Position(3, 3),
      Position(4, 0),
      Position(4, 1),
      Position(4, 2),
      Position(4, 3),
      Position(4, 4),
      Position(5, 0),
      Position(5, 1),
      Position(5, 2),
      Position(5, 3),
      Position(5, 4),
      Position(5, 5))

    context
      .spawn(
        HttpServer("0.0.0.0", port.getInt("port"), new InitRoute(sectionActor, startPos).route, context.system),
        "http")

//      singletonManager.init(
//        SingletonActor(
//          supervise(
//            HttpServer(
//              "0.0.0.0",
//              port.getInt("port"),
//              new BlockMatrixService(positionAssignment).route,
//              context.system))
//            .onFailure[Exception](SupervisorStrategy.resume),
//          "HttpServer"))

    receiveMessage[Message] {
      case message =>
        sectionActor ! message
        same
    }
  }

  class FileLocatorTmp extends FileLocator {
    def apply(fileName: String): Path = {
      val pathBuilder = (new StringBuilder)
        .append("/tmp/")
        .append(fileName)
      Paths.get(pathBuilder.toString)
    }
  }

  class FileLocatorIdentity extends FileLocator {
    def apply(fileName: String): Path = {
      Paths.get(fileName)
    }
  }
}

trait JsonSupport extends SprayJsonSupport with DefaultJsonProtocol {
  implicit val positionFormat: RootJsonFormat[Position] = jsonFormat2(Position)
}

class InitRoute(sectionActor: ActorRef[Message], positions: List[Position]) extends Directives with JsonSupport {
  val route: Route = concat(path("api" / "v1" / "init") {

    sectionActor ! Init(positions)

    complete(OK, List(`Access-Control-Allow-Origin`.`*`), "OK")
  }, path("api" / "v1" / "start") {
    sectionActor ! Start
    complete(OK, List(`Access-Control-Allow-Origin`.`*`), "OK")
  })
}

class BlockMatrixService(positionAssignment: ActorRef[ClusterDomainEvent])(implicit
                                                                           scheduler: Scheduler)
    extends Directives
    with JsonSupport {

  implicit val timeout: Timeout = 3.seconds
  val route: Route =
    concat(
      path("api" / "v1" / "sections") { //requestContext =>
        val assignment: Future[Response] =
          positionAssignment.ask(GetCurrentAssignment(_))
        onComplete(assignment) {
          case Success(value) => complete(OK, List(`Access-Control-Allow-Origin`.`*`), value.positionMatrix)
          case Failure(ex)    => complete(InternalServerError, s"An error occurred: ${ex.getMessage}")
        }
      },
      pathSingleSlash {
        getFromResource("static/dist/index.html", ContentTypes.`text/html(UTF-8)`)
      },
      path("index.html") { getFromResource("static/dist/index.html", ContentTypes.`text/html(UTF-8)`) },
      path("main.js") {
        getFromResource("static/dist/main.js", MediaTypes.`application/javascript` withCharset HttpCharsets.`UTF-8`)
      })
}

object App {
  def main(args: Array[String]): Unit = {

    implicit val system =
      ActorSystem(RootBehavior.behavior, "actormatrix")

    // Akka Management hosts the HTTP routes used by bootstrap
    AkkaManagement(system).start()
    // Starting the bootstrap process needs to be done explicitly
    ClusterBootstrap(system).start()
  }
}
