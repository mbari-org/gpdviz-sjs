package gpdviz.data

import com.typesafe.config.Config
import gpdviz.GnError
import gpdviz.model.SensorSystem

/**
  * Created by carueda on 5/7/17.
  */
class PostgresDb(config: Config) extends DbInterface {

  val details: String = s"PostgreSQL-based database.  Config: $config"

  def listSensorSystems(): Map[String, SensorSystem] = ???

  def getSensorSystem(sysid: String): Option[SensorSystem] = ???

  def saveSensorSystem(ss: SensorSystem): Either[GnError, SensorSystem] = ???

  def deleteSensorSystem(sysid: String): Either[GnError, SensorSystem] = ???
}
