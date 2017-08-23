package gpdviz.async

import gpdviz.Notif

trait Publisher {
  def details: String

  def publish(notif: Notif): Unit
}
