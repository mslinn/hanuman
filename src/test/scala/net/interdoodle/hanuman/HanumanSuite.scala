package net.interdoodle.hanuman

import domain.{SimpleCriticSuite, MonkeySuite, LetterProbabilitiesSuite}
import org.scalatest.SuperSuite


/** @see http://www.artima.com/sdp/original/org/scalatest/Suite.html
 * Other docs say SuperSuite is deprecated, and that Suites should be used instead, but Suites is not defined.
 * This will probably be addressed soon.
 * @author Mike Slinn */
class HanumanSuite extends SuperSuite(
  List(
    new LetterProbabilitiesSuite,
    new MonkeySuite,
    //new WorkVisorSuite,
    new SimpleCriticSuite
  )
)