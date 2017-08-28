package gpdviz.webapp

import org.scalajs.dom.window

object Util {
  def basePath: String = {
    // pathname = /gpdviz/waveglider-tft-2017-06-07/
    val s = window.location.pathname.replaceAll("^/|/$", "").split('/').toList
    // s = List(gpdviz, waveglider-tft-2017-06-07)
    val res = "/" + (if (s.nonEmpty) s.take(s.length - 1).mkString("/") else "")
    println(s"pathname=${window.location.pathname} s=$s res=$res")
    // res = https://okeanids.mbari.org/gpdviz
    res
  }

  def baseUrl(protocol: String = window.location.protocol): String = {
    val res = s"$protocol//${window.location.hostname}$basePath"
    println(s"baseUrl: res=$res")
    res
  }
}
