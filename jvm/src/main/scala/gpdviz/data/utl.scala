package gpdviz.data

import java.text.SimpleDateFormat
import java.util.{Date, TimeZone}

object utl {
  def iso(millis: Long): String = df.format(new Date(millis))

  def nowIso: String = df.format(new Date())

  private val df = {
    val x = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    x.setTimeZone(TimeZone.getTimeZone("UTC"))
    x
  }
}
