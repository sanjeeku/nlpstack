package org.allenai.nlpstack.parse.poly.polyparser

import org.allenai.nlpstack.parse.poly.core.Sentence
import org.allenai.nlpstack.parse.poly.fsm.{ TransitionConstraint }
import scopt.OptionParser

private case class ParseCacheCommandLine(
  fallbackParserFilename: String = "",
  goldParseFilenames: String = "", dataSource: String = "", outputFilename: String = ""
)

object ParseCache {

  def main(args: Array[String]) {
    val optionParser = new OptionParser[ParseCacheCommandLine]("ParseCache") {
      opt[String]('f', "fallbackParser") required () valueName ("<file>") action { (x, c) =>
        c.copy(fallbackParserFilename = x)
      } text ("the file containing the fallback" +
        " parser configuration")
      opt[String]('g', "goldfile") required () valueName ("<file>") action { (x, c) =>
        c.copy(goldParseFilenames = x)
      } text ("the file containing the gold parses")
      opt[String]('o', "outputfile") required () valueName ("<file>") action { (x, c) =>
        c.copy(outputFilename = x)
      } text ("where to write the CachingParser")
      opt[String]('d', "datasource") required () valueName ("<file>") action { (x, c) =>
        c.copy(dataSource = x)
      } text ("the location of the data " +
        "('datastore','local')") validate { x =>
          if (Set("datastore", "local").contains(x)) {
            success
          } else {
            failure(s"unsupported data source: ${x}")
          }
        }
    }
    val clArgs: ParseCacheCommandLine =
      optionParser.parse(args, ParseCacheCommandLine()).get
    val goldSource: PolytreeParseSource =
      MultiPolytreeParseSource(clArgs.goldParseFilenames.split(",") map { path =>
        InMemoryPolytreeParseSource.getParseSource(
          path,
          ConllX(true, makePoly = true), clArgs.dataSource
        )
      })
    val fallbackParser: TransitionParser = TransitionParser.load(clArgs.fallbackParserFilename)
    val cachingParser = ParseCache(goldSource.parseIterator.toSeq, fallbackParser)
    TransitionParser.save(cachingParser, clArgs.outputFilename)
  }
}

case class ParseCache(
    parsesToCache: Iterable[PolytreeParse],
    fallbackParser: TransitionParser
) extends TransitionParser {

  private def getSentenceKey(sentence: Sentence): String = {
    sentence.asWhitespaceSeparatedString
  }

  @transient private val cachedParseMap: Map[String, PolytreeParse] =
    (parsesToCache map { parse => (getSentenceKey(parse.sentence), parse) }).toMap

  override def parse(
    sentence: Sentence,
    constraints: Set[TransitionConstraint] = Set()
  ): Option[PolytreeParse] = {

    cachedParseMap.get(getSentenceKey(sentence)) match {
      case Some(parse) =>
        Some(parse)
      case None =>
        println(s"**CACHE MISS**: ${getSentenceKey(sentence)}")
        fallbackParser.parse(sentence, constraints)
    }
  }
}
