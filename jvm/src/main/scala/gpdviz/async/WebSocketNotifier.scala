package gpdviz.async

import akka.NotUsed
import akka.http.scaladsl.model.ws.{Message, TextMessage}
import akka.stream.scaladsl.{BroadcastHub, Flow, Keep, RunnableGraph, Sink, Source}
import akka.stream.{ActorMaterializer, ThrottleMode}
import gpdviz.model.{DataStream, ObsData, SensorSystem}

import scala.collection.immutable.Seq
import scala.concurrent.duration._


// TODO
class WebSocketNotifier()(implicit materializer: ActorMaterializer) extends Notifier {
  // initially based on https://groups.google.com/d/msg/akka-user/aA7RD2On_K0/6SJDgOPpAAAJ

  // The source to broadcast (just ints for simplicity)
  private val dataSource = Source(1 to 1000).throttle(1, 1.second, 1, ThrottleMode.Shaping).map(_.toString)

  // Go via BroadcastHub to allow multiple clients to connect
  private val runnableGraph: RunnableGraph[Source[String, NotUsed]] =
    dataSource.toMat(BroadcastHub.sink(bufferSize = 256))(Keep.right)

  private val producer: Source[String, NotUsed] = runnableGraph.run()

  // add sink to avoid backpressuring the original flow when no clients are attached
  producer.runWith(Sink.ignore)

  val wsHandler: Flow[Message, Message, NotUsed] = {
    Flow[Message]
      .mapConcat(_ ⇒ Seq.empty[String]) // Ignore any data sent from the client
      .merge(producer) // Stream the data we want to the client
      .map(l ⇒ TextMessage(l.toString))
  }

  def details: String = "WebSockets"

  def getSensorSystemIndex(sysid: String, ssOpt: Option[SensorSystem],
                           indexResource: String = "web/index.html"): String = {
    val ssVar = "undefined"
    val template = io.Source.fromResource(indexResource).mkString
    template
      .replace("#sysid", sysid)
      .replace("#ssVar", ssVar)
      // TODO should not deal with pusher here!
      .replace("#pusherKey", "")
      .replace("#pusherChannel", "")
  }

  def notifySensorSystemRegistered(ss: SensorSystem): Unit = ()

  def notifyStreamAdded(ss: SensorSystem, str: DataStream): Unit = ()

  def notifyObservations2Added(ss: SensorSystem, strid: String, observations: Map[String, List[ObsData]]): Unit = ()

  def notifyStreamRemoved(ss: SensorSystem, strid: String): Unit = ()

  def notifySensorSystemUpdated(ss: SensorSystem): Unit = ()

  def notifySensorSystemRefresh(ss: SensorSystem): Unit = ()

  def notifySensorSystemUnregistered(ss: SensorSystem): Unit = ()
}
