package gpdviz.data

import java.time.OffsetDateTime

import gpdviz.model._
import gpdviz.server.{GnError, ObservationsAdd, SensorSystemUpdate}

import scala.concurrent.Future


trait DbInterface {

  def details: String

  def dropTables(): Future[Int]

  def createTables(): Future[Unit]

  def listSensorSystems(): Future[Seq[SensorSystemSummary]]

  def getSensorSystem(sysid: String): Future[Option[SensorSystem]]

  def addSensorSystem(ss: SensorSystem): Future[Either[GnError, SensorSystemSummary]]

  def updateSensorSystem(sysid: String, ssu: SensorSystemUpdate): Future[Either[GnError, SensorSystemSummary]]

  def deleteSensorSystem(sysid: String): Future[Either[GnError, SensorSystemSummary]]

  def getDataStream(sysid: String, strid: String): Future[Option[DataStream]]

  def addDataStream(sysid: String)
                   (ds: DataStream): Future[Either[GnError, DataStreamSummary]]

  def deleteDataStream(sysid: String, strid: String): Future[Either[GnError, DataStreamSummary]]

  def addVariableDef(sysid: String, strid: String)
                    (vd: VariableDef): Future[Either[GnError, VariableDefSummary]]

  def addObservations(sysid: String, strid: String)
                     (observations: Map[OffsetDateTime, List[ObsData]]
                     ): Future[Either[GnError, ObservationsSummary]]

  def close(): Unit
}
