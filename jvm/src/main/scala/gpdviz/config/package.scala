package gpdviz

import java.io.File

import com.typesafe.config.ConfigFactory

package object config {
  val configFile = new File("gpdviz.conf")
  lazy val tsConfig = ConfigFactory.parseFile(configFile).withFallback(ConfigFactory.load()).resolve()
  lazy val cfg: GpdvizCfg = GpdvizCfg(tsConfig)
}
