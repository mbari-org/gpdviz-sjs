package gpdviz

import scala.scalajs.js
import org.scalajs.dom

object Frontend extends js.JSApp {

  def main(): Unit = {
    // TODO
    println("Frontend.main: dom.document = " + dom.document)
    val test = dom.document.getElementById("TEST")
    println("Frontend.main: test = " + test)
    dom.document.getElementById("TEST").innerHTML = "SURPI!"
  }
}
