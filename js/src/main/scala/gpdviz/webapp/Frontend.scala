package gpdviz.webapp

import autowire._
import com.thoughtworks.binding.Binding.{Constants, Var}
import com.thoughtworks.binding.{Binding, dom}
import gpdviz.pusher.PusherListener
import gpdviz.websocket.WsListener
import gpdviz._
import org.scalajs.dom.{document, window}
import org.scalajs.dom.raw.{HTMLElement, Node}

import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.scalajs.js
import scala.scalajs.js.annotation.JSGlobalScope

@js.native
@JSGlobalScope
object DOMGlobalScope extends js.Object {
  def sysid: String = js.native
}


object Frontend extends js.JSApp {
  def main(): Unit = {
    AutowireClient[Api].clientConfig().call() foreach { clientConfig ⇒
      println("clientConfig = " + clientConfig)
      new WebApp(clientConfig)
    }
  }
}

class WebApp(clientConfig: ClientConfig) {

  val sysid = Var(DOMGlobalScope.sysid)

  object vm {
    val ss: Var[VmSensorSystem] = Var(VmSensorSystem(sysid.get))

    val handleNotification: (Notif) ⇒ Unit = (n: Notif) ⇒ {
      assert ( n.sysid == sysid.get )

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
          val str = VmDataStream(n.strid) // TODO other stream stuff
          val streams = ss.get.streams updated (n.strid, str)
          ss := ss.get.copy(streams = streams)

        case n: Observations2Added ⇒

        case n: StreamRemoved ⇒
          val streams = ss.get.streams - n.strid
          ss := ss.get.copy(streams = streams)

        case _: SensorSystemUpdated ⇒

        case _: SensorSystemRefresh ⇒
          window.location.reload(true)

        case _: SensorSystemUnregistered ⇒
          ss := VmSensorSystem(sysid.get)
      }
    }
  }

  clientConfig.pusher match {
    case None     ⇒ new WsListener(vm.handleNotification)

    case Some(pc) ⇒
      val pusherChannel = s"${clientConfig.serverName}-${sysid.get}-2"
      new PusherListener(pc, pusherChannel, vm.handleNotification)
  }

  @dom def streamsBinding: Binding[Node] = {
    <ul>
      {
      for (str <- Constants(vm.ss.bind.streams.values.toSeq: _*))
        yield <li>{ str.strid } </li>
      }
    </ul>
  }

  @dom def binding(): Binding[Node] = {
    <div>
      <code>{ sysid.bind }</code> -
      <div>{ vm.ss.bind.name.getOrElse("") }</div>
      <div>{ vm.ss.bind.description.getOrElse("") }</div>
      <div>Center: { vm.ss.bind.center.map(_.toString).getOrElse("") }</div>
      { streamsBinding.bind }
    </div>
  }

  dom.render(elm.test, binding())
}

private object elm {
  def test:  HTMLElement        = byId("test")

  private def byId[T](id: String): T = document.getElementById(id).asInstanceOf[T]
}
