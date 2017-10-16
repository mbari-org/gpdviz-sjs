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
          case Right(sysid) ⇒
            notifier.notifySensorSystemRegistered(ss)
            p.success(sysid)
          case Left(error) ⇒
            p.success(InternalServerError -> error)
        }

      case true ⇒
        p.success(Conflict -> GnError(409, "Already registered", sysid = Some(ssr.sysid)))
    }

    p.future
  }

  def getSensorSystem(sysid: String): Future[ToResponseMarshallable] = withSensorSystem(sysid) { ss ⇒ Future(ss) }

  def updateSensorSystem(sysid: String, ssu: SSUpdate): Future[ToResponseMarshallable] = {
    println(s"updateSensorSystem: sysid=$sysid ssu=$ssu")
    db.updateSensorSystem(sysid, ssu) map {
      case Right(_) ⇒
        notifier.notifySensorSystemUpdated(sysid)
        if (ssu.refresh.getOrElse(false)) {
          notifier.notifySensorSystemRefresh(sysid)
        }
        sysid

      case Left(error) ⇒ InternalServerError -> error
    }
  }

  def unregisterSensorSystem(sysid: String): Future[ToResponseMarshallable] = {
    println(s"unregisterSensorSystem: sysid=$sysid")
    db.deleteSensorSystem(sysid) map {
      case Right(s) ⇒
        notifier.notifySensorSystemUnregistered(s)
        s
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
      case Right(_) ⇒
        notifier.notifyStreamAdded(sysid, ds)
        ds
      case Left(error) ⇒ InternalServerError -> error
    }
  }

  def addObservations(sysid: String, strid: String, obssr: ObservationsRegister): Future[ToResponseMarshallable] = Future {
    println(s"addObservations: sysid=$sysid, strid=$strid, obssr=${pp(obssr.observations)}")
    try {
      db.registerObservations(sysid, strid)(obssr) map { _ ⇒
        notifier.notifyObservations2Added(sysid, strid, obssr.observations)
        obssr
      }
    }
    catch {
      case NonFatal(ex) ⇒
        ex.printStackTrace()
        InternalServerError -> ex.getMessage
    }
  }

  def getStream(sysid: String, strid: String): Future[ToResponseMarshallable] = withSensorSystem(sysid) { ss ⇒
    Future {
      ss.streams.get(strid) match {
        case Some(str) ⇒ str
        case None ⇒ streamUndefined(sysid, strid)
      }
    }
  }

  def deleteStream(sysid: String, strid: String): Future[ToResponseMarshallable] = {
    println(s"deleteStream: sysid=$sysid strid=$strid")
    db.deleteDataStream(sysid, strid) map {
      case Right(_) ⇒
        notifier.notifyStreamRemoved(sysid, strid)
        strid
      case Left(error) ⇒ InternalServerError -> error
    }
  }

  private def withSensorSystem(sysid: String)(p : SensorSystem ⇒ Future[ToResponseMarshallable]): Future[ToResponseMarshallable] = {
    println(s"withSensorSystem calling getSensorSystem sysid=$sysid")
    db.getSensorSystem(sysid) map {
      case Some(ss) ⇒ p(ss)
      case None ⇒ NotFound -> GnError(404, "not registered", sysid = Some(sysid))
    }
  }

  def getSensorSystemIndex(sysid: String): Future[ToResponseMarshallable] = {
    println(s"getSensorSystemIndex calling getSensorSystem sysid=$sysid")
    db.getSensorSystem(sysid) map { ssOpt ⇒
      val ssIndex = notifier.getSensorSystemIndex(sysid, ssOpt)
      HttpEntity(ContentType(MediaTypes.`text/html`, HttpCharsets.`UTF-8`), ssIndex.getBytes("UTF-8"))
    }
  }

  private def streamUndefined(sysid: String, strid: String): (StatusCodes.ClientError, GnError) =
    NotFound -> GnError(404, "stream undefined", sysid = Some(sysid), strid = Some(strid))

  def streamAlreadyDefined(sysid: String, strid: String): (StatusCodes.ClientError, GnError) =
    Conflict -> GnError(409, "stream already defined", sysid = Some(sysid), strid = Some(strid))
}
