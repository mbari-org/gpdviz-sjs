package gpdviz.server

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import gpdviz.async.Notifier
import gpdviz.config.cfg
import gpdviz.data.{DbInterface, FileDb, PostgresDb}

import scala.io.StdIn

object GpdvizServer extends GpdvizService {
  val db: DbInterface = cfg.postgres match {
    case None     ⇒ new FileDb("data")
    case Some(pg) ⇒ new PostgresDb(pg)
  }
  val notifier: Notifier = new Notifier

  def main(args: Array[String]) {
    implicit val system = ActorSystem()
    implicit val materializer = ActorMaterializer()
    // needed for the future flatMap/onComplete in the end
    implicit val executionContext = system.dispatcher

    println(s"Gpdviz using: ${db.details}")

    val bindingFuture = Http().bindAndHandle(routes, cfg.httpInterface, cfg.httpPort)

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
