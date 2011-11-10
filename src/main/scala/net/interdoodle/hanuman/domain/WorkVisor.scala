package net.interdoodle.hanuman.domain

import akka.config.Supervision.{OneForOneStrategy, Permanent}
import akka.event.EventHandler
import net.interdoodle.hanuman.domain.Hanuman.TextMatchMapRef
import net.interdoodle.hanuman.message._
import scala.collection.mutable.HashSet
import scala.collection.JavaConversions._
import akka.actor.{ActorRef, Actor}


/** WorkCell supervisor manages simulation. It sends a TypingRequest to each WorkCell every tick.
 * Creates 'workCellsPerVisor' Akka Actor references (to type WorkCell) with identical probability distributions.
 * Dispatches requests to generate semi-random text.
 * @author Mike Slinn */
class WorkVisor(val simulationID:String,
                  val maxTicks:Int,
                  val document:String,
                  val workCellsPerVisor:Int,
                  val textMatchMapRef:TextMatchMapRef) extends Actor {
  private val documentLength = document.length()

  private val letterProbability = new LetterProbabilities()
  letterProbability.add(document)
  letterProbability.computeValues()

  self.lifeCycle = Permanent
  self.faultHandler = OneForOneStrategy(List(classOf[Throwable]), 5, 5000)

  /** WorkVisors keep simulationStatus up to date so supervisor does not have to worry about maintaining it */
  private var running = false
  private var tickNumber = 1

  /** Keep track of busy WorkCells for each tick */
  private var workingCells = HashSet[ActorRef]().empty


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
    checkTick
  }

  private def checkTick {
    if (running && tickNumber<=maxTicks) {
      EventHandler.debug(this, "Simulation tick #" + tickNumber.toString())
      tick // until this WorkVisor instance is stopped
      tickNumber += 1
    } else {
      for (workCell <- self.linkedActors.values()) // end simulation
        workCell ! "stop"
    }
  }

  /** Cause each Monkey to generate a page of semi-random text */
  private def tick {
    for (workCellRef <- self.linkedActors.values()) {
      workingCells += workCellRef
      EventHandler.debug(this, "tick " + tickNumber + "; " + workingCells.size + "; " + "workingCells")
      workCellRef ! TypingRequest(workCellRef)
    }
  }

  def receive = {
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

    case NoMatch(workCellRef) =>
      workingCells -= workCellRef
      EventHandler.debug(this, "tick " + tickNumber, workingCells.size + "; " + "workingCells (no match)")
      if (workingCells.size==0)
        checkTick

    case TextMatch(workCellRef, matchLength, matchStart, matchEnd) =>
      EventHandler.debug(this, workCellRef.uuid + " matched " + matchLength + " characters from " + matchStart + " to " + matchEnd)
      val textMatchMap = textMatchMapRef.get
      textMatchMap += workCellRef.uuid -> TextMatch(workCellRef, matchLength, matchStart, matchEnd)
      textMatchMapRef.set(textMatchMap)
      if (matchLength==documentLength) { // success! This has a very low probability of happening
        running = false
        self.supervisor ! DocumentMatch(workCellRef, matchStart)
      } else {
        workingCells -= workCellRef
        EventHandler.debug(this, "tick " + tickNumber + "; " + workingCells.size + "; " + "workingCells (match)")
        if (workingCells.size==0)
          checkTick
      }

    case _ =>
      EventHandler.debug(this, "WorkVisor received an unknown message")
  }
}
