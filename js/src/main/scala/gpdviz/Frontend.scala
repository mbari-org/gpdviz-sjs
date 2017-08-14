package gpdviz

import autowire._
import gpdviz.pusher.PusherListener
import gpdviz.websocket.WsListener

import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.scalajs.js

object Frontend extends js.JSApp {

  def main(): Unit = {
    AutowireClient[Api].clientConfig().call() foreach gotClientConfig
  }

  private def gotClientConfig(clientConfig: ClientConfig): Unit = {
    println("clientConfig = " + clientConfig)

    val sysid: js.Dynamic = js.Dynamic.global.sysid

    val notifHandler = new NotifHandler

    clientConfig.pusher match {
      case None     ⇒ new WsListener(notifHandler)

      case Some(pc) ⇒
        val pusherChannel = s"${clientConfig.serverName}-$sysid-2"
        new PusherListener(pc, pusherChannel, notifHandler)
    }
  }
}
