package org.allenai.nlpstack.parse.poly.polyparser

import org.allenai.nlpstack.parse.poly.fsm._
import org.allenai.nlpstack.parse.poly.ml.BrownClusters
import org.allenai.nlpstack.parse.poly.polyparser.labeler.ParseLabelerTransitionSystem
import scopt.OptionParser

private case class ParserTrainingConfig(baseModelPath: String = "", clustersPath: String = "",
  trainingPath: String = "",
  outputPath: String = "", testPath: String = "", dataSource: String = "")

object Training {

  /** Command-line executable for training a parser from CoNLL-format training data.
    *
    * Usage: Training [options]
    *
    * -t <file> | --train <file>
    *  the path to the training file (in ConllX format)
    * -o <file> | --output <file>
    *  where to direct the output files
    * -x <file> | --test <file>
    *  the path to the test file (in ConllX format)
    * -d <file> | --datasource <file>
    *  the location of the data ('datastore','local')
    *
    * @param args command-line arguments (specified above)
    */
  def main(args: Array[String]) {
    val optionParser = new OptionParser[ParserTrainingConfig]("Trainer") {
      opt[String]('b', "base") valueName ("<file>") action
        { (x, c) => c.copy(baseModelPath = x) } text ("an optional base model file to adapt")
      opt[String]('t', "train") required () valueName ("<file>") action
        { (x, c) => c.copy(trainingPath = x) } text ("the path to the training files " +
          "(in ConllX format, comma-separated filenames)")
      opt[String]('c', "clusters") valueName ("<file>") action
        { (x, c) => c.copy(clustersPath = x) } text ("the path to the Brown cluster files " +
          "(in Liang format, comma-separated filenames)")
      opt[String]('o', "output") required () valueName ("<file>") action
        { (x, c) => c.copy(outputPath = x) } text ("where to direct the output files")
      opt[String]('x', "test") required () valueName ("<file>") action
        { (x, c) => c.copy(testPath = x) } text ("the path to the test file (in ConllX format)")
      opt[String]('d', "datasource") required () valueName ("<file>") action
        { (x, c) => c.copy(dataSource = x) } text ("the location of the data " +
          "('datastore','local')") validate { x =>
            if (Set("datastore", "local").contains(x)) {
              success
            } else {
              failure("unsupported input format")
            }
          }
    }
    val config: ParserTrainingConfig = optionParser.parse(args, ParserTrainingConfig()).get

    val trainingSource: PolytreeParseSource =
      MultiPolytreeParseSource(config.trainingPath.split(",") map { path =>
        InMemoryPolytreeParseSource.getParseSource(path,
          ConllX(true), config.dataSource)
      })

    val clusters: Seq[BrownClusters] = {
      if(config.clustersPath != "") {
        config.clustersPath.split(",") map { path =>
          BrownClusters.fromLiangFormat(path)
        }
      } else {
        Seq[BrownClusters]()
      }
    }

    println("Training task tree.")
    val transitionSystem: TransitionSystem =
      ArcEagerTransitionSystem(ArcEagerTransitionSystem.defaultFeature, clusters)
    val taskIdentifier: TaskIdentifier = {
      val taskActivationThreshold = 5000
      TaskConjunctionIdentifier.learn(
        List(
          ApplicabilitySignatureIdentifier,
          StateRefPropertyIdentifier(BufferRef(0), 'factorieCpos),
          StateRefPropertyIdentifier(StackRef(0), 'factorieCpos),
          StateRefPropertyIdentifier(BufferRef(0), 'factoriePos)),
        new GoldParseSource(trainingSource, transitionSystem),
        taskActivationThreshold)
    }

    //val baseCostFunction: Option[ClassifierBasedCostFunction] =
    //  config.baseModelPath match {
    //    case "" => None
    //    case _ => Some(ClassifierBasedCostFunction.load(config.baseModelPath))
    //  }

    println("Training parser.")
    val baseCostFunction = None // TODO: fix this
    val trainingVectorSource = new GoldParseTrainingVectorSource(trainingSource, taskIdentifier,
      transitionSystem, baseCostFunction)
    val parsingCostFunction: StateCostFunction = {
      val trainer =
        new DTCostFunctionTrainer(taskIdentifier, transitionSystem, trainingVectorSource,
          baseCostFunction)
      trainer.costFunction
    }


    println("Saving models.")
    val parsingNbestSize = 5
    val parserConfig = ParserConfiguration(parsingCostFunction,
      BaseCostRerankingFunction, parsingNbestSize)
    val parser = RerankingTransitionParser(parserConfig)
    TransitionParser.save(parser, config.outputPath)

    ParseFile.fullParseEvaluation(parser, config.testPath, ConllX(true),
      config.dataSource, ParseFile.defaultOracleNbest)
  }
}
