package net.interdoodle.hanuman.message

import net.interdoodle.hanuman.domain.types._


/**
 * @author Mike Slinn */
case class SimulationStatuses(complete:Boolean, simulations:Simulations) {
  def getSimulation(simulationID:String) = {
    simulations.getOrElse(simulationID, None)
  }

  def putSimulation(simulationID:String, textMatchMap:TextMatchMap) = {
    simulations += simulationID -> textMatchMap
  }
}