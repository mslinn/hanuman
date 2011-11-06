package net.interdoodle.hanuman.message

import akka.actor.ScalaActorRef


/**
 * @author Mike Slinn */
case class TextMatch(val monkeyRef:ScalaActorRef, val length:Int = 0, val startPos:Int = 0, val endPos:Int = 0)