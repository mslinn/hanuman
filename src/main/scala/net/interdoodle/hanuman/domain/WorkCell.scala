package net.interdoodle.hanuman.domain

import akka.actor.Actor
import akka.config.Supervision.Permanent
import akka.event.EventHandler
import net.interdoodle.hanuman.message.TypingRequest


/** Encapsulates a Monkey instance and a Critic instance. Both classes have no knowledge of the other. Both classes
 * can be extended to be more sophisticated. Critics carry state for Monkeys.
 * @author Mike Slinn */
class WorkCell[C <: Critic](val document:String, val letterProbability:LetterProbabilities)
                           (val criticFactory:() => C) extends Actor {
  self.lifeCycle = Permanent

  private val critic = criticFactory()
  critic.self = self
  critic.document = document

  private val monkey = new Monkey(letterProbability)



  // TODO register with WorkVisor after restart

  override def preStart() {
    monkey.generatedText = ""
  }

  def receive = {
    case "stop" =>
      if (self.linkedActors.size()==0)
        self.supervisor ! "stopped"

    case TypingRequest(workCellRef) =>
      try {
        EventHandler.debug(this, workCellRef.uuid + " received TypingRequest")
        val page = monkey.generatePage
        critic.assessText(document, workCellRef, monkey.generatedText, page) // notifies WorkVisor of passage match if necessary
      } catch {
        case e:Exception => EventHandler.debug(this, e.toString)
      }

    case _ =>
      EventHandler.info(this, "WorkCell received an unknown message: " + self)
  }
}