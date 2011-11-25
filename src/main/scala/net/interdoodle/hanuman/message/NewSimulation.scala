package net.interdoodle.hanuman.message

/** Message to tell Hanuman to set up for a new simulation
 * @author Mike Slinn */
case class NewSimulation(simulationID:String, workCellsPerVisor:Int, maxTicks:Int, document:String)