package gpdviz

import scala.concurrent.ExecutionContext.Implicits.global

object AutowireServer extends autowire.Server[String, upickle.default.Reader, upickle.default.Writer] {
  def write[Result: upickle.default.Writer](r: Result): String = upickle.default.write(r)

  def read[Result: upickle.default.Reader](p: String) = upickle.default.read[Result](p)

  val routes: AutowireServer.Router = AutowireServer.route[Api](ApiImpl)
}
