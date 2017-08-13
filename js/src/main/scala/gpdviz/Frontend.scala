package gpdviz

import autowire._

import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.scalajs.js

// TODO for now, just some webSocket preparations
object Frontend extends js.JSApp {

  def main(): Unit = {

    AutowireClient[Api].usingPusher().call().foreach { usingPusher ⇒
      println("usingPusher = " + usingPusher)
      if (!usingPusher) {
        new WsListener
      }
    }
  }
}
