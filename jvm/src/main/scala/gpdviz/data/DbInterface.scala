package gpdviz.data

import gpdviz.model._
import gpdviz.server.GnError

import scala.concurrent.Future


trait DbInterface {

  def details: String

  def listSensorSystems(): Future[Seq[SensorSystemSummary]]

  def existsSensorSystem(sysid: String): Future[Boolean] = {
    import scala.concurrent.ExecutionContext.Implicits.global
    getSensorSystem(sysid).map(_.isDefined)
  }

  def getSensorSystem(sysid: String): Future[Option[SensorSystem]]

  def registerSensorSystem(ss: SensorSystem): Future[Either[GnError, String]] = ???

  def registerDataStream(sysid: String)(ds: DataStream): Future[Either[GnError, String]] = ???

  def registerVariableDef(sysid: String, strid: String)(vd: VariableDef): Future[Either[GnError, String]] = ???

  def registerObservation(sysid: String, strid: String, time: String)(obsData: ObsData): Future[Either[GnError, String]] = ???

  def deleteSensorSystem(sysid: String): Future[Either[GnError, String]]

  def close(): Unit = ()


  // TODO remove...
  def saveSensorSystem(ss: SensorSystem): Future[Either[GnError, String]] = ???

}
