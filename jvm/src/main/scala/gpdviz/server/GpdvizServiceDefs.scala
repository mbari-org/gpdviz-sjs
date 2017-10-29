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
                   strid: Option[String] = None,
                   varName: Option[String] = None
                   )

object GnErrorF {

  def sensorSystemDefined(sysid: String): GnError =
    GnError(409, "sensor system already defined", sysid = Some(sysid))

  def sensorSystemUndefined(sysid: String): GnError =
    GnError(404, "sensor system undefined", sysid = Some(sysid))

  def dataStreamDefined(sysid: String, strid: String): GnError =
    GnError(409, "data stream already defined", sysid = Some(sysid), strid = Some(strid))

  def dataStreamUndefined(sysid: String, strid: String): GnError =
    GnError(404, "data stream undefined", sysid = Some(sysid), strid = Some(strid))
}

case class SensorSystemAdd(sysid: String,
                           name: Option[String] = None,
                           description: Option[String] = None,
                           @(ApiModelProperty @field)(dataType = "boolean")
                           pushEvents: Option[Boolean] = None,
                           center: Option[LatLon] = None,
                           clickListener: Option[String] = None
                          )

case class SensorSystemUpdate(pushEvents: Option[Boolean] = None,
                              center: Option[LatLon] = None,
                              refresh: Option[Boolean] = None
                             )

case class DataStreamAdd(strid:        String,
                         name:         Option[String] = None,
                         description:  Option[String] = None,
                         @(ApiModelProperty @field)(dataType = "object")
                         mapStyle:     Option[JsObject] = None,
                         zOrder:       Option[Int] = None,
                         variables:    Option[List[VariableDef]] = None,
                         @(ApiModelProperty @field)(dataType = "object")
                         chartStyle:   Option[JsObject] = None
                        )

case class ObservationsAdd(observations: Map[String, List[ObsData]])


trait JsonImplicits extends DefaultJsonProtocol with SprayJsonSupport with GeoJsonProtocol {
  implicit val _llFormat       = jsonFormat2(LatLon)

  implicit val _sdFormat       = jsonFormat3(ScalarData)
  implicit val _odFormat       = jsonFormat3(ObsData)
  implicit val _oaFormat       = jsonFormat1(ObservationsAdd)
  implicit val _osFormat       = jsonFormat5(ObservationsSummary)

  implicit val _vdFormat       = jsonFormat3(VariableDef)
  implicit val _vdsFormat      = jsonFormat4(VariableDefSummary)

  implicit val _dsAddFormat    = jsonFormat7(DataStreamAdd)
  implicit val _dssFormat      = jsonFormat2(DataStreamSummary)
  implicit val _dsFormat       = jsonFormat8(DataStream)

  implicit val _ssAddFormat    = jsonFormat6(SensorSystemAdd)
  implicit val _ssUpdFormat    = jsonFormat3(SensorSystemUpdate)
  implicit val _sssFormat      = jsonFormat6(SensorSystemSummary)
  implicit val _ssFormat       = jsonFormat8(SensorSystem)

  implicit val _dbErrorFormat  = jsonFormat5(GnError)
}
