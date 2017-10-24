package gpdviz

import gpdviz.config.configFile
import gpdviz.data.DbFactory
import gpdviz.server.GpdvizServer

object Gpdviz {
  def main(args: Array[String]) {
    if (args.contains("generate-conf")) {
      generateConf(args)
    }
    else if (args.contains("create-tables")) {
      createTables(args)
    }
    else if (args.contains("add-some-data")) {
      addSomeData(args)
    }
    else if (args.contains("run-server")) {
      if (!configFile.canRead) {
        System.err.println(s"cannot access $configFile")
      }
      else new GpdvizServer().run(keyToStop = !args.contains("-d"))
    }
    else {
      System.err.println(
        s"""
           |Usage:
           |   gpdviz generate-conf [--overwrite]
           |   gpdviz create-tables
           |   gpdviz add-some-data
           |   gpdviz run-server [-d]
        """.stripMargin)
    }
  }

  private def generateConf(args: Array[String]): Unit = {
    if (configFile.exists() && !args.contains("--overwrite")) {
      System.err.println(s"$configFile exists.  Use --overwrite to overwrite")
    }
    else {
      val conf = scala.io.Source.fromInputStream(
        getClass.getClassLoader.getResource("params_template.conf").openStream()
      ).mkString
      import java.nio.charset.StandardCharsets
      import java.nio.file.Files
      val bytes = conf.getBytes(StandardCharsets.UTF_8)
      Files.write(configFile.toPath, bytes)
      println(s" Configuration generated: $configFile\n")
    }
  }

  private def createTables(args: Array[String]): Unit = {
    val db = DbFactory.openDb
    DbFactory.createTablesSync(db)
    db.close()
  }

  private def addSomeData(args: Array[String]): Unit = {
    val db = DbFactory.openDb
    DbFactory.addSomeDataSync(db)
    db.close()
  }
}
