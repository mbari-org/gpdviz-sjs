package gpdviz.data

import com.typesafe.config.ConfigFactory
import gpdviz.config

object DbFactory {

  def db: DbInterface = {
    new PostgresDb(config.tsConfig)
  }

  def testDb: DbInterface = {
    val tsConfig = ConfigFactory.parseString(
      s"""
         |postgres.quill {
         |  dataSourceClassName     = org.postgresql.ds.PGSimpleDataSource
         |  dataSource.user         = postgres
         |  dataSource.password     = ""
         |  dataSource.databaseName = gpdviz_test
         |  dataSource.portNumber   = 5432
         |  dataSource.serverName   = localhost
         |}
     """.stripMargin
    ).withFallback(ConfigFactory.load()).resolve()

    new PostgresDb(tsConfig)
  }
}
