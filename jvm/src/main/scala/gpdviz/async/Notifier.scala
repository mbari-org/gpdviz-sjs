package gpdviz.async

import com.pusher.rest.Pusher
import gpdviz.config.cfg
import gpdviz.model.{DataStream, ObsData, SensorSystem}
import gpdviz.server.JsonImplicits
import spray.json._

import scala.annotation.tailrec
import scala.collection.JavaConverters._


class Notifier extends JsonImplicits {

  def getSensorSystemIndex(sysid: String, ssOpt: Option[SensorSystem]): String = {
    val ssVar = ssOpt match {
      case Some(ss) => ss.toJson.prettyPrint.replace("\n", "\n      ")
      case None     => "undefined"
    }
    //val template = Files.readAllLines(Paths.get(webDir, "index.html")).asScala.mkString("\n")
    val template = io.Source.fromResource("web/index.html").mkString
    template
      .replace("#sysid", sysid)
      .replace("#ssVar", ssVar)
      .replace("#pusherKey", cfg.pusher.key)
      .replace("#pusherChannel", s"${cfg.serverName}-$sysid")
  }

  def notifySensorSystemRegistered(ss: SensorSystem): Unit = {
    if (ss.pushEvents) {
      var map = Map("sysid" -> ss.sysid)
      ss.name foreach        { v ⇒ map = map + ("name" → v) }
      ss.description foreach { v ⇒ map = map + ("description" → v) }
      ss.clickListener foreach { v ⇒ map = map + ("clickListener" → v) }
      notifyEvent(ss.sysid, "sensorSystemRegistered", map.asJava)
    }
  }

  def notifyStreamAdded(ss: SensorSystem, str: DataStream): Unit = {
    if (ss.pushEvents) {
      val map = Map(
        "sysid" -> ss.sysid,
        "str" -> str.toJson.compactPrint
      )
      notifyEvent(ss.sysid, "streamAdded", map.asJava)
    }
  }

  def notifyObservations2Added(ss: SensorSystem, strid: String, observations: Map[String, List[ObsData]]): Unit = if (ss.pushEvents) {
    //println(s"notifyObservations2Added: strid=$strid observations=${observations.size}")
    val obs = observations mapValues { list =>
      // NOTE: just passing the JSON result as a plain string.
      // (We could probably do some pusher configuration, via Gson serializer,
      // and pass the JsValue here.)
      var map = Map[String, Any]()
      list foreach  { o ⇒
        o.feature.foreach(x => map = map.updated("feature", x.toJson.compactPrint))
        o.geometry.foreach(x => map = map.updated("geometry", x.toJson.compactPrint))
        o.scalarData.foreach(x => map = map.updated("scalarData", x.toJson.compactPrint))
      }
      map.asJava
    }
    @tailrec
    def rec(from: Int): Unit = {
      if (from < obs.size) {
        val next = Math.min(from + 15, obs.size)
        val slice = obs.slice(from, next)
        val map = Map("sysid" -> ss.sysid, "strid" -> strid, "obss" -> slice.asJava)
        notifyEvent(ss.sysid, "observations2Added", map.asJava)
        rec(next)
      }
    }
    rec(0)
  }

  def notifyStreamRemoved(ss: SensorSystem, strid: String): Unit = {
    val map = Map("sysid" -> ss.sysid, "strid" -> strid)
    notifyEvent(ss.sysid, "streamRemoved", map.asJava)
  }

  def notifySensorSystemUpdated(ss: SensorSystem): Unit = {
    if (ss.pushEvents) {
      val map = Map("sysid" -> ss.sysid)
      notifyEvent(ss.sysid, "sensorSystemUpdated", map.asJava)
    }
  }

  def notifySensorSystemRefresh(ss: SensorSystem): Unit = {
    if (ss.pushEvents) {
      val map = Map("sysid" -> ss.sysid)
      notifyEvent(ss.sysid, "sensorSystemRefresh", map.asJava)
    }
  }

  def notifySensorSystemUnregistered(ss: SensorSystem): Unit = {
    if (ss.pushEvents) {
      val map = Map("sysid" -> ss.sysid)
      notifyEvent(ss.sysid, "sensorSystemUnregistered", map.asJava)
    }
  }

  private def notifyEvent(sysid: String, what: String, data: Any): Unit = {
    val map = Map("what" -> what, "data" -> data)
    //println(s"notifyEvent: sysid:$sysid  what=$what")  //  data=$data")
    val channel = s"${cfg.serverName}-$sysid"
    val res = pusher.trigger(channel, "my_event", map.asJava)
    if (res.getHttpStatus != 200)
      println(s"!!!notifyEvent: pusher.trigger ERROR: status=${res.getHttpStatus} message=${res.getMessage}")
  }

  private val pusher = new Pusher(cfg.pusher.appId, cfg.pusher.key, cfg.pusher.secret)
  pusher.setEncrypted(true)
}
