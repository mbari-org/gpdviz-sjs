package gpdviz

import gpdviz.model.{LatLon, ScalarData}

sealed trait Notif {
  def sysid: String
}

case class SensorSystemRegistered(
                                 sysid: String,
                                 name: Option[String],
                                 description: Option[String],
                                 center:       Option[LatLon],
                                 clickListener: Option[String],
                                 ) extends Notif

case class StreamAdded(
                        sysid: String,
                        strid: String,
                        str: String
                      ) extends Notif

case class ObsDataSTR(
                       feature:     Option[String] = None,
                       geometry:    Option[String] = None,
                       scalarData:  Option[ScalarData] = None
                     )

case class Observations2Added(
                               sysid: String,
                               strid: String,
                               obss: Map[String, List[ObsDataSTR]]
                             ) extends Notif

case class StreamRemoved(
                        sysid: String,
                        strid: String
                      ) extends Notif

case class SensorSystemUpdated(
                        sysid: String
                      ) extends Notif

case class SensorSystemRefresh(
                        sysid: String
                      ) extends Notif

case class SensorSystemUnregistered(
                        sysid: String
                      ) extends Notif
