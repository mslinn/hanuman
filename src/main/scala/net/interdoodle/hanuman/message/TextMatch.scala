package net.interdoodle.hanuman.message

import blueeyes.json.JsonAST.{JField, JInt, JObject, JString}
import akka.actor.ActorRef


/** Message sent when a monkey's work matches a portion of a document
 * @author Mike Slinn */
case class TextMatch(val simulationID:String, val actorRef:ActorRef, val length:Int = 0, val startPos:Int = 0, val endPos:Int = 0) {
  def decompose = JObject(
    JField("workCellRef", JString(simulationID)) ::
    JField("length",    JInt(length)) ::
    JField("startPos",  JInt(startPos)) ::
    JField("endPos",    JInt(endPos)) :: Nil
  )

  override def toString() = simulationID.toString() + "length=" + length + " from " + startPos + " to " + endPos
}