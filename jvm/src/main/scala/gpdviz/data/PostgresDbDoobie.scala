package gpdviz.data

import com.typesafe.config.Config
/*
import gpdviz.model._
import gpdviz.server.{GnError, ObservationsRegister, SSUpdate}
import pprint.PPrinter.Color.{apply ⇒ pp}

import _root_.doobie._
import _root_.doobie.implicits._
import cats._, cats.data._, cats.effect.IO, cats.implicits._


import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future


case class PgDSensorSystem(
                           sysid:        String,
                           name:         Option[String] = None,
                           description:  Option[String] = None,
                           pushEvents:   Boolean = true,
                           centerLat:    Option[Double] = None,
                           centerLon:    Option[Double] = None,
                           zoom:         Option[Int] = None,
                           clickListener: Option[String] = None
                         )

case class PgDDataStream(
                         sysid:        String,
                         strid:        String,
                         name:         Option[String] = None,
                         description:  Option[String] = None,
                         mapStyle:     Option[String] = None,
                         zOrder:       Int = 0,
                         chartStyle:   Option[String] = None
                       )

case class PgDVariableDef(
                          sysid:         String,
                          strid:         String,
                          name:          String,
                          units:         Option[String] = None,
                          chartStyle:    Option[String] = None
                        )

case class PgDObservation(
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
*/


abstract class PostgresDbDoobie(doobieConfig: Config) extends DbInterface {

/*
  private val xa = Transactor.fromDriverManager[IO](
    driver = "org.postgresql.Driver",
    url = "jdbc:postgresql:gpdviz",
    user = "postgres",
    pass = ""
  )

  val details: String = s"PostgreSQL-based database (doobie)"

  def createTables(): Future[Unit] = Future {
    def sensorsystem(): Unit = {
      val drop: Update0 = sql"DROP TABLE IF EXISTS sensorsystem".update
      val create: Update0 = sql"""
        CREATE TABLE sensorsystem2 (
          sysid             VARCHAR(255) PRIMARY KEY,
          name              VARCHAR(255),
          description       TEXT,
          pushEvents        BOOLEAN,
          centerLat         double precision,
          centerLon         double precision,
          zoom              INTEGER,
          clickListener     VARCHAR(255)
        )
      """.update
      (drop.run *> create.run).transact(xa).unsafeRunSync
    }

    def datastream(): Unit = {
      val drop: Update0 = sql"DROP TABLE IF EXISTS datastream".update
      val create: Update0 = sql"""
        CREATE TABLE datastream (
          sysid             VARCHAR(255),
          strid             VARCHAR(255),
          name              VARCHAR(255),
          description       TEXT,
          mapStyle          TEXT,
          zOrder            INT,
          chartStyle        TEXT,
          PRIMARY KEY (sysid, strid),
          constraint fk_ds_ss foreign key (sysid) references sensorsystem (sysid)
        )
      """.update
      (drop.run *> create.run).transact(xa).unsafeRunSync
    }

    def variabledef(): Unit = {
      val drop: Update0 = sql"DROP TABLE IF EXISTS variabledef".update
      val create: Update0 = sql"""
        CREATE TABLE "variabledef" (
          sysid             VARCHAR(255),
          strid             VARCHAR(255),
          name              VARCHAR(255),
          units             VARCHAR(255),
          chartStyle        TEXT,
          PRIMARY KEY (sysid, strid, name),
          constraint fk_vd_ds foreign key (sysid, strid) references datastream (sysid, strid)
      )
      """.update
      (drop.run *> create.run).transact(xa).unsafeRunSync
    }

    def observation(): Unit = {
      val drop: Update0 = sql"DROP TABLE IF EXISTS observation".update
      val create: Update0 = sql"""
        CREATE TABLE "observation" (
          sysid             VARCHAR(255) NOT NULL,
          strid             VARCHAR(255) NOT NULL,
          time              VARCHAR(255) NOT NULL,
          feature           TEXT,
          geometry          TEXT,
          -- scalarData:
          vars    VARCHAR(255)[],
          vals    double precision[],
          lat     double precision,
          lon     double precision,
          --
          constraint fk_obs_ds foreign key (sysid, strid) references datastream (sysid, strid)
      )
      """.update
      (drop.run *> create.run).transact(xa).unsafeRunSync
    }

    sensorsystem()
    datastream()
    variabledef()
    observation()
  }

  def listSensorSystems(): Future[Seq[SensorSystemSummary]] = Future {

    sql"select sysid, name, description from sensorsystem"
      .query[(String, Option[String], Option[String])]
      .list
      .transact(xa)
      .unsafeRunSync

    Seq.empty
  }

  def existsSensorSystem(sysid: String): Future[Boolean] = Future {

    false
  }

  def registerSensorSystem(ss: SensorSystem): Future[Either[GnError, SensorSystemSummary]] = Future {
    sql"""
      insert into sensorsystem (
        sysid,
        name,
        description,
        pushEvents,
        centerLat,
        centerLon,
        zoom,
        clickListener
      )
      values (
        ${ss.sysid},
        ${ss.name}
        ${ss.description},
        ${ss.pushEvents},
        ${ss.center.map(_.lat)},
        ${ss.center.map(_.lon)},
        ${ss.zoom},
        ${ss.clickListener}
      )
    """.update.run.transact(xa).unsafeRunSync

    val regStream = registerDataStream(ss.sysid) _
    ss.streams.values foreach regStream

    Right(SensorSystemSummary(
      ss.sysid,
      name = ss.name,
      description = ss.description
    ))
  }

  def updateSensorSystem(sysid: String, ssu: SSUpdate): Future[Either[GnError, SensorSystemSummary]] = Future {
    sql"""
      update sensorsystem set
        pushEvents = ${ssu.pushEvents},
        centerLat = ${ssu.center.map(_.lat)},
        centerLon = ${ssu.center.map(_.lon)}
      where
        sysid = $sysid,
      )
    """.update.run.transact(xa).unsafeRunSync

    Right(SensorSystemSummary(sysid))
  }

  def registerDataStream(sysid: String)
                        (ds: DataStream): Future[Either[GnError, DataStreamSummary]] = Future {
    sql"""
      insert into datastream (
        sysid,
        strid,
        name,
        description,
        mapStyle,
        zOrder,
        chartStyle
      )
      values (
        $sysid,
        ${ds.strid},
        ${ds.name}
        ${ds.description},
        ${ds.mapStyle.map(utl.toJsonString)},
        ${ds.zOrder},
        ${ds.chartStyle.map(utl.toJsonString)}
      )
    """.update.run.transact(xa).unsafeRunSync

    Right(DataStreamSummary(sysid, ds.strid))
  }

  def registerVariableDef(sysid: String, strid: String)
                         (vd: VariableDef): Future[Either[GnError, VariableDefSummary]] = Future {
    sql"""
      insert into variabledef (
        sysid,
        strid,
        name,
        units,
        chartStyle
      )
      values (
        $sysid,
        $strid,
        ${vd.name}
        ${vd.units},
        ${vd.chartStyle.map(utl.toJsonString)}
      )
    """.update.run.transact(xa).unsafeRunSync

    Right(VariableDefSummary(sysid, strid, vd.name, vd.units))
  }

  def registerObservations(sysid: String, strid: String)
                          (obssr: ObservationsRegister): Future[Either[GnError, ObservationsSummary]] = Future {
    var num = 0
    obssr.observations foreach { case (time, list) ⇒
      //val timeIso = if (time.startsWith("15030")) utl.iso(time.toLong) else time
      if (sysid=="ss1" && strid=="str1")
        println(s"  **- time=$time list=${pp(list)}")
      list.foreach(registerObservation(sysid, strid, time, _))
      num += list.length
    }
    Right(ObservationsSummary(sysid, strid, added = Some(99999)))
  }

  def registerObservation(sysid: String, strid: String, time: String,
                          obsData: ObsData): Future[Either[GnError, ObservationsSummary]] = Future {

    /*
    hmmm, the sql below makes doobie complain with:

    PostgresDbDoobie.scala:287: Could not find or construct Param[String :: String :: String :: Option[String] :: Option[String] :: Option[List[String]] :: Option[List[Double]] :: Option[Double] :: Option[Double] :: shapeless.HNil].
    [error] Ensure that this type is an atomic type with an Atom instance in scope, or is an HList whose members
    [error] have Atom instances in scope. You can usually diagnose this problem by trying to summon the Atom
    [error] instance for each element in the REPL. See the FAQ in the Book of Doobie for more hints.

    But I've been unable to fix this even after looking at the corresponding FAQ entry at
    https://tpolecat.github.io/doobie-0.2.3/15-FAQ.html
    */

    /*
    val feature: Option[String]  = obsData.feature.map(utl.toJsonString)
    val geometry: Option[String] = obsData.geometry.map(utl.toJsonString)

    val vars: Option[List[String]] = obsData.scalarData.map(_.vars)
    val vals: Option[List[Double]] = obsData.scalarData.map(_.vals)

    val lat: Option[Double] = obsData.scalarData.map(_.position.map(_.lat)).head
    val lon: Option[Double] = obsData.scalarData.map(_.position.map(_.lon)).head

    sql"""
      insert into observation (
        sysid,
        strid,
        time,
        feature,
        geometry,
        vars,
        vals,
        lat,
        lon
      )
      values (
        $sysid,
        $strid,
        $time,
        $feature,
        $geometry,
        $vars,
        $vals,
        $lat,
        $lon
      )
    """.update.run.transact(xa).unsafeRunSync
    */

    Right(ObservationsSummary(sysid, strid, time = Some(time), added = Some(1)))
  }

  def getSensorSystem(sysid: String): Future[Option[SensorSystem]] = Future {

    None
  }

  def deleteSensorSystem(sysid: String): Future[Either[GnError, SensorSystemSummary]] = Future {
    Right(SensorSystemSummary(sysid))
  }

  def deleteDataStream(sysid: String, strid: String): Future[Either[GnError, DataStreamSummary]] = Future {
    Right(DataStreamSummary(sysid, strid))
  }

  def close(): Unit = ()
*/
}
