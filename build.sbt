val gpdvizVersion = "0.0.8"

version := gpdvizVersion
name := "gpdviz"
scalaVersion := "2.11.8"

scalacOptions := Seq("-unchecked", "-deprecation", "-encoding", "utf8", "-Xlint")

mainClass in assembly := Some("gpdviz.WebServer")

assemblyJarName in assembly := s"gpdviz-$gpdvizVersion.jar"

lazy val doobieVersion = "0.4.1"

libraryDependencies ++= {
  val sprayV      = "1.3.2"
  val akkaV       = "2.4.10"
  val scalatestV  = "2.2.1"
  val esriV       = "1.2.1"
  val pusherV     = "1.0.0"

  Seq(
    "io.spray"             %% "spray-can"       % sprayV,
    "io.spray"             %% "spray-routing-shapeless2"   % sprayV,
    "io.spray"             %% "spray-json"      % sprayV,
    "io.spray"             %% "spray-testkit"   % sprayV     % "test",
    "org.scalatest"        %% "scalatest"       % scalatestV % "test",
    "com.typesafe.akka"    %% "akka-actor"      % akkaV,

    "com.esri.geometry"     % "esri-geometry-api" % esriV,

    "com.pusher"            % "pusher-http-java" % pusherV

    ,"org.tpolecat" %% "doobie-core"       % doobieVersion
    ,"org.tpolecat" %% "doobie-postgres"   % doobieVersion
    ,"org.tpolecat" %% "doobie-contrib-postgresql" % "0.3.0a"
    ,"org.postgis"   % "postgis-jdbc"              % "1.3.3"

    ,"org.specs2" % "specs2-core_2.11" % "3.8.6-scalaz-7.1"
    // this one to fix the following during `sbt test`:
    //    java.lang.IncompatibleClassChangeError: Found class scalaz.Memo, but interface was expected
    // Based on https://github.com/etorreborre/specs2/issues/268#issuecomment-47043143
    // but searching maven for a more recent version

  )
}
