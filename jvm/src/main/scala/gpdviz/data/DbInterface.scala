package gpdviz.data

import gpdviz.model.{SensorSystem, SensorSystemSummary}
import gpdviz.server.GnError


trait DbInterface {

  def details: String

  def listSensorSystems(): Seq[SensorSystemSummary]

  def getSensorSystem(sysid: String): Option[SensorSystem]

  def saveSensorSystem(ss: SensorSystem): Either[GnError, SensorSystem]

  def deleteSensorSystem(sysid: String): Either[GnError, SensorSystem]
}
