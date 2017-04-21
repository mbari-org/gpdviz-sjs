package gpdviz

import akka.actor.ActorSystem
import com.cloudera.science.geojson.GeoJsonProtocol
import com.typesafe.config.{Config, ConfigFactory}
import gpdviz.async.Notifier
import gpdviz.data.Db
import gpdviz.model._
import spray.http.StatusCodes._
import spray.http._
import spray.httpx.SprayJsonSupport
import spray.httpx.marshalling.ToResponseMarshallable
import spray.json.{DefaultJsonProtocol, JsValue}
import spray.routing.SimpleRoutingApp

import scala.io.StdIn


// generic error for now
case class GnError(code: Int,
                   msg: String,
                   sysid: Option[String] = None,
                   strid: Option[String] = None
                   )

case class SSRegister(sysid: String,
                      name: Option[String] = None,
                      description: Option[String] = None,
                      pushEvents: Option[Boolean] = None,
                      center: Option[LatLon] = None
                      )

case class SSUpdate(pushEvents: Option[Boolean] = None,
                    center: Option[LatLon] = None,
                    refresh: Option[Boolean] = None
                    )

case class StreamRegister(strid: String,
                          style: Option[Map[String, JsValue]] = None,
                          zOrder: Option[Int] = None
                          )

case class ObsRegister(values: List[DataObs]
                       )


trait JsonImplicits extends DefaultJsonProtocol with SprayJsonSupport with GeoJsonProtocol {
  implicit val llRegFormat  = jsonFormat2(LatLon)
  implicit val ssRegFormat  = jsonFormat5(SSRegister)
  implicit val ssUpdFormat  = jsonFormat3(SSUpdate)
  implicit val strRegFormat = jsonFormat3(StreamRegister)
  implicit val obsFormat    = jsonFormat4(DataObs)
  implicit val obsRegFormat = jsonFormat1(ObsRegister)

  implicit val streamFormat  = jsonFormat4(DataStream)
  implicit val systemFormat  = jsonFormat6(SensorSystem)

  implicit val dbErrorFormat = jsonFormat4(GnError)
}


trait MyService extends SimpleRoutingApp with JsonImplicits  {
  def config: Config
  def db: Db
  def notifier: Notifier

  def routes = {
    val ssRoute = path("api" / "ss" ) {
      (post & entity(as[SSRegister])) { ssr =>
        complete {
          registerSensorSystem(ssr)
        }
      } ~
        get {
          complete {
            db.listSensorSystems()
          }
        }
    }

    val oneSsRoute = pathPrefix("api" / "ss" / Segment) { sysid =>
      (post & entity(as[StreamRegister])) { strr =>
        complete {
          addStream(sysid, strr)
        }
      } ~
        get {
          complete {
            getSensorSystem(sysid)
          }
      } ~
        (put & entity(as[SSUpdate])) { ssu =>
          complete {
            updateSensorSystem(sysid, ssu)
          }
        } ~
        delete {
          complete {
            unregisterSensorSystem(sysid)
          }
        }
    }

    val oneStrRoute = pathPrefix("api" / "ss" / Segment / Segment) { case (sysid, strid) =>
      (post & entity(as[ObsRegister])) { obsr =>
        complete {
          addObservation(sysid, strid, obsr)
        }
      } ~
        get {
          complete {
            getStream(sysid, strid)
          }
        } ~
        delete {
          complete {
            deleteStream(sysid, strid)
          }
        }
    }

    val staticRoute = get {
      path(Segment / Rest) { case (sysid, rest) =>
        getFromFile("static/web/" + rest)
      } ~
        path(Segment ~ Slash) { sysid =>
        complete {
          getSensorSystemIndex(sysid)
        } ~
          getFromBrowseableDirectory("static/web")
      } ~
        getFromBrowseableDirectory("static/")
    }

    oneStrRoute ~ oneSsRoute ~ ssRoute ~ staticRoute
  }

  private def registerSensorSystem(ssr: SSRegister): ToResponseMarshallable = {
    db.getSensorSystem(ssr.sysid) match {
      case None =>
        val ss = SensorSystem(ssr.sysid,
          name = ssr.name,
          description = ssr.description,
          pushEvents = ssr.pushEvents.getOrElse(true),
          center = ssr.center
        )
        db.saveSensorSystem(ss) match {
          case Right(rss) =>
            notifier.notifySensorSystemRegistered(rss)
            rss
          case Left(error) => InternalServerError -> error
        }

      case Some(ss) => Conflict -> GnError(409, "Already registered", sysid = Some(ssr.sysid))
    }
  }

  private def getSensorSystem(sysid: String): ToResponseMarshallable = withSensorSystem(sysid) { ss => ss }

  private def updateSensorSystem(sysid: String, ssu: SSUpdate): ToResponseMarshallable = withSensorSystem(sysid) { ss =>
    //println(s"updateSensorSystem: sysid=$sysid ssu=$ssu")

    var updated = ss.copy()
    ssu.pushEvents foreach {
      pe => updated = updated.copy(pushEvents = pe)
    }
    ssu.center foreach { _ =>
      updated = updated.copy(center = ssu.center)
    }

    db.saveSensorSystem(updated) match {
      case Right(uss) =>
        notifier.notifySensorSystemUpdated(uss)
        if (ssu.refresh.getOrElse(false)) {
          notifier.notifySensorSystemRefresh(uss)
        }
        uss
      case Left(error) => InternalServerError -> error
    }
  }

  private def unregisterSensorSystem(sysid: String): ToResponseMarshallable = withSensorSystem(sysid) { ss =>
    db.deleteSensorSystem(sysid) match {
      case Right(s) =>
        notifier.notifySensorSystemUnregistered(s)
        s
      case Left(error) => InternalServerError -> error
    }
  }

  private def addStream(sysid: String, strr: StreamRegister): ToResponseMarshallable = withSensorSystem(sysid) { ss =>
    ss.streams.get(strr.strid) match {
      case None =>
        val ds = DataStream(strr.strid, strr.style, zOrder = strr.zOrder.getOrElse(0))
        val updated = ss.copy(streams = ss.streams.updated(strr.strid, ds))
        db.saveSensorSystem(updated) match {
          case Right(uss) =>
            notifier.notifyStreamAdded(uss, ds)
            uss
          case Left(error) => InternalServerError -> error
        }

      case Some(_) => streamAlreadyDefined(sysid, strr.strid)
    }
  }

  private def addObservation(sysid: String, strid: String, obsr: ObsRegister): ToResponseMarshallable = withSensorSystem(sysid) { ss =>
    //println(s"addObservation: sysid=$sysid, strid=$strid, obsr=$obsr")
    ss.streams.get(strid) match {
      case Some(str) =>
        val newObs = obsr.values
        val obsUpdated: List[DataObs] = str.obs.getOrElse(List.empty) ++ newObs
        val strUpdated = str.copy(obs = Some(obsUpdated))
        val ssUpdated = ss.copy(streams = ss.streams.updated(strid, strUpdated))
        db.saveSensorSystem(ssUpdated) match {
          case Right(uss) =>
            notifier.notifyObservationsAdded(uss, strid, newObs)
            newObs

          case Left(error) => InternalServerError -> error
        }

      case None => streamUndefined(sysid, strid)
    }
  }

  private def getStream(sysid: String, strid: String): ToResponseMarshallable = withSensorSystem(sysid) { ss =>
    ss.streams.get(strid) match {
      case Some(str) => str
      case None => streamUndefined(sysid, strid)
    }
  }

  private def deleteStream(sysid: String, strid: String): ToResponseMarshallable = withSensorSystem(sysid) { ss =>
    ss.streams.get(strid) match {
      case Some(_) =>
        val updated = ss.copy(streams = ss.streams - strid)
        db.saveSensorSystem(updated) match {
          case Right(uss) =>
            notifier.notifyStreamRemoved(uss, strid)
            uss
          case Left(error) => InternalServerError -> error
        }

      case None => streamUndefined(sysid, strid)
    }
  }

  private def withSensorSystem(sysid: String)(p : SensorSystem => ToResponseMarshallable): ToResponseMarshallable = {
    db.getSensorSystem(sysid) match {
      case Some(ss) => p(ss)
      case None => NotFound -> GnError(404, "not registered", sysid = Some(sysid))
    }
  }

  private def getSensorSystemIndex(sysid: String): ToResponseMarshallable = {
    val ssOpt = db.getSensorSystem(sysid)
    val ssIndex = notifier.getSensorSystemIndex(sysid, ssOpt)
    HttpEntity(ContentType(MediaTypes.`text/html`, HttpCharsets.`UTF-8`), ssIndex.getBytes("UTF-8"))
  }

  private def streamUndefined(sysid: String, strid: String) =
    NotFound -> GnError(404, "stream undefined", sysid = Some(sysid), strid = Some(strid))

  private def streamAlreadyDefined(sysid: String, strid: String) =
    Conflict -> GnError(409, "stream already defined", sysid = Some(sysid), strid = Some(strid))
}

object WebServer extends MyService {
  val config = ConfigFactory.load().resolve()
  val db = new Db("data")
  val notifier = new Notifier(config)

  def main(args: Array[String]) {
    implicit val system = ActorSystem()

    val interface = config.getString("gpdviz.http.interface")
    val port = config.getInt("gpdviz.http.port")

    startServer(interface, port) {
      routes
    }

    println(s"Server online at $interface:$port/")
    if (!args.contains("-d")) {
      println("Press RETURN to stop...")
      StdIn.readLine()
      system.terminate()
    }
  }
}
