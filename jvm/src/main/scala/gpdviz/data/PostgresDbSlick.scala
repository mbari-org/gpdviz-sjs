package gpdviz.data

import com.typesafe.config.Config
import com.typesafe.scalalogging.{LazyLogging ⇒ Logging}
import gpdviz.data.MyPostgresProfile.api._
import gpdviz.model._
import gpdviz.server.{GnError, GnErrorF, ObservationsAdd, SensorSystemUpdate}
import slick.dbio.Effect
import slick.sql.{FixedSqlAction, SqlAction}

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

  class SensorSystemTable(tag: Tag) extends Table[PgSSensorSystem](tag, "sensorsystem") {
    def sysid         = column[String]("sysid", O.PrimaryKey)
    def name          = column[Option[String]]("name")
    def description   = column[Option[String]]("description")
    def pushEvents    = column[Boolean]("pushEvents")
    def centerLat     = column[Option[Double]]("centerLat")
    def centerLon     = column[Option[Double]]("centerLon")
    def zoom          = column[Option[Int]]("zoom")
    def clickListener = column[Option[String]]("clickListener")

    def * = (sysid, name, description, pushEvents, centerLat, centerLon, zoom, clickListener
            ).mapTo[PgSSensorSystem]
  }
  private val sensorsystem = TableQuery[SensorSystemTable]

  class DataStreamTable(tag: Tag) extends Table[PgSDataStream](tag, "datastream") {
    def sysid         = column[String]("sysid")
    def strid         = column[String]("strid")
    def name          = column[Option[String]]("name")
    def description   = column[Option[String]]("description")
    def mapStyle      = column[Option[String]]("mapStyle")
    def zOrder        = column[Int]("zOrder")
    def chartStyle    = column[Option[String]]("chartStyle")

    def * = (sysid, strid, name, description, mapStyle, zOrder, chartStyle
            ).mapTo[PgSDataStream]

    def pk_ds = primaryKey("pk_ds", (sysid, strid))
    def fk_ds_ss = foreignKey("fk_ds_ss", sysid, sensorsystem)(_.sysid)
  }
  private val datastream = TableQuery[DataStreamTable]

  class VariableDefTable(tag: Tag) extends Table[PgSVariableDef](tag, "variabledef") {
    def sysid         = column[String]("sysid")
    def strid         = column[String]("strid")
    def name          = column[String]("name")
    def units         = column[Option[String]]("units")
    def chartStyle    = column[Option[String]]("chartStyle")

    def * = (sysid, strid, name, units, chartStyle
            ).mapTo[PgSVariableDef]

    def pk_vd = primaryKey("pk_vd", (sysid, strid, name))
    def fk_vd_ds = foreignKey("fk_vd_ds", (sysid, strid), datastream)(x ⇒ (x.sysid, x.strid))
  }
  private val variabledef = TableQuery[VariableDefTable]

  class ObservationTable(tag: Tag) extends Table[PgSObservation](tag, "observation") {
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
            ).mapTo[PgSObservation]

    def fk_obs_ds = foreignKey("fk_obs_ds", (sysid, strid), datastream)(x ⇒ (x.sysid, x.strid))
  }
  private val observation = TableQuery[ObservationTable]

  private val schema = sensorsystem.schema ++ datastream.schema ++ variabledef.schema ++ observation.schema

  def dropTables(): Future[Int] = {
    val action = sqlu"""
      SET CONSTRAINTS ALL DEFERRED;
      DROP TABLE IF EXISTS #${observation.baseTableRow.tableName};
      DROP TABLE IF EXISTS #${variabledef.baseTableRow.tableName};
      DROP TABLE IF EXISTS #${datastream.baseTableRow.tableName};
      DROP TABLE IF EXISTS #${sensorsystem.baseTableRow.tableName};
      """
    db.run(action.transactionally)
  }

  def createTables(): Future[Unit] =
    db.run(schema.create)

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

  def addSensorSystem(ss: SensorSystem): Future[Either[GnError, SensorSystemSummary]] = {
    val action = existsSensorSystemAction(ss.sysid) flatMap {
      case true  ⇒ DBIO.successful(GnErrorF.sensorSystemDefined(ss.sysid))
      case false ⇒
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

        val dsActions = ss.streams.values.map(dataStreamAddAction(ss.sysid, _))
        val actions = List(ssAction) ++ dsActions
        DBIO.seq(actions: _*)
    }

    db.run(action.transactionally) map {
      case e: GnError ⇒ Left(e)
      case _          ⇒
        Right(SensorSystemSummary(
          ss.sysid,
          name = ss.name,
          description = ss.description,
          pushEvents = Some(ss.pushEvents),
          center = ss.center
        ))
    }
  }

  def updateSensorSystem(sysid: String, ssu: SensorSystemUpdate): Future[Either[GnError, SensorSystemSummary]] = {
    val action = existsSensorSystemAction(sysid) flatMap {
      case false ⇒ DBIO.successful(GnErrorF.sensorSystemUndefined(sysid))
      case true  ⇒
        sensorsystem.filter(_.sysid === sysid)
          .map(p ⇒ (p.centerLat, p.centerLon))
          .update((
            ssu.center.map(_.lat),
            ssu.center.map(_.lon)
          ))
    }

    db.run(action.transactionally) map {
      case e: GnError ⇒ Left(e)
      case _          ⇒ Right(SensorSystemSummary(sysid))
    }
  }

  def getSensorSystem(sysid: String): Future[Option[SensorSystem]] =
    db.run(sensorSystemAction(sysid))

  def deleteSensorSystem(sysid: String): Future[Either[GnError, SensorSystemSummary]] = {
    val action = existsSensorSystemAction(sysid) flatMap {
      case false ⇒ DBIO.successful(GnErrorF.sensorSystemUndefined(sysid))
      case true  ⇒
        val ss = sensorsystem.filter(_.sysid === sysid)
        val dss = datastream.filter(_.sysid === sysid)
        val vds = variabledef.filter(_.sysid === sysid)
        val obs = observation.filter(_.sysid === sysid)
        obs.delete andThen vds.delete andThen dss.delete andThen ss.delete
    }

    db.run(action.transactionally) map {
      case e: GnError ⇒ Left(e)
      case _          ⇒ Right(SensorSystemSummary(sysid))
    }
  }

  def addDataStream(sysid: String)
                   (ds: DataStream): Future[Either[GnError, DataStreamSummary]] = {

    val action = existsDataStreamAction(sysid, ds.strid) flatMap {
      case true  ⇒ DBIO.successful(GnErrorF.dataStreamDefined(sysid, ds.strid))
      case false ⇒ dataStreamAddAction(sysid, ds)
    }

    db.run(action.transactionally) map {
      case e: GnError ⇒ Left(e)
      case _          ⇒ Right(DataStreamSummary(sysid, ds.strid))
    }
  }

  def getDataStream(sysid: String, strid: String): Future[Option[DataStream]] =
    db.run(dataStreamAction(sysid, strid))

  def deleteDataStream(sysid: String, strid: String): Future[Either[GnError, DataStreamSummary]] = {
    val action = existsDataStreamAction(sysid, strid) flatMap {
      case false ⇒ DBIO.successful(GnErrorF.dataStreamUndefined(sysid, strid))
      case true  ⇒
        val dss = datastream.filter( r ⇒ r.sysid === sysid && r.strid === strid)
        val vds = variabledef.filter(r ⇒ r.sysid === sysid && r.strid === strid)
        val obs = observation.filter(r ⇒ r.sysid === sysid && r.strid === strid)
        obs.delete andThen vds.delete andThen dss.delete
    }

    db.run(action.transactionally) map {
      case e: GnError ⇒ Left(e)
      case _          ⇒ Right(DataStreamSummary(sysid, strid))
    }
  }

  def addVariableDef(sysid: String, strid: String)
                    (vd: VariableDef): Future[Either[GnError, VariableDefSummary]] = {
    val action = existsDataStreamAction(sysid, strid) flatMap {
      case false ⇒ DBIO.successful(GnErrorF.dataStreamUndefined(sysid, strid))
      case true  ⇒ variableDefAddAction(sysid, strid, vd)
    }

    db.run(action) map {
      case e: GnError ⇒ Left(e)
      case _          ⇒ Right(VariableDefSummary(sysid, strid, vd.name, vd.units))
    }
  }

  def addObservations(sysid: String, strid: String)
                     (obssr: ObservationsAdd): Future[Either[GnError, ObservationsSummary]] = {
    var num = 0
    val actions = obssr.observations flatMap { case (time, list) ⇒
      num += list.length
      list.map(observationAddAction(sysid, strid, time, _))
    }
    db.run(DBIO.seq(actions.toSeq: _*).transactionally) map { _ ⇒
      Right(ObservationsSummary(sysid, strid, added = Some(num)))
    }
  }

  def close(): Unit = db.close

  ///////////////////////////////////////////////////////////////////////////

  private def existsSensorSystemAction(sysid: String): SqlAction[Boolean, NoStream, Effect.Read] =
    sensorsystem.filter(_.sysid === sysid).exists.result

  private def sensorSystemQuery(sysid: String): Query[SensorSystemTable, PgSSensorSystem, Seq] =
    sensorsystem.filter(_.sysid === sysid)

  private def sensorSystemPgAction(sysid: String): SqlAction[Option[PgSSensorSystem], NoStream, Effect.Read] =
    sensorSystemQuery(sysid).result.headOption

  private def sensorSystemAction(sysid: String, streams: Seq[DataStream] = Seq.empty
                                ): DBIOAction[Option[SensorSystem], NoStream, Effect.Read] = {
    sensorSystemPgAction(sysid) map {
      case None      ⇒ None
      case Some(pss) ⇒ Some(sensorSystem2model(pss, streams))
    }
  }

  private def sensorSystemAction(sysid: String
                                ): DBIOAction[Option[SensorSystem], NoStream, Effect.Read] = {
    for {
      streams ← dataStreamsAction(sysid)
      ss      ← sensorSystemAction(sysid, streams)
    } yield ss
  }

  private def sensorSystem2model(pss: PgSSensorSystem, streams: Seq[DataStream] = Seq.empty): SensorSystem = {
    SensorSystem(
      sysid = pss.sysid,
      name = pss.name,
      description = pss.description,
      pushEvents = pss.pushEvents,
      center = pss.centerLat.map(lat ⇒ LatLon(lat, pss.centerLon.get)),
      zoom = pss.zoom,
      clickListener = pss.clickListener,
      streams = streams.map(ds ⇒ (ds.strid, ds)).toMap
    )
  }

  ////////////

  private def existsDataStreamAction(sysid: String, strid: String): SqlAction[Boolean, NoStream, Effect.Read] =
    datastream.filter(ds ⇒ ds.sysid === sysid && ds.strid === strid).exists.result

  private def dataStreamAddAction(sysid: String, ds: DataStream) = {
    val dsAction = datastream += PgSDataStream(
      sysid,
      ds.strid,
      name = ds.name,
      description = ds.description,
      mapStyle = ds.mapStyle.map(utl.toJsonString),
      zOrder = ds.zOrder,
      chartStyle = ds.chartStyle.map(utl.toJsonString)
    )

    val vdActions = ds.variables.getOrElse(List.empty).map(variableDefAddAction(sysid, ds.strid, _))

    val obsActions = ds.observations.getOrElse(Map.empty) flatMap { case (time, obss) ⇒
      obss map (observationAddAction(sysid, ds.strid, time, _))
    }

    dsAction andThen DBIO.seq(vdActions ++ obsActions: _*)
  }

  private def dataStreamQuery(sysid: String, strid: String
                             ): Query[DataStreamTable, PgSDataStream, Seq] =
    datastream.filter(ds ⇒ ds.sysid === sysid && ds.strid === strid)

  private def dataStreamPgAction(sysid: String, strid: String
                                ): SqlAction[Option[PgSDataStream], NoStream, Effect.Read] =
    dataStreamQuery(sysid, strid).result.headOption

  private def dataStreamAction(sysid: String, strid: String,
                               variables: Seq[VariableDef] = Seq.empty,
                               observations: Map[String, List[ObsData]] = Map.empty
                              ): DBIOAction[Option[DataStream], NoStream, Effect.Read] =
    dataStreamPgAction(sysid, strid) map {
      case None      ⇒ None
      case Some(pds) ⇒ Some(dataStream2model(pds, Some(variables.toList), Some(observations)))
    }

  private def dataStreamAction(sysid: String, strid: String
                              ): DBIOAction[Option[DataStream], NoStream, Effect.Read] =
    for {
      variables    ← variableDefsAction(sysid, strid)
      observations ← observationsAction(sysid, strid)
      ss           ← dataStreamAction(sysid, strid, variables, observations)
    } yield ss

  private def dataStreamAction(sysid: String, pds: PgSDataStream
                              ): DBIOAction[DataStream, NoStream, Effect.Read] =
    for {
      variables    ← variableDefsAction(sysid, pds.strid)
      observations ← observationsAction(sysid, pds.strid)
      ss = dataStream2model(pds, Some(variables.toList), Some(observations))
    } yield ss

  private def dataStreamsQuery(sysid: String): Query[DataStreamTable, PgSDataStream, Seq] =
    datastream.filter(_.sysid === sysid)

  private def dataStreamsPgAction(sysid: String): SqlAction[Seq[PgSDataStream], NoStream, Effect.Read] =
    dataStreamsQuery(sysid).result

  private def dataStreamsAction(sysid: String): DBIOAction[Seq[DataStream], NoStream, Effect.Read] =
    dataStreamsPgAction(sysid).flatMap[Seq[DataStream], NoStream, Effect.Read](
      pdss ⇒ DBIO.sequence(pdss.map(dataStreamAction(sysid, _)))
    )

  private def dataStream2model(pds: PgSDataStream,
                               variables: Option[List[VariableDef]] = None,
                               observations: Option[Map[String, List[ObsData]]] = None
                              ): DataStream =
    DataStream(
      strid = pds.strid,
      name = pds.name,
      description = pds.description,
      mapStyle = pds.mapStyle.map(utl.toJsObject),
      zOrder = pds.zOrder,
      variables = variables,
      chartStyle = pds.chartStyle.map(utl.toJsObject),
      observations = observations
    )

  //////////////

  private def variableDefAddAction(sysid: String, strid: String, vd: VariableDef) =
    variabledef += PgSVariableDef(
      sysid,
      strid,
      name = vd.name,
      units = vd.units,
      chartStyle = vd.chartStyle.map(utl.toJsonString)
    )

  private def variableDefsQuery(sysid: String, strid: String): Query[VariableDefTable, PgSVariableDef, Seq] =
    variabledef.filter(vd ⇒ vd.sysid === sysid && vd.strid === strid)

  private def variableDefsPgAction(sysid: String, strid: String
                                  ): SqlAction[Seq[PgSVariableDef], NoStream, Effect.Read] =
    variableDefsQuery(sysid, strid).result

  private def variableDefsAction(sysid: String, strid: String
                                ): DBIOAction[Seq[VariableDef], NoStream, Effect.Read] =
    for {
      pdss ← variableDefsPgAction(sysid, strid)
    } yield pdss.map(variableDef2model)

  private def variableDef2model(pvd: PgSVariableDef): VariableDef =
    VariableDef(
      name = pvd.name,
      units = pvd.units,
      chartStyle = pvd.chartStyle.map(utl.toJsObject)
    )

  //////////////

  private def observationAddAction(sysid: String, strid: String,
                                   time: String, obsData: ObsData
                                  ): FixedSqlAction[Int, NoStream, Effect.Write] =
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

  private def observationQuery(sysid: String, strid: String): Query[ObservationTable, PgSObservation, Seq] =
    observation.filter(vd ⇒ vd.sysid === sysid && vd.strid === strid)

  private def observationPgAction(sysid: String, strid: String
                                 ): SqlAction[Seq[PgSObservation], NoStream, Effect.Read] =
    observationQuery(sysid, strid).result

  private def observationsAction(sysid: String, strid: String
                                ): DBIOAction[Map[String, List[ObsData]], NoStream, Effect.Read] =
    for {
      obss ← observationPgAction(sysid, strid)
    } yield observation2model(obss.toList)

  private def observation2model(seq: Seq[PgSObservation]): Map[String, List[ObsData]] = {
    val byTime = seq.groupBy(_.time)
    byTime.mapValues { obss ⇒
      (obss map { obs ⇒
        ObsData(
          feature = obs.feature.map(utl.toFeature),
          geometry = obs.geometry.map(utl.toGeometry),
          scalarData = if (obs.vars.nonEmpty)
            Some(ScalarData(
              obs.vars, obs.vals, position = obs.lat.map(lt ⇒ LatLon(lt, obs.lon.get))
            ))
          else None
        )
      }).toList
    }
  }
}
