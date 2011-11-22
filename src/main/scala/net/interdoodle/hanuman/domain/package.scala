package net.interdoodle.hanuman.domain

import akka.actor.Uuid
import akka.stm.Ref
import collection.mutable.HashMap
import net.interdoodle.hanuman.message.TextMatch


/** Hanuman domain types
 * @author Mike Slinn */

package object types {
  type Simulations = HashMap[String, TextMatchMap]
  type TextMatchMap = HashMap[Uuid, TextMatch]
  type TextMatchMapImmutable = scala.collection.immutable.HashMap[Uuid, TextMatch]
  type TextMatchMapRef = Ref[TextMatchMap]
}
