package net.interdoodle.hanuman.message

import akka.actor.ActorRef
import blueeyes.json.xschema.Decomposer
import blueeyes.json.JsonAST.{JField, JInt, JObject, JString}


/**
 * @author Mike Slinn */
case class TextMatch(val workCellRef:ActorRef, val length:Int = 0, val startPos:Int = 0, val endPos:Int = 0) {
  implicit val TextMatchDecomposer = new Decomposer[TextMatch] {
  def decompose(workCellRef:TextMatch) = JObject(
    JField("workCellRef", JString(workCellRef.workCellRef.uuid.toString)) ::
    JField("length",      JInt(workCellRef.length)) ::
    JField("startPos",    JInt(workCellRef.startPos)) ::
    JField("endPos",      JInt(workCellRef.endPos)) :: Nil
  )
  }

  override def toString() = workCellRef.toString() + "length=" + length + " from " + startPos + " to " + endPos
}