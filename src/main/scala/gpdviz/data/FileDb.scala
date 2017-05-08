package gpdviz.data

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Paths}

import gpdviz.GnError
import gpdviz.model.{SensorSystem, SensorSystemSummary}
import spray.json._
import gpdviz.JsonImplicits

import scala.util.control.NonFatal


class FileDb(dataDir: String) extends JsonImplicits with DbInterface {

  val details: String = s"File-based database (dir: $dataDir)"

  def listSensorSystems(): Seq[SensorSystemSummary] = {
    val files = dataPath.toFile.listFiles.filter(_.getName.endsWith(".ss.json"))
    val systems: Seq[Option[SensorSystem]] = files.toSeq.map { f =>
      getSensorSystemByFilename(f.getName)
    }
    systems.flatten.map { sys ⇒
      SensorSystemSummary(sys.sysid, sys.name, sys.description, sys.streams.keySet)
    }
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
