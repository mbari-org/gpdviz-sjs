package gpdviz.webapp

import com.thoughtworks.binding.Binding.Constants
import com.thoughtworks.binding.{Binding, dom}
import gpdviz.model.VmDataStream
import moment.Moment
import org.scalajs.dom.raw.Node

case class ChartDiv(
                   id:          String,
                   heightStr:   String,
                   minWidthStr: String
                   )

class View(vm: VModel) {

  def render(): Unit = {

    dom.render(elm.sysName, sysName)
    dom.render(elm.sysDesc, sysDesc)
    dom.render(elm.absoluteCharts, absoluteCharts)
    dom.render(elm.sysActivity, sysActivity())
  }

  @dom private val sysName = <span>{ vm.ss.bind.name.map(" - " + _).getOrElse("") }</span>

  @dom private val sysDesc = <span>{ vm.ss.bind.description.getOrElse("") }</span>

  @dom private val absoluteCharts =
    <div>
    {
      vm.absoluteCharts map { c ⇒
        //console.log("ADDING absoluteChart div id=" + c.id)
        <div id={ c.id }
             class="absoluteChart"
             style={ s"'min-width': ${c.minWidthStr}; height: ${c.heightStr}" }
        >
        </div>
      }
    }
    </div>

  @dom private def sysActivity(): Binding[Node] =
    <div>
      <div style="font-size:small;color:gray">
        <div>{ vm.ss.bind.center.map(c ⇒ "Center: lat=%.3f, lon=%.3f" format (c.lat, c.lon)).getOrElse("") }</div>
        <div title="clickListener">{ vm.ss.bind.clickListener.getOrElse("") }</div>
      </div>
      <div>{ streamsBinding.bind }</div>
    </div>

  @dom private def streamsBinding: Binding[Node] =
    <ul style="margin-left:-20px">
      {
      for (str <- Constants(vm.ss.bind.streams.sortBy(_.strid): _*))
        yield <li style="margin-bottom:10px">{ streamBinding(str).bind } </li>
      }
    </ul>

  @dom private def streamBinding(str: VmDataStream): Binding[Node] =
    <div>
      <div>
        <span style="font-weight: bold">{ str.strid }</span>
        <!--
        { str.name.map(" - " + _).getOrElse("") }
        { str.description.map(" - " + _).getOrElse("") }
        <div>{ str.chartStyle.map(" chartStyle=" + _).getOrElse("") }</div>
        <div>{ str.mapStyle.map(" mapStyle=" + _).getOrElse("") }</div>
        -->
        <ul>
          {
          for (v <- Constants(str.variables.getOrElse(List.empty): _*))
            yield <li>
              { v.name +
                v.units.map(" (" + _ + ")").getOrElse("")
                // + v.chartStyle.map( " chartStyle=" + _).getOrElse("")
              }
            </li>
          }
        </ul>
      </div>
      <div>
        {
          val obss = str.observations.getOrElse(Map.empty)
          val timeIsos = Constants(obss.keys.toSeq.sorted: _*)
          <div>Observations: { timeIsos.bind.length.toString }</div>
          <div style="font-size:small">
            Latest: { timeIsos.bind.lastOption.getOrElse("") }
          </div>
          <!--ul>
            {
            for (timeIso <- timeIsos)
              yield <li>{ timeIso + " -> " + obss(timeIso).map(pp(_)) }</li>
            }
          </ul-->
        }
      </div>
    </div>
}
