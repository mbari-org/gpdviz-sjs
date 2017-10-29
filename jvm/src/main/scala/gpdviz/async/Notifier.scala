package gpdviz.async

import gpdviz._
import gpdviz.model._
import gpdviz.server.JsonImplicits
import spray.json._

import scala.annotation.tailrec

// TODO should check pushEvents flag

class Notifier(pub: Publisher) extends JsonImplicits {

  def notifySensorSystemAdded(ss: SensorSystem): Unit = {
    if (ss.pushEvents) {
      pub.publish(SensorSystemAdded(
        sysid = ss.sysid,
        name = ss.name,
        description = ss.description,
        center = ss.center,
        zoom = ss.zoom,
        clickListener = ss.clickListener
      ))
    }
  }

  def notifyDataStreamAdded(sysid: String, str: DataStream): Unit = {
    pub.publish(DataStreamAdded(
      sysid = sysid,
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

  def notifyObservationsAdded(sysid: String, strid: String, observations: Map[String, List[ObsData]]): Unit = {
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
        pub.publish(ObservationsAdded(
          sysid = sysid,
          strid = strid,
          obss = slice
        ))
        rec(next)
      }
    }
    rec(0)
  }

  def notifyDataStreamDeleted(sysid: String, strid: String): Unit = {
    pub.publish(DataStreamDeleted(sysid, strid))
  }

  def notifySensorSystemUpdated(sysid: String): Unit = {
    pub.publish(SensorSystemUpdated(sysid))
  }

  def notifySensorSystemRefresh(sysid: String): Unit = {
    pub.publish(SensorSystemRefresh(sysid))
  }

  def notifySensorSystemDeleted(sysid: String): Unit = {
    pub.publish(SensorSystemDeleted(sysid))
  }
}
