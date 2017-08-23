package gpdviz.async

import com.pusher.rest.Pusher
import gpdviz._
import gpdviz.config.{PusherCfg, cfg}

class PusherPublisher(pusherCfg: PusherCfg) extends Publisher {
  def details: String = "Pusher"

  def publish(notif: Notif): Unit = {
    val channel = s"${cfg.serverName}-${notif.sysid}-2"
    val res = pusher.trigger(channel, "my_event", upickle.default.write(notif))
    if (res.getHttpStatus != 200)
      println(s"!!!PusherPublisher: pusher.trigger ERROR: status=${res.getHttpStatus} message=${res.getMessage}")
  }

  private val pusher = new Pusher(pusherCfg.appId, pusherCfg.key, pusherCfg.secret)
  pusher.setEncrypted(true)
}
