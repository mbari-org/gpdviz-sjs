package gpdviz.data

import com.cloudera.science.geojson.Feature
import com.esri.core.geometry.Geometry
import com.github.tminglei.slickpg._
import gpdviz.model.{LatLon, ScalarData}
import gpdviz.server.GpdvizJsonImplicits
import slick.basic.Capability
import spray.json._

trait MyPostgresProfile extends ExPostgresProfile
  with PgArraySupport
  with PgSprayJsonSupport
  with PgDate2Support
{
  override def pgjson: String = "json"   // could also be "jsonb"

  // Add back `capabilities.insertOrUpdate` to enable native `upsert` support; for postgres 9.5+
  override protected def computeCapabilities: Set[Capability] =
    super.computeCapabilities + slick.jdbc.JdbcCapabilities.insertOrUpdate

  override val api = MyAPI

  object MyAPI extends API with ArrayImplicits
    with JsonImplicits with GpdvizJsonImplicits
    with DateTimeImplicits
  {
    implicit val strListTypeMapper = new SimpleArrayJdbcType[String]("text").to(_.toList)

    implicit val latLonJsonTypeMapper = MappedJdbcType.base[LatLon, JsValue](_.toJson, _.convertTo[LatLon])
    implicit val scalaDataJsonTypeMapper = MappedJdbcType.base[ScalarData, JsValue](_.toJson, _.convertTo[ScalarData])
    implicit val featureJsonTypeMapper = MappedJdbcType.base[Feature, JsValue](_.toJson, FeatureJsonFormat.read)
    implicit val geometryJsonTypeMapper = MappedJdbcType.base[Geometry, JsValue](_.toJson, RichGeometryJsonFormat.read)
  }
}

object MyPostgresProfile extends MyPostgresProfile
