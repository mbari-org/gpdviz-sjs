package gpdviz.data

import com.typesafe.config.Config
import gpdviz.model._
import gpdviz.server.{GnError, ObservationsRegister, SSUpdate}
import com.typesafe.scalalogging.{StrictLogging ⇒ Logging}
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


class PostgresDbSlick(slickConfig: Config) extends DbInterface with Logging {

  val details: String = s"PostgreSQL-based database (slick)"

  // path: ``empty string for the top level of the Config object''
  private val db = Database.forConfig(path = "", slickConfig)

  class SensorSystems(tag: Tag) extends Table[
    PgSSensorSystem
    //(String, Option[String], Option[String], Option[Boolean], Option[Double], Option[Double], Option[Int], Option[String])
    ](tag, "sensorsystem") {

    def sysid         = column[String]("sysid", O.PrimaryKey)
    def name          = column[Option[String]]("name")
    def description   = column[Option[String]]("description")
    def pushEvents    = column[Boolean]("pushEvents")
    def centerLat     = column[Option[Double]]("centerLat")
    def centerLon     = column[Option[Double]]("centerLon")
    def zoom          = column[Option[Int]]("zoom")
    def clickListener = column[Option[String]]("clickListener")

    def * = (sysid, name, description, pushEvents, centerLat, centerLon, zoom, clickListener
            ) <> (PgSSensorSystem.tupled, PgSSensorSystem.unapply)
  }
  val sensorsystem = TableQuery[SensorSystems]

  class DataStreams(tag: Tag) extends Table[
    PgSDataStream
    //(String, String, String, String, String, Int, String)
    ](tag, "datastream") {

    def sysid         = column[String]("sysid")
    def strid         = column[String]("strid")
    def name          = column[Option[String]]("name")
    def description   = column[Option[String]]("description")
    def mapStyle      = column[Option[String]]("mapStyle")
    def zOrder        = column[Int]("zOrder")
    def chartStyle    = column[Option[String]]("chartStyle")

    def * = (sysid, strid, name, description, mapStyle, zOrder, chartStyle
            ) <> (PgSDataStream.tupled, PgSDataStream.unapply)

    def pk_ds = primaryKey("pk_ds", (sysid, strid))
    def fk_ds_ss = foreignKey("fk_ds_ss", sysid, sensorsystem)(_.sysid)
  }
  val datastream = TableQuery[DataStreams]

  class VariableDefs(tag: Tag) extends Table[
    PgSVariableDef
    //(String, String, String, String, String)
    ](tag, "variabledef") {

    def sysid         = column[String]("sysid")
    def strid         = column[String]("strid")
    def name          = column[String]("name")
    def units         = column[Option[String]]("units")
    def chartStyle    = column[Option[String]]("chartStyle")

    def * = (sysid, strid, name, units, chartStyle
            ) <> (PgSVariableDef.tupled, PgSVariableDef.unapply)

    def pk_vd = primaryKey("pk_vd", (sysid, strid, name))
    def fk_vd_ds = foreignKey("fk_vd_ds", (sysid, strid), datastream)(x ⇒ (x.sysid, x.strid))
  }
  val variabledef = TableQuery[VariableDefs]

  class Observations(tag: Tag) extends Table[
    PgSObservation
    //(String, String, String, String, String, List[String], List[Double], Double, Double)
    ](tag, "observation") {

    def sysid         = column[String]("sysid")
    def strid         = column[String]("strid")
    def time          = column[String]("time")
    def feature       = column[Option[String]]("feature")
    def geometry      = column[Option[String]]("geometry")
    def vars          = column[List[String]]("vars")
    def vals          = column[List[Double]]("vals")
    def lat           = column[Option[Double]]("lat")
    def lon           = column[Option[Double]]("lon")

    def * = (sysid, strid, time, feature, geometry, vars, vals, lat, lon
            ) <> (PgSObservation.tupled, PgSObservation.unapply)

    def fk_obs_ds = foreignKey("fk_obs_ds", (sysid, strid), datastream)(x ⇒ (x.sysid, x.strid))
  }
  val observation = TableQuery[Observations]

  private val schema = sensorsystem.schema ++ datastream.schema ++ variabledef.schema ++ observation.schema

  def createTables(): Future[Unit] = {
    db.run(schema.create)
  }

  def listSensorSystems(): Future[Seq[SensorSystemSummary]] = {
    val q = for {
      ss ← sensorsystem
      ds ← datastream if ds.sysid === ss.sysid
    } yield (ss, ds)

    val action = q.result

    db.run(action).map(_.groupBy(_._1.sysid).map({ case (sysid, ssdss) ⇒
      val ss = ssdss.head._1
      val dss = ssdss.map(_._2)
      SensorSystemSummary(
        sysid,
        ss.name,
        ss.description,
        streamIds = dss.map(_.strid).toSet
      )
    }).toSeq)
  }

  def existsSensorSystem(sysid: String): Future[Boolean] = {
    val q = sensorsystem.filter(_.sysid === sysid)
    db.run(q.result).map(_.nonEmpty)
  }

  def registerSensorSystem(ss: SensorSystem): Future[Either[GnError, SensorSystemSummary]] = {
    val ssAction =
      sensorsystem += PgSSensorSystem(
        ss.sysid,
        name = ss.name,
        description = ss.description,
        pushEvents = ss.pushEvents,
        centerLat = ss.center.map(_.lat),
        centerLon = ss.center.map(_.lon),
        zoom = ss.zoom,
        clickListener = ss.clickListener
      )

    val dsActions = ss.streams.values.map(addDataStreamAction(ss.sysid, _))

    val actions = List(ssAction) ++ dsActions

    db.run(DBIO.seq(actions: _*)) map { _ ⇒
      Right(SensorSystemSummary(
        ss.sysid,
        name = ss.name,
        description = ss.description
      ))
    }
  }

  def updateSensorSystem(sysid: String, ssu: SSUpdate): Future[Either[GnError, SensorSystemSummary]] = {
    val action = sensorsystem.filter(_.sysid === sysid)
      .map(p ⇒ (p.centerLat, p.centerLon))
      .update((
        ssu.center.map(_.lat),
        ssu.center.map(_.lon)
      ))

    db.run(action) map { _ ⇒
      Right(SensorSystemSummary(sysid))
    }
  }

  private def addDataStreamAction(sysid: String, ds: DataStream) = {
    val dsAction = datastream += PgSDataStream(
      sysid,
      ds.strid,
      name = ds.name,
      description = ds.description,
      mapStyle = ds.mapStyle.map(utl.toJsonString),
      zOrder = ds.zOrder,
      chartStyle = ds.chartStyle.map(utl.toJsonString)
    )

    val vdActions = ds.variables.getOrElse(List.empty).map(addVariableDefAction(sysid, ds.strid, _))

    val obsActions = ds.observations.getOrElse(Map.empty) flatMap { case (time, obss) ⇒
      obss map (addObservationAction(sysid, ds.strid, time, _))
    }

    val actions = List(dsAction) ++ vdActions ++ obsActions
    DBIO.seq(actions: _*)
  }

  def registerDataStream(sysid: String)
                        (ds: DataStream): Future[Either[GnError, DataStreamSummary]] = {

    db.run(addDataStreamAction(sysid, ds)) map { _ ⇒
      Right(DataStreamSummary(sysid, ds.strid))
    }
  }

  private def addVariableDefAction(sysid: String, strid: String, vd: VariableDef) = {
    variabledef += PgSVariableDef(
      sysid,
      strid,
      name = vd.name,
      units = vd.units,
      chartStyle = vd.chartStyle.map(utl.toJsonString)
    )
  }

  def registerVariableDef(sysid: String, strid: String)
                         (vd: VariableDef): Future[Either[GnError, VariableDefSummary]] = {

    db.run(addVariableDefAction(sysid, strid, vd)) map { _ ⇒
      Right(VariableDefSummary(sysid, strid, vd.name, vd.units))
    }
  }

  def registerObservations(sysid: String, strid: String)
                          (obssr: ObservationsRegister): Future[Either[GnError, ObservationsSummary]] = {
    var num = 0
    val actions = obssr.observations flatMap { case (time, list) ⇒
      num += list.length
      list.map(addObservationAction(sysid, strid, time, _))
    }
    db.run(DBIO.seq(actions.toSeq: _*)) map { _ ⇒
      Right(ObservationsSummary(sysid, strid, added = Some(num)))
    }
  }

  private def addObservationAction(sysid: String, strid: String, time: String, obsData: ObsData) = {
    observation += PgSObservation(
      sysid,
      strid,
      time,
      feature = obsData.feature.map(utl.toJsonString),
      geometry = obsData.geometry.map(utl.toJsonString),
      vars = obsData.scalarData.map(_.vars).getOrElse(List.empty),
      vals = obsData.scalarData.map(_.vals).getOrElse(List.empty),
      lat = obsData.scalarData.flatMap(_.position.map(_.lat)),
      lon = obsData.scalarData.flatMap(_.position.map(_.lon)),
    )
  }

  def registerObservation(sysid: String, strid: String, time: String,
                          obsData: ObsData): Future[Either[GnError, ObservationsSummary]] = {

    db.run(addObservationAction(sysid, strid, time, obsData)) map { _ ⇒
      Right(ObservationsSummary(sysid, strid, time = Some(time), added = Some(1)))
    }
  }

  def getSensorSystem(sysid: String): Future[Option[SensorSystem]] = {
    val q = sensorsystem.filter(_.sysid === sysid)
    db.run(q.result.headOption).map(_ map { pss ⇒
      val streams = Map[String, DataStream]() // TODO streams
      SensorSystem(
        sysid = pss.sysid,
        name = pss.name,
        description = pss.description,
        streams = streams,
        pushEvents = pss.pushEvents,
        center = pss.centerLat.map(lat ⇒ LatLon(lat, pss.centerLon.get)),
        zoom = pss.zoom,
        clickListener = pss.clickListener
      )
    })
  }

  def deleteSensorSystem(sysid: String): Future[Either[GnError, SensorSystemSummary]] = {
    val ss = sensorsystem.filter(_.sysid === sysid)
    val dss = datastream.filter(_.sysid === sysid)
    val vds = variabledef.filter(_.sysid === sysid)
    val obs = observation.filter(_.sysid === sysid)

    db.run((
      obs.delete andThen
      vds.delete andThen
      dss.delete andThen
      ss.delete).transactionally) map { _ ⇒
      Right(SensorSystemSummary(sysid))
    }
  }

  def deleteDataStream(sysid: String, strid: String): Future[Either[GnError, DataStreamSummary]] = {
    val dss = datastream.filter(_.sysid === sysid)
    val vds = variabledef.filter(_.sysid === sysid)
    val obs = observation.filter(_.sysid === sysid)

    db.run((
      obs.delete andThen
        vds.delete andThen
        dss.delete).transactionally) map { _ ⇒
      Right(DataStreamSummary(sysid, strid))
    }
  }

  def close(): Unit = db.close
}
