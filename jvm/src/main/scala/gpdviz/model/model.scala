package gpdviz.model

import com.cloudera.science.geojson.Feature
import com.esri.core.geometry.Geometry
import io.swagger.annotations.ApiModelProperty
import spray.json.JsValue

import scala.annotation.meta.field

case class SensorSystem(
                         sysid:         String,
                         name:          Option[String] = None,
                         description:   Option[String] = None,
                         pushEvents:    Boolean = true,
                         center:        Option[LatLon] = None,
                         zoom:          Option[Int] = None,
                         clickListener: Option[String] = None,
                         streams:       Map[String, DataStream] = Map.empty
                       )

case class DataStream(
                       strid:        String,
                       name:         Option[String] = None,
                       description:  Option[String] = None,
                       @(ApiModelProperty @field)(dataType = "object")
                       mapStyle:     Option[JsValue] = None,
                       zOrder:       Int = 0,
                       @(ApiModelProperty @field)(dataType = "object")
                       chartStyle:   Option[JsValue] = None,
                       variables:    Option[List[VariableDef]] = None,
                       observations: Option[Map[String, List[ObsData]]] = None
                     )

case class VariableDef(
                        name:          String,
                        units:         Option[String] = None,
                        @(ApiModelProperty @field)(dataType = "object")
                        chartStyle:    Option[JsValue] = None
                      )

case class ObsData(
                    @(ApiModelProperty @field)(dataType = "object")
                    feature:     Option[Feature] = None,
                    @(ApiModelProperty @field)(dataType = "object")
                    geometry:    Option[Geometry] = None,
                    scalarData:  Option[ScalarData] = None
                  )
