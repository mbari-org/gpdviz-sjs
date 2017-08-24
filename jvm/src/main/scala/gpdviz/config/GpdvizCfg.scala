package gpdviz.config

import carueda.cfg._

@Cfg
case class GpdvizCfg(
                      serverName:    String = "mygpdviz",
                      httpInterface: String = "0.0.0.0",
                      httpPort:      Int    = 5050,
                      map:           MapCfg = MapCfg(),
                      pusher:        Option[PusherCfg],
                      postgres:      Option[PostgresCfg]
                    )

@Cfg
case class MapCfg(
                   center:  LatLonCfg = LatLonCfg(36.79, -122.02),
                   zoom:    Int = 11
                 )

@Cfg
case class LatLonCfg(
                      lat:   Double,
                      lon:   Double
                    )

@Cfg
case class PusherCfg(
                      appId:   String,
                      key:     String,
                      secret:  String
                    )

@Cfg
case class PostgresCfg(
                        url: String        = "jdbc:postgresql:gpdviz",
                        userName: String   = "postgres",
                        password: String   = "",
                        driverName: String = "org.postgresql.Driver"
                      )
