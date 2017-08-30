package gpdviz.data

import gpdviz.model.{SensorSystem, SensorSystemSummary}
import gpdviz.server.GnError

import scala.concurrent.Future


trait DbInterface {

  def details: String

  def listSensorSystems(): Future[Seq[SensorSystemSummary]]

  def getSensorSystem(sysid: String): Future[Option[SensorSystem]]

  def saveSensorSystem(ss: SensorSystem): Future[Either[GnError, SensorSystem]]

  def deleteSensorSystem(sysid: String): Future[Either[GnError, SensorSystem]]
}
