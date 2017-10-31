package gpdviz.server

import javax.ws.rs.Path

import akka.http.scaladsl.server.{Directives, Route}
import ch.megard.akka.http.cors.scaladsl.CorsDirectives.cors
import gpdviz.model._
import gpdviz.{ApiImpl, AutowireServer}
import io.swagger.annotations._

import scala.concurrent.ExecutionContext.Implicits.global

trait GpdvizService extends
  SsService with OneSsService with OneStrService with VariableDefService with ObsService
  with StaticAndAjaxService with RootIndexService {

  def routes: Route = {
    cors()(SwaggerSpecService.routes) ~
      swaggerUi ~
      staticAndAjaxRoute ~
      rootIndexRoute ~
      variableDefRoute ~ obsRoute ~ oneStrRoute ~ oneSsRoute ~ ssRoute
  }

  private val swaggerUi: Route =
    path("api-docs") { getFromResource("swaggerui/index.html") } ~
      getFromResourceDirectory("swaggerui")
}


@Api(produces = "application/json")
@Path("/ss")
trait SsService extends GpdvizServiceImpl with Directives {
  def ssRoute: Route = {
    ssAdd ~ ssList
  }

  @ApiOperation(value = "Register sensor system", nickname = "registerSystem",
    tags = Array("sensor system"),
    httpMethod = "POST", code = 201,
    response = classOf[SensorSystemSummary])
  @ApiImplicitParams(Array(
    new ApiImplicitParam(
      name = "body", value = "sensor system definition", required = true,
      dataTypeClass = classOf[SensorSystemAdd], paramType = "body")
  ))
  @ApiResponses(Array(
    new ApiResponse(code = 409, message = "Sensor system already registered")
  ))
  def ssAdd: Route = path("api" / "ss") {
    (post & entity(as[SensorSystemAdd])) { ssr ⇒
      complete {
        addSensorSystem(ssr)
      }
    }
  }

  @ApiOperation(value = "List all registered sensor systems", nickname = "listSystems",
    tags = Array("sensor system"),
    httpMethod = "GET", response = classOf[Array[SensorSystemSummary]])
  def ssList: Route = path("api" / "ss") {
    cors() {
      get {
        complete {
          db.listSensorSystems()
        }
      }
    }
  }
}

@Api(produces = "application/json")
@Path("/ss")
trait OneSsService extends GpdvizServiceImpl with Directives {

  def oneSsRoute: Route = {
    strAdd ~ ssGet ~ ssUpdate ~ ssDelete
  }

  @Path("/{sysid}")
  @ApiOperation(value = "Add a data stream", nickname = "registerStream",
    tags = Array("sensor system", "data stream"),
    httpMethod = "POST", code = 201,
    response = classOf[DataStreamSummary])
  @ApiImplicitParams(Array(
    new ApiImplicitParam(
      name = "sysid", value = "sensor system id", required = true,
      dataType = "string", paramType = "path"),
    new ApiImplicitParam(
      name = "body", value = "stream definition", required = true,
      dataTypeClass = classOf[DataStreamAdd], paramType = "body")
  ))
  @ApiResponses(Array(
    new ApiResponse(code = 404, message = "Undefined sensor system")
  ))
  def strAdd: Route = pathPrefix("api" / "ss" / Segment) { sysid ⇒
    (post & entity(as[DataStreamAdd])) { strr ⇒
      complete {
        addDataStream(sysid, strr)
      }
    }
  }

  @Path("/{sysid}")
  @ApiOperation(value = "Get a sensor system", nickname = "getSystem",
    tags = Array("sensor system"),
    httpMethod = "GET", response = classOf[SensorSystem])
  @ApiImplicitParams(Array(
    new ApiImplicitParam(
      name = "sysid", value = "sensor system id", required = true,
      dataType = "string", paramType = "path")
  ))
  @ApiResponses(Array(
    new ApiResponse(code = 404, message = "Undefined sensor system")
  ))
  def ssGet: Route = pathPrefix("api" / "ss" / Segment) { sysid ⇒
    cors() {
      get {
        complete {
          getSensorSystem(sysid)
        }
      }
    }
  }

  @Path("/{sysid}")
  @ApiOperation(value = "Update a sensor system", nickname = "updateSystem",
    tags = Array("sensor system"),
    httpMethod = "PUT", response = classOf[SensorSystemSummary])
  @ApiImplicitParams(Array(
    new ApiImplicitParam(
      name = "sysid", value = "sensor system id", required = true,
      dataType = "string", paramType = "path"),
    new ApiImplicitParam(
      name = "body", value = "Properties to update. All elements are optional.",
      required = true,
      dataTypeClass = classOf[SensorSystemUpdate], paramType = "body")
  ))
  @ApiResponses(Array(
    new ApiResponse(code = 404, message = "Undefined sensor system")
  ))
  def ssUpdate: Route = pathPrefix("api" / "ss" / Segment) { sysid ⇒
    (put & entity(as[SensorSystemUpdate])) { ssu ⇒
      complete {
        updateSensorSystem(sysid, ssu)
      }
    }
  }

  @Path("/{sysid}")
  @ApiOperation(value = "Unregister a sensor system", nickname = "deleteSystem",
    tags = Array("sensor system"),
    httpMethod = "DELETE", response = classOf[SensorSystemSummary])
  @ApiImplicitParams(Array(
    new ApiImplicitParam(
      name = "sysid", value = "sensor system id", required = true,
      dataType = "string", paramType = "path")
  ))
  @ApiResponses(Array(
    new ApiResponse(code = 404, message = "Undefined sensor system")
  ))
  def ssDelete: Route = pathPrefix("api" / "ss" / Segment) { sysid ⇒
    delete {
      complete {
        deleteSensorSystem(sysid)
      }
    }
  }
}

@Api(produces = "application/json", tags = Array("data stream"))
@Path("/ss")
trait OneStrService extends GpdvizServiceImpl with Directives {
  def oneStrRoute: Route = {
    strGet ~ strDelete
  }

  @Path("/{sysid}/{strid}")
  @ApiOperation(value = "Get a data stream", nickname = "getStream",
    httpMethod = "GET", response = classOf[DataStream])
  @ApiImplicitParams(Array(
    new ApiImplicitParam(
      name = "sysid", value = "sensor system id", required = true,
      dataType = "string", paramType = "path"),
    new ApiImplicitParam(
      name = "strid", value = "data stream id", required = true,
      dataType = "string", paramType = "path")
  ))
  @ApiResponses(Array(
    new ApiResponse(code = 404, message = "Undefined sensor system or data stream")
  ))
  def strGet: Route = {
    pathPrefix("api" / "ss" / Segment / Segment) { case (sysid, strid) ⇒
      cors() {
        get {
          complete {
            getDataStream(sysid, strid)
          }
        }
      }
    }
  }

  @Path("/{sysid}/{strid}")
  @ApiOperation(value = "Delete a data stream", nickname = "deleteStream",
    httpMethod = "DELETE", response = classOf[DataStreamSummary])
  @ApiImplicitParams(Array(
    new ApiImplicitParam(
      name = "sysid", value = "sensor system id", required = true,
      dataType = "string", paramType = "path"),
    new ApiImplicitParam(
      name = "strid", value = "data stream id", required = true,
      dataType = "string", paramType = "path")
  ))
  @ApiResponses(Array(
    new ApiResponse(code = 404, message = "Undefined sensor system or data stream")
  ))
  def strDelete: Route = {
    pathPrefix("api" / "ss" / Segment / Segment) { case (sysid, strid) ⇒
      delete {
        complete {
          deleteDataStream(sysid, strid)
        }
      }
    }
  }
}

@Api(produces = "application/json", tags = Array("variable definition"))
@Path("/ss")
trait VariableDefService extends GpdvizServiceImpl with Directives {
  def variableDefRoute: Route = {
    vdAdd
  }

  @Path("/{sysid}/{strid}/vd")
  @ApiOperation(
    value = "Add variable definition", nickname = "addVariableDef",
    httpMethod = "POST",
    code = 201, response = classOf[VariableDefSummary])
  @ApiImplicitParams(Array(
    new ApiImplicitParam(
      name = "sysid", value = "sensor system id", required = true,
      dataType = "string", paramType = "path"),
    new ApiImplicitParam(
      name = "strid", value = "data stream id", required = true,
      dataType = "string", paramType = "path"),
    new ApiImplicitParam(
      name = "body", value = "The variable definition", required = true,
      dataTypeClass = classOf[VariableDef], paramType = "body")
  ))
  @ApiResponses(Array(
    new ApiResponse(code = 404, message = "Undefined sensor system or data stream")
  ))
  def vdAdd: Route = {
    pathPrefix("api" / "ss" / Segment / Segment / "vd") { case (sysid, strid) ⇒
      (post & entity(as[VariableDef])) { vd ⇒
        complete {
          addVariableDef(sysid, strid, vd)
        }
      }
    }
  }
}

@Api(produces = "application/json", tags = Array("observation"))
@Path("/ss")
trait ObsService extends GpdvizServiceImpl with Directives {
  def obsRoute: Route = {
    obsAdd
  }

  @Path("/{sysid}/{strid}/obs")
  @ApiOperation(value = "Add observations", nickname = "addObservations",
    httpMethod = "POST", code = 201,
    response = classOf[ObservationsSummary])
  @ApiImplicitParams(Array(
    new ApiImplicitParam(
      name = "sysid", value = "sensor system id", required = true,
      dataType = "string", paramType = "path"),
    new ApiImplicitParam(
      name = "strid", value = "data stream id", required = true,
      dataType = "string", paramType = "path"),
    new ApiImplicitParam(
      name = "body", value = "The observations", required = true,
      dataTypeClass = classOf[ObservationsAdd], paramType = "body")
  ))
  @ApiResponses(Array(
    new ApiResponse(code = 404, message = "Undefined sensor system or data stream"),
    new ApiResponse(code = 400, message = "Malformed ISO-8601 timestamp")
  ))
  def obsAdd: Route = {
    pathPrefix("api" / "ss" / Segment / Segment / "obs") { case (sysid, strid) ⇒
      (post & entity(as[ObservationsAdd])) { obssr ⇒
        complete {
          addObservations(sysid, strid, obssr)
        }
      }
    }
  }
}

trait StaticAndAjaxService extends GpdvizServiceImpl with Directives {
  def staticAndAjaxRoute: Route = {
    val staticRoute = {
      val index = (get & path(Segment ~ Slash)) { sysid ⇒
        complete {
          getSensorSystemIndex(sysid)
        }
      }

      val jsStuff = pathSuffix("gpdviz-fastopt.js" / Segments) { _ ⇒
        getFromResource("gpdviz-fastopt.js")
      } ~ pathSuffix("gpdviz-fastopt.js.map" / Segments) { _ ⇒
        getFromResource("gpdviz-fastopt.js.map")
      } ~ pathSuffix("gpdviz-jsdeps.js" / Segments) { _ ⇒
        getFromResource("gpdviz-jsdeps.js")
      }

      val staticFile = (get & path(Segment / Remaining)) { case (sysid, rest) ⇒
        getFromResource("web/" + rest)
      }

      val staticWeb = get {
        getFromResourceDirectory("web")
      }

      val staticRoot = get {
        getFromResourceDirectory("")
      }

      index ~ staticFile ~ jsStuff ~ staticWeb ~ staticRoot
    }

    val apiImpl = new ApiImpl(db)

    val autowireServer = new AutowireServer(apiImpl)

    val ajax = post {
      path("ajax" / Segments) { s ⇒
        entity(as[String]) { e ⇒
          complete {
            autowireServer.route[gpdviz.Api](apiImpl)(
              autowire.Core.Request(
                s,
                upickle.default.read[Map[String, String]](e)
              )
            )
          }
        }
      }
    }

    ajax ~ staticRoute
  }
}
