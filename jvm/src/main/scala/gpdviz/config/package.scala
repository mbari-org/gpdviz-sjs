package gpdviz

import java.io.File
import fansi.Color.Yellow

import com.typesafe.config.{Config, ConfigFactory}

package object config {
  val configFile: File = new File("./conf", "gpdviz.conf")
  lazy val tsConfig: Config = {
    println(Yellow(s"Configuration file: ${configFile.getCanonicalPath}"))
    ConfigFactory.parseFile(configFile)
      .withFallback(ConfigFactory.load()).resolve()
  }
  lazy val cfg: GpdvizCfg = GpdvizCfg(tsConfig)
}
