package gpdviz.data

import java.util.concurrent.TimeUnit

import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.{StrictLogging ⇒ Logging}
import gpdviz.config

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.util.Failure

object DbFactory extends Logging {

  def openDb: DbInterface = {
    new PostgresDbSlick(config.tsConfig.getConfig("postgres.slick"))
  }

  def testDb: DbInterface = {
    val config = ConfigFactory.parseString(
      s"""
         |dataSourceClass = "org.postgresql.ds.PGSimpleDataSource"
         |properties = {
         |  user         = postgres
         |  password     = ""
         |  databaseName = gpdviz_test
         |  portNumber   = 5432
         |  serverName   = localhost
         |}
         |numThreads = 10
     """.stripMargin
    ).resolve()

    new PostgresDbSlick(config)
  }

  def createTablesSync(db: DbInterface): Unit = {
    logger.info("Creating tables")
    Await.ready(db.createTables() andThen {
      case Failure(e) ⇒ logger.error("error creating tables", e)
    }, Duration(30, TimeUnit.SECONDS))
  }

  def addSomeDataSync(db: DbInterface): Unit = {
    import pprint.PPrinter.Color.{apply ⇒ pp}
    import java.util.UUID

    import gpdviz.model._

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

    logger.info("Registering a sensor system...\n" + pp(ss, height = Int.MaxValue))
    Await.ready(db.registerSensorSystem(ss) andThen {
      case Failure(e) ⇒ logger.error("error registering data", e)
    }, Duration(10, TimeUnit.SECONDS))
  }
}
