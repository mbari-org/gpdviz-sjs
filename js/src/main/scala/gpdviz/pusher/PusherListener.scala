package gpdviz.pusher

import gpdviz._

import scala.scalajs.js
import scala.scalajs.js.JSConverters._
import scala.scalajs.js.annotation.JSGlobal

@js.native
@JSGlobal
class Pusher(key: String, conf: js.Dictionary[_]) extends js.Object {
  def subscribe(channel: String): js.Dynamic = js.native
}

class PusherListener(pusherConfig: ClientPusherConfig, channelName: String,
                     handleNotification: (Notif) ⇒ Unit) {
  println(s"PusherListener: pusherChannel=$channelName")

  val pusher = new Pusher(pusherConfig.key, Map("encrypted" → true).toJSDictionary)
  println("PusherListener: pusher=" + pusher)

  val channel: js.Dynamic = pusher.subscribe(channelName)

  channel.bind("my_event", handleEvent _)

  def handleEvent(payload: js.Dynamic): Unit = {
    val n = upickle.default.read[Notif](payload.asInstanceOf[String])
    handleNotification(n)
  }
}
