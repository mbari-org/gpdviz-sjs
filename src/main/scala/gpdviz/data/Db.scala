package gpdviz.data

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Paths}

import gpdviz.GnError
import gpdviz.model.SensorSystem
import spray.json._
import gpdviz.JsonImplicits

import scala.util.control.NonFatal


class Db(dataDir: String) extends JsonImplicits {

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
    // make sure the data in each stream are sorted in increasing timestamp
    val ordered = ss.streams.mapValues { dataStream =>
      val orderedObs = dataStream.obs.map { _.sortBy(_.timestamp) }
      dataStream.copy(obs = orderedObs)
    }
    doSave(ss.copy(streams = ordered))
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
