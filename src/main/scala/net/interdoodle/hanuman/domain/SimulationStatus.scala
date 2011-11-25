package net.interdoodle.hanuman.domain

import net.interdoodle.hanuman.message.TextMatch


/** Status of one simulation
 * @author Mike Slinn */
class SimulationStatus (val SimulationID:String) {
  var complete:Boolean = false
  var bestTextMatch:TextMatch = new TextMatch("", null, -1, 0, 0)
}