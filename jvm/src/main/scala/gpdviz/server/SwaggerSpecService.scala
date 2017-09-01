package gpdviz.server

import com.github.swagger.akka.SwaggerHttpService
import com.github.swagger.akka.model.Info

object SwaggerSpecService extends SwaggerHttpService {
  override val info = Info(
    description = "gpdviz API",
    version = "0.3.0"  // TODO set version from build.sbt
  )

  // the url of your api, not swagger's json endpoint
  override val host = "localhost:5050"  // TODO set host according to actual deployment

  override val basePath = "/api/"    //the basePath for the API you are exposing

  override val apiDocsPath = "api-docs" //where you want the swagger-json endpoint exposed

  override val apiClasses: Set[Class[_]] = Set(
    classOf[SsService],
    classOf[OneSsService],
    classOf[OneStrService],
    classOf[ObsService]
  )
}
