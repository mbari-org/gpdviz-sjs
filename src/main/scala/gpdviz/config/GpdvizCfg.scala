package gpdviz.config

import carueda.cfg._

@Cfg
case class GpdvizCfg(
                      serverName:    String = "mygpdviz",
                      httpInterface: String = "0.0.0.0",
                      httpPort:      Int    = 5050
                    ) {
  object pusher {
    val appId:   String = $
    val key:     String = $
    val secret:  String = $
  }

  val postgres: Option[PostgresCfg] = $
}

@Cfg
case class PostgresCfg(
                        url: String        = "jdbc:postgresql:gpdviz",
                        userName: String   = "postgres",
                        password: String   = "",
                        driverName: String = "org.postgresql.Driver"
                      )
