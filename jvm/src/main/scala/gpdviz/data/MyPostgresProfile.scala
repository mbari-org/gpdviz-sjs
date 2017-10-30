package gpdviz.data

import com.github.tminglei.slickpg._
import slick.basic.Capability

trait MyPostgresProfile extends ExPostgresProfile
  with PgArraySupport
  with PgSprayJsonSupport
  //with PgDate2Support
  //with PgRangeSupport
  //with PgPostGISSupport
{
  override def pgjson: String = "json"   // could also be "jsonb"

  // Add back `capabilities.insertOrUpdate` to enable native `upsert` support; for postgres 9.5+
  override protected def computeCapabilities: Set[Capability] =
    super.computeCapabilities + slick.jdbc.JdbcCapabilities.insertOrUpdate

  override val api = MyAPI

  object MyAPI extends API with ArrayImplicits
    with JsonImplicits
    //with DateTimeImplicits
    //with RangeImplicits
  {
    implicit val strListTypeMapper = new SimpleArrayJdbcType[String]("text").to(_.toList)
  }
}

object MyPostgresProfile extends MyPostgresProfile
