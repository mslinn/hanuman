package net.interdoodle.hanuman.domain

import akka.event.EventHandler
import net.interdoodle.hanuman.message._
import scala.collection.JavaConversions._
import types._
import collection.mutable.HashMap
import akka.actor.{ActorRef, Actor}


/** This actor supervisor never stops, however the SimulationSupervisors that it supervises are torn down at the end of
 * each simulation
 * @author Mike Slinn */
class Hanuman extends Actor {
  private val simulationStatuses = new SimulationStatuses

  /** Keep track of running simulations */
  private val simulationSupervisors = new HashMap[String, ActorRef].empty


  def receive = {
    case DocumentMatch(simulationId, startIndex) =>
      EventHandler.debug(this, "Hanuman: Simulation completed with a DocumentMatch (hooray!)")
      val simulationStatus = simulationStatuses.get(simulationId)
      self.channel ! simulationStatus // signal completion
      simulationSupervisors.get(simulationId) match {
        case Some(simulationSupervisorRef) => simulationSupervisorRef ! Stop
        case None =>
      }
      simulationSupervisors -= simulationId

    case GetSimulationStatus(simulationId) =>
      EventHandler.debug(this, "Hanuman was requested to provide status for simulation " + simulationId)
      self.channel ! simulationStatuses.get(simulationId)

    case ListSimulations() =>
      self.channel ! simulationStatuses // TODO should a copy be sent instead?
      
    case NewSimulation(simulationId, workCellsPerVisor, maxTicks, document) =>
      EventHandler.debug(this, "Hanuman was requested create new simulation " + simulationId)
      simulationStatuses += simulationId -> new SimulationStatus(simulationId, maxTicks, workCellsPerVisor)
      val simulationSupervisorRef = Actor.actorOf(new SimulationSupervisor(simulationId, maxTicks, document, workCellsPerVisor))
      self.link(simulationSupervisorRef)
      simulationSupervisors += simulationId -> simulationSupervisorRef
      simulationSupervisorRef.start()

    case SimulationStopped(simulationId) =>
      EventHandler.debug(this, "Hanuman: SimulationSupervisor acknowledged a Stop message")
      EventHandler.notify(SimulationStopped(simulationId)) // signal completion to non-actor

    case StopSimulation(simulationId) =>
      EventHandler.debug(this, "Hanuman received a StopSimulation message for " + simulationId)
      simulationSupervisors.get(simulationId) match {
        case Some(simulationSupervisorRef) => simulationSupervisorRef ! Stop
        case None =>
      }
      simulationSupervisors -= simulationId

    /** sent every tick by every simulationSupervisor */
    case SimulationStatus(simulationId, maxTicks, workCellsPerVisor, complete, bestTextMatch, tick, timeStarted) =>
      simulationStatuses += simulationId ->
        SimulationStatus(simulationId, maxTicks, workCellsPerVisor, complete, bestTextMatch, tick, timeStarted)
  }
}
