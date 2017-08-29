package gpdviz.webapp

import org.scalajs.dom
import org.scalajs.dom.window.console

import scala.concurrent.Future
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

object AutowireClient extends autowire.Client[String, upickle.default.Reader, upickle.default.Writer] {
  def write[Result: upickle.default.Writer](r: Result): String = upickle.default.write(r)

  def read[Result: upickle.default.Reader](p: String): Result = upickle.default.read[Result](p)

  override def doCall(req: Request): Future[String] = {
    val url = s"${Util.baseUrl()}ajax/${req.path.mkString("/")}"
    // url = /gpdviz/ajax/gpdviz/Api/clientConfig
    console.log("doCall: url=" + url + " req.path=" + req.path.mkString("/"))
    dom.ext.Ajax.post(
      url = url,
      data = upickle.default.write(req.args)
    ).map(_.responseText)
  }
}
