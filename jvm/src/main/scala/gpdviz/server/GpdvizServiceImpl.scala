package gpdviz.server

import akka.http.scaladsl.marshalling.ToResponseMarshallable
import akka.http.scaladsl.model.StatusCodes.{Conflict, InternalServerError, NotFound}
import akka.http.scaladsl.model._
import com.typesafe.scalalogging.{LazyLogging ⇒ Logging}
import gpdviz.async.Notifier
import gpdviz.data.DbInterface
import gpdviz.model.{DataStream, SensorSystem}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Future, Promise}
import scala.util.control.NonFatal

trait GpdvizServiceImpl extends JsonImplicits with Logging {
  def db: DbInterface
  def notifier: Notifier

  def addSensorSystem(ssr: SensorSystemAdd): Future[ToResponseMarshallable] = {
    val p = Promise[ToResponseMarshallable]()
    db.existsSensorSystem(ssr.sysid) map {
      case false ⇒
        val ss = SensorSystem(ssr.sysid,
          name = ssr.name,
          description = ssr.description,
          pushEvents = ssr.pushEvents.getOrElse(true),
          center = ssr.center,
          clickListener = ssr.clickListener
        )
        db.addSensorSystem(ss) map {
          case Right(ssSum) ⇒
            notifier.notifySensorSystemAdded(ss)
            p.success(ssSum)
          case Left(error) ⇒
            p.success(InternalServerError -> error)
        }

      case true ⇒
        p.success(Conflict -> GnErrorF.sensorSystemDefined(ssr.sysid))
    }

    p.future
  }

  def getSensorSystem(sysid: String): Future[ToResponseMarshallable] = {
    db.getSensorSystem(sysid) map {
      case Some(ss) ⇒ ss
      case None     ⇒ NotFound -> GnErrorF.sensorSystemUndefined(sysid)
    }
  }

  def updateSensorSystem(sysid: String, ssu: SensorSystemUpdate): Future[ToResponseMarshallable] = {
    logger.debug(s"updateSensorSystem: sysid=$sysid ssu=$ssu")
    db.updateSensorSystem(sysid, ssu) map {
      case Right(ssSum) ⇒
        notifier.notifySensorSystemUpdated(sysid)
        if (ssu.refresh.getOrElse(false)) {
          notifier.notifySensorSystemRefresh(sysid)
        }
        ssSum

      case Left(error) ⇒ InternalServerError -> error
    }
  }

  def deleteSensorSystem(sysid: String): Future[ToResponseMarshallable] = {
    logger.debug(s"deleteSensorSystem: sysid=$sysid")
    db.deleteSensorSystem(sysid) map {
      case Right(ssSum) ⇒
        notifier.notifySensorSystemDeleted(sysid)
        ssSum
      case Left(error) ⇒
        if (error.code < 500)
          StatusCodes.custom(error.code, error.msg)
        else
          InternalServerError -> error
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
        dsSum
      case Left(error) ⇒
        if (error.code < 500)
          StatusCodes.custom(error.code, error.msg)
        else
          InternalServerError -> error
    }
  }

  def addObservations(sysid: String, strid: String, obssr: ObservationsAdd): Future[ToResponseMarshallable] = Future {
    import pprint.PPrinter.Color.{apply ⇒ pp}
    logger.debug(s"addObservations: sysid=$sysid, strid=$strid, obssr=${pp(obssr.observations)}")
    try {
      db.addObservations(sysid, strid)(obssr) map { obsSum ⇒
        notifier.notifyObservationsAdded(sysid, strid, obssr.observations)
        obsSum
      }
    }
    catch {
      case NonFatal(ex) ⇒
        ex.printStackTrace()
        InternalServerError -> ex.getMessage
    }
  }

  def getDataStream(sysid: String, strid: String): Future[ToResponseMarshallable] = {
    db.getDataStream(sysid, strid) map {
      case Some(ds) ⇒ ds
      case None     ⇒ NotFound -> GnErrorF.dataStreamUndefined(sysid, strid)
    }
  }

  def deleteDataStream(sysid: String, strid: String): Future[ToResponseMarshallable] = {
    import fansi.Color._
    logger.debug(Red(s"deleteDataStream: sysid=$sysid strid=$strid").toString)
    db.deleteDataStream(sysid, strid) map {
      case Right(dsSum) ⇒
        notifier.notifyDataStreamDeleted(sysid, strid)
        dsSum
      case Left(error) ⇒
        if (error.code < 500)
          StatusCodes.custom(error.code, error.msg)
        else
          InternalServerError -> error
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
    }

    logger.debug(s"getSensorSystemIndex calling getSensorSystem sysid=$sysid")
    db.getSensorSystem(sysid) map { ssOpt ⇒
      val ssIndex = getIndex(ssOpt)
      HttpEntity(ContentType(MediaTypes.`text/html`, HttpCharsets.`UTF-8`), ssIndex.getBytes("UTF-8"))
    }
  }
}
