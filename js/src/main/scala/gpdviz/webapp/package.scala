package gpdviz

import com.thoughtworks.binding.Binding

import scala.language.implicitConversions

package object webapp {

  // https://stackoverflow.com/a/42617445/830737
  implicit def makeIntellijHappy[T <: org.scalajs.dom.raw.Node](x: scala.xml.Node): Binding[T] = ???

}
