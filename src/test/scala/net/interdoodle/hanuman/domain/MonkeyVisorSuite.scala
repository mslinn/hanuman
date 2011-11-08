package net.interdoodle.hanuman.domain

import akka.actor.{Actor, Uuid}
import akka.stm.Ref
import net.interdoodle.hanuman.domain.Hanuman.{Simulations, TextMatchMap, TextMatchMapRef}
import net.interdoodle.hanuman.message.{SimulationStatus, TextMatch}
import org.scalatest.FunSuite


/** @see http://www.scalatest.org/scaladoc/1.6.1/#org.scalatest.FunSuite
 * @author Mike Slinn */
class MonkeyVisorSuite extends FunSuite {
  test("generatePage") {
    val simulationID = "simulation1"
    val workCellActorRef = null
    val textMatch:TextMatch = new TextMatch(workCellActorRef, 0, 0, 0)
    val textMatchMap = new TextMatchMap()
    val uuid = new Uuid
    textMatchMap += uuid -> textMatch
    val textMatchMapRef = new TextMatchMapRef()
    textMatchMapRef.set(textMatchMap)
    val simulations:Simulations = new Simulations()
    val simulationStatusRef = Ref(new SimulationStatus(false, None, simulations))

    /** Rough character frequency approximation */
    val document = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"*5 +
      "abcdefghijklmnopqrstuvwxyz"*25 +
      "0123456789"*2 +
      "`~!@#$%^&*()_-+={[}]|\\\"':;<,>.?/"
    val monkeyVisor = Actor.actorOf(new MonkeyVisor(simulationID, 10, document, 10, textMatchMapRef)).start()
    val future = monkeyVisor ? "generatePages"
    val result:Any = future.get
    // todo write more tests and MonkeyVisor business logic
  }
}