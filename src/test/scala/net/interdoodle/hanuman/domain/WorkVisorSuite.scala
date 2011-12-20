package net.interdoodle.hanuman.domain

import akka.actor.{Actor, ActorSystem, Props, Uuid}
import net.interdoodle.hanuman.message.TextMatch
import org.scalatest.FunSuite
import types._


/** @see http://www.scalatest.org/scaladoc/1.6.1/#org.scalatest.FunSuite
 * @author Mike Slinn */
class WorkVisorSuite extends FunSuite {
  test("generatePage") {
    val simulationID = "simulation1"
    val workCellActorRef = null
    val textMatch:TextMatch = new TextMatch("", workCellActorRef, 0, 0, 0)
    val textMatchMap = new TextMatchMap()
    val uuid = new Uuid
    textMatchMap += uuid -> textMatch
    val system = ActorSystem("SimulationSupervisorSuite")

    /** Rough character frequency approximation */
    val document = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"*5 +
      "abcdefghijklmnopqrstuvwxyz"*25 +
      "0123456789"*2 +
      "`~!@#$%^&*()_-+={[}]|\\\"':;<,>.?/"
    val simulationSupervisor = system.actorOf(Props().withCreator(new SimulationSupervisor(simulationID, 10, document, 10))).start()
    // todo write more tests and SimulationSupervisor business logic
  }
}