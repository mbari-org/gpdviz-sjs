package gpdviz

import gpdviz.model.VmSensorSystem

case class ClientConfig(
                         serverName: String,
                         pusher:     Option[ClientPusherConfig]
                       )

case class ClientPusherConfig(
                               key:     String
                             )

trait Api {

  def clientConfig(): ClientConfig

  def refresh(sysid: String): Option[VmSensorSystem]
}
