package gpdviz

case class ClientConfig(
                         pusher: Option[ClientPusherConfig]
                       )

case class ClientPusherConfig(
                               key:     String
                             )

trait Api {

  def clientConfig(): ClientConfig

}
