package gpdviz

import autowire._

import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.scalajs.js

// TODO for now, just some webSocket preparations
object Frontend extends js.JSApp {

  def main(): Unit = {

    AutowireClient[Api].clientConfig().call().foreach { clientConfig ⇒
      println("clientConfig = " + clientConfig)

      clientConfig.pusher match {
        case None ⇒ new WsListener

        case Some(pc) ⇒
      }
    }
  }
}
