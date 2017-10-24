lazy val gpdvizVersion = setVersion("0.3.2")
val scalaV        = "2.12.2"
val akkaHttpV     = "10.0.9"
val akkaHttpCorsV = "0.2.1"
val cfgV          = "0.0.7"
val scalatestV    = "3.0.3"
val esriV         = "1.2.1"
val pusherV       = "1.0.0"
val autowireV     = "0.2.6"
val upickleV      = "0.4.4"
val pprintV       = "0.5.2"
/*
val quillV        = "2.0.0"
val doobieVersion = "0.5.0-M8"
*/
val swaggerAkkaV  = "0.11.0"

val scalaJsDomV      = "0.9.3"
val bindingV         = "10.0.2"
val macrosParadiseV  = "2.1.0"
val momentScalaJsV   = "0.9.0"


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

mainClass in assembly := Some("gpdviz.server.GpdvizServer")
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

      ,"org.postgresql"       % "postgresql"           % "42.1.4"

/*
      ,"io.getquill"         %% "quill-jdbc"           % quillV

      ,"org.tpolecat"        %% "doobie-core"          % doobieVersion
      ,"org.tpolecat"        %% "doobie-postgres"      % doobieVersion
      ,"org.tpolecat"        %% "doobie-specs2"        % doobieVersion
*/

      ,"com.typesafe.slick"  %% "slick"                % "3.2.1"
      ,"com.typesafe.slick"  %% "slick-hikaricp"       % "3.2.1"
      ,"com.github.tminglei" %% "slick-pg"             % "0.15.3"

      ,"com.typesafe.scala-logging" %% "scala-logging" % "3.7.2"

      ,"com.github.swagger-akka-http" %% "swagger-akka-http" % swaggerAkkaV
      ,"org.slf4j"                     % "slf4j-simple"      % "1.7.25" // used by swagger-akka-http
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
      "ru.pavkin"                 %%%  "scala-js-momentjs"  %  momentScalaJsV
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

def setVersion(version: String): String = {
  println(s"setting version $version")
  val indexFile = file("jvm/src/main/resources/web/index.html")
  val contents = IO.readLines(indexFile).mkString("\n")
  val updated = contents.replaceAll("<!--v-->[^<]*<!--v-->", s"<!--v-->$version<!--v-->")
  IO.write(indexFile, updated)
  IO.write(file("jvm/src/main/resources/reference.conf"), s"gpdviz.version = $version")
  version
}
