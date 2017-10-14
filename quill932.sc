// https://github.com/getquill/quill/issues/932

import $ivy.`io.getquill::quill-sql:2.0.0`, io.getquill._
val ctx = new SqlMirrorContext(PostgresDialect, Literal)

//import $ivy.`io.getquill::quill-sql:1.4.0`, io.getquill._
//val ctx = new SqlMirrorContext[PostgresDialect, Literal]

import ctx._

case class LatLon(lat: Double, lon: Double) extends Embedded

case class ScalarData(value: Double
                      ,position: LatLon
                      //,position: Option[LatLon] = None
                     ) extends Embedded

case class Observation(scalarData: Option[ScalarData] = None,
                       foo: Option[String] = None
                      )

val observation = quote {
  querySchema[Observation]("observation",
    _.scalarData.map(_.value)        → "sdValue"
    ,_.scalarData.map(_.position.lat) → "sdLat"
    ,_.scalarData.map(_.position.lon) → "sdLon"
  )
}

val m = run(observation)
println(m.string)

/*
Expected:
 SELECT x.sdValue, x.sdLat, x.sdLon, x.foo FROM observation x

Actual:
 SELECT x.sdValue, x.lat, x.lon, x.foo FROM observation x
*/
