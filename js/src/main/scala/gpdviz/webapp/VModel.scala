package gpdviz.webapp

import com.thoughtworks.binding.Binding.Var
import gpdviz._
import gpdviz.model.VmSensorSystem
import org.scalajs.dom.window

class VModel(val sysid: String) {
  val ss: Var[VmSensorSystem] = Var(VmSensorSystem(sysid))

  val handleNotification: (Notif) ⇒ Unit = (n: Notif) ⇒ {
    assert ( n.sysid == sysid )

    n match {
      case n: SensorSystemRegistered ⇒
        ss := VmSensorSystem(
          sysid = n.sysid,
          name = n.name,
          description = n.description,
          streams = Map.empty,
          center = n.center,
          clickListener = n.clickListener
        )

      case n: StreamAdded ⇒
        ss := ss.get.copy(streams = ss.get.streams updated (n.str.strid, n.str))

      case n: Observations2Added ⇒
        println(s"Observations2Added: ${n.strid} -> ${n.obss.size}")
        val str = ss.get.streams.getOrElse(n.strid, throw new IllegalStateException(s"undefined stream ${n.strid}"))
        val currObss = str.observations getOrElse Map.empty
        val newObss = currObss ++ n.obss
        val updatedStr = str.copy(observations = Some(newObss))
        ss := ss.get.copy(streams = ss.get.streams updated (updatedStr.strid, updatedStr))

      case n: StreamRemoved ⇒
        val streams = ss.get.streams - n.strid
        ss := ss.get.copy(streams = streams)

      case _: SensorSystemUpdated ⇒

      case _: SensorSystemRefresh ⇒
        window.location.reload(true)

      case _: SensorSystemUnregistered ⇒
        ss := VmSensorSystem(sysid)
    }
  }
}

