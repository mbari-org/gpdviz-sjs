val gpdvizVersion = "0.1.0"

version := gpdvizVersion
name := "gpdviz"
scalaVersion := "2.11.8"

scalacOptions := Seq("-unchecked", "-deprecation", "-encoding", "utf8", "-Xlint")

mainClass in assembly := Some("gpdviz.WebServer")

assemblyJarName in assembly := s"gpdviz-$gpdvizVersion.jar"

lazy val doobieVersion = "0.4.1"

libraryDependencies ++= {
  val akkaHttpV   = "10.0.6"
  val scalatestV  = "2.2.1"
  val esriV       = "1.2.1"
  val pusherV     = "1.0.0"

  Seq(
    "com.typesafe.akka"    %% "akka-http"            % akkaHttpV,
    "com.typesafe.akka"    %% "akka-http-spray-json" % akkaHttpV,
    "com.typesafe.akka"    %% "akka-http-testkit"    % akkaHttpV,
    "org.scalatest"        %% "scalatest"            % scalatestV % "test",
    "ch.megard"            %% "akka-http-cors"       % "0.2.1",

    "com.esri.geometry"     % "esri-geometry-api" % esriV,

    "com.pusher"            % "pusher-http-java" % pusherV

/*
    ,"org.tpolecat" %% "doobie-core"       % doobieVersion
    ,"org.tpolecat" %% "doobie-postgres"   % doobieVersion
    ,"org.tpolecat" %% "doobie-contrib-postgresql" % "0.3.0a"
    ,"org.postgis"   % "postgis-jdbc"              % "1.3.3"

    ,"org.specs2" % "specs2-core_2.11" % "3.8.6-scalaz-7.1"
    // this one to fix the following during `sbt test`:
    //    java.lang.IncompatibleClassChangeError: Found class scalaz.Memo, but interface was expected
    // Based on https://github.com/etorreborre/specs2/issues/268#issuecomment-47043143
    // but searching maven for a more recent version
*/

  )
}
