package gpdviz.webapp

import com.thoughtworks.binding.Binding.Constants
import com.thoughtworks.binding.{Binding, dom}
import gpdviz.model.{VmDataStream, VmObsData}
import org.scalajs.dom.raw.Node
import pprint.PPrinter.BlackWhite.{apply ⇒ pp}

import scala.scalajs.js
import scala.scalajs.js.JSConverters._

@js.native
trait LLMap extends js.Object {

  def addSelectionPoint(p: js.Array[Double]): String = js.native

  def addGeoJson(strid: String, feature: String, style: js.UndefOr[String]): Unit = js.native
}


class View(vm: VModel, llmap: LLMap) {

  llmap.addSelectionPoint(js.Array(36.646, -122.02))

  def render(): Unit = {

    @dom val sysName = <span>{ vm.ss.bind.name.map(" - " + _).getOrElse("") }</span>
    @dom val sysDesc = <span>{ vm.ss.bind.description.getOrElse("") }</span>

    dom.render(elm.sysName, sysName)
    dom.render(elm.sysDesc, sysDesc)
    dom.render(elm.sysActivity, sysActivity())
  }

  @dom private def sysActivity(): Binding[Node] = {
    <div>
      <div>Center: { vm.ss.bind.center.map(_.toString).getOrElse("") }</div>
      <div>{ streamsBinding.bind }</div>
    </div>
  }

  @dom private def streamsBinding: Binding[Node] = {
    <ul>
      {
      for (str <- Constants(vm.ss.bind.streams.values.toSeq: _*))
        yield <li>{ streamBinding(str).bind } </li>
      }
    </ul>
  }

  @dom private def streamBinding(str: VmDataStream): Binding[Node] = {
    <div>
      { str.strid + str.description.map(" - " + _).getOrElse("") }
      <div>Variables:
        <ul>
          {
          for (v <- Constants(str.variables.getOrElse(List.empty): _*))
            yield <li>{ v.name + v.units.map( " units=" + _).getOrElse("") }</li>
          }
        </ul>
      </div>
      <div>Observations:
        <ul>
          {
          val obss = str.observations.getOrElse(Map.empty)
          for (timestamp <- Constants(obss.keys.toSeq.sorted: _*))
            yield <li>{ timestamp + " -> " + obss(timestamp).map(handleObservation(str)) }</li>
          }
        </ul>
      </div>
    </div>
  }

  private def handleObservation(str: VmDataStream)(obs: VmObsData): String = {
    obs.feature foreach { feature ⇒
      llmap.addGeoJson(str.strid, feature, str.mapStyle.orUndefined)
    }

    obs.geometry foreach { geometry ⇒
      llmap.addGeoJson(str.strid, geometry, str.mapStyle.orUndefined)
    }

    pp(obs).toString
  }
}
