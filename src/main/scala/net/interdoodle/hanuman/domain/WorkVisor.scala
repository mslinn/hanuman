package net.interdoodle.hanuman.domain

import akka.actor.Actor
import akka.config.Supervision.{OneForOneStrategy, Permanent}
import akka.event.EventHandler
import akka.stm.Ref
import net.interdoodle.hanuman.domain.Hanuman.TextMatchMapRef
import net.interdoodle.hanuman.message._
import scala.collection.JavaConversions._


/** Monkey supervisor manages simulation.
 * Creates 'monkeysPerVisor' Akka Actor references (to type Monkey) with identical probability distributions.
 * Dispatches requests to generate semi-random text.
 * @author Mike Slinn */
class WorkVisor(val simulationID:String,
                  val maxTicks:Int,
                  val document:String,
                  val monkeysPerVisor:Int,
                  val textMatchMapRef:TextMatchMapRef) extends Actor {
  val documentLength = document.length()

  val letterProbability = new LetterProbabilities()
  letterProbability.add(document)
  letterProbability.computeValues()

  self.lifeCycle = Permanent
  self.faultHandler = OneForOneStrategy(List(classOf[Throwable]), 5, 5000)

  /** MonkeyVisors keep simulationStatus up to date so supervisor does not have to worry about maintaining it */
  var running = false
  var tickNumber = 1


  def generatePages() {
    for (workUnitActorRef <- self.linkedActors.values())
      workUnitActorRef ! TypingRequest(workUnitActorRef)
  }

  /** If any monkey finishes, we are done */
  override def postStop() {
    EventHandler.debug(this, "Simulation stopped")
    // TODO how to delete WorkUnit?
  }

  override def preStart() {
    running = true
    tickNumber = 1
    for (i <- 1 to monkeysPerVisor) {
      val workCellRef = Actor.actorOf(new WorkCell[SimpleCritic](document, letterProbability)(() => new SimpleCritic))
      self.link(workCellRef)
      workCellRef.start()
    }

    while (running && tickNumber<=maxTicks) {
      EventHandler.debug(this, "Simulation tick #" + tickNumber.toString())
      generatePages() // until this Actor is stopped
      tickNumber += 1
    }

    for (monkey <- self.linkedActors.values()) // simulation is now over
      monkey ! "stop"
  }

  def receive = {
    case "generatePages" =>
      EventHandler.debug(this, "MonkeyVisor received 'generatePages' message")
      generatePages()

    case "stop" =>
      EventHandler.debug(this, "MonkeyVisor received 'stop' message")
      running = false
      for (workUnitActorRef <- self.linkedActors.values())
        workUnitActorRef ! "stop"

    case "stopped" =>
      EventHandler.debug(this, "MonkeyVisor received 'stopped' message")
      self.unlink(self.sender.get)
      if (self.linkedActors.size()==0) {
        EventHandler.debug(this, "All Monkeys have stopped")
        self.supervisor ! "stopped"
      }

    case TextMatch(workUnitRefRef, matchLength, matchStart, matchEnd) =>
      EventHandler.debug(this, workUnitRefRef.uuid + " matched " + matchLength + " characters from " + matchStart + " to " + matchEnd)
      if (matchLength==documentLength) // success!
        running = false
      val textMatchMap = textMatchMapRef.get
      textMatchMap += workUnitRefRef.uuid -> TextMatch(workUnitRefRef, matchLength, matchStart, matchEnd)
      textMatchMapRef.set(textMatchMap)
      self.supervisor ! DocumentMatch(workUnitRefRef, matchStart)

    case _ =>
      EventHandler.info(this, "MonkeyVisor received an unknown message")
  }
}
