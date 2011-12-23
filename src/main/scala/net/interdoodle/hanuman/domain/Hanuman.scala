package net.interdoodle.hanuman.domain

import akka.actor.{ActorRef, Actor, ActorSystem, Props}
import akka.event.Logging
import collection.mutable.HashMap
import net.interdoodle.hanuman.message._
import scala.collection.JavaConversions._
import types._


/** This actor supervisor never stops, however the SimulationSupervisors that it supervises are torn down at the end of
 * each simulation
 * @author Mike Slinn */
class Hanuman extends Actor {
  private val log = Logging(context.system, this)
  private val simulationStatuses = new SimulationStatuses()

  /** Keep track of running simulations */
  private val simulationSupervisors = new HashMap[String, ActorRef].empty
  private val system = ActorSystem("Hanuman")


  def receive = {
    case DocumentMatch(simulationId, startIndex) =>
      log.debug("Hanuman: Simulation completed with a DocumentMatch (hooray!)")
      val simulationStatus = simulationStatuses.get(simulationId)
      context.sender ! simulationStatus // signal completion
      simulationSupervisors.get(simulationId) match {
        case Some(simulationSupervisorRef) => simulationSupervisorRef ! Stop
        case None =>
      }
      simulationSupervisors -= simulationId

    case GetSimulationStatus(simulationId) =>
      log.debug("Hanuman was requested to provide status for simulation " + simulationId)
      context.sender ! simulationStatuses.get(simulationId)

    case NewSimulation(simulationId, workCellsPerVisor, maxTicks, document) =>
      log.debug("Hanuman was requested create new simulation " + simulationId)
      simulationStatuses += simulationId -> new SimulationStatus(simulationId, maxTicks, workCellsPerVisor)
      val simulationSupervisorRef = context.actorOf(Props().withCreator(new SimulationSupervisor(simulationId, maxTicks, document, workCellsPerVisor)))
      simulationSupervisors += simulationId -> simulationSupervisorRef

    case SimulationStopped(simulationId) =>
      log.debug("Hanuman: SimulationSupervisor acknowledged a Stop message")
      // TODO figure out how to do this with Akka 2: EventHandler.notify(SimulationStopped(simulationId)) // signal completion to non-actor

    case StopSimulation(simulationId) =>
      log.debug("Hanuman received a StopSimulation message for " + simulationId)
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
