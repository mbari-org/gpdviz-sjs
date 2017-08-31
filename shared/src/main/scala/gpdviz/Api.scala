package gpdviz

import gpdviz.model.{LatLon, VmSensorSystem}

import scala.concurrent.Future

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

  def clientConfig(): Future[ClientConfig]

  def refresh(sysid: String): Future[Option[VmSensorSystem]]
}
