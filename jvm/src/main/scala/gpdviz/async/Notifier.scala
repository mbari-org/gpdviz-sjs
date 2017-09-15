package gpdviz.async

import gpdviz._
import gpdviz.config.cfg
import gpdviz.model._
import gpdviz.server.JsonImplicits
import spray.json._

import scala.annotation.tailrec


class Notifier(pub: Publisher) extends JsonImplicits {

  def getSensorSystemIndex(sysid: String, ssOpt: Option[SensorSystem],
                           indexResource: String = "web/index.html"): String = {
    val template = scala.io.Source.fromResource(indexResource).mkString
    template.replace("#sysid", sysid)
      .replace("#externalUrl", cfg.externalUrl)
  }

  def notifySensorSystemRegistered(ss: SensorSystem): Unit = {
    if (ss.pushEvents) {
      pub.publish(SensorSystemRegistered(
        sysid = ss.sysid,
        name = ss.name,
        description = ss.description,
        center = ss.center,
        zoom = ss.zoom,
        clickListener = ss.clickListener
      ))
    }
  }

  def notifyStreamAdded(ss: SensorSystem, str: DataStream): Unit = {
    if (ss.pushEvents) {
      pub.publish(StreamAdded(
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
      ))
    }
  }

  def notifyObservations2Added(ss: SensorSystem, strid: String, observations: Map[String, List[ObsData]]): Unit = if (ss.pushEvents) {
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
        pub.publish(Observations2Added(
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
    if (ss.pushEvents) {
      pub.publish(StreamRemoved(ss.sysid, strid))
    }
  }

  def notifySensorSystemUpdated(ss: SensorSystem): Unit = {
    if (ss.pushEvents) {
      pub.publish(SensorSystemUpdated(ss.sysid))
    }
  }

  def notifySensorSystemRefresh(ss: SensorSystem): Unit = {
    if (ss.pushEvents) {
      pub.publish(SensorSystemRefresh(ss.sysid))
    }
  }

  def notifySensorSystemUnregistered(ss: SensorSystem): Unit = {
    if (ss.pushEvents) {
      pub.publish(SensorSystemUnregistered(ss.sysid))
    }
  }
}
