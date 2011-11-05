package net.interdoodle.hanuman.message

import akka.actor.ScalaActorRef

/** Only startPos is returned, so S/N ratio can be computed if desired
 * @author Mike Slinn */

case class DocumentMatch(val monkeyRef:ScalaActorRef, val startPos:Int = 0)