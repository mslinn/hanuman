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
    sCritic.assessText(document, null, "ab")
    assert(sCritic.textMatch===TextMatch(null, 2, 0, 2))

    // Generates: JObject(List(JField(monkeyRef,JString(Null workCellRef)), JField(length,JInt(2)), JField(startPos,JInt(0)), JField(endPos,JInt(2))))
    // Instead I want actual JSON, something like:
    // {"result":[{"monkeyRef":"b4a73ed1-0bd3-11e1-ab88-485b3988902c","length":4,"startPos":587,"endPos":591},{"monkeyRef":"b4a73ed4-0bd3-11e1-ab88-485b3988902c","length":5,"startPos":88053,"endPos":58} }
    // What is the simplest way to emit the JSON so it can be compared to expected results?
    println(sCritic.textMatch.decompose)

    sCritic.assessText(document, null, "_abcd_")
    assert(sCritic.textMatch===TextMatch(null, 4, 1, 5))

    sCritic.assessText(document, null, "ab_ab__abcd_abcdefgh")
    assert(sCritic.textMatch===TextMatch(null, 8, 12, 20))
  }
}