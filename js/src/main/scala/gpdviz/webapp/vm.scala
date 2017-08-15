package gpdviz.webapp

import gpdviz.model.LatLon

case class VmSensorSystem(sysid:        String,
                        name:         Option[String] = None,
                        description:  Option[String] = None,
                        streams:      Map[String, VmDataStream] = Map.empty,
                        center:       Option[LatLon] = None,
                        clickListener: Option[String] = None
                        )

case class VmDataStream(strid:    String,
                      name:         Option[String] = None,
                      description:  Option[String] = None,
                      mapStyle:     Option[String] = None,
                      zOrder:       Int = 0,
                      variables:    Option[String] = None,
                      chartStyle:   Option[String] = None,
                      observations: Option[Map[String, List[VmObsData]]] = None
                      )

case class VmObsData(feature:     Option[String],
                   geometry:    Option[String],
                   scalarData:  Option[String]
                   )
