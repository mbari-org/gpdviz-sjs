val gpdvizVersion = "0.0.4"

version := gpdvizVersion
name := "gpdviz"
scalaVersion := "2.11.8"

scalacOptions := Seq("-unchecked", "-deprecation", "-encoding", "utf8", "-Xlint")

mainClass in assembly := Some("gpdviz.WebServer")

assemblyJarName in assembly := s"gpdviz-$gpdvizVersion.jar"


libraryDependencies ++= {
  val sprayV      = "1.3.2"
  val akkaV       = "2.4.10"
  val scalatestV  = "2.2.1"
  val esriV       = "1.2.1"
  val pusherV     = "1.0.0"

  Seq(
    "io.spray"             %% "spray-can"       % sprayV,
    "io.spray"             %% "spray-routing"   % sprayV,
    "io.spray"             %% "spray-json"      % sprayV,
    "io.spray"             %% "spray-testkit"   % sprayV     % "test",
    "org.scalatest"        %% "scalatest"       % scalatestV % "test",
    "com.typesafe.akka"    %% "akka-actor"      % akkaV,

    "com.esri.geometry"     % "esri-geometry-api" % esriV,

    "com.pusher"            % "pusher-http-java" % pusherV
  )
}
