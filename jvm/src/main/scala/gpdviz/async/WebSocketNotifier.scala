package gpdviz.async

import akka.NotUsed
import akka.actor.{ActorRef, ActorSystem, Props}
import akka.http.scaladsl.model.ws.TextMessage
import akka.stream.ActorMaterializer
import akka.stream.actor.ActorPublisher
import akka.stream.scaladsl.{Flow, Sink, Source}
import gpdviz._
import gpdviz.model._
import gpdviz.server.JsonImplicits
import spray.json._

import scala.annotation.tailrec
import scala.concurrent.{ExecutionContext, ExecutionContextExecutor}

// Some refs:
// - https://stackoverflow.com/a/35313963/830737
// - https://groups.google.com/d/msg/akka-user/aA7RD2On_K0/6SJDgOPpAAAJ

class MyPublisher extends ActorPublisher[Notif] {
  override def preStart: Unit = context.system.eventStream.subscribe(self, classOf[Notif])

  override def receive: Receive = {
    case msg: Notif ⇒
      if (isActive && totalDemand > 0) {
        // Pushes the message onto the stream
        onNext(msg)
      }
  }
}

object MyPublisher {
  def props(implicit ctx: ExecutionContext): Props = Props(new MyPublisher())
}

// TODO factor out common stuff between the two notifiers

class WebSocketNotifier()(implicit materializer: ActorMaterializer,
                          executionContext: ExecutionContextExecutor,
                          system: ActorSystem
) extends Notifier with JsonImplicits {

  private val dataSource: Source[Notif, ActorRef] = Source.actorPublisher[Notif](MyPublisher.props)

  val wsHandler: Flow[Any, TextMessage.Strict, NotUsed] = Flow
    .fromSinkAndSource(Sink.ignore, dataSource map { s ⇒
      TextMessage.Strict(upickle.default.write(s))
    })

  def details: String = "WebSockets"

  def getSensorSystemIndex(sysid: String, ssOpt: Option[SensorSystem],
                           indexResource: String = "web/index.html"): String = {
    val ssVar = "undefined"
    val template = io.Source.fromResource(indexResource).mkString
    template
      .replace("#sysid", sysid)
      .replace("#ssVar", ssVar)
      // TODO should not deal with pusher here!
      .replace("#pusherKey", "")
      .replace("#pusherChannel", "")
  }

  def notifySensorSystemRegistered(ss: SensorSystem): Unit = {
    if (ss.pushEvents) {
      println(s"WebSocketNotifier: notifySensorSystemRegistered")
      system.eventStream publish SensorSystemRegistered(
        sysid = ss.sysid,
        name = ss.name,
        description = ss.description,
        center = ss.center,
        clickListener = ss.clickListener
      )
    }
  }

  def notifyStreamAdded(ss: SensorSystem, str: DataStream): Unit = {
    if (ss.pushEvents) {
      println(s"WebSocketNotifier: notifyStreamAdded")
      system.eventStream publish StreamAdded(
        sysid = ss.sysid,
        str = VmDataStream(
          str.strid,
          str.name,
          str.description,
          mapStyle = str.mapStyle.map(_.toJson.compactPrint),
          str.zOrder,
          chartStyle = str.chartStyle.map(_.toJson.compactPrint),
          variables = str.variables.map(_.map(v ⇒ VmVariableDef(v.name, v.units, v.chartStyle.map(_.toJson.compactPrint))))
          // TODO NOTE observations not captured at time of stream registration
        )
      )
    }
  }

  def notifyObservations2Added(ss: SensorSystem, strid: String,
                               observations: Map[String, List[ObsData]]): Unit = {
    if (ss.pushEvents) {
      val obs = observations mapValues { list ⇒
        val obsDataList = collection.mutable.ListBuffer[VmObsData]()
        list foreach  { o ⇒
          obsDataList += VmObsData(
            feature = o.feature.map(_.toJson.compactPrint),
            geometry = o.geometry.map(_.toJson.compactPrint),
            scalarData = o.scalarData
          )
        }
        obsDataList.toList
      }

      @tailrec
      def rec(from: Int): Unit = {
        if (from < obs.size) {
          val next = Math.min(from + 15, obs.size)
          val slice = obs.slice(from, next)
          system.eventStream publish Observations2Added(
            sysid = ss.sysid,
            strid = strid,
            obss = slice
          )
          rec(next)
        }
      }
      rec(0)
    }
  }

  def notifyStreamRemoved(ss: SensorSystem, strid: String): Unit = {
    if (ss.pushEvents) {
      system.eventStream publish StreamRemoved(ss.sysid, strid)
    }
  }

  def notifySensorSystemUpdated(ss: SensorSystem): Unit = {
    if (ss.pushEvents) {
      system.eventStream publish SensorSystemUpdated(ss.sysid)
    }
  }

  def notifySensorSystemRefresh(ss: SensorSystem): Unit = {
    if (ss.pushEvents) {
      system.eventStream publish SensorSystemRefresh(ss.sysid)
    }
  }

  def notifySensorSystemUnregistered(ss: SensorSystem): Unit = {
    if (ss.pushEvents) {
      system.eventStream publish SensorSystemUnregistered(ss.sysid)
    }
  }
}
