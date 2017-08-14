package gpdviz.model

case class SensorSystemSummary(sysid:        String,
                               name:         Option[String] = None,
                               description:  Option[String] = None,
                               streamIds:    Set[String] = Set.empty
                              )

case class LatLon(lat: Double, lon: Double)

case class ScalarData(vars:      List[String],
                      vals:      List[Double],
                      position:  Option[LatLon] = None)
