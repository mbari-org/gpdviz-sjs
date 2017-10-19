package gpdviz.server

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import com.cloudera.science.geojson.GeoJsonProtocol
import gpdviz.model._
import io.swagger.annotations.ApiModelProperty
import spray.json.{DefaultJsonProtocol, JsObject}

import scala.annotation.meta.field

// generic error for now
case class GnError(code: Int,
                   msg: String,
                   sysid: Option[String] = None,
                   strid: Option[String] = None
                   )

case class SSRegister(sysid: String,
                      name: Option[String] = None,
                      description: Option[String] = None,
                     @(ApiModelProperty @field)(dataType = "boolean")
                      pushEvents: Option[Boolean] = None,
                      center: Option[LatLon] = None,
                      clickListener: Option[String] = None
                      )

case class SSUpdate(pushEvents: Option[Boolean] = None,
                    center: Option[LatLon] = None,
                    refresh: Option[Boolean] = None
                    )

case class StreamRegister(strid:        String,
                          name:         Option[String] = None,
                          description:  Option[String] = None,
                          @(ApiModelProperty @field)(dataType = "object")
                          mapStyle:     Option[JsObject] = None,
                          zOrder:       Option[Int] = None,
                          variables:    Option[List[VariableDef]] = None,
                          @(ApiModelProperty @field)(dataType = "object")
                          chartStyle:   Option[JsObject] = None
                          )

case class ObservationsRegister(observations: Map[String, List[ObsData]])


trait JsonImplicits extends DefaultJsonProtocol with SprayJsonSupport with GeoJsonProtocol {
  implicit val llRegFormat  = jsonFormat2(LatLon)
  implicit val sssRegFormat = jsonFormat4(SensorSystemSummary)
  implicit val dsSumFormat  = jsonFormat2(DataStreamSummary)
  implicit val vdSumFormat  = jsonFormat4(VariableDefSummary)
  implicit val obsSumFormat = jsonFormat5(ObservationsSummary)
  implicit val ssRegFormat  = jsonFormat6(SSRegister)
  implicit val ssUpdFormat  = jsonFormat3(SSUpdate)
  implicit val varDefFormat = jsonFormat3(VariableDef)
  implicit val strRegFormat = jsonFormat7(StreamRegister)

  implicit val scalarDataFormat = jsonFormat3(ScalarData)
  implicit val obsDataFormat  = jsonFormat3(ObsData)
  implicit val streamFormat  = jsonFormat8(DataStream)
  implicit val obssRegFormat = jsonFormat1(ObservationsRegister)
  implicit val systemFormat  = jsonFormat8(SensorSystem)

  implicit val dbErrorFormat = jsonFormat4(GnError)
}
