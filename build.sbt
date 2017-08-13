val gpdvizVersion = "0.1.0"

val scalaV        = "2.12.2"
val akkaHttpV     = "10.0.9"
val akkaHttpCorsV = "0.2.1"
val cfgV          = "0.0.5"
val scalatestV    = "3.0.3"
val esriV         = "1.2.1"
val pusherV       = "1.0.0"
val autowireV     = "0.2.6"
val upickleV      = "0.4.4"


def commonSettings = Seq(
  name := "gpdviz",
  version := gpdvizVersion,
  scalaVersion := scalaV,
  scalaJSUseMainModuleInitializer := true,
  libraryDependencies ++= Seq(
    "com.lihaoyi"    %%%   "autowire"   % autowireV,
    "com.lihaoyi"    %%%   "upickle"    % upickleV
  ),
  scalacOptions ++= Seq("-deprecation", "-feature", "-encoding", "utf8",
    "-Ywarn-dead-code", "-unchecked", "-Xlint", "-Ywarn-unused-import")
)

mainClass in assembly := Some("gpdviz.WebServer")
assemblyJarName in assembly := s"gpdviz-$gpdvizVersion.jar"

lazy val root = project.in(file("."))
  .aggregate(gpdvizJS, gpdvizJVM)
  .settings(
    publish := {},
    publishLocal := {}
  )

//lazy val gpdviz = crossProject(JVMPlatform, JSPlatform) // Use this with ScalaJs 1.x
lazy val gpdviz = crossProject
  .in(file("."))
  .settings(commonSettings: _*)
  .jvmSettings(
    libraryDependencies ++= Seq(
      "org.scala-js"         %% "scalajs-stubs"        % scalaJSVersion % "provided",
      "com.typesafe.akka"    %% "akka-http"            % akkaHttpV,
      "com.typesafe.akka"    %% "akka-http-spray-json" % akkaHttpV,
      "com.typesafe.akka"    %% "akka-http-testkit"    % akkaHttpV,
      "ch.megard"            %% "akka-http-cors"       % akkaHttpCorsV,
      "org.scalatest"        %% "scalatest"            % scalatestV % "test",
      "com.github.carueda"   %% "cfg"                  % cfgV % "provided",
      "com.esri.geometry"     % "esri-geometry-api"    % esriV,
      "com.pusher"            % "pusher-http-java"     % pusherV

      /*
      ,"org.tpolecat"   %%  "doobie-core"                % "0.4.1"
      ,"org.tpolecat"   %%  "doobie-postgres"            % "0.4.1"
      ,"org.tpolecat"   %%  "doobie-contrib-postgresql"  % "0.3.0a"
      ,"org.postgis"     %  "postgis-jdbc"               % "1.3.3"
      */
    ),
    addCompilerPlugin(
      ("org.scalameta" % "paradise" % "3.0.0-M8").cross(CrossVersion.full)
    )
  )
  //.jsConfigure(_.enablePlugins(ScalaJSPlugin, JSDependenciesPlugin)).
  .jsSettings(
    libraryDependencies ++= Seq(
      "org.scala-js"         %%%  "scalajs-dom"        % "0.9.3"
    ),
    jsDependencies += RuntimeDOM % "test",
    jsDependencies ++= Seq(
      "org.webjars"   %    "leaflet"    % "1.0.0" / "1.0.0/leaflet.js"
    )
  )

lazy val gpdvizJS  = gpdviz.js
lazy val gpdvizJVM = gpdviz.jvm.settings(
  (resources in Compile) += (fastOptJS in (gpdvizJS, Compile)).value.data
)

// Puts the js source map under jvm's classpath so it can be resolved.
// Execute 'package' to trigger this.
// TODO how to include this as part or 'gpdvizJVM/runMain gpdviz.server.GpdvizServer`?
resourceGenerators in Compile += Def.task {
  val f1 = (fastOptJS in Compile in gpdvizJS).value.data
  val f1SourceMap = f1.getParentFile / (f1.getName + ".map")
  val file = (classDirectory in Compile in gpdvizJVM).value / f1SourceMap.name
  println("copying source map " + file)
  IO.copyFile(f1SourceMap, file)
  Seq(file)
}.taskValue
