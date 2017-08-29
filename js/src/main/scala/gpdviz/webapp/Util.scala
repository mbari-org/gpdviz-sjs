package gpdviz.webapp

import org.scalajs.dom.window
import org.scalajs.dom.window.console

object Util {
  /**
    * Returns the requested path excluding the last fragment corresponding to
    * the sysid. Always has a trailing slash.
    */
  def basePath: String = {
    // With full request  http://localhost:5050/ss1/ :
    // pathname = "/ss1/"
    val s = window.location.pathname.replaceAll("^/|/$", "").split('/').toList
    val t = "/" + (if (s.nonEmpty) s.take(s.length - 1).mkString("/") else "")
    val res = if (t.endsWith("/")) t else t + "/"
    console.log(s"basePath: pathname=${window.location.pathname} s=$s res=$res")
    // res = "/"
    res
  }

  /**
    * Returns the full requested URL excluding the last fragment corresponding to
    * the sysid. Always has a trailing slash.
    */
  def baseUrl(protocol: String = window.location.protocol): String = {
    val portPart = Option(window.location.port) match {
      case None    ⇒ ""
      case Some(p) ⇒ if (p == "80" || p == "443") "" else ":" + p
    }
    val res = s"$protocol//${window.location.hostname}$portPart$basePath"
    console.log(s"baseUrl: res=$res")
    res
  }
}
