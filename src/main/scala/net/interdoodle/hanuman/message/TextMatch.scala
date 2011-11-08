package net.interdoodle.hanuman.message

import akka.actor.ActorRef


/**
 * @author Mike Slinn */
case class TextMatch(val workCellRef:ActorRef, val length:Int = 0, val startPos:Int = 0, val endPos:Int = 0) {
  // TODO put this out in JSON format
  override def toString() = workCellRef.toString() + "length=" + length + " from " + startPos + " to " + endPos
}