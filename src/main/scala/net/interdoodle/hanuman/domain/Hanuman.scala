package net.interdoodle.hanuman.domain

import akka.actor.Actor
import akka.event.EventHandler
import net.interdoodle.hanuman.message._
import scala.collection.JavaConversions._
import types._


/** This actor supervisor never stops, however the SimulationSupervisors that it supervises are torn down at the end of
 * each simulation
 * @author Mike Slinn */
class Hanuman extends Actor {
  private val simulationStatuses = new SimulationStatuses

  
  def receive = {
    case DocumentMatch(simulationId, startIndex) =>
      EventHandler.debug(this, "Hanuman: Simulation completed with a DocumentMatch (hooray!)")
      val simulationStatus = simulationStatuses.get(simulationId)
      simulationStatus.get.complete = true
      self.channel ! simulationStatus // signal completion

    case GetSimulationStatus(simulationId) =>
      EventHandler.debug(this, "Hanuman was requested to provide status for simulation " + simulationId)
      self.channel ! simulationStatuses.get(simulationId)

    case NewSimulation(simulationId, workCellsPerVisor, maxTicks, document) =>
      EventHandler.debug(this, "Hanuman was requested create new simulation " + simulationId)
      simulationStatuses += simulationId -> new SimulationStatus(simulationId)
      val simVisorRef = Actor.actorOf(new SimulationSupervisor(simulationId, maxTicks, document, workCellsPerVisor))
      self.link(simVisorRef)
      simVisorRef.start()

    case SimulationStopped(simulationId) =>
      EventHandler.debug(this, "Hanuman: SimulationSupervisor acknowledged a Stop message")
      EventHandler.notify(SimulationStopped(simulationId)) // signal completion to non-actor

    case Stop =>
      EventHandler.debug(this, "Hanuman received a Stop message")
      for (simVisorRef <- self.linkedActors.values())
        simVisorRef ! Stop

    /** Only the newly top-ranked TextMatches for a simulation are sent to Hanuman */
    case TextMatch(simulationId, workCellRef, matchLength, matchStart, matchEnd) =>
      val simulationStatus = simulationStatuses.get(simulationId)
      simulationStatus.get.bestTextMatch = TextMatch(simulationId, workCellRef, matchLength, matchStart, matchEnd)
  }
}
