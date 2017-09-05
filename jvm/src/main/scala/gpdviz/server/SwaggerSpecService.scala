package gpdviz.server

import com.github.swagger.akka.SwaggerHttpService
import com.github.swagger.akka.model.Info
import gpdviz.config.cfg

object SwaggerSpecService extends SwaggerHttpService {
  override val info = Info(
    version = cfg.version,
    description =
      """
        |# Basic sequence
        |
        |- Register a sensor system - `POST /ss`
        |- Add one or more data streams to the sensor system - `POST /ss/{sysid}`
        |- Add data stream observations - `POST /ss/{sysid}/{strid}/obs`
        |
      """.stripMargin,
  )

  // the url of your api, not swagger's json endpoint
  override val host: String = cfg.externalUrl

  override val basePath: String = "/api/"    //the basePath for the API you are exposing

  override val apiDocsPath: String = "api-docs" //where you want the swagger-json endpoint exposed

  override val apiClasses: Set[Class[_]] = Set(
    classOf[SsService],
    classOf[OneSsService],
    classOf[OneStrService],
    classOf[ObsService]
  )
}
