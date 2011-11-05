package net.interdoodle.hanuman.domain

import akka.config.Supervision.{OneForOneStrategy, Permanent}
import akka.event.EventHandler
import akka.stm.Ref
import akka.actor.{Actor, ActorRef}
import scala.collection.JavaConversions._
import net.interdoodle.hanuman.message._


/** Monkey supervisor manages simulation.
 * Creates 'monkeysPerVisor' Akka Actor references (to type Monkey) with identical probability distributions.
 * Dispatches requests to generate semi-random text.
 * @author Mike Slinn */
class MonkeyVisor(val simulationID:String,
                  val maxTicks:Int,
                  val document:String,
                  val monkeysPerVisor:Int,
                  val textMatchRefMap:Hanuman.TextMatchRefMap,
                  val simulationStatusRef:Ref[SimulationStatus]) extends Actor {
  var simulationStatus = simulationStatusRef.get
  val letterProbability = new LetterProbabilities()

  letterProbability.add(document)
  letterProbability.computeValues()
  val documentLength = document.length()

  self.lifeCycle = Permanent
  self.faultHandler = OneForOneStrategy(List(classOf[Throwable]), 5, 5000)

  var running = true // is this boolean required or is there a better way?
  var tickNumber = 1


  def generatePages() {
    for (monkeyActorRef <- self.linkedActors.values())
      monkeyActorRef ! TypingRequest(monkeyActorRef)
  }

  /** If any monkey finishes, we are done */
  override def postStop() {
    EventHandler.info(this, "Simulation stopped")
    // TODO how to delete Monkeys?
  }

  override def preStart() {
    for (i <- 1 to monkeysPerVisor) {
      val monkeyRef = Actor.actorOf(new WorkCell[SimpleCritic](document, letterProbability)(() => new SimpleCritic))
      self.link(monkeyRef)
      monkeyRef.start()
    }

    while (running && tickNumber<=maxTicks) {
      EventHandler.info(this, "Simulation tick #" + tickNumber.toString())
      generatePages() // until this Actor is stopped
      tickNumber += 1
    }

    for (val monkey <- self.linkedActors.values()) // simulation is now over
      monkey ! "stop"
  }

  def receive = {
    case "generatePages" =>
      EventHandler.info(this, "MonkeyVisor received 'generatePages' message")
      generatePages()

    case "stop" =>
      EventHandler.info(this, "MonkeyVisor received 'stop' message")
      running = false
      for (monkeyActorRef <- self.linkedActors.values())
        monkeyActorRef ! "stop"

    case "stopped" =>
      EventHandler.info(this, "MonkeyVisor received 'stopped' message")
      self.unlink(self.sender.get)
      if (self.linkedActors.size()==0) {
        EventHandler.info(this, "All Monkeys have stopped")
        self.supervisor ! "stopped"
      }

    case TextMatch(monkeyActorRef, matchLength, matchStart, matchEnd) =>
      EventHandler.info(this, monkeyActorRef.uuid + " matched " + document.substring(matchStart, matchEnd))
      if (matchLength==documentLength) { // success!
        running = false
        simulationStatus = new SimulationStatus(true, Some(monkeyActorRef), simulationStatus.simulations)
        // FIXME simulationStatus.simulations.put(monkeyActorRef, ??)
        self.supervisor ! DocumentMatch(monkeyActorRef, matchStart)
      } else {
        // FIXME simulationStatus.simulations.put(monkeyActorRef, ??)
      }
      simulationStatusRef.set(simulationStatus)

    case _ =>
      EventHandler.info(this, "MonkeyVisor received an unknown message")
  }
}
