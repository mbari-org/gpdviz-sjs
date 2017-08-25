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
val pprintV       = "0.5.2"

val scalaJsDomV      = "0.9.3"
val bindingV         = "10.0.2"
val macrosParadiseV  = "2.1.0"


def commonSettings = Seq(
  name := "gpdviz",
  version := gpdvizVersion,
  scalaVersion := scalaV,
  scalaJSUseMainModuleInitializer := true,
  libraryDependencies ++= Seq(
    "com.lihaoyi"    %%%   "autowire"   % autowireV,
    "com.lihaoyi"    %%%   "upickle"    % upickleV,
    "com.lihaoyi"    %%%   "pprint"     % pprintV
  ),
  scalacOptions ++= Seq("-deprecation", "-feature", "-encoding", "utf8"
    //, "-Ywarn-dead-code"
    //, "-unchecked",
    //, "-Xlint"
    //, "-Ywarn-unused-import"
  )
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
      "org.scala-js"              %%%  "scalajs-dom"        %  scalaJsDomV,
      "com.thoughtworks.binding"  %%%  "dom"                %  bindingV,
      "com.thoughtworks.binding"  %%%  "futurebinding"      %  bindingV
    ),
    addCompilerPlugin("org.scalamacros" % "paradise" % macrosParadiseV cross CrossVersion.full),
    jsDependencies += RuntimeDOM % "test",
    jsDependencies ++= Seq(
      "org.webjars"       %  "momentjs"     %  "2.18.1"  / "moment.js"      minified "moment.min.js",
      "org.webjars"       %  "lodash"       %  "4.17.4"  / "lodash.js"      minified "lodash.min.js",
      "org.webjars"       %  "jquery"       %  "3.2.1"   / "jquery.js"      minified "jquery.min.js",
      "org.webjars"       %  "leaflet"      %  "1.0.0"   / "leaflet.js",
      "org.webjars"       %  "esri-leaflet" %  "2.0.7"   / "esri-leaflet.js" dependsOn "leaflet.js",
      "org.webjars"       %  "highstock"    %  "5.0.14"  / "5.0.14/highstock.js"
    )
  )

lazy val gpdvizJS  = gpdviz.js
lazy val gpdvizJVM = gpdviz.jvm.settings(
  (resources in Compile) += (fastOptJS in (gpdvizJS, Compile)).value.data
)

// Puts some js resources under jvm's classpath so they can be resolved.
// Execute 'package' to trigger this.
resourceGenerators in Compile += Def.task {
  val parentDir = (fastOptJS in Compile in gpdvizJS).value.data.getParentFile

  def copy(name: String): File = {
    val sourceFile = parentDir / name
    require (sourceFile.exists())
    val destFile = (classDirectory in Compile in gpdvizJVM).value / sourceFile.name
    println(s"Copying $sourceFile --> $destFile")
    IO.copyFile(sourceFile, destFile)
    destFile
  }
  Seq(
    copy("gpdviz-fastopt.js.map"),
    copy("gpdviz-jsdeps.js")
  )
}.taskValue
