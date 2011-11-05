package net.interdoodle.hanuman.domain

import akka.actor.Actor
import akka.stm.Ref
import net.interdoodle.hanuman.message.{SimulationStatus,TextMatch}
import org.scalatest.FunSuite
import net.interdoodle.hanuman.domain.Hanuman._
import collection.mutable.HashMap


/** @see http://www.scalatest.org/scaladoc/1.6.1/#org.scalatest.FunSuite
 * @author Mike Slinn */
class MonkeyVisorSuite extends FunSuite {
  test("generatePage") {
    val simulationID:String = "bogusSimulationID"
    val monkeyResult:TextMatch = new TextMatch(null, 0, 0, 0)
    val monkeyResultRef = Ref(monkeyResult)
    val textMatchRefMap = new TextMatchRefMap()
    textMatchRefMap.put(simulationID, monkeyResultRef)
    val simulations:Simulations = new Simulations()
    val simulationStatusRef = Ref(new SimulationStatus(false, None, simulations))

    /** Rough character frequency approximation */
    val document = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"*5 +
      "abcdefghijklmnopqrstuvwxyz"*25 +
      "0123456789"*2 +
      "`~!@#$%^&*()_-+={[}]|\\\"':;<,>.?/"
    val monkeyVisor = Actor.actorOf(new MonkeyVisor(simulationID, 10, document, 10, textMatchRefMap, simulationStatusRef)).start()
    val future = monkeyVisor ? "generatePages"
    val result:Any = future.get
    // todo write more tests and MonkeyVisor business logic
  }
}