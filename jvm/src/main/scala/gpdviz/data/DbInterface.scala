package gpdviz.data

import gpdviz.model._
import gpdviz.server.{GnError, ObservationsRegister, SSUpdate}

import scala.concurrent.Future


trait DbInterface {

  def details: String

  def createTables(): Future[Unit]

  def listSensorSystems(): Future[Seq[SensorSystemSummary]]

  def existsSensorSystem(sysid: String): Future[Boolean]

  def getSensorSystem(sysid: String): Future[Option[SensorSystem]]

  def getDataStream(sysid: String, strid: String): Future[Option[DataStream]]

  def registerSensorSystem(ss: SensorSystem): Future[Either[GnError, SensorSystemSummary]]

  def updateSensorSystem(sysid: String, ssu: SSUpdate): Future[Either[GnError, SensorSystemSummary]]

  def registerDataStream(sysid: String)
                        (ds: DataStream): Future[Either[GnError, DataStreamSummary]]

  def registerVariableDef(sysid: String, strid: String)
                         (vd: VariableDef): Future[Either[GnError, VariableDefSummary]]

  def registerObservations(sysid: String, strid: String)
                          (obssr: ObservationsRegister): Future[Either[GnError, ObservationsSummary]]

  def registerObservation(sysid: String, strid: String, time: String,
                          obsData: ObsData): Future[Either[GnError, ObservationsSummary]]

  def deleteSensorSystem(sysid: String): Future[Either[GnError, SensorSystemSummary]]

  def deleteDataStream(sysid: String, strid: String): Future[Either[GnError, DataStreamSummary]]

  def close(): Unit
}
