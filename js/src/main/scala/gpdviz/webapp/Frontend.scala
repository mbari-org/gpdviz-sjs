package gpdviz.webapp

import autowire._
import gpdviz._
import gpdviz.pusher.PusherListener
import gpdviz.websocket.WsListener
import org.scalajs.dom.document
import org.scalajs.dom.raw.HTMLElement
import pprint.PPrinter.BlackWhite.{apply ⇒ pp}

import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.scalajs.js
import scala.scalajs.js.annotation.JSGlobalScope

@js.native
@JSGlobalScope
object DOMGlobalScope extends js.Object {
  def sysid: String = js.native

  def setupLLMap(): LLMap = js.native
}

@js.native
trait LLMap extends js.Object {

  def sensorSystemRegistered(): Unit = js.native

  def sensorSystemUnregistered(): Unit = js.native

  def addStream(str: js.Dictionary[_]): Unit = js.native

  def removeStream(strid: String): Unit = js.native

  def addGeoJson(strid: String, timestamp: String, feature: String): Unit = js.native

  def addObsScalarData(strid: String, timestamp: String, scalarData: js.Dictionary[_]): Unit = js.native

  def addSelectionPoint(p: js.Array[Double]): String = js.native
}

object Frontend extends js.JSApp {
  def main(): Unit = if (elm.scalajs != null) {
    val llmap = DOMGlobalScope.setupLLMap()
    AutowireClient[Api].clientConfig().call() foreach { clientConfig ⇒
      println("clientConfig = " + pp(clientConfig))
      new WebApp(clientConfig, DOMGlobalScope.sysid, llmap)
    }
  }
}

class WebApp(clientConfig: ClientConfig, sysid: String, llmap: LLMap) {
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
}

private object elm {
  def scalajs:          HTMLElement  = byId("scalajs")
  def sysName:          HTMLElement  = byId("sysName")
  def sysDesc:          HTMLElement  = byId("sysDesc")
  def absoluteCharts:   HTMLElement  = byId("absoluteCharts")
  def sysActivity:      HTMLElement  = byId("sysActivity")

  private def byId[T](id: String): T = document.getElementById(id).asInstanceOf[T]
}
