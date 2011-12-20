package net.interdoodle.hanuman.message

import blueeyes.json.JsonAST.{JBool, JInt, JField, JObject, JString, JValue}

import org.joda.time.{DateTime, Period}
import org.joda.time.format.{DateTimeFormat, PeriodFormatterBuilder}
import net.interdoodle.hanuman.Configuration


/** Status of one simulation.
 * Simulation starts counting time from the moment a SimulationStatus object is constructed.
 * @author Mike Slinn */
case class SimulationStatus(simulationId:String, maxTicks:Int, workCellsPerVisor:Int,
    var complete:Boolean = false,
    var bestTextMatch:TextMatch = new TextMatch("", null, 0, 0, 0),
    var tick:Int = 0,
    var timeStarted:DateTime = new DateTime) {

  val document = Configuration().defaultDocument


  def decompose = JObject(
    JField("complete",             JBool(complete)) ::
    JField("documentLength",       document.length().asInstanceOf[JValue]) ::
    JField("simulationId",         JString(simulationId)) ::
    JField("length",               JInt(bestTextMatch.length)) ::
    JField("formattedElapsedTime", JString(formattedElapsedTime)) ::
    JField("formattedTimeStarted", JString(formattedTimeStarted)) ::
    JField("matchedPortion",       matchedPortion.asInstanceOf[JString]) ::
    JField("maxTicks",             JInt(maxTicks)) ::
    JField("percentComplete",      JInt(percentComplete)) ::
    JField("tick",                 JInt(tick)) ::
    JField("monkeys",              JInt(workCellsPerVisor)) ::
    Nil
  )

  /** Use a copy of this case class for messaging */
  def copy(simulationId:String = this.simulationId, maxTicks:Int = this.maxTicks, workCellsPerVisor:Int = this.workCellsPerVisor) = {
    var ss = new SimulationStatus(simulationId, maxTicks, workCellsPerVisor)
    ss.complete = this.complete
    ss.bestTextMatch = this.bestTextMatch.copy(simulationId)
    ss.tick = this.tick
    ss.timeStarted = this.timeStarted
    ss
  }

  def formattedElapsedTime = SimulationStatus.periodFormatter.print(new Period(timeStarted, new DateTime()))

  def formattedTimeStarted = timeStarted.toString(SimulationStatus.timeFormatter)

  def percentComplete =
      ((tick.asInstanceOf[Float] / maxTicks.asInstanceOf[Float]) * 100.0).asInstanceOf[Int]

  def matchedPortion = {
    val textMatch = bestTextMatch
    //println(simulationStatus.tick, document.length, textMatch.length)
    if (textMatch.length>0)
      document.substring(0, scala.math.min(document.length, textMatch.length)-1)
    else
      ""
  }
}

object SimulationStatus {
  private val periodFormatter = new PeriodFormatterBuilder().printZeroAlways().minimumPrintedDigits(2)
    .appendHours().appendSuffix(":").appendMinutes().appendSuffix(":").appendSeconds().toFormatter()

  private val timeFormatter = DateTimeFormat.forPattern("hh:mm:ss a")
}