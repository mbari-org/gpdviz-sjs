package gpdviz.webapp

import org.scalajs.dom

import scala.concurrent.Future
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

object AutowireClient extends autowire.Client[String, upickle.default.Reader, upickle.default.Writer] {
  def write[Result: upickle.default.Writer](r: Result): String = upickle.default.write(r)

  def read[Result: upickle.default.Reader](p: String): Result = upickle.default.read[Result](p)

  override def doCall(req: Request): Future[String] = {
    //println("doCall: req=" + req)
    dom.ext.Ajax.post(
      url = "/ajax/" + req.path.mkString("/"),
      data = upickle.default.write(req.args)
    ).map(_.responseText)
  }
}
