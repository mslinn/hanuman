package net.interdoodle.hanuman.domain

import akka.actor.ScalaActorRef
import net.interdoodle.hanuman.message.TextMatch


/**
 * @author Mike Slinn */

class SimpleCritic extends Critic {
  private val documentLength = document.length


  private strPos = 0
  override def assessText(document:String, monkeyRef:ScalaActorRef, textSoFar:String, page:String) {
    val docMatch = findLongestSubstring(document, textSoFar)
    val longestMatch = docMatch.map.groupBy(_._1).toArray.sortBy(_._1) tail
    textMatch = new TextMatch(monkeyRef, longestMatch._1, longestMatch._2, longestMatch._3)
    super.assessText(monkeyRef, textSoFar, page)
  }

  protected def findLongestSubstring(doc:String, str:String, strStart:Int=0, offset:Int=0, length:Int=0):List[Int, Int, Int] = {
    if (documentLength==0 || documentLength==strStart || str.length==0) {
      (offset, strStart, length)
    } else if (document[i]==str[j]) {
      findLongestSubstring(doc.substring(1), str.substring(1), strStart, offset, length++)
    } else {
      findLongestSubstring(doc, str.substring(1), strStart+1, 0, 0)
    }
  }
}

object SimpleCritic {
  def apply() = new SimpleCritic()
}
