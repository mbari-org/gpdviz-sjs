package gpdviz.data

import gpdviz.config.MongoCfg
import gpdviz.model._
import gpdviz.server.GnError
import org.mongodb.scala._
import org.mongodb.scala.bson.ObjectId
import org.mongodb.scala.model.Filters._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Future, Promise}


private case class MgSensorSystem(_id: ObjectId,
                        sysid:        String,
                        name:         Option[String] = None,
                        description:  Option[String] = None,
                        streams:      Map[String, MgDataStream] = Map.empty,
                        pushEvents:   Boolean = true,
                        center:       Option[LatLon] = None,
                        zoom:         Option[Int] = None,
                        clickListener: Option[String] = None
                       )

private case class MgDataStream(
                      strid:    String,
                      name:         Option[String] = None,
                      description:  Option[String] = None,
                      mapStyle:     Option[Document] = None,
                      zOrder:       Int = 0,
                      variables:    Option[List[MgVariableDef]] = None,
                      chartStyle:   Option[Document] = None,
                      observations: Option[Map[String, List[MgObsData]]] = None
                     )

private case class MgVariableDef(
                       name:          String,
                       units:         Option[String] = None,
                       chartStyle:    Option[Document] = None
                      )

private case class MgObsData(
                   feature:     Option[Document] = None,
                   geometry:    Option[Document] = None,
                   scalarData:  Option[ScalarData] = None
                  )

class MongoDb(mCfg: MongoCfg) extends DbInterface {

  val details: String = s"MongoDb-based database (uri: ${mCfg.uri})"

  def listSensorSystems(): Future[Seq[SensorSystemSummary]] = {
    collection.find().map(sys ⇒
      SensorSystemSummary(sys.sysid, sys.name, sys.description, sys.streams.keySet)
    ).toFuture()
  }

  def getSensorSystem(sysid: String): Future[Option[SensorSystem]] = {
    collection.find(equal("sysid", sysid)).first().map(mss2ss).toFuture().map(_.headOption)
  }

  private def mss2ss(mss: MgSensorSystem): SensorSystem = {
    SensorSystem(
      mss.sysid,
      mss.name,
      mss.description,
      mss.streams.mapValues(v ⇒ DataStream(
        v.strid,
        v.name,
        v.description,
        zOrder = v.zOrder,
        variables = v.variables.map(_ map (w ⇒ VariableDef(w.name, w.units)))
      )),
      mss.pushEvents,
      mss.center,
      mss.zoom,
      mss.clickListener
    )
  }

  private def ss2mss(ss: SensorSystem): MgSensorSystem = {
    MgSensorSystem(
      new ObjectId(),
      ss.sysid,
      ss.name,
      ss.description,
      ss.streams.mapValues(v ⇒ MgDataStream(
        v.strid,
        v.name,
        v.description,
        zOrder = v.zOrder,
        variables = v.variables.map(_ map (w ⇒ MgVariableDef(w.name, w.units)))
      )),
      ss.pushEvents,
      ss.center,
      ss.zoom,
      ss.clickListener
    )
  }

  override def saveSensorSystem(ss: SensorSystem): Future[Either[GnError, String]] = {

    val p = Promise[Either[GnError, String]]()

    def insert(ss: SensorSystem): Unit = {
      //println(s"MongoDb: saveSensorSystem: saving ...")
      collection.insertOne(ss2mss(ss)).subscribe(new Observer[Completed] {
        override def onNext(result: Completed): Unit = {
          println(s"MongoDb: saveSensorSystem: saved.")
          p.success(Right(ss.sysid))
        }

        override def onError(e: Throwable): Unit = {
          println(s"MongoDb: saveSensorSystem: error: ${e.getMessage}")
          p.success(Left(GnError(500, s"error saving sensor system: ${e.getMessage}")))
        }

        override def onComplete(): Unit = {
          //println(s"MongoDb: saveSensorSystem: onComplete.")
        }
      })
    }

    getSensorSystem(ss.sysid) map {
      case Some(_) ⇒
        deleteSensorSystem(ss.sysid) map {
          case Right(_) ⇒ insert(ss)
          case l@Left(_)  ⇒ p.success(l)
        }

      case None ⇒
        insert(ss)
    }

    p.future
  }

  def deleteSensorSystem(sysid: String): Future[Either[GnError, String]] = {
    val p = Promise[Either[GnError, String]]()
    getSensorSystem(sysid) map {
      case Some(ss) ⇒
        collection.deleteOne(equal("sysid", sysid)).toFuture().map { x ⇒
          p.success(Right(ss.sysid))
        }

      case None ⇒
        p.success(Left(GnError(404, "Not registered", sysid = Some(sysid))))
    }
    p.future
  }

  private val mongoClient: MongoClient = MongoClient(mCfg.uri)

  private val database: MongoDatabase = {
    import org.bson.codecs.configuration.CodecRegistries.{fromProviders, fromRegistries}
    import org.mongodb.scala.bson.codecs.DEFAULT_CODEC_REGISTRY
    import org.mongodb.scala.bson.codecs.Macros._

    val codecRegistry = fromRegistries(
      fromProviders(
        classOf[MgSensorSystem],
        classOf[MgDataStream],
        classOf[LatLon],
        classOf[MgVariableDef]
        //,classOf[JsObject]
        //,classOf[JsString]
        //,classOf[JsNumber]
      ),
      DEFAULT_CODEC_REGISTRY
    )
    mongoClient.getDatabase(mCfg.database).withCodecRegistry(codecRegistry)
  }

  private val collection: MongoCollection[MgSensorSystem] = database.getCollection(mCfg.collection)
}
