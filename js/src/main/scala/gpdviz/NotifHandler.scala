package gpdviz

class NotifHandler {

  // TODO
  def handleNotification(n: Notif): Unit = {
    println("TODO handleNotification: " + n)
    n match {
      case n: SensorSystemRegistered ⇒
      case n: StreamAdded ⇒
      case n: Observations2Added ⇒
      case n: StreamRemoved ⇒
      case n: SensorSystemUpdated ⇒
      case n: SensorSystemRefresh ⇒
      case n: SensorSystemUnregistered ⇒
    }
  }
}
