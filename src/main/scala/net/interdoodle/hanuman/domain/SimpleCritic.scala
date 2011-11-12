package net.interdoodle.hanuman.domain

import akka.actor.ScalaActorRef
import net.interdoodle.hanuman.message.TextMatch
import net.lag.logging.Logger


/**
 * @author Mike Slinn */
class SimpleCritic extends Critic {
  private val log = Logger.get
  private var strPos = 0


  /* TODO Optimize by not rescanning textSoFar and scanning page, possibly continuing previous scan if textSoFar ended
     in a match */
  override def assessText(document:String, monkeyRef:ScalaActorRef, textSoFar:String, page:String) {
    val generatedText = textSoFar + page
    val docMatches = for (i <- 0 to generatedText.length;
      val len = matchLen(document, generatedText.substring(i)) if len>0
    ) yield TextMatch(monkeyRef, len, i, len + i)
    // REPL accepts: docMatches.toList.map {s => (s.length, s)} sortBy(_._1) head
    // ...but Scala compiler does not
    log.debug("docMatches", docMatches)
    val longestMatch = docMatches.map{s => (s.length, s)}
    log.debug("longestMatch", longestMatch)
    if (longestMatch.size>0) {
      textMatch = longestMatch.sortBy(_._1).last._2 // side effect, bad dog!
      log.debug("textMatch", textMatch)
      super.assessText(document, monkeyRef, textSoFar, page)
    }
  }

  /** @return number of common chars at start of document and str */
  protected[domain] def matchLen(document:String, str:String):Int = {
    val upperLimit = scala.math.min(document.length, str.length)
    for (i <- 0 until upperLimit) {
      if (document.charAt(i)!=str.charAt(i))
          return i
    }
    return upperLimit
  }
}

object SimpleCritic {
  def apply() = new SimpleCritic()
}
