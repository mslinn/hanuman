package net.interdoodle.hanuman.domain

import akka.actor.{Actor, ActorRef, ActorSystem, OneForOneStrategy, Props}
import akka.actor.FaultHandlingStrategy._
import akka.event.Logging
import collection.mutable.HashMap
import net.interdoodle.hanuman.message._
import scala.collection.JavaConversions._
import types._
import akka.actor.Terminated


/** This actor supervisor never stops, however the SimulationSupervisors that it supervises are torn down at the end of
 * each simulation
 * @author Mike Slinn */
class Hanuman extends Actor {
  private val log = Logging(context.system, this)
  private val simulationStatuses = new SimulationStatuses()

  /** Keep track of running simulations */
  private val simulationSupervisors = new collection.mutable.HashMap[String, ActorRef].empty
  
  /** Keep track of simulations that are shutting down. This allows us to remember the simulationId of an actorRef that was terminated */
  private val stoppingSupervisors = new collection.mutable.HashMap[ActorRef, String].empty
  private val system = ActorSystem("Hanuman")
    
  private val simulationSupervisorStrategy = OneForOneStrategy({
      case _: ArithmeticException      => Restart
      case _: NullPointerException     => Restart
      case _: IllegalArgumentException => Stop
      case _: Exception                => Escalate
  }: Decider, maxNrOfRetries = Some(10), withinTimeRange = Some(5000))

  
  def receive = {
    case DocumentMatch(simulationId, startIndex) =>
      log.debug("Hanuman: Simulation completed with a DocumentMatch (hooray!)")
      val simulationStatus = simulationStatuses.get(simulationId)
      context.sender ! simulationStatus // signal completion
      simulationSupervisors.get(simulationId) match {
        case Some(simulationSupervisorRef) => simulationSupervisorRef ! StopMsg
        case None =>
      }
      simulationSupervisors -= simulationId

    case GetSimulationStatus(simulationId) =>
      log.debug("Hanuman was requested to provide status for simulation " + simulationId)
      context.sender ! simulationStatuses.get(simulationId)

    case NewSimulation(simulationId, workCellsPerVisor, maxTicks, document) =>
      log.debug("Hanuman was requested create new simulation " + simulationId)
      simulationStatuses += simulationId -> new SimulationStatus(simulationId, maxTicks, workCellsPerVisor)
      val simulationSupervisorRef = context.actorOf(Props()
                                                      .withCreator(new SimulationSupervisor(simulationId, maxTicks, document, workCellsPerVisor))
                                                      .withFaultHandler(simulationSupervisorStrategy))
      context.watch(simulationSupervisorRef); // Terminated() message is the important one
      simulationSupervisors += simulationId -> simulationSupervisorRef

    case Terminated(simSuperRef) =>
      log.debug("Hanuman: SimulationSupervisor and its child actors have all terminated")
      // TODO figure out how to do this with Akka 2: EventHandler.notify(SimulationStopped(stoppingSupervisors.get(simSuperRef))) // signal completion to non-actor

    case StopSimulation(simulationId) =>
      log.debug("Hanuman received a StopSimulation message for " + simulationId)
      val simulationSupervisor = simulationSupervisors.get(simulationId).get
      stoppingSupervisors += simulationSupervisor -> simulationId
      context.stop(simulationSupervisor)
      simulationSupervisors -= simulationId

    /** sent every tick by every simulationSupervisor */
    case SimulationStatus(simulationId, maxTicks, workCellsPerVisor, complete, bestTextMatch, tick, timeStarted) =>
      simulationStatuses += simulationId ->
        SimulationStatus(simulationId, maxTicks, workCellsPerVisor, complete, bestTextMatch, tick, timeStarted)
  }
}
