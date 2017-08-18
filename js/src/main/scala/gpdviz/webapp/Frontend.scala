package gpdviz.webapp

import autowire._
import gpdviz._
import gpdviz.pusher.PusherListener
import gpdviz.websocket.WsListener
import org.scalajs.dom.document
import org.scalajs.dom.raw.HTMLElement
import pprint.PPrinter.BlackWhite.{apply ⇒ pp}

import scala.collection.mutable
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.scalajs.js
import scala.scalajs.js.annotation.JSGlobalScope

@js.native
@JSGlobalScope
object DOMGlobalScope extends js.Object {
  def sysid: String = js.native

  def setupLLMap(hoveredPoint: js.Function1[js.Dictionary[_], Any]): LLMap = js.native
}

@js.native
trait LLMap extends js.Object {

  def sensorSystemRegistered(): Unit = js.native

  def sensorSystemUnregistered(): Unit = js.native

  def addStream(str: js.Dictionary[_]): Unit = js.native

  def removeStream(strid: String): Unit = js.native

  def addGeoJson(strid: String, timestamp: String, feature: String): Unit = js.native

  def addObsScalarData(strid: String, timestamp: String, scalarData: js.Dictionary[_]): Unit = js.native

  def addSelectionPoint(p: js.Array[Double]): Unit = js.native
}

object Frontend extends js.JSApp {
  def main(): Unit = if (elm.scalajs != null) {
    AutowireClient[Api].clientConfig().call() foreach { new WebApp(_) }
  }
}

class WebApp(clientConfig: ClientConfig) {
  println("clientConfig = " + pp(clientConfig))
  val sysid: String = DOMGlobalScope.sysid
  val llmap: LLMap  = DOMGlobalScope.setupLLMap(hoveredPoint)

  val vm = new VModel(sysid)

  val notifHandler = new NotifHandler(sysid, llmap, vm)

  clientConfig.pusher match {
    case None ⇒
      new WsListener(notifHandler.handleNotification)

    case Some(pc) ⇒
      val pusherChannel = s"${clientConfig.serverName}-$sysid-2"
      new PusherListener(pc, pusherChannel, notifHandler.handleNotification)
  }

  new View(vm).render()

  private def hoveredPoint: js.Function1[js.Dictionary[_], Any] = {
    (dict: js.Dictionary[_]) ⇒ {
      val p: mutable.Map[String, _] = dict
      val strid = p("strid").asInstanceOf[String]
      val x = p("x").asInstanceOf[Float].toLong
      //val y = p("y").asInstanceOf[Double].toLong
      //val isoTime = p("isoTime").asInstanceOf[String]
      //println("hoveredPoint: p=" + p + " x=" +x+ " strid=" + strid)

      PositionsByTime.get(strid, x) foreach { latLon ⇒
        llmap.addSelectionPoint(js.Array(latLon.lat, latLon.lon))
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
