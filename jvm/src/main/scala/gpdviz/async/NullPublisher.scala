package gpdviz.async

import gpdviz.Notif

object NullPublisher extends Publisher {
  def details: String = "no-publisher"

  def publish(notif: Notif): Unit = ()
}
