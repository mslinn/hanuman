package net.interdoodle.hanuman.domain

import akka.actor.Actor
import akka.config.Supervision.Permanent
import akka.event.EventHandler
import net.interdoodle.hanuman.message.TypingRequest


/** Random or semi-random typist
 * @author Mike Slinn */

class Monkey[C <: Critic](val letterProbability:LetterProbabilities)(val criticFactory:() => C) extends Actor {
  var generatedText = ""
  val critic = criticFactory()
  critic.self = self

  self.lifeCycle = Permanent


  // TODO register with MonkeyVisor after restart

  override def preStart() {
    generatedText = ""
  }

  /** @return a semi-random character */
  def generateChar = letterProbability.letter(math.random)

  /** @return 1000 semi-random characters */
  def generatePage = {
    val sb = new StringBuilder();
    { for (i <- 1 to 1000)
        yield(generateChar.toString)
    }.addString(sb)
    sb.toString()
  }

  def receive = {
    case "stop" =>
      self.supervisor ! "stopped"

    case TypingRequest(monkeyRef) =>
      EventHandler.info(this, monkeyRef.uuid + " received TypingRequest")
      var page = generatePage
      generatedText += page
      critic.assessText(monkeyRef, page, generatedText) // notifies MonkeyVisor of passage match if necessary

    case _ =>
      EventHandler.info(this, "Monkey received an unknown message: " + self)
  }
}