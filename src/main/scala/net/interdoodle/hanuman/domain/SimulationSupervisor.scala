package net.interdoodle.hanuman.domain

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.config.Supervision.{OneForOneStrategy, Permanent}
import akka.event.Logging
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
  private val system = ActorSystem("SimulationSupervisor")
  private val log = Logging(context.system, this)
  /** Keep simulationStatus up to date so Hanuman does not have to worry about maintaining it */
  var simulationStatus = new SimulationStatus(simulationId, maxTicks, workCellsPerVisor)
  private val documentLength = document.length()

  private val letterProbability = new LetterProbabilities()
  letterProbability.add(document)
  letterProbability.computeValues()

  self.lifeCycle = Permanent
  self.faultHandler = OneForOneStrategy(List(classOf[Throwable]), 5, 5000)

  private var running = false

  /** Keep track of busy WorkCells for each tick */
  private var workingCells = HashSet[ActorRef]().empty


  /** If any monkey finishes, we are done */
  override def postStop() {
    log.debug("Simulation stopped")
    workingCells.empty // should already be empty; no refs to WorkCells means they will be GC'd
  }

  override def preStart() {
    running = true
    simulationStatus.bestTextMatch = new TextMatch(null, null, 0, 0, 0)
    simulationStatus.tick = 1
    for (i <- 1 to workCellsPerVisor) {
      val workCellRef = system.actorOf(Props().withCreator(new WorkCell[SimpleCritic](document, letterProbability)(() => new SimpleCritic)))
      self.link(workCellRef)
      workCellRef.start()
    }
    checkTick
  }

  private def checkTick {
    if (running && simulationStatus.tick<=maxTicks) {
      log.debug("Simulation tick #" + simulationStatus.tick.toString())
      tick // until this WorkVisor instance is stopped
      simulationStatus.tick += 1
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
    log.debug("All WorkCells have stopped for simulation " + simulationId)
    self.supervisor ! SimulationStopped(simulationId)
  }

  /** Cause each Monkey to generate a page of semi-random text */
  private def tick {
    for (workCellRef <- self.linkedActors.values()) {
      workingCells += workCellRef
      EventHandler.debug(this, "tick " + simulationStatus.tick + "; " + workingCells.size + "; " + "workingCells")
      workCellRef ! TypingRequest(simulationId, workCellRef)
    }
  }

  def receive = {
    case Stop =>
      log.debug("SimulationSupervisor received Stop message")
      running = false
      stopWorkCells

    case NoMatch(workCellRef) =>
      workingCells -= workCellRef
      self.supervisor ! simulationStatus.copy()
      log.debug("tick " + simulationStatus.tick + "; " + workingCells.size + "; " + "workingCells (no match)")
      if (workingCells.size==0)
        checkTick

    case TextMatch(simulationId, workCellRef, matchLength, matchStart, matchEnd) =>
      log.debug("Simulation " + simulationId + " matched " + matchLength + " characters from " + matchStart + " to " + matchEnd)
      if (matchLength>simulationStatus.bestTextMatch.length) {
        simulationStatus.bestTextMatch = TextMatch(simulationId, workCellRef, matchLength, matchStart, matchEnd)
      }
      self.supervisor ! simulationStatus.copy()
      if (matchLength==documentLength) { // success! This has a very low probability of happening
        simulationStatus.complete = true
        workingCells.clear()
        running = false
        self.supervisor ! DocumentMatch(simulationId, matchStart)
      } else {
        workingCells -= workCellRef
        log.debug("tick " + simulationStatus.tick + "; " + workingCells.size + "; " + "workingCells (match)")
        if (workingCells.size==0)
          checkTick
      }
  }
}
