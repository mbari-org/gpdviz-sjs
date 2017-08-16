package gpdviz.webapp

import autowire._
import gpdviz._
import gpdviz.pusher.PusherListener
import gpdviz.websocket.WsListener
import org.scalajs.dom.document
import org.scalajs.dom.raw.HTMLElement

import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.scalajs.js
import scala.scalajs.js.annotation.JSGlobalScope

@js.native
@JSGlobalScope
object DOMGlobalScope extends js.Object {
  def sysid: String = js.native

  def setupLLMap(): LLMap = js.native
}

object Frontend extends js.JSApp {
  def main(): Unit = if (elm.scalajs != null) {
    val llmap = DOMGlobalScope.setupLLMap()
    AutowireClient[Api].clientConfig().call() foreach { clientConfig ⇒
      println("clientConfig = " + clientConfig)
      new WebApp(clientConfig, DOMGlobalScope.sysid, llmap)
    }
  }
}

class WebApp(clientConfig: ClientConfig, sysid: String, llmap: LLMap) {
  val vm = new VModel(sysid)

  clientConfig.pusher match {
    case None ⇒
      new WsListener(vm.handleNotification)

    case Some(pc) ⇒
      val pusherChannel = s"${clientConfig.serverName}-$sysid-2"
      new PusherListener(pc, pusherChannel, vm.handleNotification)
  }

  new View(vm, llmap).render()
}

private object elm {
  def scalajs:      HTMLElement  = byId("scalajs")
  def sysName:      HTMLElement  = byId("sysName")
  def sysDesc:      HTMLElement  = byId("sysDesc")
  def sysActivity:  HTMLElement  = byId("sysActivity")

  private def byId[T](id: String): T = document.getElementById(id).asInstanceOf[T]
}
