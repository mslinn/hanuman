package net.interdoodle.hanuman.domain

import akka.actor.Actor
import akka.event.EventHandler
import akka.stm.Ref
import collection.mutable.HashMap
import net.interdoodle.hanuman.domain.Hanuman.TextMatchMapRef
import net.interdoodle.hanuman.message._
import scala.collection.JavaConversions._
import akka.actor.Uuid


/** Monkey god (supervises simulations/Monkey supervisors)
 * @author Mike Slinn */
class Hanuman(val simulationID:String,
              val maxTicks:Int,
              val monkeysPerVisor:Int,
              val document:String,
              val simulationStatusRef:Ref[SimulationStatus]) extends Actor {
  var simulationStatus = simulationStatusRef.get
  val textMatchMapRef = new TextMatchMapRef()


  override def postStop() {
  }

  override def preStart() {
    createMonkeyVisor()
  }

  def createMonkeyVisor() {
    val monkeyVisorRef = Actor.actorOf(
      new MonkeyVisor(simulationID, maxTicks, document, monkeysPerVisor, textMatchMapRef))
    simulationStatus.putSimulation(simulationID, textMatchMapRef.get)
    self.link(monkeyVisorRef)
    monkeyVisorRef.start()
  }

  def receive = {
    case DocumentMatch(workUnitRef, startIndex) =>
      // TODO summarize
      //simulationStatus.put(workUnitRef.uuid, ??)
      simulationStatusRef.set(simulationStatus)
      EventHandler.debug(this, "Hanuman is done")

    case "stop" =>
      EventHandler.debug(this, "Hanuman received a stop message")
      for (monkeyVisorRef <- self.linkedActors.values()) {
        monkeyVisorRef.stop() // monkeyVisor's postStop() also stops linked Monkeys
        self.unlink(monkeyVisorRef)
      }

    case "stopped" =>
      EventHandler.debug(this, "Hanuman received a 'stopped' message from a MonkeyVisor")
      if (self.linkedActors.size()==0) { // MonkeyVisor already summarized its simulation
        //self.stop() // Keep Hanuman running
      }

    case _ =>
      EventHandler.info(this, "Hanuman received an unknown message")
  }
}

object Hanuman {
  /** map of simulation sessionID to TextMatch map */
  type Simulations = HashMap[String, TextMatchMap]
  type TextMatchMap = HashMap[Uuid, TextMatch]
  type TextMatchMapRef = Ref[TextMatchMap]
}