package net.interdoodle.hanuman.domain

import akka.actor.ScalaActorRef
import net.interdoodle.hanuman.message.TextMatch


/**
 * @author Mike Slinn */

abstract class Critic {
  var self:ScalaActorRef = null
  var document = ""
  protected var textMatch = new TextMatch(null, 0, 0, 0)
  private var lastTextMatch = new TextMatch(null, 0, 0, 0)

  /** TODO make this a configurable parameter */
  private val minimumMatchLength = 2


  /** Update textMatch; subclass must call super.assessText() as last line of this overridden method */
  def assessText(monkeyRef:ScalaActorRef, totalText:String, page:String) {
    notifySupervisor()
  }

  /** Subclass must calculate match and figure out what to send */
  def notifySupervisor() {
    if (textMatch._1>lastTextMatch._1 && textMatch._1>=minimumMatchLength) {
       self.supervisor ! textMatch
       lastTextMatch = textMatch
    }
  }
}
