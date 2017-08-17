package gpdviz.webapp

import gpdviz._
import gpdviz.model.VmSensorSystem
import org.scalajs.dom.window

import scala.scalajs.js.JSConverters._

class NotifHandler(sysid: String, llmap: LLMap, vm: VModel) {
  private val ss = vm.ss

  val handleNotification: (Notif) ⇒ Unit = (n: Notif) ⇒ {
    assert ( n.sysid == sysid )

    n match {
      case SensorSystemRegistered(_, name, description, center, clickListener) ⇒
        ss := VmSensorSystem(
          sysid = sysid,
          name = name,
          description = description,
          streams = Map.empty,
          center = center,
          clickListener = clickListener
        )
        llmap.sensorSystemRegistered()

      case SensorSystemUnregistered(_) ⇒
        ss := VmSensorSystem(sysid)
        llmap.sensorSystemUnregistered()

      case StreamAdded(_, str) ⇒
        ss := ss.get.copy(streams = ss.get.streams updated (str.strid, str))

        vm.absCharts.get += {
          val chartId = "chart-container-" + str.strid
          val chartHeightStr = "500px" // TODO chartHeightStr
          val minWidthStr    = "400px" // TODO minWidthStr
          AbsChart(chartId, chartHeightStr, minWidthStr)
        }

        llmap.addStream(Map(
          "strid"        → str.strid,
          "name"         → str.name.orUndefined,
          "description"  → str.description.orUndefined,
          //"mapStyle"     → str.mapStyle.orUndefined,
          "zOrder"       → str.zOrder,

          "variables"    → str.variables.map(vars ⇒ vars.map(v ⇒ Map(
            "name"       → v.name,
            "units"      → v.units.orUndefined,
            "chartStyle" → v.chartStyle.orUndefined
          ).toJSDictionary).toJSArray).orUndefined

          //, "chartStyle"   → str.chartStyle.orUndefined
          //,"observations" → str.observations.toJSArray
        ).toJSDictionary)

      case StreamRemoved(_, strid) ⇒
        val streams = ss.get.streams - strid
        ss := ss.get.copy(streams = streams)
        llmap.removeStream(strid)

      case Observations2Added(_, strid, obss) ⇒
        println(s"Observations2Added: $strid -> ${obss.size}")
        val str = ss.get.streams.getOrElse(strid, throw new IllegalStateException(s"undefined stream $strid"))

        obss.keys.toSeq.sorted foreach { timestamp ⇒
          obss(timestamp) foreach { obs ⇒
            obs.feature foreach { feature ⇒
              llmap.addGeoJson(str.strid, timestamp, feature, str.mapStyle.orUndefined)
            }
            obs.geometry foreach { geometry ⇒
              llmap.addGeoJson(str.strid, timestamp, geometry, str.mapStyle.orUndefined)
            }
            obs.scalarData foreach { scalarData ⇒
              llmap.addObsScalarData(str.strid, timestamp, Map(
                "vars" → scalarData.vars.toJSArray,
                "vals" → scalarData.vals.toJSArray,
                "position" → scalarData.position.map(p ⇒
                  Map("lat" → p.lat, "lon" → p.lon).toJSDictionary).orUndefined
              ).toJSDictionary)
            }
          }
        }
        val currObss = str.observations getOrElse Map.empty
        val newObss = currObss ++ obss
        val updatedStr = str.copy(observations = Some(newObss))
        ss := ss.get.copy(streams = ss.get.streams updated (updatedStr.strid, updatedStr))

      case SensorSystemUpdated(_) ⇒

      case SensorSystemRefresh(_) ⇒
        window.location.reload(true)
    }
  }
}
