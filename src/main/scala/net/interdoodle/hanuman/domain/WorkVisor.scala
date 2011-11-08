package net.interdoodle.hanuman.domain

import akka.actor.Actor
import akka.config.Supervision.{OneForOneStrategy, Permanent}
import akka.event.EventHandler
import net.interdoodle.hanuman.domain.Hanuman.TextMatchMapRef
import net.interdoodle.hanuman.message._
import scala.collection.JavaConversions._


/** WorkCell supervisor manages simulation.
 * Creates 'workCellsPerVisor' Akka Actor references (to type WorkCell) with identical probability distributions.
 * Dispatches requests to generate semi-random text.
 * @author Mike Slinn */
class WorkVisor(val simulationID:String,
                  val maxTicks:Int,
                  val document:String,
                  val workCellsPerVisor:Int,
                  val textMatchMapRef:TextMatchMapRef) extends Actor {
  val documentLength = document.length()

  val letterProbability = new LetterProbabilities()
  letterProbability.add(document)
  letterProbability.computeValues()

  self.lifeCycle = Permanent
  self.faultHandler = OneForOneStrategy(List(classOf[Throwable]), 5, 5000)

  /** WorkVisors keep simulationStatus up to date so supervisor does not have to worry about maintaining it */
  var running = false
  var tickNumber = 1


  def generatePages() {
    for (workUnitActorRef <- self.linkedActors.values())
      workUnitActorRef ! TypingRequest(workUnitActorRef)
  }

  /** If any monkey finishes, we are done */
  override def postStop() {
    EventHandler.debug(this, "Simulation stopped")
    // TODO how to delete WorkCell?
  }

  override def preStart() {
    running = true
    tickNumber = 1
    for (i <- 1 to workCellsPerVisor) {
      val workCellRef = Actor.actorOf(new WorkCell[SimpleCritic](document, letterProbability)(() => new SimpleCritic))
      self.link(workCellRef)
      workCellRef.start()
    }

    while (running && tickNumber<=maxTicks) {
      EventHandler.debug(this, "Simulation tick #" + tickNumber.toString())
      generatePages() // until this Actor is stopped
      tickNumber += 1
    }

    for (workCell <- self.linkedActors.values()) // simulation is now over
      workCell ! "stop"
  }

  def receive = {
    case "generatePages" =>
      EventHandler.debug(this, "WorkVisor received 'generatePages' message")
      generatePages()

    case "stop" =>
      EventHandler.debug(this, "WorkVisor received 'stop' message")
      running = false
      for (workCellActorRef <- self.linkedActors.values())
        workCellActorRef ! "stop"

    case "stopped" =>
      EventHandler.debug(this, "WorkVisor received 'stopped' message")
      self.unlink(self.sender.get)
      if (self.linkedActors.size()==0) {
        EventHandler.debug(this, "All WorkCells have stopped")
        self.supervisor ! "stopped"
      }

    case TextMatch(workCellRef, matchLength, matchStart, matchEnd) =>
      EventHandler.debug(this, workCellRef.uuid + " matched " + matchLength + " characters from " + matchStart + " to " + matchEnd)
      if (matchLength==documentLength) // success!
        running = false
      val textMatchMap = textMatchMapRef.get
      textMatchMap += workCellRef.uuid -> TextMatch(workCellRef, matchLength, matchStart, matchEnd)
      textMatchMapRef.set(textMatchMap)
      self.supervisor ! DocumentMatch(workCellRef, matchStart)

    case _ =>
      EventHandler.info(this, "WorkVisor received an unknown message")
  }
}
