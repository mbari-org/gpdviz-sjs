package gpdviz.async

import com.pusher.rest.Pusher
import gpdviz._
import gpdviz.config.{PusherCfg, cfg}
import gpdviz.model._
import gpdviz.server.JsonImplicits
import spray.json._

import scala.annotation.tailrec
import scala.collection.JavaConverters._


class PusherNotifier(pusherCfg: PusherCfg) extends Notifier with JsonImplicits {

  def details: String = "Pusher"

  def getSensorSystemIndex(sysid: String, ssOpt: Option[SensorSystem],
                           indexResource: String = "web/index.html"): String = {
    val ssVar = ssOpt match {
      case Some(ss) ⇒ ss.toJson.prettyPrint.replace("\n", "\n      ")
      case None     ⇒ "undefined"
    }
    val template = io.Source.fromResource(indexResource).mkString
    template
      .replace("#sysid", sysid)
      .replace("#ssVar", ssVar)
      .replace("#pusherKey", pusherCfg.key)
      .replace("#pusherChannel", s"${cfg.serverName}-$sysid")
  }

  def notifySensorSystemRegistered(ss: SensorSystem): Unit = {
    if (ss.pushEvents) {
      var map = Map("sysid" -> ss.sysid)
      ss.name foreach        { v ⇒ map = map + ("name" → v) }
      ss.description foreach { v ⇒ map = map + ("description" → v) }
      ss.clickListener foreach { v ⇒ map = map + ("clickListener" → v) }
      notifyEvent(ss.sysid, "sensorSystemRegistered", map.asJava)

      notifyEvent2(ss.sysid, SensorSystemRegistered(
        sysid = ss.sysid,
        name = ss.name,
        description = ss.description,
        center = ss.center,
        clickListener = ss.clickListener
      ))
    }
  }

  def notifyStreamAdded(ss: SensorSystem, str: DataStream): Unit = {
    if (ss.pushEvents) {
      val map = Map(
        "sysid" -> ss.sysid,
        "str" -> str.toJson.compactPrint
      )
      notifyEvent(ss.sysid, "streamAdded", map.asJava)

      notifyEvent2(ss.sysid, StreamAdded(
        sysid = ss.sysid,
        str = VmDataStream(str.strid, str.name, str.description,
          mapStyle = str.mapStyle.map(_.toJson.compactPrint), str.zOrder,
          variables = str.variables.map(_.map(v ⇒ VmVariableDef(v.name, v.units, v.chartStyle.map(_.toJson.compactPrint))))
        )
      ))
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

    notifyObservations2AddedNEW(ss, strid, observations)
  }

  private def notifyObservations2AddedNEW(ss: SensorSystem, strid: String, observations: Map[String, List[ObsData]]): Unit = if (ss.pushEvents) {
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
        notifyEvent2(ss.sysid, Observations2Added(
          sysid = ss.sysid,
          strid = strid,
          obss = slice
        ))
        rec(next)
      }
    }
    rec(0)
  }

  def notifyStreamRemoved(ss: SensorSystem, strid: String): Unit = {
    val map = Map("sysid" -> ss.sysid, "strid" -> strid)
    notifyEvent(ss.sysid, "streamRemoved", map.asJava)

    notifyEvent2(ss.sysid, StreamRemoved(ss.sysid, strid))
  }

  def notifySensorSystemUpdated(ss: SensorSystem): Unit = {
    if (ss.pushEvents) {
      val map = Map("sysid" -> ss.sysid)
      notifyEvent(ss.sysid, "sensorSystemUpdated", map.asJava)

      notifyEvent2(ss.sysid, SensorSystemUpdated(ss.sysid))
    }
  }

  def notifySensorSystemRefresh(ss: SensorSystem): Unit = {
    if (ss.pushEvents) {
      val map = Map("sysid" -> ss.sysid)
      notifyEvent(ss.sysid, "sensorSystemRefresh", map.asJava)

      notifyEvent2(ss.sysid, SensorSystemRefresh(ss.sysid))
    }
  }

  def notifySensorSystemUnregistered(ss: SensorSystem): Unit = {
    if (ss.pushEvents) {
      val map = Map("sysid" -> ss.sysid)
      notifyEvent(ss.sysid, "sensorSystemUnregistered", map.asJava)

      notifyEvent2(ss.sysid, SensorSystemUnregistered(ss.sysid))
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

  private def notifyEvent2(sysid: String, notif: Notif): Unit = {
    val channel = s"${cfg.serverName}-$sysid-2"
    val res = pusher.trigger(channel, "my_event", upickle.default.write(notif))
    if (res.getHttpStatus != 200)
      println(s"!!!notifyEvent2: pusher.trigger ERROR: status=${res.getHttpStatus} message=${res.getMessage}")

  }

  private val pusher = new Pusher(pusherCfg.appId, pusherCfg.key, pusherCfg.secret)
  pusher.setEncrypted(true)
}
