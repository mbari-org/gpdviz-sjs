package gpdviz

import com.typesafe.config.ConfigFactory
import gpdviz.async.Notifier
import gpdviz.data.Db
import gpdviz.model.{LatLon, SensorSystem}
import spray.http.StatusCodes._
import spray.http.ContentTypes._
import spray.testkit.ScalatestRouteTest
import org.scalatest._


class GpdvizSpec extends FlatSpec with Matchers with ScalatestRouteTest with MyService {
  val config = ConfigFactory.load().resolve()
  val notifier = new Notifier(config)

  override def actorRefFactory = system // connect the DSL to the test ActorSystem

  val db = new Db("data_test")

  var sysid: Option[String] = None

  "sensor system service " should "respond with all sensor systems" in {
    Get("/api/ss") ~> routes ~> check {
      status shouldBe OK
      val systems = responseAs[Map[String, SensorSystem]]
      sysid = Some(s"test_sys${systems.size}")
    }
  }

  it should "return 404 for unregistered sensor system" in {
    Get(s"/api/ss/${sysid.get}") ~> routes ~> check {
      status shouldBe NotFound
    }
  }

  it should "register a new sensor system" in {
    val ssRegister = SSRegister(sysid.get,
      name = Some("test ss"),
      center = Some(LatLon(36.8, -122.04)))
    Post(s"/api/ss", ssRegister) ~> routes ~> check {
      status shouldBe OK
      contentType shouldBe `application/json`
      val ss = responseAs[SensorSystem]
      ss.sysid shouldBe sysid.get
      ss.name shouldBe Some("test ss")
      ss.center shouldBe Some(LatLon(36.8, -122.04))
      ss.pushEvents shouldBe true
    }
  }

  it should "find registered sensor system" in {
    Get(s"/api/ss/${sysid.get}") ~> routes ~> check {
      status shouldBe OK
    }
  }

  it should "update an existing sensor system" in {
    val ssUpdate = SSUpdate(center = Some(LatLon(36, -122)), pushEvents = Some(false))
    Put(s"/api/ss/${sysid.get}", ssUpdate) ~> routes ~> check {
      status shouldBe OK
      contentType shouldBe `application/json`
      val ss = responseAs[SensorSystem]
      ss.sysid shouldBe sysid.get
      ss.center shouldBe Some(LatLon(36, -122))
      ss.pushEvents shouldBe false
    }
  }

  it should "delete an existing sensor system" in {
    Delete(s"/api/ss/${sysid.get}") ~> routes ~> check {
      status shouldBe OK
      contentType shouldBe `application/json`
      val ss = responseAs[SensorSystem]
      ss.sysid shouldBe sysid.get
    }
  }

  it should "not find unregistered sensor system" in {
    Get(s"/api/ss/${sysid.get}") ~> routes ~> check {
      status shouldBe NotFound
    }
  }
}
