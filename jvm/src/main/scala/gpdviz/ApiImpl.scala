package gpdviz

import gpdviz.config.cfg

object ApiImpl extends Api {

  def clientConfig(): ClientConfig = ClientConfig(
    pusher = cfg.pusher.map(p ⇒ ClientPusherConfig(p.key))
  )

}

