package gpdviz

import gpdviz.model.{LatLon, VmDataStream, VmObsData}

sealed trait Notif {
  def sysid: String
}

case class SensorSystemAdded(
                              sysid:         String,
                              name:          Option[String],
                              description:   Option[String],
                              center:        Option[LatLon],
                              zoom:          Option[Int],
                              clickListener: Option[String],
                            ) extends Notif

case class DataStreamAdded(
                            sysid: String,
                            str:   VmDataStream
                          ) extends Notif

case class ObservationsAdded(
                               sysid: String,
                               strid: String,
                               obss: Map[String, List[VmObsData]]
                             ) extends Notif

case class DataStreamDeleted(
                              sysid: String,
                              strid: String
                            ) extends Notif

case class SensorSystemUpdated(
                                sysid: String
                              ) extends Notif

case class SensorSystemRefresh(
                                sysid: String
                              ) extends Notif

case class SensorSystemDeleted(
                                sysid: String
                              ) extends Notif
