// https://github.com/getquill/quill/issues/932

interp.repositories() ++= Seq(coursier.MavenRepository(
  "https://oss.sonatype.org/content/repositories/snapshots"
))
@
import $ivy.`io.getquill::quill-sql:2.1.1-SNAPSHOT`

import io.getquill._
val ctx = new SqlMirrorContext(PostgresDialect, Literal)

import ctx._

case class LatLon(lat: Double, lon: Double) extends Embedded

case class ScalarData(value: Double,
                      position: Option[LatLon] = None
                     ) extends Embedded

case class Observation(scalarData: Option[ScalarData] = None,
                       foo: Option[String] = None
                      )

val observation = quote {
  querySchema[Observation]("observation",
    _.scalarData.map(_.value)                → "sdValue"
    ,_.scalarData.map(_.position.map(_.lat)) → "sdLat"
    ,_.scalarData.map(_.position.map(_.lon)) → "sdLon"
  )
}

val m = run(observation)
println(m.string)

/*
quill932.sc:32: exception during macro expansion:
scala.reflect.macros.TypecheckException: type mismatch;
 found   : ammonite.$file.quill932_2.LatLon
 required: Option[ammonite.$file.quill932_2.LatLon]
	at scala.reflect.macros.contexts.Typers.$anonfun$typecheck$3(Typers.scala:32)
	at scala.reflect.macros.contexts.Typers.$anonfun$typecheck$2(Typers.scala:26)
	at scala.reflect.macros.contexts.Typers.doTypecheck$1(Typers.scala:25)
	at scala.reflect.macros.contexts.Typers.$anonfun$typecheck$7(Typers.scala:38)
	at scala.reflect.internal.Trees.wrappingIntoTerm(Trees.scala:1710)
	at scala.reflect.internal.Trees.wrappingIntoTerm$(Trees.scala:1707)
	at scala.reflect.internal.SymbolTable.wrappingIntoTerm(SymbolTable.scala:16)
	at scala.reflect.macros.contexts.Typers.typecheck(Typers.scala:38)
	at scala.reflect.macros.contexts.Typers.typecheck$(Typers.scala:20)
	at scala.reflect.macros.contexts.Context.typecheck(Context.scala:6)
	at scala.reflect.macros.contexts.Context.typecheck(Context.scala:6)
	at io.getquill.context.QueryMacro.expandQueryWithMeta(QueryMacro.scala:41)
	at io.getquill.context.QueryMacro.expandQuery(QueryMacro.scala:20)
	at io.getquill.context.QueryMacro.runQuery(QueryMacro.scala:12)

val m = run(observation)
           ^
Compilation Failed
*/
