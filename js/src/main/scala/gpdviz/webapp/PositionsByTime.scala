package gpdviz.webapp

import gpdviz.model.LatLon

object PositionsByTime {

  // TODO use Double for timeMs (as Long gets an "opaque" type, perhaps less efficient)

  def set(strid: String, timeMs: Long, position: LatLon): Unit = {
    val strTimePos = strTimePoss.get(strid) match {
      case Some(stp) ⇒ stp
      case None ⇒
        val stp = new StrTimePos
        strTimePoss += strid → stp
        stp
    }
    strTimePos.push(TimePos(timeMs, position))
  }

  def get(strid: String, timeMs: Long): Option[LatLon] = {
    strTimePoss.get(strid).map { stp ⇒
      val list = stp.sort()
      var ii = 0
      var mid = 0
      var break = false
      var kk = list.size - 1
      while (ii < kk && !break) {
        mid = Math.floor((ii + kk) / 2).toInt
        val mid_timeMs = list(mid).timeMs
        if (timeMs < mid_timeMs) {
          kk = mid
        }
        else if (timeMs > mid_timeMs) {
          ii = mid + 1
        }
        else break = true
      }
      list(mid).position
    }
  }

  def reset(strid: String): Unit = strTimePoss.remove(strid)

  def resetAll(): Unit = strTimePoss.clear()

  private val strTimePoss = collection.mutable.HashMap[String, StrTimePos]()

  private case class TimePos(timeMs: Long, position: LatLon)

  private class StrTimePos {
    def push(tp: TimePos): Unit = {
      sorted = false
      list = tp :: list
    }
    def sort(): List[TimePos] = {
      if (!sorted) {
        list = list.sortWith { case (p,q) ⇒ p.timeMs < q.timeMs }
        sorted = true
      }
      list
    }
    private var list = List[TimePos]()
    private var sorted = false
  }
}
