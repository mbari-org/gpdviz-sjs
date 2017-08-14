package gpdviz.webapp

import autowire._
import gpdviz.pusher.PusherListener
import gpdviz.websocket.WsListener
import gpdviz.{Api, ClientConfig}

import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.scalajs.js
import scala.scalajs.js.annotation.JSGlobalScope

@js.native
@JSGlobalScope
object DOMGlobalScope extends js.Object {
  def sysid: String = js.native
}


object Frontend extends js.JSApp {

  val sysid: String = DOMGlobalScope.sysid

  def main(): Unit = {
    AutowireClient[Api].clientConfig().call() foreach gotClientConfig
  }

  private def gotClientConfig(clientConfig: ClientConfig): Unit = {
    println("clientConfig = " + clientConfig)

    val notifHandler = new NotifHandler

    clientConfig.pusher match {
      case None     ⇒ new WsListener(notifHandler)

      case Some(pc) ⇒
        val pusherChannel = s"${clientConfig.serverName}-$sysid-2"
        new PusherListener(pc, pusherChannel, notifHandler)
    }
  }
}
