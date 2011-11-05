package net.interdoodle.hanuman.domain

import akka.stm.Ref
import akka.event.EventHandler
import collection.mutable.HashMap
import scala.collection.JavaConversions._
import net.interdoodle.hanuman.message._
import akka.actor.{ActorRef, Actor}


/** Monkey god (supervises simulations/Monkey supervisors)
 * @author Mike Slinn */

class Hanuman(val simulationID:String,
              val maxTicks:Int,
              val monkeysPerVisor:Int,
              val document:String,
              val simulationStatusRef:Ref[SimulationStatus]) extends Actor {
  var simulationStatus = simulationStatusRef.get
  val monkeyResultRefMap = new Hanuman.TextMatchRefMap()

  
  override def postStop() {
  }

  override def preStart() {
    createMonkeyVisor()
  }

  def createMonkeyVisor() {
    val monkeyResult = new TextMatch(null, 0, 0, 0)
    val monkeyVisorRef = Actor.actorOf(new MonkeyVisor(simulationID, maxTicks, document, monkeysPerVisor, monkeyResultRefMap, simulationStatusRef))
    simulationStatus.putSimulation(simulationID, Some(monkeyVisorRef))
    self.link(monkeyVisorRef)
    monkeyVisorRef.start()
  }

  def receive = {
    case DocumentMatch(monkeyRef, startIndex) =>
      // TODO summarize
      //simulationStatus.put(monkeyRef.uuid, ??)
      simulationStatusRef.set(simulationStatus)
      EventHandler.info(this, "Hanuman is done")

    case SendSimulationStatus(sessionID) =>
      EventHandler.info(this, "TODO send simulation status")

    case "stop" =>
      EventHandler.info(this, "Hanuman received a stop message")
      for (val monkeyVisorRef <- self.linkedActors.values()) {
        monkeyVisorRef.stop() // monkeyVisor's postStop() also stops linked Monkeys
        self.unlink(monkeyVisorRef)
      }
      //simulationStatus.put(monkeyRef.uuid, ??)
      simulationStatusRef.set(simulationStatus)

    case "stopped" =>
      EventHandler.info(this, "Hanuman received a 'stopped' message from a MonkeyVisor")
      if (self.linkedActors.size()==0) { // MonkeyVisor already summarized its simulation
        //self.stop() // Keep Hanuman running
      }

    case _ =>
      EventHandler.info(this, "Hanuman received an unknown message")
  }
}

object Hanuman {
  type Simulations = HashMap[String, Option[ActorRef]]
  type TextMatchMap = HashMap[String, TextMatch]
  type TextMatchRefMap = HashMap[String, Ref[TextMatch]]
}