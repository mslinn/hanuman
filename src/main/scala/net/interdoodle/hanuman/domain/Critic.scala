package net.interdoodle.hanuman.domain

import akka.actor.ScalaActorRef
import net.interdoodle.hanuman.message.TextMatch


/**
 * @author Mike Slinn */

abstract class Critic {
  var self:ScalaActorRef = null
  protected var textMatch = new TextMatch(null, 0, 0, 0)
  private var lastTextMatch = new TextMatch(null, 0, 0, 0)


  /** Update textMatch; subclass must call super.assessText() as last line of this overridden method */
  def assessText(monkeyRef:ScalaActorRef, totalText:String, page:String) {
    notifySupervisor()
  }

  /** Called from textMatch */
  def notifySupervisor() {
    // subclass must calculate match and figure out what to send
    if (textMatch!=lastTextMatch) {
       self.sender.foreach(_ ! textMatch)
       lastTextMatch = textMatch
    }
  }
}
