package net.interdoodle.hanuman.domain

import akka.actor.{ActorRef, Actor}
import akka.config.Supervision.{OneForOneStrategy, Permanent}
import akka.event.EventHandler
import net.interdoodle.hanuman.message._
import scala.collection.mutable.HashSet
import scala.collection.JavaConversions._
import types._


/** WorkCell supervisor manages simulation. It sends a TypingRequest to each WorkCell every tick.
 * Creates 'workCellsPerVisor' Akka Actor references (to type WorkCell) with identical probability distributions.
 * Dispatches requests to generate semi-random text.
 * @author Mike Slinn */
class SimulationSupervisor(val simulationId:String,
                  val maxTicks:Int,
                  val document:String,
                  val workCellsPerVisor:Int) extends Actor {
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
  private var bestTextMatch = new TextMatch(null, null, 0, 0, 0)


  /** If any monkey finishes, we are done */
  override def postStop() {
    EventHandler.debug(this, "Simulation stopped")
    workingCells.empty // should already be empty; no refs to WorkCells means they will be GC'd
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
      stopWorkCells
    }
  }

  private def stopWorkCells {
    for (workCellActorRef <- self.linkedActors.values()) {
      workCellActorRef.stop()
      workingCells -= workCellActorRef
      self.unlink(workCellActorRef)
    }
    EventHandler.debug(this, "All WorkCells have stopped for simulation " + simulationId)
    self.supervisor ! SimulationStopped(simulationId)
  }

  /** Cause each Monkey to generate a page of semi-random text */
  private def tick {
    for (workCellRef <- self.linkedActors.values()) {
      workingCells += workCellRef
      EventHandler.debug(this, "tick " + tickNumber + "; " + workingCells.size + "; " + "workingCells")
      workCellRef ! TypingRequest(simulationId, workCellRef)
    }
  }

  def receive = {
    case Stop =>
      EventHandler.debug(this, "SimulationSupervisor received Stop message")
      running = false
      stopWorkCells

    case NoMatch(workCellRef) =>
      workingCells -= workCellRef
      EventHandler.debug(this, "tick " + tickNumber + "; " + workingCells.size + "; " + "workingCells (no match)")
      if (workingCells.size==0)
        checkTick

    case TextMatch(simulationId, workCellRef, matchLength, matchStart, matchEnd) =>
      EventHandler.debug(this, "Simulation " + simulationId + " matched " + matchLength + " characters from " + matchStart + " to " + matchEnd)
      if (matchLength>bestTextMatch.length) {
        bestTextMatch = TextMatch(simulationId, workCellRef, matchLength, matchStart, matchEnd)
        self.supervisor ! bestTextMatch
      }
      if (matchLength==documentLength) { // success! This has a very low probability of happening
        workingCells.clear()
        running = false
        self.supervisor ! DocumentMatch(simulationId, matchStart)
      } else {
        workingCells -= workCellRef
        EventHandler.debug(this, "tick " + tickNumber + "; " + workingCells.size + "; " + "workingCells (match)")
        if (workingCells.size==0)
          checkTick
      }
  }
}
