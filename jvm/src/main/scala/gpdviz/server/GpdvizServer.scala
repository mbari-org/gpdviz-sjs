package gpdviz.server

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import gpdviz.async.{PusherNotifier, WebSocketNotifier}
import gpdviz.config.cfg
import gpdviz.data.{DbInterface, FileDb, PostgresDb}

import scala.concurrent.ExecutionContextExecutor
import scala.io.StdIn

object GpdvizServer extends GpdvizService {
  val db: DbInterface = cfg.postgres match {
    case None     ⇒ new FileDb("data")
    case Some(pg) ⇒ new PostgresDb(pg)
  }

  implicit val system: ActorSystem = ActorSystem()
  implicit val materializer: ActorMaterializer = ActorMaterializer()

  val (notifier, route) = cfg.pusher match {
    case Some(pc) ⇒
      (new PusherNotifier(pc), routes)

    case None     ⇒
      val wsn = new WebSocketNotifier
      val wsRoute = path("ws") {
        handleWebSocketMessages(wsn.wsHandler)
      }
      (wsn, routes ~ wsRoute)
  }

  def main(args: Array[String]) {
    // needed for the future flatMap/onComplete in the end
    implicit val executionContext: ExecutionContextExecutor = system.dispatcher

    println(s"Gpdviz using: DB: ${db.details}  Async Notifications: ${notifier.details}")

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
