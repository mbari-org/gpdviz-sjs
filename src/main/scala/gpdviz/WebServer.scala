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
import spray.json.{DefaultJsonProtocol, JsObject, JsValue}
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

case class StreamRegister(strid:        String,
                          name:         Option[String] = None,
                          description:  Option[String] = None,
                          mapStyle:     Option[Map[String, JsValue]] = None,
                          zOrder:       Option[Int] = None,
                          variables:    Option[JsObject] = None,
                          chartStyle:   Option[JsObject] = None
                          )

case class ObservationsRegister(observations: Map[String, List[ObsData]])


trait JsonImplicits extends DefaultJsonProtocol with SprayJsonSupport with GeoJsonProtocol {
  implicit val llRegFormat  = jsonFormat2(LatLon)
  implicit val ssRegFormat  = jsonFormat5(SSRegister)
  implicit val ssUpdFormat  = jsonFormat3(SSUpdate)
  implicit val strRegFormat = jsonFormat7(StreamRegister)

  implicit val scalarDataFormat = jsonFormat3(ScalarData)
  implicit val obsDataFormat  = jsonFormat3(ObsData)
  implicit val streamFormat  = jsonFormat8(DataStream)
  implicit val obssRegFormat = jsonFormat1(ObservationsRegister)
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

    val oneStr2Route = pathPrefix("api" / "ss" / Segment / Segment / "obs") { case (sysid, strid) =>
      (post & entity(as[ObservationsRegister])) { obssr =>
        complete {
          addObservations(sysid, strid, obssr)
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

    oneStr2Route ~ oneStrRoute ~ oneSsRoute ~ ssRoute ~ staticRoute
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
        val ds = DataStream(
          strid       = strr.strid,
          name        = strr.name,
          description = strr.description,
          mapStyle    = strr.mapStyle,
          zOrder      = strr.zOrder.getOrElse(0),
          variables   = strr.variables,
          chartStyle  = strr.chartStyle
        )
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

  private def addObservations(sysid: String, strid: String, obssr: ObservationsRegister): ToResponseMarshallable = withSensorSystem(sysid) { ss =>
    println(s"addObservations: sysid=$sysid, strid=$strid, obssr=${obssr.observations.size}")
    ss.streams.get(strid) match {
      case Some(str) =>
        val newObs = obssr.observations
        val previous = str.observations.getOrElse(Map.empty)
        var obsUpdated = previous
        newObs foreach { case (k, v) ⇒
          //v.map(_.geometry).filter(_.isDefined).map(_.get) foreach {geometry ⇒
          //  println(s"::: geometry: type=${geometry.getType}: $geometry")
          //}
          obsUpdated = obsUpdated.updated(k, v)
        }
        val strUpdated = str.copy(observations = Some(obsUpdated))
        val ssUpdated = ss.copy(streams = ss.streams.updated(strid, strUpdated))
        db.saveSensorSystem(ssUpdated) match {
          case Right(uss) =>
            notifier.notifyObservations2Added(uss, strid, newObs)
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
  val config: Config = ConfigFactory.load().resolve()
  val db: Db = new Db("data")
  val notifier: Notifier = new Notifier(config)

  def main(args: Array[String]) {
    implicit val system = ActorSystem()

    val interface = config.getString("gpdviz.http.interface")
    val port = config.getInt("gpdviz.http.port")

    startServer(interface, port) {
      routes
    }

    println(s"Gpdviz server '${notifier.serverName}' online at $interface:$port/")
    if (!args.contains("-d")) {
      println("Press RETURN to stop...")
      StdIn.readLine()
      system.terminate()
    }
  }
}
