package gpdviz

import gpdviz.config.cfg
import gpdviz.data.DbInterface
import gpdviz.model._
import gpdviz.server.JsonImplicits
import spray.json._


class ApiImpl(db: DbInterface) extends Api with JsonImplicits {

  def clientConfig(): ClientConfig = ClientConfig(
    serverName = cfg.serverName,
    center = LatLon(cfg.map.center.lat, cfg.map.center.lon),
    zoom = cfg.map.zoom,
    pusher = cfg.pusher.map(p ⇒ ClientPusherConfig(p.key))
  )

  def refresh(sysid: String): Option[VmSensorSystem] = {
    db.getSensorSystem(sysid) map { ss ⇒
      VmSensorSystem(
        sysid = sysid,
        name = ss.name,
        description = ss.description,
        streams = ss.streams.values.toList.map { ds ⇒
          val mappedObservations = ds.observations map { observations ⇒
            observations mapValues { list ⇒
              val obsDataList = collection.mutable.ListBuffer[VmObsData]()
              list foreach  { o ⇒
                obsDataList += VmObsData(
                  feature = o.feature.map(_.toJson.compactPrint),
                  geometry = o.geometry.map(_.toJson.compactPrint),
                  scalarData = o.scalarData
                )
              }
              obsDataList.toList
            }
          }
          VmDataStream(
            strid = ds.strid,
            name = ds.name,
            description = ds.description,
            mapStyle = ds.mapStyle.map(_.toJson.compactPrint),
            zOrder = ds.zOrder,
            variables = ds.variables.map(_.map(v ⇒ VmVariableDef(v.name, v.units, v.chartStyle.map(_.toJson.compactPrint)))),
            chartStyle = ds.chartStyle.map(_.toJson.compactPrint),
            observations = mappedObservations
          )
        },
        center = ss.center,
        zoom = ss.zoom,
        clickListener = ss.clickListener
      )
    }
  }
}
