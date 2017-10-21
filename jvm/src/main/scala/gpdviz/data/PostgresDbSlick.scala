package gpdviz.data

import com.typesafe.config.Config
import gpdviz.model._
import gpdviz.server.{GnError, ObservationsRegister, SSUpdate}
import pprint.PPrinter.Color.{apply ⇒ pp}

import MyPostgresProfile.api._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future


case class PgSSensorSystem(
                           sysid:        String,
                           name:         Option[String] = None,
                           description:  Option[String] = None,
                           pushEvents:   Boolean = true,
                           centerLat:    Option[Double] = None,
                           centerLon:    Option[Double] = None,
                           zoom:         Option[Int] = None,
                           clickListener: Option[String] = None
                         )

case class PgSDataStream(
                         sysid:        String,
                         strid:        String,
                         name:         Option[String] = None,
                         description:  Option[String] = None,
                         mapStyle:     Option[String] = None,
                         zOrder:       Int = 0,
                         chartStyle:   Option[String] = None
                       )

case class PgSVariableDef(
                          sysid:         String,
                          strid:         String,
                          name:          String,
                          units:         Option[String] = None,
                          chartStyle:    Option[String] = None
                        )

case class PgSObservation(
                          sysid:         String,
                          strid:         String,
                          time:          String,
                          feature:       Option[String] = None,
                          geometry:      Option[String] = None,
                          // scalarData:
                          vars:      List[String],
                          vals:      List[Double],
                          lat:       Option[Double],
                          lon:       Option[Double]
                        )


class PostgresDbSlick(slickConfig: Config) extends DbInterface {

  val details: String = s"PostgreSQL-based database (slick)"

  val db = Database.forConfig("slick", slickConfig)

  class SensorSystems(tag: Tag) extends Table[
    (String, Option[String], Option[String], Option[Boolean], Option[Double], Option[Double], Option[Int], Option[String])](tag, "sensorsystem") {

    def sysid         = column[String]("sysid", O.PrimaryKey)
    def name          = column[Option[String]]("name")
    def description   = column[Option[String]]("description")
    def pushEvents    = column[Option[Boolean]]("pushEvents")
    def centerLat     = column[Option[Double]]("centerLat")
    def centerLon     = column[Option[Double]]("centerLon")
    def zoom          = column[Option[Int]]("zoom")
    def clickListener = column[Option[String]]("clickListener")

    def * = (sysid, name, description, pushEvents, centerLat, centerLon, zoom, clickListener)
  }
  val sensorsystem = TableQuery[SensorSystems]

  class DataStreams(tag: Tag) extends Table[
    (String, String, String, String, String, Int, String)](tag, "datastream") {

    def sysid         = column[String]("sysid", O.PrimaryKey)
    def strid         = column[String]("strid", O.PrimaryKey)
    def name          = column[String]("name")
    def description   = column[String]("description")
    def mapStyle      = column[String]("mapStyle")
    def zOrder        = column[Int]("zOrder")
    def chartStyle    = column[String]("chartStyle")

    def * = (sysid, strid, name, description, mapStyle, zOrder, chartStyle)

    def ss = foreignKey("fk_ds_ss", sysid, sensorsystem)(_.sysid)
  }
  val datastream = TableQuery[DataStreams]

  class VariableDefs(tag: Tag) extends Table[
    (String, String, String, String, String)](tag, "variabledef") {

    def sysid         = column[String]("sysid", O.PrimaryKey)
    def strid         = column[String]("strid", O.PrimaryKey)
    def name          = column[String]("name")
    def units         = column[String]("units")
    def chartStyle    = column[String]("chartStyle")

    def * = (sysid, strid, name, units, chartStyle)

    def fk_vd_ds = foreignKey("fk_vd_ds", (sysid, strid), datastream)(x ⇒ (x.sysid, x.strid))
  }
  val variabledef = TableQuery[VariableDefs]

  class Observations(tag: Tag) extends Table[
    (String, String, String, String, String, List[String], List[Double], Double, Double)](tag, "observation") {

    def sysid         = column[String]("sysid")
    def strid         = column[String]("strid")
    def time          = column[String]("time")
    def feature       = column[String]("feature")
    def geometry      = column[String]("geometry")
    def vars          = column[List[String]]("vars")
    def vals          = column[List[Double]]("vals")
    def lat           = column[Double]("lat")
    def lon           = column[Double]("lon")

    def * = (sysid, strid, time, feature, geometry, vars, vals, lat, lon)

    def fk_obs_ds = foreignKey("fk_obs_ds", (sysid, strid), datastream)(x ⇒ (x.sysid, x.strid))
  }
  val observation = TableQuery[Observations]

  def createTables(): Unit = {
  }

  def listSensorSystems(): Future[Seq[SensorSystemSummary]] = {
    val q = sensorsystem.map(ss ⇒ (ss.sysid, ss.name, ss.description))
    val action = q.result

    db.run(action).map(_.map { pss ⇒ SensorSystemSummary(
      pss._1,
      pss._2,
      pss._3
      //,strIds.toSet
    )})
  }

  def existsSensorSystem(sysid: String): Future[Boolean] = Future {

    false
  }

  def registerSensorSystem(ss: SensorSystem): Future[Either[GnError, SensorSystemSummary]] = Future {

    // ...

    val regStream = registerDataStream(ss.sysid) _
    ss.streams.values foreach regStream

    Right(SensorSystemSummary(
      ss.sysid,
      name = ss.name,
      description = ss.description
    ))
  }

  def updateSensorSystem(sysid: String, ssu: SSUpdate): Future[Either[GnError, SensorSystemSummary]] = Future {

    Right(SensorSystemSummary(sysid))
  }

  def registerDataStream(sysid: String)
                        (ds: DataStream): Future[Either[GnError, DataStreamSummary]] = Future {

    Right(DataStreamSummary(sysid, ds.strid))
  }

  def registerVariableDef(sysid: String, strid: String)
                         (vd: VariableDef): Future[Either[GnError, VariableDefSummary]] = Future {

    Right(VariableDefSummary(sysid, strid, vd.name, vd.units))
  }

  def registerObservations(sysid: String, strid: String)
                          (obssr: ObservationsRegister): Future[Either[GnError, ObservationsSummary]] = Future {
    var num = 0
    obssr.observations foreach { case (time, list) ⇒
      list.foreach(registerObservation(sysid, strid, time, _))
      num += list.length
    }
    Right(ObservationsSummary(sysid, strid, added = Some(num)))
  }

  def registerObservation(sysid: String, strid: String, time: String,
                          obsData: ObsData): Future[Either[GnError, ObservationsSummary]] = Future {

    Right(ObservationsSummary(sysid, strid, time = Some(time), added = Some(1)))
  }

  def getSensorSystem(sysid: String): Future[Option[SensorSystem]] = Future {

 /*   val q = sensorsystem.filter(_.sysid == sysid)
    val action = q.result

    db.run(action).map(_.map(pss ⇒ SensorSystem(
      sysid = pss._1,
      name = pss._2,
      description = pss._3
      //,strIds.toSet
    )).headOption)
 */
    None
  }

  def deleteSensorSystem(sysid: String): Future[Either[GnError, SensorSystemSummary]] = Future {
    Right(SensorSystemSummary(sysid))
  }

  def deleteDataStream(sysid: String, strid: String): Future[Either[GnError, DataStreamSummary]] = Future {
    Right(DataStreamSummary(sysid, strid))
  }

  def close(): Unit = db.close
}
