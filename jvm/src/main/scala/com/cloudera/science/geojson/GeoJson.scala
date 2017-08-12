// From https://github.com/jwills/geojson/blob/master/src/main/scala/com/cloudera/science/geojson/GeoJson.scala
// with slight modifications including:
// - use Geometry instead of RichGeometry
// - GeoJsonProtocol as a trait, not object
// - remove redundant val modifier

/**
 * Copyright (c) 2014, Cloudera, Inc. All Rights Reserved.
 *
 * Cloudera, Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"). You may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * This software is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for
 * the specific language governing permissions and limitations under the
 * License.
 */
package com.cloudera.science.geojson

import com.esri.core.geometry.{Geometry, GeometryEngine}
import spray.json._

case class Feature(id: Option[JsValue],
                   properties: Map[String, JsValue],
                   geometry: Geometry) {
  def apply(property: String) = properties(property)
  def get(property: String) = properties.get(property)
}

case class FeatureCollection(features: Array[Feature])
    extends IndexedSeq[Feature] {
  def apply(index: Int) = features(index)
  def length = features.length
}

case class GeometryCollection(geometries: Array[Geometry])
    extends IndexedSeq[Geometry] {
  def apply(index: Int) = geometries(index)
  def length = geometries.length
}

trait GeoJsonProtocol extends DefaultJsonProtocol {
  implicit object RichGeometryJsonFormat extends RootJsonFormat[Geometry] {
    def write(g: Geometry) = {
      GeometryEngine.geometryToGeoJson(g).parseJson
    }
    def read(value: JsValue) = {
      val mg = GeometryEngine.geometryFromGeoJson(value.compactPrint, 0, Geometry.Type.Unknown)
      mg.getGeometry
    }
  }

  implicit object FeatureJsonFormat extends RootJsonFormat[Feature] {
    def write(f: Feature) = {
      val buf = scala.collection.mutable.ArrayBuffer(
        "type" -> JsString("Feature"),
        "properties" -> JsObject(f.properties),
        "geometry" -> f.geometry.toJson)
      f.id.foreach(v => { buf += "id" -> v})
      JsObject(buf.toMap)
    }

    def read(value: JsValue) = {
      val jso = value.asJsObject
      val id = jso.fields.get("id")
      val properties = jso.fields("properties").asJsObject.fields
      val geometry = jso.fields("geometry").convertTo[Geometry]
      Feature(id, properties, geometry)
    }
  }

  implicit object FeatureCollectionJsonFormat extends RootJsonFormat[FeatureCollection] {
    def write(fc: FeatureCollection) = {
      JsObject(
        "type" -> JsString("FeatureCollection"),
        "features" -> JsArray(fc.features.map(_.toJson): _*)
      )
    }

    def read(value: JsValue) = {
      FeatureCollection(value.asJsObject.fields("features").convertTo[Array[Feature]])
    }
  }

  implicit object GeometryCollectionJsonFormat extends RootJsonFormat[GeometryCollection] {
    def write(gc: GeometryCollection) = {
      JsObject(
        "type" -> JsString("GeometryCollection"),
        "geometries" -> JsArray(gc.geometries.map(_.toJson): _*))
    }

    def read(value: JsValue) = {
      GeometryCollection(value.asJsObject.fields("geometries").convertTo[Array[Geometry]])
    }
  }
}
