package gpdviz.async

import gpdviz.model.{DataStream, ObsData, SensorSystem}


trait Notifier {

  def details: String

  def getSensorSystemIndex(sysid: String, ssOpt: Option[SensorSystem]): String

  def notifySensorSystemRegistered(ss: SensorSystem): Unit

  def notifyStreamAdded(ss: SensorSystem, str: DataStream): Unit

  def notifyObservations2Added(ss: SensorSystem, strid: String, observations: Map[String, List[ObsData]]): Unit

  def notifyStreamRemoved(ss: SensorSystem, strid: String): Unit

  def notifySensorSystemUpdated(ss: SensorSystem): Unit

  def notifySensorSystemRefresh(ss: SensorSystem): Unit

  def notifySensorSystemUnregistered(ss: SensorSystem): Unit
}
