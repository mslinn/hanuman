package net.interdoodle.hanuman.domain

import org.scalatest.Assertions._
import org.scalatest.FunSuite
import net.interdoodle.hanuman.message.TextMatch
import blueeyes.json.JsonAST.JObject
import blueeyes.json.Printer

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
    sCritic.assessText(document, null, null, "ab")
    var actual = Printer.compact(Printer.render(sCritic.textMatch.decompose)).toString
    assert(actual==="""{"workCellRef":"Null workCellRef","length":2,"startPos":0,"endPos":2}""")

    sCritic.assessText(document, null, null, "_abcd_")
    actual = Printer.compact(Printer.render(sCritic.textMatch.decompose)).toString
    assert(actual==="""{"workCellRef":"Null workCellRef","length":4,"startPos":3,"endPos":5}""")

    sCritic.assessText(document, null, null, "ab_ab__abcd_abcdefgh")
    actual = Printer.compact(Printer.render(sCritic.textMatch.decompose)).toString
    assert(actual==="""{"workCellRef":"Null workCellRef","length":8,"startPos":12,"endPos":20}""")
  }
}