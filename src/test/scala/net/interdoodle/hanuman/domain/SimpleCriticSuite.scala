package net.interdoodle.hanuman.domain

import org.scalatest.Assertions._
import org.scalatest.FunSuite
import net.interdoodle.hanuman.message.TextMatch


/**
 * @author Mike Slinn */
class SimpleCriticSuite extends FunSuite {
  val document = "abcdefghijklmnopqrstuvwxyz"
  val sCritic = new SimpleCritic

  ignore("matchLen") {
    expect(0)               { sCritic.matchLen(document,     "") }
    expect(1)               { sCritic.matchLen(document,     "a") }
    expect(2)               { sCritic.matchLen(document,     "ab") }
    expect(0)               { sCritic.matchLen(document,     "x") }
    expect(2)               { sCritic.matchLen(document,     "abx") }
    expect(document.length) { sCritic.matchLen(document,     document) }
    expect(document.length) { sCritic.matchLen(document+"x", document) }
    expect(document.length) { sCritic.matchLen(document,     document+"y") }
    expect(document.length) { sCritic.matchLen(document+"x", document+"y") }
  }

  test("assessText") {
    sCritic.assessText(document, null, "", "ab")
    assert(sCritic.textMatch===TextMatch(null, 2, 0, 2))

    sCritic.assessText(document, null, "", "_abcd_")
    assert(sCritic.textMatch===TextMatch(null, 4, 1, 5))

    sCritic.assessText(document, null, "", "ab_ab__abcd_abcdefgh")
    assert(sCritic.textMatch===TextMatch(null, 8, 12, 20))
  }
}