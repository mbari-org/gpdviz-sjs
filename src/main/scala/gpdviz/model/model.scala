package gpdviz.model

import com.cloudera.science.geojson.Feature
import com.esri.core.geometry.Geometry
import spray.json.{JsObject, JsValue}

case class LatLon(lat: Double, lon: Double)

case class SensorSystem(sysid:        String,
                        name:         Option[String] = None,
                        description:  Option[String] = None,
                        streams:      Map[String, DataStream] = Map.empty,
                        pushEvents:   Boolean = true,
                        center:       Option[LatLon] = None
                        )

case class DataStream(strid:    String,
                      name:         Option[String] = None,
                      description:  Option[String] = None,
                      mapStyle:     Option[Map[String, JsValue]] = None,
                      zOrder:       Int = 0,
                      variables:    Option[JsObject] = None,
                      chartStyle:   Option[JsObject] = None,
                      observations: Option[Map[String, List[ObsData]]] = None,
                      obs:          Option[List[DataObs]] = None
                      )

case class ObsData(feature:     Option[Feature] = None,
                   geometry:    Option[Geometry] = None,
                   scalarData:  Option[ScalarData] = None
                   )

case class ScalarData(vars:      List[String],
                      vals:      List[Double],
                      position:  Option[LatLon] = None)

case class TimestampedData(timestamp: Long,
                           values:    List[Double],
                           position:  Option[LatLon] = None)

case class DataObs(timestamp:   Long,
                   feature:     Option[Feature] = None,
                   geometry:    Option[Geometry] = None,
                   chartTsData: Option[List[TimestampedData]] = None
                   )
