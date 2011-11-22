package net.interdoodle.hanuman.domain

import akka.actor.Actor
import akka.event.EventHandler
import akka.stm.Ref
import net.interdoodle.hanuman.message._
import scala.collection.JavaConversions._
import types._


/** Monkey god (supervises simulations/Monkey supervisors)
 * @author Mike Slinn */
class Hanuman(val simulationID:String,
              val workCellsPerVisor:Int,
              val maxTicks:Int,
              val document:String,
              val simulationStatusRef:Ref[SimulationStatuses]) extends Actor {
  val textMatchMapRef = new TextMatchMapRef()
  var textMatchMap = new TextMatchMap()
  textMatchMapRef.set(textMatchMap)
  var simulationStatus = simulationStatusRef.get
  simulationStatus.putSimulation(simulationID, textMatchMap)


  override def preStart() {
    createWorkVisor()
  }

  def receive = {
    case GetSimulationStatus(simulationID) =>
      EventHandler.debug(this, "Hanuman returning TextMatchMap for simulation " + simulationID)
      self.channel ! simulationStatus.simulations.get(simulationID)

    case DocumentMatch(workUnitRef, startIndex) =>
      EventHandler.debug(this, "Hanuman is done")
      simulationStatusRef.set(simulationStatus) //FIXME return result

    case "stop" =>
      EventHandler.debug(this, "Hanuman received a stop message")
      for (workVisorRef <- self.linkedActors.values())
        workVisorRef ! "stop"

    case SimulationComplete(simulationID) =>
      EventHandler.debug(this, "Hanuman received a 'stopped' message from a WorkVisor")
      self.unlink(self.sender.get)
      if (self.linkedActors.size()==0) { // WorkVisors are all stopped
        //self.stop() // Keep Hanuman running
        val ss = new SimulationStatuses(true, simulationStatusRef.get.simulations)
        simulationStatusRef.set(ss)
      }
      EventHandler.notify(SimulationComplete(simulationID))

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
