package gpdviz.server

import akka.http.scaladsl.marshalling.ToResponseMarshallable
import akka.http.scaladsl.model.StatusCodes.{Conflict, InternalServerError, NotFound}
import akka.http.scaladsl.model._
import gpdviz.async.Notifier
import gpdviz.data.DbInterface
import gpdviz.model.{DataStream, SensorSystem}
import pprint.PPrinter.Color.{apply ⇒ pp}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Future, Promise}
import scala.util.control.NonFatal

trait GpdvizServiceImpl extends JsonImplicits  {
  def db: DbInterface
  def notifier: Notifier

  def registerSensorSystem(ssr: SSRegister): Future[ToResponseMarshallable] = {
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
        db.registerSensorSystem(ss) map {
          case Right(ssSum) ⇒
            notifier.notifySensorSystemRegistered(ss)
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

  def updateSensorSystem(sysid: String, ssu: SSUpdate): Future[ToResponseMarshallable] = {
    println(s"updateSensorSystem: sysid=$sysid ssu=$ssu")
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

  def unregisterSensorSystem(sysid: String): Future[ToResponseMarshallable] = {
    println(s"unregisterSensorSystem: sysid=$sysid")
    db.deleteSensorSystem(sysid) map {
      case Right(ssSum) ⇒
        notifier.notifySensorSystemUnregistered(sysid)
        ssSum
      case Left(error) ⇒
        if (error.code < 500)
          StatusCodes.custom(error.code, error.msg)
        else
          InternalServerError -> error
    }
  }

  def addStream(sysid: String, strr: StreamRegister): Future[ToResponseMarshallable] = {
    println(s"addStream: sysid=$sysid strid=${strr.strid}")
    val ds = DataStream(
      strid       = strr.strid,
      name        = strr.name,
      description = strr.description,
      mapStyle    = strr.mapStyle,
      zOrder      = strr.zOrder.getOrElse(0),
      variables   = strr.variables,
      chartStyle  = strr.chartStyle
    )
    db.registerDataStream(sysid)(ds) map {
      case Right(dsSum) ⇒
        notifier.notifyStreamAdded(sysid, ds)
        dsSum
      case Left(error) ⇒
        if (error.code < 500)
          StatusCodes.custom(error.code, error.msg)
        else
          InternalServerError -> error
    }
  }

  def addObservations(sysid: String, strid: String, obssr: ObservationsRegister): Future[ToResponseMarshallable] = Future {
    println(s"addObservations: sysid=$sysid, strid=$strid, obssr=${pp(obssr.observations)}")
    try {
      db.registerObservations(sysid, strid)(obssr) map { obsSum ⇒
        notifier.notifyObservations2Added(sysid, strid, obssr.observations)
        obsSum
      }
    }
    catch {
      case NonFatal(ex) ⇒
        ex.printStackTrace()
        InternalServerError -> ex.getMessage
    }
  }

  def getStream(sysid: String, strid: String): Future[ToResponseMarshallable] = {
    db.getDataStream(sysid, strid) map {
      case Some(ds) ⇒ ds
      case None     ⇒ NotFound -> GnErrorF.dataStreamUndefined(sysid, strid)
    }
  }

  def deleteStream(sysid: String, strid: String): Future[ToResponseMarshallable] = {
    println(s"deleteStream: sysid=$sysid strid=$strid")
    db.deleteDataStream(sysid, strid) map {
      case Right(dsSum) ⇒
        notifier.notifyStreamRemoved(sysid, strid)
        dsSum
      case Left(error) ⇒ InternalServerError -> error
    }
  }

  def getSensorSystemIndex(sysid: String): Future[ToResponseMarshallable] = {
    println(s"getSensorSystemIndex calling getSensorSystem sysid=$sysid")
    db.getSensorSystem(sysid) map { ssOpt ⇒
      val ssIndex = notifier.getSensorSystemIndex(sysid, ssOpt)
      HttpEntity(ContentType(MediaTypes.`text/html`, HttpCharsets.`UTF-8`), ssIndex.getBytes("UTF-8"))
    }
  }
}
