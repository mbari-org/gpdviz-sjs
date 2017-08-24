package gpdviz

import gpdviz.model.{LatLon, VmSensorSystem}

case class ClientConfig(
                         serverName: String,
                         center:     LatLon,
                         zoom:       Int,
                         pusher:     Option[ClientPusherConfig]
                       )

case class ClientPusherConfig(
                               key:     String
                             )

trait Api {

  def clientConfig(): ClientConfig

  def refresh(sysid: String): Option[VmSensorSystem]
}
