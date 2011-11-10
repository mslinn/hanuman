package net.interdoodle.hanuman.domain

import akka.actor.Actor
import akka.event.EventHandler
import akka.stm.Ref
import collection.mutable.HashMap
import net.interdoodle.hanuman.domain.Hanuman.{TextMatchMap, TextMatchMapRef}
import net.interdoodle.hanuman.message._
import scala.collection.JavaConversions._
import akka.actor.Uuid


/** Monkey god (supervises simulations/Monkey supervisors)
 * @author Mike Slinn */
class Hanuman(val simulationID:String,
              val workCellsPerVisor:Int,
              val maxTicks:Int,
              val document:String,
              val simulationStatusRef:Ref[SimulationStatus]) extends Actor {
  val textMatchMapRef = new TextMatchMapRef()
  var textMatchMap = new TextMatchMap()
  textMatchMapRef.set(textMatchMap)
  var simulationStatus = simulationStatusRef.get
  simulationStatus.putSimulation(simulationID, textMatchMap)


  override def postStop() {
  }

  override def preStart() {
    createWorkVisor()
  }

  def receive = {
    case DocumentMatch(workUnitRef, startIndex) =>
      // TODO summarize
      //simulationStatus.put(workUnitRef.uuid, ??)
      simulationStatusRef.set(simulationStatus)
      EventHandler.debug(this, "Hanuman is done")

    case "stop" =>
      EventHandler.debug(this, "Hanuman received a stop message")
      for (workVisorRef <- self.linkedActors.values()) {
        workVisorRef.stop() // workVisor's postStop() also stops linked WorkCells
        self.unlink(workVisorRef)
      }

    case "stopped" =>
      EventHandler.debug(this, "Hanuman received a 'stopped' message from a WorkVisor")
      if (self.linkedActors.size()==0) { // MonkeyVisor already summarized its simulation
        //self.stop() // Keep Hanuman running
      }

    case _ =>
      EventHandler.info(this, "Hanuman received an unknown message")
  }

  private def createWorkVisor() {
    val workVisorRef = Actor.actorOf(
      new WorkVisor(simulationID, maxTicks, document, workCellsPerVisor, textMatchMapRef))
    self.link(workVisorRef)
    workVisorRef.start()
  }
}

object Hanuman {
  /** map of simulation sessionID to TextMatch map */
  type Simulations = HashMap[String, TextMatchMap]
  type TextMatchMap = HashMap[Uuid, TextMatch]
  type TextMatchMapRef = Ref[TextMatchMap]
}