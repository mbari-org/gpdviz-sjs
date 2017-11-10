package gpdviz.config

import carueda.cfg._

@Cfg
case class GpdvizCfg(
                      serverName:    String,
                      externalUrl:   String,
                      httpInterface: String = "0.0.0.0",
                      httpPort:      Int    = 5050,
                      map:           MapCfg = MapCfg(),
                      pusher:        Option[PusherCfg]
                    ) {
  object gpdviz {
    val version: String = $  // from reference.conf
  }
}

@Cfg
case class MapCfg(
                   center:  LatLonCfg = LatLonCfg(36.79, -122.02),
                   zoom:    Int = 11,
                   googleMapApiKey: Option[String] = None
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
