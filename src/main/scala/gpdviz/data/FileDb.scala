package gpdviz.data

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Paths}

import gpdviz.GnError
import gpdviz.model.SensorSystem
import spray.json._
import gpdviz.JsonImplicits

import scala.util.control.NonFatal


class FileDb(dataDir: String) extends JsonImplicits with DbInterface {

  val details: String = s"File-based database.  dataDir: $dataDir"

  def listSensorSystems(): Map[String, SensorSystem] = {
    val files = dataPath.toFile.listFiles.filter(_.getName.endsWith(".ss.json"))
    files.toSeq.map { f =>
      val sysid = f.getName.replaceAll(""".ss.json$""", "")
      sysid -> getSensorSystem(sysid).get
    }.toMap
  }

  def getSensorSystem(sysid: String): Option[SensorSystem] = {
    getSensorSystemByFilename(sysid + ".ss.json")
  }

  def saveSensorSystem(ss: SensorSystem): Either[GnError, SensorSystem] = {
    doSave(ss)
  }

  def deleteSensorSystem(sysid: String): Either[GnError, SensorSystem] = {
    getSensorSystem(sysid) match {
      case Some(ss) =>
        val filename = sysid + ".ss.json"
        val ssPath = Paths.get(dataDir, filename)
        try {
          Files.delete(ssPath)
          Right(ss)
        }
        catch {
          case NonFatal(e) => Left(GnError(500, s"error saving sensor system: ${e.getMessage}"))
        }

      case None => Left(GnError(404, "Not registered", sysid = Some(sysid)))
    }
  }

  private def getSensorSystemByFilename(filename: String): Option[SensorSystem] = {
    val ssPath = Paths.get(dataDir, filename)
    val ssFile = ssPath.toFile
    if (ssFile.exists()) {
      val contents = io.Source.fromFile(ssFile).mkString
      Option(contents.parseJson.convertTo[SensorSystem])
    }
    else None
  }

  private def doSave(ss: SensorSystem): Either[GnError, SensorSystem] = {
    val filename = ss.sysid + ".ss.json"
    //println(s"SAVE $filename")
    val ssPath = Paths.get(dataDir, filename)
    try {
      Files.write(ssPath, ss.toJson.prettyPrint.getBytes(StandardCharsets.UTF_8))
      Right(ss)
    }
    catch {
      case NonFatal(e) => e.printStackTrace()
        Left(GnError(500, s"error saving sensor system: ${e.getMessage}"))
    }
  }

  private val dataPath = Paths.get(dataDir)
  dataPath.toFile.mkdir()
}
