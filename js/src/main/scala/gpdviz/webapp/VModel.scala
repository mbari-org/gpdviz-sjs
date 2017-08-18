package gpdviz.webapp

import com.thoughtworks.binding.Binding.{Var, Vars}
import gpdviz.model.VmSensorSystem

class VModel(sysid: String) {

  val ss: Var[VmSensorSystem] = Var(VmSensorSystem(sysid))

  val absoluteCharts: Vars[ChartDiv] = Vars.empty
}
