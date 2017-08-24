package gpdviz.webapp

import com.thoughtworks.binding.Binding.{Var, Vars}
import gpdviz.ClientConfig
import gpdviz.model.{LatLon, VmDataStream, VmObsData, VmSensorSystem}
import upickle.Js

import scala.scalajs.js
import scala.scalajs.js.JSConverters._


class VModel(sysid: String, cc: ClientConfig, llmap: LLMap) {

  val ss: Var[VmSensorSystem] = Var(VmSensorSystem(sysid))

  val absoluteCharts: Vars[ChartDiv] = Vars.empty

  def refreshSystem(vss: VmSensorSystem): Unit = {
    ss := vss

    llmap.setView(jsCenter(vss.center), vss.zoom.getOrElse(cc.zoom))

    vss.streams foreach { str ⇒
      addAbsoluteChart(str.strid, str.chartStyle)
      addStreamToMap(str)
      str.observations foreach { obss ⇒
        addObservationsToMap(str, obss)
      }
    }
  }

  def registerSystem(name:          Option[String],
                     description:   Option[String],
                     center:        Option[LatLon],
                     zoom:          Option[Int],
                     clickListener: Option[String]): Unit = {
    ss := VmSensorSystem(
      sysid = sysid,
      name = name,
      description = description,
      streams = List.empty,
      center = center,
      clickListener = clickListener
    )
    llmap.sensorSystemRegistered(jsCenter(center), zoom.getOrElse(cc.zoom))
  }

  private def jsCenter(center: Option[LatLon]): js.Array[Double] = {
    val c = center.getOrElse(cc.center)
    js.Array(c.lat, c.lon)
  }

  def unregisterSystem(): Unit = {
    ss := VmSensorSystem(sysid)
    llmap.sensorSystemUnregistered()
  }

  def addStream(str: VmDataStream): Unit = {
    ss := ss.get.copy(streams = str :: ss.get.streams)
    addAbsoluteChart(str.strid, str.chartStyle)
    addStreamToMap(str)
  }

  private def addAbsoluteChart(strid: String, chartStyle: Option[String]): Unit = {
    val useChartPopup = chartStyle.map(upickle.default.read[Js.Obj]) exists { chartStyle: Js.Obj ⇒
      chartStyle.obj.get("useChartPopup").contains(Js.True)
    }
    if (!useChartPopup) {
      absoluteCharts.get += {
        val chartId = "chart-container-" + strid
        val chartHeightStr = "500px" // TODO chartHeightStr
        val minWidthStr    = "400px" // TODO minWidthStr
        ChartDiv(chartId, chartHeightStr, minWidthStr)
      }
    }
  }

  private def addStreamToMap(str: VmDataStream): Unit = {
    llmap.addStream(Map(
      "strid"        → str.strid,
      "name"         → str.name.orUndefined,
      "description"  → str.description.orUndefined,
      "mapStyle"     → str.mapStyle.orUndefined,
      "zOrder"       → str.zOrder,

      "chartStyle"   → str.chartStyle.orUndefined,
      "variables"    → str.variables.map(vars ⇒ vars.map(v ⇒ Map(
        "name"       → v.name,
        "units"      → v.units.orUndefined,
        "chartStyle" → v.chartStyle.orUndefined
      ).toJSDictionary).toJSArray).orUndefined

      //,"observations" → str.observations.toJSArray
    ).toJSDictionary)
  }

  def removeStream(strid: String): Unit = {
    val streams = ss.get.streams.filterNot(_.strid == strid)
    ss := ss.get.copy(streams = streams)
    llmap.removeStream(strid)
  }

  def addObservations(strid: String,
                      obss: Map[String, List[VmObsData]]): Unit = {
    val str = ss.get.streams.find(_.strid == strid).getOrElse(throw new IllegalStateException(s"undefined stream $strid"))
    val currObss = str.observations getOrElse Map.empty
    val newObss = currObss ++ obss
    val updatedStr = str.copy(observations = Some(newObss))
    ss := ss.get.copy(streams = updatedStr :: ss.get.streams.filterNot(_.strid == strid))

    addObservationsToMap(str, obss)
  }

  private def addObservationsToMap(str: VmDataStream,
                                   obss: Map[String, List[VmObsData]]): Unit = {
    obss.keys.toSeq.sorted foreach { timestamp ⇒
      obss(timestamp) foreach { obs ⇒
        obs.feature foreach { feature ⇒
          llmap.addGeoJson(str.strid, timestamp, feature)
        }
        obs.geometry foreach { geometry ⇒
          llmap.addGeoJson(str.strid, timestamp, geometry)
        }
        obs.scalarData foreach { scalarData ⇒
          llmap.addObsScalarData(str.strid, timestamp, Map(
            "vars" → scalarData.vars.toJSArray,
            "vals" → scalarData.vals.toJSArray,
            "position" → scalarData.position.map(p ⇒
              Map("lat" → p.lat, "lon" → p.lon).toJSDictionary).orUndefined
          ).toJSDictionary)

          scalarData.position foreach { position ⇒
            val timeMs = timestamp.toLong
            PositionsByTime.set(str.strid, timeMs, position)
          }
        }
      }
    }
  }
}
