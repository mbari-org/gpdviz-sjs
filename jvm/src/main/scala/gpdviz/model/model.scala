package gpdviz.model

import com.cloudera.science.geojson.Feature
import com.esri.core.geometry.Geometry
import io.swagger.annotations.ApiModelProperty
import spray.json.JsObject

import scala.annotation.meta.field

case class SensorSystem(sysid:        String,
                        name:         Option[String] = None,
                        description:  Option[String] = None,
                        streams:      Map[String, DataStream] = Map.empty,
                        pushEvents:   Boolean = true,
                        center:       Option[LatLon] = None,
                        zoom:         Option[Int] = None,
                        clickListener: Option[String] = None
                        )

case class VariableDef(name:          String,
                       units:         Option[String] = None,
                       @(ApiModelProperty @field)(dataType = "object")
                       chartStyle:    Option[JsObject] = None
                      )

case class DataStream(strid:    String,
                      name:         Option[String] = None,
                      description:  Option[String] = None,
                      @(ApiModelProperty @field)(dataType = "object")
                      mapStyle:     Option[JsObject] = None,
                      zOrder:       Int = 0,
                      variables:    Option[List[VariableDef]] = None,
                      @(ApiModelProperty @field)(dataType = "object")
                      chartStyle:   Option[JsObject] = None,
                      observations: Option[Map[String, List[ObsData]]] = None
                      )

case class ObsData(
                    @(ApiModelProperty @field)(dataType = "object")
                    feature:     Option[Feature] = None,
                    @(ApiModelProperty @field)(dataType = "object")
                    geometry:    Option[Geometry] = None,
                    scalarData:  Option[ScalarData] = None
                   )
