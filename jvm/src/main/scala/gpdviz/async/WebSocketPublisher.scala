package gpdviz.async

import akka.NotUsed
import akka.actor.{ActorRef, ActorSystem, Props}
import akka.http.scaladsl.model.ws.TextMessage
import akka.stream.ActorMaterializer
import akka.stream.actor.ActorPublisher
import akka.stream.scaladsl.{Flow, Sink, Source}
import gpdviz._

import scala.concurrent.{ExecutionContext, ExecutionContextExecutor}

class WebSocketPublisher()(implicit materializer: ActorMaterializer,
                          executionContext: ExecutionContextExecutor,
                          system: ActorSystem
) extends Publisher {

  def details: String = "WebSockets"

  def publish(notif: Notif): Unit = {
    system.eventStream publish notif
  }

  // Some refs:
  // - https://stackoverflow.com/a/41359874/830737 -- possible mechanism to track connected clients
  // - https://stackoverflow.com/a/35313963/830737
  // - https://groups.google.com/d/msg/akka-user/aA7RD2On_K0/6SJDgOPpAAAJ

  private val dataSource: Source[Notif, ActorRef] = Source.actorPublisher[Notif](MyActorPublisher.props)

  val wsHandler: Flow[Any, TextMessage.Strict, NotUsed] = Flow
    .fromSinkAndSource(Sink.ignore, dataSource map { notif ⇒
      TextMessage.Strict(upickle.default.write(notif))
    })
}

class MyActorPublisher extends ActorPublisher[Notif] {
  override def preStart: Unit = context.system.eventStream.subscribe(self, classOf[Notif])

  override def receive: Receive = {
    case msg: Notif ⇒
      if (isActive && totalDemand > 0) {
        // Pushes the message onto the stream
        onNext(msg)
      }

    //case x ⇒ println("RECEIVE: " + x + " " +x.getClass.getName)
  }
}

object MyActorPublisher {
  def props(implicit ctx: ExecutionContext): Props = Props(new MyActorPublisher())
}
