package net.interdoodle.hanuman.message

import akka.actor.ActorRef
import net.interdoodle.hanuman.domain.Hanuman.{Simulations, TextMatchMap}


/** 
 * @author Mike Slinn */
case class SimulationStatus(complete:Boolean, winner:Option[ActorRef], simulations:Simulations) {
  def getSimulation(simulationID:String) = {
    simulations.getOrElse(simulationID, None)
  }

  def putSimulation(simulationID:String, textMatchMap:TextMatchMap) = {
    simulations += simulationID -> textMatchMap
  }
}