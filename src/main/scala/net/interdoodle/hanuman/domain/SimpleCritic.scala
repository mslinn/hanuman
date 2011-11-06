package net.interdoodle.hanuman.domain

import akka.actor.ScalaActorRef
import net.interdoodle.hanuman.message.TextMatch


/**
 * @author Mike Slinn */

class SimpleCritic extends Critic {
  private var strPos = 0


  override def assessText(document:String, monkeyRef:ScalaActorRef, textSoFar:String, page:String) {
    val generatedText = textSoFar + page // TODO optimize by getting fancy with textSoFar and page
    val docMatches = for (i <- 0 to generatedText.length;
      val len = matchLen(document, generatedText.substring(i)) if len>0
    ) yield TextMatch(monkeyRef, len, i, len + i)
    // REPL accepts: docMatches.toList.map {s => (s.length, s)} sortBy(_._1) head
    // ...but Scala compiler does not
    println("docMatches", docMatches)
    val longestMatch = docMatches.map{s => (s.length, s)}
    println("longestMatch", longestMatch)
    if (longestMatch.size>0) {
      textMatch = longestMatch.sortBy(_._1).last._2 // side effect, bad dog!
      println("textMatch", textMatch)
      super.assessText(document, monkeyRef, textSoFar, page)
    }
  }

  /** @return number of common chars at start of document and str */
  protected[domain] def matchLen(document:String, str:String):Int = {
    val upperLimit = Math.min(document.length, str.length)
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
