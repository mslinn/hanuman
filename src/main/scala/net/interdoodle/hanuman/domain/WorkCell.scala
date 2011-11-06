package net.interdoodle.hanuman.domain

import akka.actor.Actor
import akka.config.Supervision.Permanent
import akka.event.EventHandler
import net.interdoodle.hanuman.message.TypingRequest


/** Encapsulates a Monkey instance and a Critic instance. Both classes have no knowledge of the other. Both classes
 * can be extended to be more sophisticated. Critics carry state for Monkeys.
 * @author Mike Slinn */

class WorkCell[C <: Critic](val document:String, val letterProbability:LetterProbabilities)(val criticFactory:() => C) extends Actor {
  self.lifeCycle = Permanent

  val critic = criticFactory()
  critic.self = self
  critic.document = document

  val monkey = new Monkey(letterProbability)



  // TODO register with MonkeyVisor after restart

  override def preStart() {
    monkey.generatedText = ""
  }

  def receive = {
    case "stop" =>
      self.supervisor ! "stopped"

    case TypingRequest(workCellRef) =>
      EventHandler.info(this, workCellRef.uuid + " received TypingRequest")
      var page = monkey.generatePage
      critic.assessText(document, workCellRef, monkey.generatedText, page) // notifies MonkeyVisor of passage match if necessary

    case _ =>
      EventHandler.info(this, "Monkey received an unknown message: " + self)
  }
}