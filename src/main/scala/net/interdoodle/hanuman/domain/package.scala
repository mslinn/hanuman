package net.interdoodle.hanuman.domain

import akka.actor.Uuid
import collection.mutable.HashMap
import net.interdoodle.hanuman.message.{TextMatch, SimulationStatus}


/** Hanuman domain types
 * @author Mike Slinn */
package object types {
  /** Map of simulationID -> SimulationStatus */
  type SimulationStatuses = HashMap[String, SimulationStatus]
  type TextMatchMap = HashMap[Uuid, TextMatch]
  type TextMatchMapImmutable = scala.collection.immutable.HashMap[Uuid, TextMatch]
}
