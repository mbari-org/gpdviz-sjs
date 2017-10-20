package gpdviz.data

import java.text.SimpleDateFormat
import java.util.{Date, TimeZone}

import com.cloudera.science.geojson.{Feature, GeoJsonProtocol}
import com.esri.core.geometry.Geometry
import spray.json._

object utl extends GeoJsonProtocol {
  def toJsonString(x: JsObject): String = x.prettyPrint
  def toJsonString(x: Feature): String = x.toJson.prettyPrint
  def toJsonString(x: Geometry): String = x.toJson.prettyPrint

  def toJsObject(x: String): JsObject = x.toJson.asJsObject
  def toFeature(x: String): Feature = FeatureJsonFormat.read(x.parseJson)
  def toGeometry(x: String): Geometry = RichGeometryJsonFormat.read(x.parseJson)

  def iso(millis: Long): String = df.format(new Date(millis))

  def nowIso: String = df.format(new Date())

  private val df = {
    val x = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    x.setTimeZone(TimeZone.getTimeZone("UTC"))
    x
  }
}
