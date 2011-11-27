package net.interdoodle.hanuman.domain

import akka.actor.Actor
import akka.config.Supervision.Permanent
import akka.event.EventHandler
import net.interdoodle.hanuman.message.TypingRequest


/** Encapsulates a Monkey instance and a Critic instance. Both classes have no knowledge of the other. Both classes
 * can be extended to be more sophisticated. Critics carry state for Monkeys. Critic subclasses are interchangeable.
 * @author Mike Slinn */
class WorkCell[C <: Critic](val document:String, val letterProbability:LetterProbabilities)
                           (val criticFactory:() => C) extends Actor {
  self.lifeCycle = Permanent

  private val critic = criticFactory()
  critic.self = self
  critic.document = document

  private val monkey = new Monkey(letterProbability)



  // TODO register with SimulationSupervisor after restart

  def receive = {
    case TypingRequest(simulationId, workCellRef) =>
      try {
        EventHandler.debug(this, "WorkCell received a TypingRequest")
        val page = monkey.generatePage
        critic.assessText(document, simulationId, workCellRef, page) // notifies SimulationSupervisor of passage match if necessary
      } catch {
        case e:Exception => EventHandler.debug(this, e.toString)
      }
  }
}