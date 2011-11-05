package net.interdoodle.hanuman.message

import net.interdoodle.hanuman.domain.Hanuman.Simulations
import akka.actor.ActorRef


/** Contains map of simulationID->Option[MonkeyVisorRef]
 * @author Mike Slinn */

case class SimulationStatus(complete:Boolean, winner:Option[ActorRef], simulations:Simulations) {

  def getSimulation(simulationID:String) = {
    simulations.getOrElse(simulationID, None)
  }

  def putSimulation(simulationID:String, monkeyRef:Option[ActorRef]) = {
    simulations += simulationID -> monkeyRef
  }
}