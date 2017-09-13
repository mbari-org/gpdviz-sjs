package gpdviz.server

import com.github.swagger.akka.SwaggerHttpService
import com.github.swagger.akka.model.Info
import gpdviz.config.cfg

object SwaggerSpecService extends SwaggerHttpService {
  override val info = Info(
    version = cfg.version,
    title = "Gpdviz REST API",
    description = "The Gpdviz REST API deals with three kinds of resources: " +
      "sensor systems, data streams, and observations."
  )

  // the url of your api, not swagger's json endpoint.
  // Note that no scheme should be included.
  override val host: String = cfg.externalUrl.replaceFirst("^https?://", "")

  override val basePath: String = "/api"    //the basePath for the API you are exposing

  override val apiDocsPath: String = "api-docs" //where you want the swagger-json endpoint exposed

  override val apiClasses: Set[Class[_]] = Set(
    classOf[SsService],
    classOf[OneSsService],
    classOf[OneStrService],
    classOf[ObsService]
  )
}
