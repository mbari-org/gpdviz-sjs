package gpdviz.websocket

import gpdviz.Notif
import gpdviz.webapp.Util
import org.scalajs.dom
import org.scalajs.dom.raw._
import org.scalajs.dom.window.console

import scala.scalajs.js
import scala.scalajs.js.timers._


class WsListener(sysid: String, handleNotification: (Notif) ⇒ Unit) {

  private val connectButton = button("connect") { (event: MouseEvent) ⇒
    connect()
    event.preventDefault()
  }
  private var wsOpt: Option[WebSocket] = None
  private var keepAliveHandleOpt: Option[SetIntervalHandle] = None

  dom.document.getElementById("websocket").appendChild(connectButton)
  updateButtons()
  connect()

  private def connect(): Unit = {
    connectButton.textContent = "Connecting ..."
    connectButton.disabled = true

    val ws = new WebSocket(getWebSocketUri)
    ws.onopen = { (event: Event) ⇒
      wsOpt = Some(ws)
      keepAliveHandleOpt = Some(setInterval(40 * 1000) {
        wsOpt foreach { ws ⇒
          //console.log("ping")
          ws.send("keep-alive")
        }
      })
      updateButtons()
      event
    }
    ws.onerror = { (event: ErrorEvent) ⇒
      console.error(s"Failed: code: ${event.message}")
      closed()
    }
    ws.onmessage = { (event: MessageEvent) ⇒
      val notif = upickle.default.read[Notif](event.data.toString)
      handleNotification(notif)
    }
    ws.onclose = { _ ⇒
      closed()
    }
  }

  private def closed(): Unit = {
    wsOpt = None
    console.warn("Connection closed")
    keepAliveHandleOpt foreach clearInterval
    keepAliveHandleOpt = None
    updateButtons()
  }

  private def updateButtons(): Unit = {
    connectButton.disabled = wsOpt.isDefined
    connectButton.textContent = wsOpt.map(_ ⇒ "Connected").getOrElse("Reconnect")
  }

  private def getWebSocketUri: String = {
    val wsProtocol = if (dom.document.location.protocol == "https:") "wss:" else "ws:"
    s"${Util.baseUrl(wsProtocol)}/ws/$sysid"
  }

  private def button(label: String)(onClick: js.Function1[MouseEvent, _]): HTMLButtonElement = {
    val b = dom.document.createElement("button").asInstanceOf[HTMLButtonElement]
    b.textContent = label
    b.onclick = onClick
    b
  }
}
