package gpdviz

import akka.http.scaladsl.model.ContentTypes._
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.testkit.ScalatestRouteTest
import gpdviz.async.{Notifier, Publisher}
import gpdviz.data.{DbFactory, DbInterface}
import gpdviz.model._
import gpdviz.server._
import org.scalatest.{Matchers, WordSpec}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class MockPublisher extends Publisher {
  def details: String = "mock-publisher"

  def publish(notif: Notif): Unit = {
    list = notif :: list
    //println(s"MockPublisher: publish=" + pprint.PPrinter.Color(notif))
  }

  def lastPublished: Future[Option[Notif]] = Future { list.headOption }
  // TODO: the global execution context seems to help with the checks
  // but this needs to be revisited.

  private var list: List[Notif] = List.empty
}

class GpdvizSpec extends WordSpec with Matchers with ScalatestRouteTest with GpdvizService {
  val dbFactory: DbFactory = new DbFactory
  val db: DbInterface = dbFactory.testDb
  val publisher = new MockPublisher
  val notifier: Notifier = new Notifier(db, publisher)

  var sysid: Option[String] = None
  val strid = "aStrId"
  val strid2 = "bStrId"

  override def beforeAll(): Unit = {
    super.beforeAll()
    dbFactory.createTablesSync(db, dropFirst = true)
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
        status shouldBe Created
        contentType shouldBe `application/json`
        val ss = responseAs[SensorSystemSummary]
        ss.sysid shouldBe sysid.get
        ss.name shouldBe Some("test ss")
        ss.description shouldBe Some("test description")
        ss.center shouldBe Some(LatLon(36.8, -122.04))
        ss.pushEvents shouldBe Some(true)
      }
    }

    "eventually have generated notification about new sensor system" in {
      publisher.lastPublished map { lastNotif ⇒
        lastNotif shouldBe Some(_:SensorSystemAdded)
        lastNotif.head.asInstanceOf[SensorSystemAdded].sysid === sysid.get
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
        ss.center shouldBe Some(LatLon(36, -122))
        ss.pushEvents shouldBe Some(false)
      }
    }

    "not have generated notification about updated sensor system" in {
      // so check for the same previously notified SensorSystemAdded
      publisher.lastPublished map { lastNotif ⇒
        lastNotif shouldBe Some(_: SensorSystemAdded)
        lastNotif.head.asInstanceOf[SensorSystemAdded].sysid === sysid.get
      }
    }

    "update existing sensor system (with pushEvents=true)" in {
      val ssUpdate = SensorSystemUpdate(pushEvents = Some(true))
      Put(s"/api/ss/${sysid.get}", ssUpdate) ~> routes ~> check {
        status shouldBe OK
        contentType shouldBe `application/json`
        val ss = responseAs[SensorSystemSummary]
        ss.sysid shouldBe sysid.get
        ss.pushEvents shouldBe Some(true)
      }
    }

    "eventually have generated notification about updated sensor system (with pushEvents=true)" in {
      publisher.lastPublished map { lastNotif ⇒
        lastNotif shouldBe Some(_: SensorSystemUpdated)
        lastNotif.head.asInstanceOf[SensorSystemUpdated].sysid === sysid.get
      }
    }

    "fail to update non-existing sensor system" in {
      Put(s"/api/ss/NoSS", SensorSystemUpdate(pushEvents = Some(false))) ~> routes ~> check {
        status shouldBe NotFound
        contentType shouldBe `application/json`
        val ss = responseAs[GnError]
        ss.sysid shouldBe Some("NoSS")
      }
    }

    "add data streams" in {
      val variables = Some(List(VariableDef("temperature")))
      val dsAdd = DataStreamAdd(strid = strid, variables = variables)
      Post(s"/api/ss/${sysid.get}", dsAdd) ~> routes ~> check {
        status shouldBe Created
        contentType shouldBe `application/json`
        val ds = responseAs[DataStreamSummary]
        ds.sysid === sysid.get
        ds.strid === strid
        //ds.variables === variables
      }

      val variables2 = Some(List(VariableDef("fooVar")))
      val dsAdd2 = DataStreamAdd(strid = strid2, variables = variables2)
      Post(s"/api/ss/${sysid.get}", dsAdd2) ~> routes ~> check {
        status shouldBe Created
        contentType shouldBe `application/json`
        val ds = responseAs[DataStreamSummary]
        ds.sysid === sysid.get
        ds.strid === strid2
        //ds.variables === variables
      }
    }

    "eventually have generated notification about added data streams" in {
      publisher.lastPublished map { lastNotif ⇒
        lastNotif shouldBe Some(_: DataStreamAdded)
        val notif = lastNotif.head.asInstanceOf[DataStreamAdded]
        notif.sysid === sysid.get
        notif.str.strid === strid2
      }
    }

    val vd = VariableDef("bazVar", units = Some("meter"))

    "add a variable definition" in {
      Post(s"/api/ss/${sysid.get}/$strid/vd", vd) ~> routes ~> check {
        //println(s"::::: status=$status: " + pprint.PPrinter.Color(response))
        status shouldBe Created
        contentType shouldBe `application/json`
        val ds = responseAs[VariableDefSummary]
        ds.sysid === sysid.get
        ds.strid === strid
        ds.name === "bazVar"
        ds.units === Some("meter")
      }
    }

    "eventually have generated notification about added variable definition" in {
      publisher.lastPublished map { lastNotif ⇒
        lastNotif shouldBe Some(_: VariableDefAdded)
        val notif = lastNotif.head.asInstanceOf[VariableDefAdded]
        notif.sysid === sysid.get
        notif.strid === strid
        notif.vd === vd
      }
    }

    "fail to add a variable definition to non-existing stream" in {
      val vd = VariableDef("bazVar")
      Post(s"/api/ss/${sysid.get}/NoStr/vd", vd) ~> routes ~> check {
        //println(s"::::: status=$status: " + pprint.PPrinter.Color(response))
        status shouldBe NotFound
        contentType shouldBe `application/json`
        val ds = responseAs[GnError]
        ds.sysid === sysid.get
        ds.strid === Some("NoStr")
      }
    }

    "fail to add an existing a stream" in {
      Post(s"/api/ss/${sysid.get}", DataStreamAdd(strid)) ~> routes ~> check {
        //println(s"::::: status=$status: " + pprint.PPrinter.Color(response))
        status shouldBe Conflict
        contentType shouldBe `application/json`
        val error = responseAs[GnError]
        error.sysid shouldBe sysid
        error.strid shouldBe Some(strid)
      }
    }

    "delete data stream" in {
      Delete(s"/api/ss/${sysid.get}/$strid2") ~> routes ~> check {
        status shouldBe OK
        contentType shouldBe `application/json`
        val ss = responseAs[DataStreamSummary]
        ss.sysid shouldBe sysid.get
        ss.strid shouldBe strid2
      }
    }

    "eventually have generated notification about deleted data stream" in {
      publisher.lastPublished map { lastNotif ⇒
        lastNotif shouldBe Some(_: DataStreamDeleted)
        val notif = lastNotif.head.asInstanceOf[DataStreamDeleted]
        notif.sysid === sysid.get
        notif.strid === strid
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

    val vars = List("temperature")
    val list1 = List(ObsData(
      scalarData = Some(ScalarData(
        vars = vars,
        vals = List(13.0)
      ))))
    val list2 = List(ObsData(
      scalarData = Some(ScalarData(
        vars = vars,
        vals = List(11.0)
      ))))
    val list3 = List(ObsData(
      scalarData = Some(ScalarData(
        vars = vars,
        vals = List(12.0)
      ))))
    val list4 = List(ObsData(
      scalarData = Some(ScalarData(
        vars = vars,
        vals = List(11.5)
      ))))

    val observations = Map(
      "2017-10-10T10:10:00Z" → list1,
      "2017-10-10T10:10:01Z" → list2,
      "2017-10-10T10:10:02Z" → list3,
      "2017-10-10T10:10:03Z" → list4
    )

    "add observations" in {
      val obsAdd = ObservationsAdd(observations = observations)

      Post(s"/api/ss/${sysid.get}/$strid/obs", obsAdd) ~> routes ~> check {
        status shouldBe Created
        contentType shouldBe `application/json`
        val map = responseAs[ObservationsSummary]
        map.sysid === sysid.get
        map.strid === strid
        map.added shouldBe Some(4)
      }
    }

    "eventually have generated notification about added observations" in {
      publisher.lastPublished map { lastNotif ⇒
        lastNotif shouldBe Some(_: ObservationsAdded)
        val notif = lastNotif.head.asInstanceOf[ObservationsAdded]
        notif.sysid === sysid.get
        notif.strid === strid
      }
    }

    "verify added observations" in {
      Get(s"/api/ss/${sysid.get}") ~> routes ~> check {
        status shouldBe OK
        contentType shouldBe `application/json`
        val ss = responseAs[SensorSystem]
        //println(s"sensor system: ss=${pprint.PPrinter.Color(ss)}")
        ss.streams.contains("aStrId") shouldBe true
        val str = ss.streams("aStrId")
        observations foreach { case (time, list) ⇒
          str.observations.nonEmpty === true
          val retrievedObservations = str.observations.get
          retrievedObservations.get(time).nonEmpty === true
          val obsDataList = retrievedObservations(time)
          val scalarData = obsDataList.flatMap(_.scalarData)
          scalarData === list
        }
      }
    }

    "fail to add observation with malformed timestamp" in {
      val timestamp = "1503090553"
      val observations = Map(
        timestamp → List(ObsData(
          scalarData = Some(ScalarData(
            vars = List("temperature"),
            vals = List(13.0)
          )))))
      val obsAdd = ObservationsAdd(observations = observations)

      Post(s"/api/ss/${sysid.get}/$strid/obs", obsAdd) ~> routes ~> check {
        status shouldBe BadRequest
        contentType shouldBe `application/json`
        val error = responseAs[GnError]
        //println(s"error=${pprint.PPrinter.Color(error)}")
        error.sysid shouldBe sysid
        error.strid shouldBe Some(strid)
        error.timestamp shouldBe Some(timestamp)
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
