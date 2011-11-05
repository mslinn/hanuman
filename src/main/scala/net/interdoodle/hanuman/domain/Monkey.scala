package net.interdoodle.hanuman.domain

/** Random or semi-random typist
 * @author Mike Slinn */
class Monkey(val letterProbability:LetterProbabilities) {
  var generatedText = ""


  /** @return a semi-random character */
  def generateChar = letterProbability.letter(math.random)

  /** @return 1000 semi-random characters */
  def generatePage = {
    val sb = new StringBuilder();
    { for (i <- 1 to 1000)
        yield(generateChar.toString)
    }.addString(sb)
    val page = sb.toString()
    generatedText += page
    page
  }
}