package gpdviz.pusher

import gpdviz.ClientPusherConfig

import scala.scalajs.js
import scala.scalajs.js.JSConverters._
import scala.scalajs.js.JSON

class PusherListener(pusherConfig: ClientPusherConfig, pusherChannel: String) {

  println(s"PusherListener: pusherChannel=$pusherChannel")

  // no pusher facade out there ...

  //val pusher = new Pusher(pc.key, { encrypted: true })
  val pusher: js.Object with js.Dynamic = js.Dynamic.newInstance(js.Dynamic.global.Pusher)(
    pusherConfig.key,
    Map("encrypted" → true).toJSDictionary
  )

  println("PusherListener: pusher=" + pusher)

  val channel: js.Dynamic = pusher.subscribe(pusherChannel)

  channel.bind("my_event", handleNotification _)

  // TODO
  def handleNotification(payload: js.Dynamic): Unit = {
    println("handleNotification: payload.what=" + payload.what)
    println("handleNotification: payload.data=" + JSON.stringify(payload.data))
  }
}
