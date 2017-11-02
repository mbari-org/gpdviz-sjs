package gpdviz.server

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import gpdviz.async._
import gpdviz.config.cfg
import gpdviz.data.{DbFactory, DbInterface}

import scala.concurrent.{ExecutionContext, ExecutionContextExecutor}
import scala.io.StdIn

class GpdvizServer extends GpdvizService {
  val dbFactory: DbFactory = new DbFactory()(ExecutionContext.global)
  val db: DbInterface = dbFactory.openDb

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

  val notifier = new Notifier(db, publisher)(ExecutionContext.global)

  def run(keyToStop: Boolean): Unit = {
    println(s"Gpdviz ${cfg.gpdviz.version} using: DB: ${db.details}  Async Notifications: ${publisher.details}")
    val bindingFuture = Http().bindAndHandle(route, cfg.httpInterface, cfg.httpPort)
    println(s"Gpdviz server '${cfg.serverName}' online at ${cfg.httpInterface}:${cfg.httpPort}/")
    if (keyToStop) {
      println("Press RETURN to stop...")
      StdIn.readLine()
      // trigger unbinding from the port and shutdown when done
      bindingFuture
        .flatMap(_.unbind())
        .onComplete { _ ⇒
          db.close()
          system.terminate()
        }
    }
  }
}
