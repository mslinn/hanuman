package net.interdoodle.hanuman.domain

import net.interdoodle.hanuman.message.TextMatch
import blueeyes.json.JsonAST.{JBool, JInt, JField, JObject, JString}
import org.joda.time.format.{DateTimeFormat, PeriodFormatterBuilder}
import org.joda.time.{DateTime, Period}


/** Status of one simulation.
 * Simulation starts counting time from the moment a SimulationStatus object is constructed.
 * @author Mike Slinn */
class SimulationStatus (val id:String, val maxTicks:Int, val workCellsPerVisor:Int) {
  var complete = false
  var bestTextMatch = new TextMatch("", null, 0, 0, 0)
  val timeStarted = new DateTime
  var tick = 0


  def decompose = JObject(
    JField("complete",             JBool(complete)) ::
    JField("id",                   JString(id)) ::
    JField("length",               JInt(bestTextMatch.length)) ::
    JField("formattedElapsedTime", JString(formattedElapsedTime)) ::
    JField("formattedTimeStarted", JString(formattedTimeStarted)) ::
    JField("maxTicks",             JInt(maxTicks)) ::
    JField("percentComplete",      JInt(percentComplete)) ::
    JField("tick",                 JInt(tick)) ::
    JField("monkeys",              JInt(workCellsPerVisor)) ::
    Nil
  )

  def formattedElapsedTime = SimulationStatus.periodFormatter.print(new Period(timeStarted, new DateTime()))

  def formattedTimeStarted = timeStarted.toString(SimulationStatus.timeFormatter)

  def percentComplete =
      ((tick.asInstanceOf[Float] / maxTicks.asInstanceOf[Float]) * 100.0).asInstanceOf[Int]
}

object SimulationStatus {
  private val periodFormatter = new PeriodFormatterBuilder().printZeroAlways().minimumPrintedDigits(2)
    .appendHours().appendSuffix(":").appendMinutes().appendSuffix(":").appendSeconds().toFormatter()

  private val timeFormatter = DateTimeFormat.forPattern("hh:mm:ss a")
}