package net.interdoodle.hanuman.domain

import akka.actor.Actor
import net.interdoodle.hanuman.message.TypingRequest
import akka.event.Logging


/** Encapsulates a Monkey instance and a Critic instance. Both classes have no knowledge of the other. Both classes
 * can be extended to be more sophisticated. Critics carry state for Monkeys. Critic subclasses are interchangeable.
 * @author Mike Slinn */
class WorkCell[C <: Critic](val document:String, val letterProbability:LetterProbabilities)
                           (val criticFactory:() => C) extends Actor {
  private val log = Logging(context.system, this)
  self.lifeCycle = Permanent

  private val critic = criticFactory()
  critic.self = self
  critic.document = document

  private val monkey = new Monkey(letterProbability)



  // TODO register with SimulationSupervisor after restart

  def receive = {
    case TypingRequest(simulationId, workCellRef) =>
      try {
        log.debug("WorkCell received a TypingRequest")
        val page = monkey.generatePage
        critic.assessText(document, simulationId, workCellRef, page) // notifies SimulationSupervisor of passage match if necessary
      } catch {
        case e:Exception => log.debug(e.toString)
      }
  }
}