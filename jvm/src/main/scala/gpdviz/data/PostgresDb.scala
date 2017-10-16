package gpdviz.data

import java.text.SimpleDateFormat
import java.util.concurrent.TimeUnit
import java.util.{Date, TimeZone, UUID}

import com.cloudera.science.geojson.{Feature, GeoJsonProtocol}
import com.esri.core.geometry.Geometry
import com.typesafe.config.Config
import gpdviz.model._
import gpdviz.server.{GnError, ObservationsRegister, SSUpdate}
import io.getquill.{Embedded, LowerCase, PostgresJdbcContext}
import pprint.PPrinter.Color.{apply ⇒ pp}
import spray.json._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration.Duration

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
                       // TODO quill#932
                       //position:  Option[PgLatLon] = None
                         lat:       Option[Double],
                         lon:       Option[Double]
                       ) extends Embedded

case class PgObservation(
                          sysid:         String,
                          strid:         String,
                          time:          String,
                          feature:       Option[String] = None,
                          geometry:      Option[String] = None,
                          scalarData:    Option[PgScalarData] = None
                        )

class PostgresDb(tsConfig: Config) extends DbInterface {

  private val ctx = new PostgresJdbcContext(LowerCase, tsConfig.getConfig("postgres.quill"))
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
      // TODO quill#932
      //,
      //_.scalarData.map(_.vars) → "scalarDataVars",
      //_.scalarData.map(_.vals) → "scalarDataVals",
      //_.scalarData.map(_.position.map(_.lat)) → "scalarDataLat",
      //_.scalarData.map(_.position.map(_.lon)) → "scalarDataLon",
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
            "1503090553000" → List(
              ObsData(
                scalarData = Some(ScalarData(
                  vars = List("temperature", "distance"),
                  vals = List(14.51, 1201.0),
                  position = Some(LatLon(36.71, -122.11))
                ))
              ),
              ObsData(
                scalarData = Some(ScalarData(
                  vars = List("temperature", "distance"),
                  vals = List(14.52, 1202.0),
                  position = Some(LatLon(36.72, -122.12))
                ))
              )
            ),
            "1503090555000" → List(
              ObsData(
                scalarData = Some(ScalarData(
                  vars = List("temperature", "distance"),
                  vals = List(14.53, 1203.0),
                  position = Some(LatLon(36.73, -122.13))
                ))
              )
            )
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

  initStuff()


  val details: String = s"PostgreSQL-based database"

  def listSensorSystems(): Future[Seq[SensorSystemSummary]] = Future {
    ctx.run(sensorSystem) map { pss ⇒
      val strIds = ctx.run(dataStream.filter(_.sysid == lift(pss).sysid)).map(_.strid)
      SensorSystemSummary(
        pss.sysid,
        pss.name,
        pss.description,
        strIds.toSet
      )
    }
  }

  def existsSensorSystem(sysid: String): Future[Boolean] = Future {
    ctx.run(sensorSystem.filter(_.sysid == lift(sysid))).nonEmpty
  }

  def registerSensorSystem(ss: SensorSystem): Future[Either[GnError, String]] = Future {
    ctx.run(sensorSystem.insert(lift(PgSensorSystem(
      sysid = ss.sysid,
      name = ss.name,
      pushEvents = ss.pushEvents,
      center = ss.center.map(c ⇒ PgLatLon(c.lat, c.lon)),
      clickListener = ss.clickListener
    ))))

    val regStream = registerDataStream(ss.sysid) _
    ss.streams.values foreach regStream

    Right(ss.sysid)
  }

  def updateSensorSystem(sysid: String, ssu: SSUpdate): Future[Either[GnError, String]] = Future {
    ctx.run(sensorSystem.filter(_.sysid == lift(sysid)).update { ss ⇒
      ss.pushEvents → lift(ssu.pushEvents getOrElse ss.pushEvents)
      ss.center → lift(ssu.center orElse ss.center)
      // TODO 'refresh'?
    })
    Right(sysid)
  }

  def registerDataStream(sysid: String)
                        (ds: DataStream): Future[Either[GnError, String]] = Future {
    ctx.run(dataStream.insert(lift(PgDataStream(
      sysid = sysid,
      strid = ds.strid,
      name = ds.name,
      description = ds.description,
      mapStyle = ds.mapStyle.map(utl.toJsonString),
      zOrder = ds.zOrder,
      chartStyle = ds.chartStyle.map(utl.toJsonString)
    ))))

    val variables = ds.variables.getOrElse(List.empty)
    val observations = ds.observations.getOrElse(Map.empty)

    println(s"  ** sysid=$sysid strid=${ds.strid} Registering ${variables.size} variables: $variables")
    variables foreach registerVariableDef(sysid, ds.strid)

    println(s"  ** sysid=$sysid strid=${ds.strid} Registering ${observations.size} observations")
    observations foreach { case (time, list) ⇒
      //println(s"  **- time=$time list.size=${list.size}")
      //val timeIso = if (time.startsWith("15030")) utl.iso(time.toLong) else time
      list.foreach(registerObservation(sysid, ds.strid, time, _))
    }

    Right(ds.strid)
  }

  def registerVariableDef(sysid: String, strid: String)
                         (vd: VariableDef): Future[Either[GnError, String]] = Future {
    println(s"  *** registerVariableDef ${vd.name}")
    ctx.run(variableDef.insert(lift(PgVariableDef(
      sysid = sysid,
      strid = strid,
      name = vd.name,
      units = vd.units
    ))))
    Right(vd.name)
  }

  def registerObservations(sysid: String, strid: String)
                          (obssr: ObservationsRegister): Future[Either[GnError, String]] = Future {

    var num = 0
    obssr.observations foreach { case (time, list) ⇒
      //val timeIso = if (time.startsWith("15030")) utl.iso(time.toLong) else time
      if (sysid=="ss1" && strid=="str1")
        println(s"  **- time=$time list=${pp(list)}")
      list.foreach(registerObservation(sysid, strid, time, _))
      num += list.length
    }
    Right(s"observations added: $num")
  }

  def registerObservation(sysid: String, strid: String, time: String,
                          obsData: ObsData): Future[Either[GnError, String]] = Future {

    val feature = obsData.feature.map(utl.toJsonString)
    val geometry = obsData.geometry.map(utl.toJsonString)

    val pgScalarData = obsData.scalarData map { s ⇒
      PgScalarData(
        vars = s.vars,
        vals = s.vals,
      //position = s.position.map(p ⇒ PgLatLon(p.lat, p.lon))
        lat = s.position.map(_.lat),
        lon = s.position.map(_.lon)
      )
    }
    //pgScalarData foreach { x ⇒ println(s"  *** registerObservation sysid=$sysid strid=$strid time=$time pgScalarData=" + pp(x)) }

    val pgObservation = PgObservation(
      sysid = sysid,
      strid = strid,
      time = time,
      feature = feature,
      geometry = geometry,
      scalarData = pgScalarData
    )
    println(s"  *** registerObservation sysid=$sysid strid=$strid pgObservation=" + pp(pgObservation))
    ctx.run(observation.insert(lift(pgObservation)))
    Right(time)
  }

  private def getPgObservations(sysid: String, strid: String): List[PgObservation] = {
    ctx.run(observation
      .filter(o ⇒ o.sysid == lift(sysid) && o.strid == lift(strid))
    )
  }

  def getSensorSystem(sysid: String): Future[Option[SensorSystem]] = Future {
    val r = ctx.run(sensorSystem.filter(_.sysid == lift(sysid)))
    r.headOption.map { pss ⇒
      val streams = ctx.run(dataStream.filter(_.sysid == lift(pss).sysid)).map { ds ⇒

        val varDefs: List[VariableDef] = {
          ctx.run(variableDef.filter(vd ⇒ vd.sysid == lift(ds).sysid && vd.strid == lift(ds).strid)).map { vd ⇒
            VariableDef(
              name = vd.name,
              units = vd.units,
              chartStyle = vd.chartStyle.map(x ⇒ JsonParser(x).asJsObject)
            )
          }
        }

        val observationsMap: Map[String, List[ObsData]] = {

          def pgObs2ObsData(o: PgObservation): ObsData = {
            ObsData(
              feature = o.feature.map(utl.toFeature),
              geometry = o.geometry.map(utl.toGeometry),
              scalarData = o.scalarData.map { sd ⇒
                ScalarData(
                  vars = sd.vars,
                  vals = sd.vals,
                //position = sd.position.map(p ⇒ LatLon(p.lat, p.lon))
                  position = for {
                    lat ← sd.lat
                    lon ← sd.lon
                  } yield LatLon(lat, lon)
                )
              }
            )
          }

          // how's quill's groupBy actually work?

          val observations = getPgObservations(ds.sysid, ds.strid)

          observations.groupBy(_.time).mapValues(_ map pgObs2ObsData)
        }

        DataStream(
          ds.strid,
          ds.name,
          ds.description,
          mapStyle = ds.mapStyle.map(x ⇒ JsonParser(x).asJsObject),
          zOrder = ds.zOrder,
          chartStyle = ds.chartStyle.map(x ⇒ JsonParser(x).asJsObject),
          variables = if (varDefs.nonEmpty) Some(varDefs) else None,
          observations = Some(observationsMap)
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

  def deleteDataStream(sysid: String, strid: String): Future[Either[GnError, String]] = Future {
    ctx.transaction {
      ctx.run(observation.filter(x ⇒ x.sysid == lift(sysid) && x.strid == lift(strid)).delete)
      ctx.run(variableDef.filter(x ⇒ x.sysid == lift(sysid) && x.strid == lift(strid)).delete)
      ctx.run(dataStream.filter( x ⇒ x.sysid == lift(sysid) && x.strid == lift(strid)).delete)
    }
    Right(sysid)
  }

  def close(): Unit = ctx.close()
}

private object utl extends GeoJsonProtocol {
  def toJsonString(x: JsObject): String = x.prettyPrint
  def toJsonString(x: Feature): String = x.toJson.prettyPrint
  def toJsonString(x: Geometry): String = x.toJson.prettyPrint

  def toJsObject(x: String): JsObject = x.toJson.asJsObject
  def toFeature(x: String): Feature = FeatureJsonFormat.read(x.parseJson)
  def toGeometry(x: String): Geometry = RichGeometryJsonFormat.read(x.parseJson)

  def iso(millis: Long): String = df.format(new Date(millis))

  def nowIso: String = df.format(new Date())

  private val df = {
    val x = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    x.setTimeZone(TimeZone.getTimeZone("UTC"))
    x
  }
}
