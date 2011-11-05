package net.interdoodle.hanuman.domain

import akka.actor.ScalaActorRef


/**
 * @author Mike Slinn */

class SimpleCritic extends Critic {
  private var lastIndex = 0;

  
  override def assessText(monkeyRef:ScalaActorRef, totalText:String, page:String) {
    // TODO actually compare text
    //totalText.indexOf(passage)
    super.assessText(monkeyRef, totalText, page)
  }
}

object SimpleCritic {
  def apply() = new SimpleCritic()
}
