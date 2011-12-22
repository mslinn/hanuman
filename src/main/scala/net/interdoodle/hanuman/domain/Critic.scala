package net.interdoodle.hanuman.domain

import net.interdoodle.hanuman.Configuration
import net.interdoodle.hanuman.message.{NoMatch, TextMatch}
import akka.actor.ActorRef


/** Critics and Monkeys were intended to evolve as a result of competition. The base functionality for Critic subclasses
 * is defined here; subclasses might become fairly sophisticated.
 * @author Mike Slinn */
abstract class Critic {
  var document = ""
  private var lastTextMatch = new TextMatch(null, null, 0, 0, 0)
  protected val minimumMatchLength = Configuration().minimumMatchLength
  var self:Option[ActorRef] = None

  /** Length of previous match if it extended to the end of the page. Set by subclass */
  protected var carriedMatchLength = 0

  /** Number of characters generated in all previous pages */
  protected var prevPageLengths = 0

  /** Set by subclass */
  protected[domain] var textMatch = new TextMatch(null, null, 0, 0, 0)


  /** Update textMatch; subclass must call super.assessText() as last line of this overridden method */
  def assessText(document:String, simulationId:String, monkeyRef:ActorRef, page:String) {
    takeAction()
  }

  /** Subclass must calculate match and figure out what to send */
  private def takeAction() {
    if (textMatch.length>lastTextMatch.length && textMatch.length>=minimumMatchLength) {
      self match {
        case Some(actorRef) =>
          if (actorRef.supervisor!=null) {
            actorRef.getSupervisor() ! textMatch
            lastTextMatch = textMatch
          }
        case None =>
      }
    } else self match {
        case Some(actorRef) =>
          if (actorRef.supervisor!=null) 
            actorRef.supervisor ! NoMatch(self)
    }
  }
}
