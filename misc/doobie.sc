import $ivy.`org.tpolecat::doobie-core:0.5.0-M8`
import $ivy.`org.tpolecat::doobie-postgres:0.5.0-M8`
import $ivy.`org.tpolecat::doobie-specs2:0.5.0-M8`

import pprint.PPrinter.Color.{apply ⇒ pp}

import _root_.doobie._
import _root_.doobie.implicits._
import cats._, cats.data._, cats.effect.IO, cats.implicits._

val xa = Transactor.fromDriverManager[IO](
    driver = "org.postgresql.Driver",
    url = "jdbc:postgresql:gpdviz",
    user = "postgres",
    pass = ""
  )

case class PgLatLon(lat: Double, lon: Double)

case class PgSensorSystem(
                           sysid:        String,
                           name:         Option[String] = None,
                           description:  Option[String] = None,
                           pushEvents:   Boolean = true,
                           centerLat:    Option[Double] = None,
                           centerLon:    Option[Double] = None,
                           zoom:         Option[Int] = None,
                           clickListener: Option[String] = None
                         )

case class SensorSystemSummary(sysid:        String,
                               name:         Option[String] = None,
                               description:  Option[String] = None
                               //,streamIds:    Set[String] = Set.empty
                              )

def createTables(): Unit = {
  val drop: Update0 =
    sql"""
    DROP TABLE IF EXISTS sensorsystem2
  """.update

  val create: Update0 =
    sql"""
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

def listSensorSystems() = sql"""
  select sysid, name, description from sensorsystem
""".query[SensorSystemSummary]


//createTables()


//listSensorSystems().check.unsafeRunSync.foreach(r ⇒ println(pp(r)))
listSensorSystems().process.transact(xa).runLog.unsafeRunSync.foreach(r ⇒ println(pp(r)))
