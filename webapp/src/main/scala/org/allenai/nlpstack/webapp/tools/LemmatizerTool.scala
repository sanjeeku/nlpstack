package org.allenai.nlpstack.webapp.tools

import org.allenai.nlpstack.core.{ Lemmatized, Token }

object LemmatizerTool extends Tool("lemmatize") {
  type Output = Seq[Lemmatized[Token]]

  override def info = ToolInfo(Impl.lemmatizer.getClass.getSimpleName, Impl.obamaSentences)

  override def split(input: String) = input split "\n"
  override def process(section: String) = {
    val tokens = Impl.tokenizer.tokenize(section)
    val postagged = Impl.postagger.postagTokenized(tokens)
    postagged map Impl.lemmatizer.lemmatizePostaggedToken
  }
  override def visualize(output: Output) = Seq.empty
  override def format(output: Output) = Seq(output mkString " ")
}