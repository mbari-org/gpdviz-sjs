package gpdviz.server

import java.time.OffsetDateTime
import java.time.format.DateTimeParseException

import akka.http.scaladsl.marshalling.ToResponseMarshallable
import akka.http.scaladsl.model._
import com.typesafe.scalalogging.{LazyLogging ⇒ Logging}
import gpdviz.async.Notifier
import gpdviz.data.DbInterface
import gpdviz.model.{DataStream, SensorSystem, VariableDef}
import spray.json._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait GpdvizServiceImpl extends GpdvizJsonImplicits with Logging {
  def db: DbInterface
  def notifier: Notifier

  def addSensorSystem(ssr: SensorSystemAdd): Future[ToResponseMarshallable] = {
    logger.debug(s"addSensorSystem: sysid=${ssr.sysid}")
    val ss = SensorSystem(ssr.sysid,
      name = ssr.name,
      description = ssr.description,
      pushEvents = ssr.pushEvents.getOrElse(true),
      center = ssr.center,
      zoom = ssr.zoom,
      clickListener = ssr.clickListener
    )
    db.addSensorSystem(ss) map {
      case Right(ssSum) ⇒
        notifier.notifySensorSystemAdded(ss)
        goodResponse(StatusCodes.Created, ssSum.toJson)
      case Left(error) ⇒ errorResponse(error)
    }
  }

  def getSensorSystem(sysid: String): Future[ToResponseMarshallable] = {
    db.getSensorSystem(sysid) map {
      case Some(ss) ⇒ ss
      case None     ⇒ errorResponse(GnErrorF.sensorSystemUndefined(sysid))
    }
  }

  def updateSensorSystem(sysid: String, ssu: SensorSystemUpdate): Future[ToResponseMarshallable] = {
    logger.debug(s"updateSensorSystem: sysid=$sysid ssu=$ssu")
    db.updateSensorSystem(sysid, ssu) map {
      case Right(ssSum) ⇒
        notifier.notifySensorSystemUpdated(sysid, ssSum.pushEvents.contains(true))
        ssSum

      case Left(error) ⇒ errorResponse(error)
    }
  }

  def deleteSensorSystem(sysid: String): Future[ToResponseMarshallable] = {
    logger.debug(s"deleteSensorSystem: sysid=$sysid")
    db.deleteSensorSystem(sysid) map {
      case Right(ssSum) ⇒
        notifier.notifySensorSystemDeleted(sysid, ssSum.pushEvents.contains(true))
        ssSum

      case Left(error) ⇒ errorResponse(error)
    }
  }

  def addDataStream(sysid: String, strr: DataStreamAdd): Future[ToResponseMarshallable] = {
    logger.debug(s"addDataStream: sysid=$sysid strid=${strr.strid}")
    val ds = DataStream(
      strid       = strr.strid,
      name        = strr.name,
      description = strr.description,
      mapStyle    = strr.mapStyle,
      zOrder      = strr.zOrder.getOrElse(0),
      variables   = strr.variables,
      chartStyle  = strr.chartStyle
    )
    db.addDataStream(sysid)(ds) map {
      case Right(dsSum) ⇒
        notifier.notifyDataStreamAdded(sysid, ds)
        goodResponse(StatusCodes.Created, dsSum.toJson)
      case Left(error) ⇒ errorResponse(error)
    }
  }

  def addVariableDef(sysid: String, strid: String, vd: VariableDef): Future[ToResponseMarshallable] = {
    logger.debug(s"addVariableDef: sysid=$sysid strid=$strid vd=$vd")
    db.addVariableDef(sysid, strid)(vd) map {
      case Right(vdSum) ⇒
        notifier.notifyVariableDefAdded(sysid, strid, vd)
        goodResponse(StatusCodes.Created, vdSum.toJson)
      case Left(error) ⇒ errorResponse(error)
    }
  }

  def addObservations(sysid: String, strid: String, obssr: ObservationsAdd): Future[ToResponseMarshallable] = {
    import pprint.PPrinter.Color.{apply ⇒ pp}
    logger.debug(s"addObservations: sysid=$sysid, strid=$strid, obssr=${pp(obssr.observations)}")

    try {
      val observations = obssr.observations map { case (time, obss) ⇒
        OffsetDateTime.parse(time) -> obss
      }
      db.addObservations(sysid, strid)(observations) map { obsSum ⇒
        notifier.notifyObservationsAdded(sysid, strid, obssr.observations)
        goodResponse(StatusCodes.Created, obsSum.toJson)
      }
    }
    catch {
      case e: DateTimeParseException ⇒ Future {
        errorResponse(GnErrorF.malformedTimestamp(sysid, strid,
          timestamp = Some(e.getParsedString), msg = Some(e.getMessage)))
      }
    }
  }

  def getDataStream(sysid: String, strid: String): Future[ToResponseMarshallable] = {
    db.getDataStream(sysid, strid) map {
      case Some(ds) ⇒ ds
      case None     ⇒ errorResponse(GnErrorF.dataStreamUndefined(sysid, strid))
    }
  }

  def deleteDataStream(sysid: String, strid: String): Future[ToResponseMarshallable] = {
    import fansi.Color._
    logger.debug(Red(s"deleteDataStream: sysid=$sysid strid=$strid").toString)
    db.deleteDataStream(sysid, strid) map {
      case Right(dsSum) ⇒
        notifier.notifyDataStreamDeleted(sysid, strid)
        dsSum
      case Left(error) ⇒ errorResponse(error)
    }
  }

  def getSensorSystemIndex(sysid: String): Future[ToResponseMarshallable] = {
    def getIndex(ssOpt: Option[SensorSystem]): String = {
      import gpdviz.config.cfg
      val indexResource = "web/index.html"
      val template = scala.io.Source.fromResource(indexResource).mkString
      template
        .replace("#sysid", sysid)
        .replace("#externalUrl", cfg.externalUrl)
        .replace("#pusher", cfg.pusher.map(_ ⇒
          """<script src="//js.pusher.com/3.2/pusher.min.js"></script>""").getOrElse(""))
        .replace("#googleMap", cfg.map.googleMapApiKey.map(key ⇒
          s"""<script src="//maps.googleapis.com/maps/api/js?key=$key" async defer></script>""" +
            """<script src="leaflet/leaflet.gridlayer.googlemutant/Leaflet.GoogleMutant.js"></script>"""
            ).getOrElse(""))
    }

    logger.debug(s"getSensorSystemIndex calling getSensorSystem sysid=$sysid")
    db.getSensorSystem(sysid) map { ssOpt ⇒
      val ssIndex = getIndex(ssOpt)
      HttpEntity(ContentType(MediaTypes.`text/html`, HttpCharsets.`UTF-8`), ssIndex.getBytes("UTF-8"))
    }
  }

  private def goodResponse(status: StatusCode, jsonValue: JsValue): HttpResponse = {
    HttpResponse(
      status = status,
      entity = HttpEntity(
        ContentType(MediaTypes.`application/json`),
        jsonValue.compactPrint
      )
    )
  }

  private def errorResponse(error: GnError): HttpResponse = {
    HttpResponse(
      status = error.code,
      entity = HttpEntity(
        ContentType(MediaTypes.`application/json`),
        error.toJson.compactPrint
      )
    )
  }
}
