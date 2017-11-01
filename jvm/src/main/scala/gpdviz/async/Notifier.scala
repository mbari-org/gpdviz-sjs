package gpdviz.async

import gpdviz._
import gpdviz.data.DbInterface
import gpdviz.model._
import gpdviz.server.GpdvizJsonImplicits
import spray.json._

import scala.annotation.tailrec
import scala.concurrent.ExecutionContext.Implicits.global

class Notifier(db: DbInterface, pub: Publisher) extends GpdvizJsonImplicits {

  def notifySensorSystemAdded(ss: SensorSystem): Unit = if (ss.pushEvents) {
    pub.publish(SensorSystemAdded(
      sysid = ss.sysid,
      name = ss.name,
      description = ss.description,
      center = ss.center,
      zoom = ss.zoom,
      clickListener = ss.clickListener
    ))
  }

  def notifySensorSystemUpdated(sysid: String, pushEvents: Boolean): Unit = if (pushEvents) {
    pub.publish(SensorSystemUpdated(sysid))
  }

  def notifySensorSystemDeleted(sysid: String, pushEvents: Boolean): Unit = if (pushEvents) {
    pub.publish(SensorSystemDeleted(sysid))
  }

  def notifyDataStreamAdded(sysid: String, str: DataStream): Unit = ifPushing(sysid) {
    // NOTE observations not captured at time of stream registration
    require(str.observations.isEmpty)

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
      )
    ))
  }

  def notifyDataStreamDeleted(sysid: String, strid: String): Unit = ifPushing(sysid) {
    pub.publish(DataStreamDeleted(sysid, strid))
  }

  def notifyVariableDefAdded(sysid: String, strid: String, vd: VariableDef): Unit = ifPushing(sysid) {
    pub.publish(VariableDefAdded(
      sysid = sysid,
      strid = strid,
      vd = VmVariableDef(
        name = vd.name,
        units = vd.units,
        chartStyle = vd.chartStyle.map(_.toJson.compactPrint)
      )
    ))
  }

  def notifyObservationsAdded(sysid: String, strid: String,
                              observations: Map[String, List[ObsData]]): Unit = ifPushing(sysid) {
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

  private def ifPushing(sysid: String)(p : ⇒ Unit): Unit = {
    for {
      ssOpt <- db.getSensorSystem(sysid)
      ss ← ssOpt
      if ss.pushEvents
    } p
  }
}
