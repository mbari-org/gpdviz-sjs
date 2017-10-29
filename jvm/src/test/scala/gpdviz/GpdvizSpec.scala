package gpdviz

import akka.http.scaladsl.model.ContentTypes._
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.testkit.ScalatestRouteTest
import gpdviz.async.{Notifier, NullPublisher}
import gpdviz.data.{DbFactory, DbInterface}
import gpdviz.model._
import gpdviz.server._
import org.scalatest.{Matchers, WordSpec}
//import pprint.PPrinter.Color.{apply ⇒ pp}


class GpdvizSpec extends WordSpec with Matchers with ScalatestRouteTest with GpdvizService {
  val notifier: Notifier = new Notifier(NullPublisher)
  val db: DbInterface = DbFactory.testDb

  var sysid: Option[String] = None
  val strid = "aStrId"
  val strid2 = "bStrId"

  override def beforeAll(): Unit = {
    super.beforeAll()
    DbFactory.createTablesSync(db, dropFirst = true)
  }

  override def afterAll(): Unit = {
    db.close()
    super.afterAll()
  }

  "sensor system service " should {
    "respond with all sensor systems" in {
      Get("/api/ss") ~> routes ~> check {
        status shouldBe OK
        val systems = responseAs[Seq[SensorSystemSummary]]
        sysid = Some(s"test_sys${systems.size}")
      }
    }

    "return 404 for non-existent sensor system" in {
      Get(s"/api/ss/${sysid.get}") ~> routes ~> check {
        status shouldBe NotFound
      }
    }

    "add a new sensor system" in {
      val ssAdd = SensorSystemAdd(sysid.get,
        name = Some("test ss"),
        description = Some("test description"),
        center = Some(LatLon(36.8, -122.04))
      )
      Post(s"/api/ss", ssAdd) ~> routes ~> check {
        status shouldBe OK
        contentType shouldBe `application/json`
        val ss = responseAs[SensorSystemSummary]
        ss.sysid shouldBe sysid.get
        ss.name shouldBe Some("test ss")
        ss.description shouldBe Some("test description")
        ss.center shouldBe Some(LatLon(36.8, -122.04))
        ss.pushEvents shouldBe Some(true)
      }
    }

    "fail to add existing sensor system" in {
      Post(s"/api/ss", SensorSystemAdd(sysid.get)) ~> routes ~> check {
        status shouldBe Conflict
        contentType shouldBe `application/json`
        val error = responseAs[GnError]
        error.sysid shouldBe sysid
      }
    }

    "find existing sensor system" in {
      Get(s"/api/ss/${sysid.get}") ~> routes ~> check {
        status shouldBe OK
        contentType shouldBe `application/json`
        val ss = responseAs[SensorSystem]
        ss.sysid shouldBe sysid.get
        ss.name shouldBe Some("test ss")
        ss.description shouldBe Some("test description")
        ss.center shouldBe Some(LatLon(36.8, -122.04))
        ss.pushEvents shouldBe true
      }
    }

    "update an existing sensor system" in {
      val ssUpdate = SensorSystemUpdate(center = Some(LatLon(36, -122)), pushEvents = Some(false))
      Put(s"/api/ss/${sysid.get}", ssUpdate) ~> routes ~> check {
        status shouldBe OK
        contentType shouldBe `application/json`
        val ss = responseAs[SensorSystemSummary]
        ss.sysid shouldBe sysid.get
        //ss.center shouldBe Some(LatLon(36, -122))
        //ss.pushEvents shouldBe Some(false)
      }
    }

    "add streams" in {
      val variables = Some(List(VariableDef("temperature")))
      val dsAdd = DataStreamAdd(strid = strid, variables = variables)
      Post(s"/api/ss/${sysid.get}", dsAdd) ~> routes ~> check {
        status shouldBe OK
        contentType shouldBe `application/json`
        val ds = responseAs[DataStreamSummary]
        ds.sysid === sysid.get
        ds.strid === strid
        //ds.variables === variables
      }

      val variables2 = Some(List(VariableDef("fooVar")))
      val dsAdd2 = DataStreamAdd(strid = strid2, variables = variables2)
      Post(s"/api/ss/${sysid.get}", dsAdd2) ~> routes ~> check {
        status shouldBe OK
        contentType shouldBe `application/json`
        val ds = responseAs[DataStreamSummary]
        ds.sysid === sysid.get
        ds.strid === strid2
        //ds.variables === variables
      }
    }

    "fail to add an existing a stream" in {
      Post(s"/api/ss/${sysid.get}", DataStreamAdd(strid)) ~> routes ~> check {
        //println(s"::::: status=$status: " + pp(response))
        status shouldBe Conflict
        contentType shouldBe `application/json`
        val error = responseAs[GnError]
        error.sysid shouldBe sysid
        error.strid shouldBe Some(strid)
      }
    }

    "delete stream" in {
      Delete(s"/api/ss/${sysid.get}/$strid2") ~> routes ~> check {
        status shouldBe OK
        contentType shouldBe `application/json`
        val ss = responseAs[DataStreamSummary]
        ss.sysid shouldBe sysid.get
        ss.strid shouldBe strid2
      }
    }

    "fail to delete non-existent stream" in {
      Delete(s"/api/ss/${sysid.get}/NoStr") ~> routes ~> check {
        status shouldBe NotFound
        contentType shouldBe `application/json`
        val error = responseAs[GnError]
        error.sysid shouldBe sysid
        error.strid shouldBe Some("NoStr")
      }
    }

    "add observations" in {
      val vars = List("temperature")
      val obsAdd = ObservationsAdd(observations = Map(
        "0" → List(ObsData(
          scalarData = Some(ScalarData(
            vars = vars,
            vals = List(13.0)
          )))),
        "1" → List(ObsData(
          scalarData = Some(ScalarData(
            vars = vars,
            vals = List(11.0)
          )))),
        "5" → List(ObsData(
          scalarData = Some(ScalarData(
            vars = vars,
            vals = List(12.0)
          )))),
        "9" → List(ObsData(
          scalarData = Some(ScalarData(
            vars = vars,
            vals = List(11.5)
          ))))
      ))

      Post(s"/api/ss/${sysid.get}/$strid/obs", obsAdd) ~> routes ~> check {
        status shouldBe OK
        contentType shouldBe `application/json`
        val map = responseAs[ObservationsSummary]
        map.sysid === sysid.get
        map.strid === strid
        map.added shouldBe Some(4)
      }
    }

    "delete an existing sensor system" in {
      Delete(s"/api/ss/${sysid.get}") ~> routes ~> check {
        status shouldBe OK
        contentType shouldBe `application/json`
        val ss = responseAs[SensorSystemSummary]
        ss.sysid shouldBe sysid.get
      }
    }

    "fail to delete non-existent sensor system" in {
      Delete(s"/api/ss/NoSs") ~> routes ~> check {
        status shouldBe NotFound
        contentType shouldBe `application/json`
        val error = responseAs[GnError]
        error.sysid shouldBe Some("NoSs")
      }
    }

    "not find just removed sensor system" in {
      Get(s"/api/ss/${sysid.get}") ~> routes ~> check {
        status shouldBe NotFound
        contentType shouldBe `application/json`
        val error = responseAs[GnError]
        error.sysid shouldBe sysid
      }
    }
  }
}
