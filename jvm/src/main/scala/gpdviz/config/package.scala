package gpdviz

import com.typesafe.config.ConfigFactory

package object config {
  val cfg: GpdvizCfg = GpdvizCfg(ConfigFactory.load().resolve().getConfig("gpdviz"))
}
