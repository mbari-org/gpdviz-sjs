package gpdviz.data

import java.text.SimpleDateFormat
import java.util.{Date, TimeZone, UUID}
import java.util.concurrent.TimeUnit

import com.cloudera.science.geojson.{Feature, GeoJsonProtocol}
import com.esri.core.geometry.Geometry
import gpdviz.config
import gpdviz.config.PostgresCfg
import gpdviz.model._
import gpdviz.server.GnError
import io.getquill.{Embedded, LowerCase, PostgresJdbcContext}
import spray.json._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.Future

case class PgLatLon(lat: Double, lon: Double) extends Embedded

case class PgSensorSystem(
                          sysid:        String,
                          name:         Option[String] = None,
                          description:  Option[String] = None,
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
                        zOrder:       Int = 0,
                        chartStyle:   Option[String] = None
                       )

case class PgVariableDef(
                          sysid:         String,
                          strid:         String,
                          name:          String,
                          units:         Option[String] = None,
                          chartStyle:    Option[String] = None
                        )

case class PgScalarData(
                         vars:      List[String],
                         vals:      List[Double],
                         position:  Option[PgLatLon] = None
                       ) extends Embedded

case class PgObservation(
                          sysid:         String,
                          strid:         String,
                          time:          String,
                          feature:       Option[String] = None,
                          geometry:      Option[String] = None,
                          scalarData:    Option[PgScalarData] = None
                        )

class PostgresDb(pgCfg: PostgresCfg) extends DbInterface {

  private val ctx = new PostgresJdbcContext(LowerCase, config.tsConfig.getConfig("postgres.quill"))
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

  private val observation = quote {
    querySchema[PgObservation]("observation"
      ,
      _.scalarData.map(_.vars) → "scalarDataVars",
      _.scalarData.map(_.vals) → "scalarDataVals",
       //TODO doubly-nested not working in quill?
      _.scalarData.map(_.position.map(_.lat)) → "scalarDataLat",
      _.scalarData.map(_.position.map(_.lon)) → "scalarDataLon",
    )
  }

  def initStuff() : Unit = {
    val sysid = UUID.randomUUID().toString.substring(0, 8)
    val strid1 = "STR1"
    val strid2 = "STR2"

    val ss = SensorSystem(
      sysid = sysid,
      name = Some(s"ss name of $sysid"),
      pushEvents = false,
      center = Some(LatLon(36.785, -122.0)),
      clickListener = Some(s"http://clickListener/$sysid"),
      streams = Map(
        strid1 → DataStream(
          strid = strid1,
          name = Some(s"str name of $strid1"),
          variables = Some(List(
            VariableDef(
              name = "temperature",
              units = Some("°C")
            ),
            VariableDef(
              name = "distance",
              units = Some("m")
            )
          )),
          observations = Some(Map(
            utl.nowIso → List(ObsData(
              scalarData = Some(ScalarData(
                vars = List("temperature", "distance"),
                vals = List(14.5, 1200.0),
                position = Some(LatLon(36.7, -122.12))
              ))
            ))
          ))
        )
        , strid2 → DataStream(
          strid = strid2,
          name = Some(s"str name of $strid2")
        )
      )
    )

    import scala.concurrent.Await
    Await.ready(registerSensorSystem(ss), Duration(3, TimeUnit.SECONDS))
  }

  //initStuff()


  val details: String = s"PostgreSQL-based database (url: ${pgCfg.url})"

  def listSensorSystems(): Future[Seq[SensorSystemSummary]] = Future {
    ctx.run(quote(sensorSystem)) map { pss ⇒
      val strIds = ctx.run(dataStream.filter(_.sysid == lift(pss).sysid)).map(_.strid)
      SensorSystemSummary(
        pss.sysid,
        pss.name,
        pss.description,
        strIds.toSet
      )
    }
  }

  override def existsSensorSystem(sysid: String): Future[Boolean] = Future {
    ctx.run(quote(sensorSystem.filter(_.sysid == lift(sysid)))).nonEmpty
  }

  override def registerSensorSystem(ss: SensorSystem): Future[Either[GnError, String]] = Future {
    ctx.run(quote(sensorSystem.insert(lift(PgSensorSystem(
      sysid = ss.sysid,
      name = ss.name,
      pushEvents = ss.pushEvents,
      center = ss.center.map(c ⇒ PgLatLon(c.lat, c.lon)),
      clickListener = ss.clickListener
    )))))

    ss.streams.values foreach registerDataStream(ss.sysid)

    Right(ss.sysid)
  }

  override def registerDataStream(sysid: String)(ds: DataStream): Future[Either[GnError, String]] = Future {
    ctx.run(quote(dataStream.insert(lift(PgDataStream(
      sysid = sysid,
      strid = ds.strid,
      name = ds.name,
      description = ds.description,
      mapStyle = ds.mapStyle.map(utl.toJsonString),
      zOrder = ds.zOrder,
      chartStyle = ds.chartStyle.map(utl.toJsonString)
    )))))

    val variables = ds.variables.getOrElse(List.empty)
    val observations = ds.observations.getOrElse(Map.empty)

    println(s"  ** sysid=$sysid strid=${ds.strid} Registering ${variables.size} variables: $variables")
    variables foreach registerVariableDef(sysid, ds.strid)

    println(s"  ** sysid=$sysid strid=${ds.strid} Registering ${observations.size} observations")
    observations foreach { case (time, list) ⇒
      println(s"  **- time=$time list.size=${list.size}")
      list foreach registerObservation(sysid, ds.strid, time)
    }

    Right(ds.strid)
  }

  override def registerVariableDef(sysid: String, strid: String)(vd: VariableDef): Future[Either[GnError, String]] = Future {
    println(s"  *** registerVariableDef ${vd.name}")
    ctx.run(quote(variableDef.insert(lift(PgVariableDef(
      sysid = sysid,
      strid = strid,
      name = vd.name,
      units = vd.units
    )))))
    Right(vd.name)
  }

  override def registerObservation(sysid: String, strid: String, time: String)(obsData: ObsData): Future[Either[GnError, String]] = Future {
    println(s"  *** registerObservation time=$time")
    val feature = obsData.feature.map(utl.toJsonString)
    val geometry = obsData.geometry.map(utl.toJsonString)
    ctx.run(quote(observation.insert(lift(PgObservation(
      sysid = sysid,
      strid = strid,
      time = time,
      feature = feature,
      geometry = geometry,
      scalarData = obsData.scalarData map { s ⇒
        PgScalarData(
          vars = s.vars,
          vals = s.vals,
          position = s.position.map(p ⇒ PgLatLon(p.lat, p.lon))
        )
      }
    )))))
    Right(time)
  }

  def getSensorSystem(sysid: String): Future[Option[SensorSystem]] = Future {
    val r = ctx.run(quote(sensorSystem.filter(_.sysid == lift(sysid))))
    r.headOption.map { pss ⇒
      val streams = ctx.run(dataStream.filter(_.sysid == lift(pss).sysid)).map { ds ⇒

        val varDefs = ctx.run(variableDef.filter(vd ⇒ vd.sysid == lift(ds).sysid && vd.strid == lift(ds).strid)).map { vd ⇒
          VariableDef(
            name = vd.name,
            units = vd.units,
            chartStyle = vd.chartStyle.map(x ⇒ JsonParser(x).asJsObject)
          )
        }

        DataStream(
          ds.strid,
          ds.name,
          ds.description,
          mapStyle = ds.mapStyle.map(x ⇒ JsonParser(x).asJsObject),
          zOrder = ds.zOrder,
          chartStyle = ds.chartStyle.map(x ⇒ JsonParser(x).asJsObject),
          variables = if (varDefs.nonEmpty) Some(varDefs) else None,
          // TODO observations = ds.o
        )
      }

      SensorSystem(
        pss.sysid,
        pss.name,
        pss.description,
        pushEvents = pss.pushEvents,
        center = pss.center.map(c ⇒ LatLon(c.lat, c.lon)),
        zoom = pss.zoom,
        clickListener = pss.clickListener,
        streams = streams.map(s ⇒ (s.strid, s)).toMap
      )
    }
  }

  def deleteSensorSystem(sysid: String): Future[Either[GnError, String]] = Future {
    ctx.transaction {
      ctx.run(observation.filter(_.sysid  == lift(sysid)).delete)
      ctx.run(variableDef.filter(_.sysid  == lift(sysid)).delete)
      ctx.run(dataStream.filter(_.sysid   == lift(sysid)).delete)
      ctx.run(sensorSystem.filter(_.sysid == lift(sysid)).delete)
    }
    Right(sysid)
  }

  override def close(): Unit = ctx.close()
}

private object utl extends GeoJsonProtocol {
  def toJsonString(x: JsObject): String = x.toJson.prettyPrint
  def toJsonString(x: Feature): String = x.toJson.prettyPrint
  def toJsonString(x: Geometry): String = x.toJson.prettyPrint

  def nowIso: String = df.format(new Date())

  private val df = {
    val x = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    x.setTimeZone(TimeZone.getTimeZone("UTC"))
    x
  }
}
