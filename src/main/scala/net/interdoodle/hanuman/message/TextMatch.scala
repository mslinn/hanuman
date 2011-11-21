package net.interdoodle.hanuman.message

import akka.actor.ActorRef
import blueeyes.json.JsonAST.{JField, JInt, JObject, JString}


/**
 * @author Mike Slinn */
case class TextMatch(val workCellRef:ActorRef, val length:Int = 0, val startPos:Int = 0, val endPos:Int = 0) {
  def decompose = JObject(
    JField("monkeyRef", JString(if (workCellRef!=null) workCellRef.uuid.toString else "Null workCellRef")) ::
    JField("length",    JInt(length)) ::
    JField("startPos",  JInt(startPos)) ::
    JField("endPos",    JInt(endPos)) :: Nil
  )

  override def toString() = workCellRef.toString() + "length=" + length + " from " + startPos + " to " + endPos
}