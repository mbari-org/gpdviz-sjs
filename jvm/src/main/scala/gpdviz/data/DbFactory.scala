package gpdviz.data

import com.typesafe.config.ConfigFactory
import gpdviz.config

object DbFactory {

  def db: DbInterface = {
    new PostgresDbQuill(config.tsConfig.getConfig("postgres.quill"))
  }

  def testDb: DbInterface = {
    val quillConfig = ConfigFactory.parseString(
      s"""
         |dataSourceClassName     = org.postgresql.ds.PGSimpleDataSource
         |dataSource.user         = postgres
         |dataSource.password     = ""
         |dataSource.databaseName = gpdviz_test
         |dataSource.portNumber   = 5432
         |dataSource.serverName   = localhost
     """.stripMargin
    ).resolve()

    new PostgresDbQuill(quillConfig)
  }

  def initStuff(db: DbInterface): Unit = {
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

    import scala.concurrent.Await
    import java.util.concurrent.TimeUnit
    import scala.concurrent.duration.Duration
    Await.ready(db.registerSensorSystem(ss), Duration(3, TimeUnit.SECONDS))
  }
}
