package gpdviz.data

import com.typesafe.config.Config
import gpdviz.GnError
import gpdviz.model.{SensorSystem, SensorSystemSummary}
/*
import doobie.imports._
import doobie.postgres.imports._
import org.postgresql.geometric.PGpoint

import scalaz._
import Scalaz._
*/

/**
  * experimenting with doobie/postgres...
  */
class PostgresDb(config: Config) extends DbInterface {

  val details: String = s"PostgreSQL-based database (url: ${config.getString("connection.url")})"

  def listSensorSystems(): Seq[SensorSystemSummary] = ???
/*
  def listSensorSystems(): Seq[SensorSystemSummary] = {
    val q = sql"""
      select sysid, name, description from sensorsystem
      """.query[(String, Option[String], Option[String])]

    q.list.transact(xa).unsafePerformIO map { case (sysid, name, description) ⇒
      SensorSystemSummary(sysid, name, description)
    }
  }
*/

  def getSensorSystem(sysid: String): Option[SensorSystem] = ???

  def saveSensorSystem(ss: SensorSystem): Either[GnError, SensorSystem] = ???

  def deleteSensorSystem(sysid: String): Either[GnError, SensorSystem] = ???

/*
  createTables()
  insertSomeStuff()

  private def insertSomeStuff(): Unit = {
    def insert1(sysid: String,
                name: Option[String],
                description: Option[String],
                pushEvents: Boolean
                //,center: Option[PointType]
               ): Update0 =
      sql"""
        insert into sensorsystem (sysid, name, description, pushEvents)
        values ($sysid, $name, $description, $pushEvents)
        """.update

    insert1("sysid1", Some("name of sysid1"), Some("sysid1 description"), pushEvents = false).run.transact(xa).unsafePerformIO
    insert1("sysid2", Some("name of sysid2"), Some("sysid2 description"), pushEvents = true).run.transact(xa).unsafePerformIO
  }

  private def someSelects(): Unit = {

  }

  private def createTables(): Unit = {
    val drop: Update0 = sql""" DROP TABLE IF EXISTS sensorsystem""".update

    val create: Update0 =
      sql"""
        CREATE TABLE sensorsystem (
          sysid        VARCHAR NOT NULL UNIQUE,
          name         VARCHAR,
          description  VARCHAR,
          pushEvents   BOOLEAN,
          center       geography(POINT,4326)
        )
      """.update

    (drop.run *> create.run).transact(xa).unsafePerformIO
  }

  // A custom Point type with a Meta instance xmapped from the PostgreSQL native type (which
  // would be weird to use directly in a data model). Note that the presence of this `Meta`
  // instance precludes mapping `Point` to two columns. If you want two mappings you need two types.
  case class Point(x: Double, y: Double)
  object Point {
    implicit val PointType: Meta[Point] =
      Meta[PGpoint].xmap(p => new Point(p.x, p.y), p => new PGpoint(p.x, p.y))
  }
  // Point is now a perfectly cromulent input/output type
  val q = sql"select '(1, 2)'::point".query[Point]
  val a = q.list.transact(xa).unsafePerformIO
  println(a) // List(Point(1.0,2.0))

  private lazy val xa = DriverManagerTransactor[IOLite](
    driver = config.getString("connection.driverName"),
    url    = config.getString("connection.url"),
    user   = config.getString("connection.userName"),
    pass   = config.getString("connection.password")
  )
*/
}
