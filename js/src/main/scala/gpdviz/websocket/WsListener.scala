package gpdviz.websocket

import org.scalajs.dom
import org.scalajs.dom.raw._

import scala.scalajs.js

// TODO for now, just some webSocket preparations
class WsListener {

  private val webSocketDiv = dom.document.getElementById("websocket")

  private val connectButton = button("connect") { (event: MouseEvent) ⇒
    connect()
    event.preventDefault()
  }

  private val disconnectButton = button("disconnect") { (event: MouseEvent) ⇒
    disconnect()
    event.preventDefault()
  }

  private val messagesDiv = dom.document.createElement("div")

  private var wsOpt: Option[WebSocket] = None

  webSocketDiv.appendChild(connectButton)
  webSocketDiv.appendChild(disconnectButton)
  webSocketDiv.appendChild(messagesDiv)

  updateButtons()

  private def connect(): Unit = {
    writeMsg("connecting ...")
    connectButton.disabled = true

    val ws = new WebSocket(getWebSocketUri(dom.document))
    ws.onopen = { (event: Event) ⇒
      writeMsg("Connected.")
      wsOpt = Some(ws)
      updateButtons()
      event
    }
    ws.onerror = { (event: ErrorEvent) ⇒
      writeMsg(s"Failed: code: ${event.colno}")
      wsOpt = None
      updateButtons()
    }

    ws.onmessage = { (event: MessageEvent) ⇒
      writeMsg(event.data.toString)
    }

    ws.onclose = { (event: Event) ⇒
      writeMsg("Connection closed")
      wsOpt = None
      updateButtons()
    }
  }

  private def disconnect(): Unit = {
    wsOpt foreach(_.close())
    wsOpt = None
    updateButtons()
    writeMsg("disconnected")
  }

  private def updateButtons(): Unit = {
    connectButton.disabled = wsOpt.isDefined
    disconnectButton.disabled = wsOpt.isEmpty
  }

  private def writeMsg(msg: String): Unit = {
    messagesDiv.innerHTML = msg
    println(msg)
  }

  private def getWebSocketUri(document: Document): String = {
    val wsProtocol = if (dom.document.location.protocol == "https:") "wss" else "ws"
    s"$wsProtocol://${dom.document.location.host}/ws"
  }

  private def button(label: String)(onClick: js.Function1[MouseEvent, _]): HTMLButtonElement = {
    val b = dom.document.createElement("button").asInstanceOf[HTMLButtonElement]
    b.textContent = label
    b.onclick = onClick
    b
  }
}
