package gpdviz.model

case class SensorSystemSummary(sysid:        String,
                               name:         Option[String] = None,
                               description:  Option[String] = None,
                               pushEvents:   Option[Boolean] = None,
                               center:       Option[LatLon] = None,
                               zoom:         Option[Int] = None,
                               streamIds:    Set[String] = Set.empty
                              )

case class DataStreamSummary(sysid:        String,
                             strid:        String
                            )

case class VariableDefSummary(sysid:        String,
                              strid:        String,
                              name:         String,
                              units:        Option[String] = None
                             )

case class ObservationsSummary(sysid:        String,
                               strid:        String,
                               time:         Option[String] = None,
                               added:        Option[Int] = None,
                               removed:      Option[Int] = None
                              )

case class LatLon(lat: Double, lon: Double)

case class ScalarData(vars:      List[String],
                      vals:      List[Double],
                      position:  Option[LatLon] = None)

case class VmSensorSystem(sysid:        String,
                          name:         Option[String] = None,
                          description:  Option[String] = None,
                          streams:      List[VmDataStream] = List.empty,
                          center:       Option[LatLon] = None,
                          zoom:         Option[Int] = None,
                          clickListener: Option[String] = None
                         )

case class VmVariableDef(name:          String,
                         units:         Option[String] = None,
                         chartStyle:    Option[String] = None
                        )

case class VmDataStream(strid:    String,
                        name:         Option[String] = None,
                        description:  Option[String] = None,
                        mapStyle:     Option[String] = None,
                        zOrder:       Int = 0,
                        variables:    Option[List[VmVariableDef]] = None,
                        chartStyle:   Option[String] = None,
                        observations: Option[Map[String, List[VmObsData]]] = None
                       )

case class VmObsData(feature:     Option[String],
                     geometry:    Option[String],
                     scalarData:  Option[ScalarData]
                    )
