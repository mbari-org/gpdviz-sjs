package gpdviz.data

import java.util.UUID

import gpdviz.config
import gpdviz.config.PostgresCfg
import gpdviz.model.{SensorSystem, SensorSystemSummary}
import gpdviz.server.GnError
import io.getquill.{Embedded, LowerCase, PostgresJdbcContext}

import scala.concurrent.Future

case class PgLatLon(lat: Double, lon: Double) extends Embedded

case class PgSensorSystem(
                          sysid:        String,
                          name:         Option[String] = None,
                          description:  Option[String] = None,
                          // streams:      Map[String, MgDataStream] = Map.empty,
                          pushEvents:   Boolean = true,
                          center:       Option[PgLatLon] = None,
                          zoom:         Option[Int] = None,
                          clickListener: Option[String] = None
                         )

case class PgDataStream(
                        sysid:        String,
                        strid:        String,
                        name:         Option[String] = None,
                        description:  Option[String] = None,
                        mapStyle:     Option[String] = None,
                        zOrder:       Int = 0
//                        variables:    Option[List[VmVariableDef]] = None,
//                        chartStyle:   Option[String] = None,
//                        observations: Option[Map[String, List[VmObsData]]] = None
                       )

case class PgVariableDef(
                          sysid:         String,
                          strid:         String,
                          name:          String,
                          units:         Option[String] = None,
                          chartStyle:    Option[String] = None
                        )

class PostgresDb(pgCfg: PostgresCfg) extends DbInterface {

  private lazy val ctx = new PostgresJdbcContext[LowerCase](config.tsConfig.getConfig("postgres.quill"))
  import ctx._

  private val sensorSystem = quote {
    querySchema[PgSensorSystem]("sensorsystem",
      _.center.map(_.lat) → "centerLat",
      _.center.map(_.lon) → "centerLon"
    )
  }

  private val dataStream = quote {
    querySchema[PgDataStream]("datastream"
    )
  }

  private val variableDef = quote {
    querySchema[PgVariableDef]("variabledef"
    )
  }

  val sysid = UUID.randomUUID().toString.substring(0, 8)

  ctx.run(quote(sensorSystem.insert(lift(PgSensorSystem(
    sysid = sysid,
    name = Some("name here"),
    pushEvents = false,
    //center = Some(PgLatLon(36.3, -121.0)),
    clickListener = Some("http://example/clickListener")
  )))))

  val strid = UUID.randomUUID().toString.substring(0, 8)

  ctx.run(quote(dataStream.insert(lift(PgDataStream(
    sysid = sysid,
    strid = strid,
    name = Some("str name here")
  )))))

  ctx.run(quote(variableDef.insert(lift(PgVariableDef(
    sysid = sysid,
    strid = strid,
    name = "temperature",
    units = Some("°C")
  )))))

  val details: String = s"PostgreSQL-based database (url: ${pgCfg.url})"

  def listSensorSystems(): Future[Seq[SensorSystemSummary]] = ???

  def getSensorSystem(sysid: String): Future[Option[SensorSystem]] = ???

  def saveSensorSystem(ss: SensorSystem): Future[Either[GnError, SensorSystem]] = ???

  def deleteSensorSystem(sysid: String): Future[Either[GnError, SensorSystem]] = ???

}
