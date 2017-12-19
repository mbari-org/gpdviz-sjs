package gpdviz.webapp

import autowire._
import gpdviz._
import gpdviz.pusher.PusherListener
import gpdviz.websocket.WsListener
import org.scalajs.dom
import org.scalajs.dom.{document, window}
import org.scalajs.dom.raw.HTMLElement
import org.scalajs.dom.window.console
import pprint.PPrinter.BlackWhite.{apply ⇒ pp}

import scala.collection.mutable
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.scalajs.js
import scala.scalajs.js.JSON
import scala.scalajs.js.annotation.JSGlobalScope
import scala.util.{Failure, Success}

@js.native
@JSGlobalScope
object DOMGlobalScope extends js.Object {
  def sysid: String = js.native

  def setupLLMap(center: js.Array[Double],
                 zoom: Int,
                 hoveredPoint: js.Function1[js.UndefOr[js.Dictionary[_]], Any],
                 mouseOutside: js.Function0[Unit],
                 clickHandler: js.Function1[js.Dictionary[_], Any],
                 includeGoogleMap: Boolean
                ): LLMap = js.native
}

@js.native
trait LLMap extends js.Object {

  def sensorSystemAdded(center: js.Array[Double], zoom: Int): Unit = js.native

  def sensorSystemDeleted(): Unit = js.native

  def addDataStream(str: js.Dictionary[_]): Unit = js.native

  def deleteDataStream(strid: String): Unit = js.native

  def addVariableDef(strid: String, str: js.Dictionary[_]): Unit = js.native

  def addGeoJson(strid: String, timeMs: Double, feature: String): Unit = js.native

  def addObsScalarData(strid: String, timeMs: Double, scalarData: js.Dictionary[_]): Unit = js.native

  def addSelectionPoint(p: js.UndefOr[js.Array[Double]]): Unit = js.native

  def setView(center: js.Array[Double], zoom: Int): Unit = js.native
}

object Frontend {
  def main(): Unit = if (elm.scalajs != null) {
    AutowireClient[Api].clientConfig().call() foreach { new WebApp(_) }
  }
}

class WebApp(cc: ClientConfig) {
  console.log("clientConfig = " + pp(cc))
  val sysid: String = DOMGlobalScope.sysid
  val llmap: LLMap  = {
    val center = js.Array(cc.center.lat, cc.center.lon)
    DOMGlobalScope.setupLLMap(center, cc.zoom,
      hoveredPoint,
      mouseOutside,
      clickHandler,
      includeGoogleMap = cc.includeGoogleMap)
  }

  val vm = new VModel(sysid, cc, llmap)

  startUp()

  private def startUp(): Unit = {
    new View(vm).render()

    refresh()

    val handleNotification: Notif ⇒ Unit = {
      case SensorSystemAdded(_, name, description, center, zoom, clickListener) ⇒
        vm.addSensorSystem(name, description, center, zoom, clickListener)

      case SensorSystemDeleted(_) ⇒
        vm.deleteSensorSystem()

      case DataStreamAdded(_, str) ⇒
        vm.addDataStream(str)

      case DataStreamDeleted(_, strid) ⇒
        vm.deleteDataStream(strid)

      case VariableDefAdded(_, strid, vd) ⇒
        vm.addVariableDef(strid, vd)

      case ObservationsAdded(_, strid, obss) ⇒
        vm.addObservations(strid, obss)

      case SensorSystemUpdated(_) ⇒

      case SensorSystemRefresh(_) ⇒
        window.location.reload(true)
    }

    cc.pusher match {
      case None ⇒
        new WsListener(sysid, handleNotification)

      case Some(pc) ⇒
        val pusherChannel = s"${cc.serverName}-$sysid-2"
        new PusherListener(pc, pusherChannel, handleNotification)
    }
  }

  private def refresh(): Unit = {
    AutowireClient[Api].refresh(sysid).call() foreach {
      case Some(vss) ⇒
        console.log("got initial state")
        vm.refreshSystem(vss)

      case None ⇒
        console.log("no initial state")
    }
  }

  private def mouseOutside: js.Function0[Unit] =
    () ⇒ llmap.addSelectionPoint(js.undefined)

  private def hoveredPoint: js.Function1[js.UndefOr[js.Dictionary[_]], Any] = {
    (dict: js.UndefOr[js.Dictionary[_]]) ⇒ {
      dict.toOption foreach { dic ⇒
        val p: mutable.Map[String, _] = dic
        val strid = p("strid").asInstanceOf[String]
        val x = p("x").asInstanceOf[Float].toLong
        //val y = p("y").asInstanceOf[Double].toLong
        //val isoTime = p("isoTime").asInstanceOf[String]
        //console.log("hoveredPoint: p=" + p + " x=" +x+ " strid=" + strid)

        PositionsByTime.get(strid, x) foreach { latLon ⇒
          llmap.addSelectionPoint(js.Array(latLon.lat, latLon.lon))
        }
      }
    }
  }

  private def clickHandler: js.Function1[js.Dictionary[_], Any] = {
    (dict: js.Dictionary[_]) ⇒ {

      vm.ss.get.clickListener foreach { url ⇒
        //require(dict.contains("lat"))
        //require(dict.contains("lon"))

        lazy val shiftKey = dict("shiftKey").asInstanceOf[Boolean]
        lazy val altKey   = dict("altKey").asInstanceOf[Boolean]
        lazy val metaKey  = dict("metaKey").asInstanceOf[Boolean]

        //val p: mutable.Map[String, _] = dict
        //console.log("clickHandler: p=" + p)

        if (shiftKey || altKey || metaKey) {
          dom.ext.Ajax.post(
            url = url,
            data = JSON.stringify(dict),
            headers = Map("Content-type" → "application/json")
          ).onComplete {
            case Success(_) ⇒ // ok
            case Failure(t) ⇒
              console.warn(s"failure in call to click listener $url: $t")
          }
        }
      }
    }
  }
}

private object elm {
  def scalajs:          HTMLElement  = byId("scalajs")
  def sysName:          HTMLElement  = byId("sysName")
  def sysDesc:          HTMLElement  = byId("sysDesc")
  def absoluteCharts:   HTMLElement  = byId("absoluteCharts")
  def sysActivity:      HTMLElement  = byId("sysActivity")

  private def byId[T](id: String): T = document.getElementById(id).asInstanceOf[T]
}
