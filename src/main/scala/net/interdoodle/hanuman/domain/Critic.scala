package net.interdoodle.hanuman.domain

import akka.actor.ScalaActorRef
import net.interdoodle.hanuman.Configuration
import net.interdoodle.hanuman.message.TextMatch


/**
 * @author Mike Slinn */
abstract class Critic {
  var document = ""
  private var lastTextMatch = new TextMatch(null, 0, 0, 0)
  protected val minimumMatchLength = Configuration().workCellsPerVisor
  var self:ScalaActorRef = null

  /** Set by subclass */
  protected[domain] var textMatch = new TextMatch(null, 0, 0, 0)


  /** Update textMatch; subclass must call super.assessText() as last line of this overridden method */
  def assessText(document:String, monkeyRef:ScalaActorRef, textSoFar:String, page:String) {
    takeAction()
  }

  /** Subclass must calculate match and figure out what to send */
  def takeAction() {
    if (textMatch.length>lastTextMatch.length && textMatch.length>=minimumMatchLength) {
      if (self!=null && self.supervisor!=null)
        self.supervisor ! textMatch
      lastTextMatch = textMatch
    }
  }
}
