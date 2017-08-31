package gpdviz.server

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import gpdviz.async._
import gpdviz.config.cfg
import gpdviz.data.{DbInterface, FileDb, MongoDb, PostgresDb}

import scala.concurrent.ExecutionContextExecutor
import scala.io.StdIn

object GpdvizServer extends GpdvizService {
//  val db: DbInterface = new FileDb("data")
  val db: DbInterface = cfg.mongo.map(new MongoDb(_))
    .getOrElse(cfg.postgres.map(new PostgresDb(_))
    .getOrElse(new FileDb("data")))

  implicit val system: ActorSystem = ActorSystem()
  implicit val executionContext: ExecutionContextExecutor = system.dispatcher
  implicit val materializer: ActorMaterializer = ActorMaterializer()

  val (publisher, route) = cfg.pusher match {
    case Some(pc) ⇒
      (new PusherPublisher(pc), routes)

    case None     ⇒
      val wsp = new WebSocketPublisher
      val wsRoute = pathPrefix("ws" / Segment) { sysid ⇒
        handleWebSocketMessages(wsp.wsHandler(sysid))
      }
      (wsp, routes ~ wsRoute)
  }

  val notifier = new Notifier(publisher)

  def main(args: Array[String]) {
    println(s"Gpdviz using: DB: ${db.details}  Async Notifications: ${publisher.details}")

    val bindingFuture = Http().bindAndHandle(route, cfg.httpInterface, cfg.httpPort)

    println(s"Gpdviz server '${cfg.serverName}' online at ${cfg.httpInterface}:${cfg.httpPort}/")
    if (!args.contains("-d")) {
      println("Press RETURN to stop...")
      StdIn.readLine()
      bindingFuture
        .flatMap(_.unbind()) // trigger unbinding from the port
        .onComplete(_ ⇒ system.terminate()) // and shutdown when done
    }
  }
}
