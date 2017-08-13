package gpdviz

case class ClientConfig(
                         serverName: String,
                         pusher:     Option[ClientPusherConfig]
                       )

case class ClientPusherConfig(
                               key:     String
                             )

trait Api {

  def clientConfig(): ClientConfig

}
