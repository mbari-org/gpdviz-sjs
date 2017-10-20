package gpdviz.server

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import gpdviz.async._
import gpdviz.config.{cfg, configFile}
import gpdviz.data.{DbFactory, DbInterface}

import scala.concurrent.ExecutionContextExecutor
import scala.io.StdIn

object GpdvizServer {
  def main(args: Array[String]) {
    if (args.contains("generate-conf")) {
      generateConf(args)
    }
    else if (args.contains("run")) {
      if (!configFile.canRead) {
        System.err.println(s"cannot access $configFile")
      }
      else new GpdvizServer().run(!args.contains("-d"))
    }
    else {
      System.err.println(s"""
        |Usage:
        |   gpdviz generate-conf [--overwrite]
        |   gpdviz run [-d]
        """.stripMargin)
    }
  }

  private def generateConf(args: Array[String]): Unit = {
    if (configFile.exists() && !args.contains("--overwrite")) {
      System.err.println(s"$configFile exists.  Use --overwrite to overwrite")
    }
    else {
      val conf = scala.io.Source.fromInputStream(
        getClass.getClassLoader.getResource("params_template.conf").openStream()
      ).mkString
      import java.nio.charset.StandardCharsets
      import java.nio.file.Files
      val bytes = conf.getBytes(StandardCharsets.UTF_8)
      Files.write(configFile.toPath, bytes)
      println(s" Configuration generated: $configFile\n")
    }
  }
}

class GpdvizServer extends GpdvizService {
  val db: DbInterface = DbFactory.db

  DbFactory.initStuff(db)

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
