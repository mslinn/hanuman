package net.interdoodle.hanuman.domain

import akka.config.Supervision.{OneForOneStrategy, Permanent}
import akka.event.EventHandler
import akka.stm.Ref
import akka.actor.{Actor, ActorRef}
import scala.collection.JavaConversions._
import net.interdoodle.hanuman.message._

/** Monkey supervisor creates 'monkeysPerVisor' Akka Actor references (to type Monkey) with identical probability distributions.
 * Dispatches requests to generate semi-random text.
 * @author Mike Slinn */
class MonkeyVisor(val simulationID:String,
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


  def generatePages() {
    for (monkeyActorRef <- self.linkedActors.values())
      monkeyActorRef ! TypingRequest(monkeyActorRef)
  }

  /** If any monkey finishes, we are done */
  override def postStop() {
    /*for (val monkeyRef <- self.linkedActors.values()) {
      monkeyRef.stop()
      self.unlink(monkeyRef)
      // TODO how to delete Monkeys?
    }*/
  }

  override def preStart() {
    for (i <- 1 to monkeysPerVisor) {
      val monkeyRef = Actor.actorOf(new Monkey[SimpleCritic](letterProbability)(() => new SimpleCritic))
      self.link(monkeyRef)
      monkeyRef.start()
    }
  }

  def receive = {
    case "generatePages" =>
      EventHandler.info(this, "MonkeyVisor received 'generatePages' request")
      generatePages()

    case "stop" =>
      for (monkeyActorRef <- self.linkedActors.values())
        monkeyActorRef ! "stop"

    case "stopped" =>
      self.unlink(self.sender.get)
      if (self.linkedActors.size()==0)
        self.supervisor ! "stopped"

    case TextMatch(monkeyActorRef, matchLength, matchStart, matchEnd) =>
      EventHandler.info(this, monkeyActorRef.uuid + " matched " + document.substring(matchStart, matchEnd))
      if (matchLength==documentLength) { // success!
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

  
  /** Remove item from a list */
  private def remove[A](c:A, l:List[A]) = l indexOf c match {
    case -1 => l
    case n => (l take n) ++ (l drop (n + 1))
  }
}
