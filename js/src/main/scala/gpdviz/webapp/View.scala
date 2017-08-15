package gpdviz.webapp

import com.thoughtworks.binding.Binding.Constants
import com.thoughtworks.binding.{Binding, dom}
import gpdviz.model.VmDataStream
import org.scalajs.dom.raw.Node

import pprint.PPrinter.BlackWhite.{apply ⇒ pp}

class View(vm: VModel) {

  def render(node: Node): Unit = dom.render(node, binding())

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
            yield <li>{ timestamp + " -> " + obss(timestamp).map(pp(_)) }</li>
          }
        </ul>
      </div>
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

  @dom private def binding(): Binding[Node] = {
    <div>
      <code>{ vm.sysid }</code> -
      <div>{ vm.ss.bind.name.getOrElse("") }</div>
      <div>{ vm.ss.bind.description.getOrElse("") }</div>
      <div>Center: { vm.ss.bind.center.map(_.toString).getOrElse("") }</div>
      { streamsBinding.bind }
    </div>
  }
}
