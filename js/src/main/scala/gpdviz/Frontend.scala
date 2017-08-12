package gpdviz

import scala.scalajs.js

// TODO for now, just some webSocket preparations
object Frontend extends js.JSApp {

  def main(): Unit = {
    new WsListener
  }

}
