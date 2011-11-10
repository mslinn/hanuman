package net.interdoodle.hanuman.domain

import akka.actor.ScalaActorRef
import net.interdoodle.hanuman.message.TextMatch
import net.lag.logging.Logger


/**
 * @author Mike Slinn */
class SimpleCritic extends Critic {
  private val log = Logger.get


  override def assessText(document:String, monkeyRef:ScalaActorRef, page:String) {
    val docMatches = for (i <- 0 to page.length;
      val len = matchLen(document, page.substring(i)) if len>0
    ) yield TextMatch(monkeyRef, carriedMatchLength+len, i+prevPageLengths, len + i)
    // REPL accepts: docMatches.toList.map {s => (s.length, s)} sortBy(_._1) head
    // ...but Scala compiler does not
    log.debug("docMatches", docMatches)
    val longestMatch = docMatches.map{ s => (s.length, s) }
    log.debug("longestMatch", longestMatch)
    prevPageLengths += page.length()
    if (longestMatch.size>0) {
      textMatch = longestMatch.sortBy(_._1).last._2 // side effect, bad dog!
      log.debug("textMatch", textMatch)
      carriedMatchLength = if (textMatch.endPos==page.length()-1) // match might continue into next page
        textMatch.length
      else
        0
      super.assessText(document, monkeyRef, page)
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
