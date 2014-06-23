package org.allenai.nlpstack.postag

import cc.factorie.app.nlp.pos.OntonotesForwardPosTagger
import cc.factorie.app.nlp._
import org.allenai.nlpstack.tokenize.Token
import org.allenai.nlpstack.tokenize.defaultTokenizer

class FactoriePostagger extends Postagger {
  private val tagger = OntonotesForwardPosTagger

  override def postagTokenized(tokens: Seq[Token]): Seq[PostaggedToken] = {
    // translate the tokens into a Factorie document
    val str = new StringBuilder
    for (token <- tokens) {
      if (str.length < token.offset)
        str.append(" " * (token.offset - str.length))
      str.replace(token.offset, token.offset + token.string.length, token.string)
    }
    val factorieDoc = new Document(str.mkString)
    val section = new BasicSection(factorieDoc, 0, str.length)
    val factorieTokens = tokens.map(
      t => new cc.factorie.app.nlp.Token(t.offset, t.offset + t.string.length))
    section ++= factorieTokens

    tagger.predict(factorieTokens)  // modifies factoryTokens

    for (token <- factorieTokens)
      yield PostaggedToken(
      tagger.tokenAnnotationString(token),
      token.string,
      token.stringStart)
  }
}

object FactoriePostaggerMain extends PostaggerMain {
  override val tokenizer = defaultTokenizer
  override val postagger = new FactoriePostagger()
}
