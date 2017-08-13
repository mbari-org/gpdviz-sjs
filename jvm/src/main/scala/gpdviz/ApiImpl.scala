package gpdviz

import gpdviz.config.cfg

object ApiImpl extends Api {
  def usingPusher(): Boolean = cfg.pusher.isDefined

}

