package gpdviz.server

import akka.http.scaladsl.model._
import akka.http.scaladsl.server.{Directives, Route}
import gpdviz.config.cfg

import scala.concurrent.ExecutionContext.Implicits.global


trait RootIndexService extends GpdvizServiceImpl with Directives {
  def rootIndexRoute: Route = get {
    pathEndOrSingleSlash {
      complete {
        db.listSensorSystems() map { list ⇒
          val items = list map { ss ⇒
            s"""
               |<tr>
               |  <td style="white-space: nowrap;font-size:small">
               |    <a href="${cfg.externalUrl.replaceAll("/+$", "") + "/" + ss.sysid}/">
               |    ${ss.sysid}
               |    </a>
               |  </td>
               |  <td>
               |    ${ss.name.getOrElse("")}
               |  </td>
               |  <td>
               |    ${ss.description.getOrElse("")}
               |  </td>
               |  <td>
               |    ${ss.streamIds.mkString(", ")}
               |  </td>
               |</tr>
               |""".stripMargin
          }
          HttpResponse(entity = HttpEntity(ContentType(MediaTypes.`text/html`, HttpCharsets.`UTF-8`),
            s"""<html>
               |<head>
               |  <title>gpdviz</title>
               |  <link href="https://maxcdn.bootstrapcdn.com/bootstrap/3.3.7/css/bootstrap.min.css" rel="stylesheet">
               |</head>
               |<body style="margin:10px">
               |<div style="width: 100%;text-align: right">
               |  <div style="font-size: small">
               |    <a style="color:gray;text-decoration: none" target="_blank"
               |       href="https://github.com/gpdviz/gpdviz">gpdviz</a>
               |    <a style="color:gray;text-decoration:none;font-size:smaller" target="_blank"
               |       href="https://github.com/gpdviz/gpdviz/blob/master/ChangeLog.md">${cfg.version}</a>
               |    |&nbsp;<a style="color:gray;text-decoration: none" target="_blank"
               |       href="../api-docs">api-docs</a>
               |  </div>
               |</div>
               |<div style="margin:20px">
               |<label>Registered sensor systems</label>
               |<table class="table table-bordered table-condensed">
               |<thead>
               |<tr>
               |  <th>
               |    sysid
               |  </th>
               |  <th>
               |    Name
               |  </th>
               |  <th>
               |    Description
               |  </th>
               |  <th>
               |    Streams
               |  </th>
               |</tr>
               |</thead>
               |<tbody>
               |${items.mkString("\n")}
               |</tbody>
               |</table>
               |</div>
               |</body>
               |</html>
               |""".stripMargin
          ))
        }
      }
    }
  }
}
