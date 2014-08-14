package org.allenai.nlpstack.tokenize

import org.allenai.common.testkit.UnitSpec
import org.allenai.nlpstack.core.Tokenizer

abstract class TokenizerSpec extends UnitSpec {
  def tokenizerToTest: Tokenizer

  val testSentences = Seq(
    """|The battle station is heavily shielded and carries a firepower greater
       |than half the star fleet. Its defenses are designed around a direct,
       |large-scale assault. A small one-man fighter should be able to
       |penetrate the outer defense.""".stripMargin,
    """|Pardon me for asking, sir, but what good are snub fighters going to be
       |against that?""".stripMargin)

  val tokenizedTestSentences = Seq(
    """|The 0
       |battle 4
       |station 11
       |is 19
       |heavily 22
       |shielded 30
       |and 39
       |carries 43
       |a 51
       |firepower 53
       |greater 63
       |than 71
       |half 76
       |the 81
       |star 85
       |fleet 90
       |. 95
       |Its 97
       |defenses 101
       |are 110
       |designed 114
       |around 123
       |a 130
       |direct 132
       |, 138
       |large-scale 140
       |assault 152
       |. 159
       |A 161
       |small 163
       |one-man 169
       |fighter 177
       |should 185
       |be 192
       |able 195
       |to 200
       |penetrate 203
       |the 213
       |outer 217
       |defense 223
       |. 230""".stripMargin,
    """|Pardon 0
       |me 7
       |for 10
       |asking 14
       |, 20
       |sir 22
       |, 25
       |but 27
       |what 31
       |good 36
       |are 41
       |snub 45
       |fighters 50
       |going 59
       |to 65
       |be 68
       |against 71
       |that 79
       |? 83""".stripMargin)

  "tokenizer implementation" should "correctly tokenize two example sentences" in {
    for ((text, expected) <- testSentences zip tokenizedTestSentences) {
      val tokenized = tokenizerToTest.tokenize(text)
      val tokenizedString = tokenized.mkString("\n")
      assert(tokenizedString === expected)
    }
  }

  it should "not throw an exception" in {
    tokenizerToTest.tokenize("<" + ("x" * 2000))

    val s = "For a given document , we want to find the summary text that maximizes !#\" . Using Bayes rule, we flip this so we end up maximizing $%&\"'&\". Thus, we are left with modelling two probability distributions: $%&\" , the probability of a document given a summary , and (\" , the probability of a summary. We assume that we are given the discourse structure of each document and the syntactic structures of each of its EDUs. The intuitive way of thinking about this application of Bayes rule, reffered to as the noisy-channel model, is that we start with a summary and add “noise” to it, yielding a longer document . The noise added in our model consists of words, phrases and discourse units. For instance, given the document “John Doe has secured the vote of most democrats.” we could add words to it (namely the word “already”) to generate “John Doe has already secured the vote of most democrats.” We could also choose to add an entire syntactic constituent, for instance a prepositional phrase, to generate “John Doe has secured the vote of most democrats in his constituency.” These are both examples of sentence expansion as used previously by Knight & Marcu (2000). Our system, however, also has the ability to expand on a core message by adding discourse constituents. For instance, it could decide to add another discourse constituent to the original summary “John Doe has secured the vote of most democrats” by CONTRASTing the information in the summary with the uncertainty regarding the support of the governor, thus yielding the text: “John Doe has secured the vote of most democrats. But without the support of the governor, he is still on shaky ground.” As in any noisy-channel application, there are three parts that we have to account for if we are to build a complete document compression system: the channel model, the source model and the decoder. We describe each of these below. The source model assigns to a string the probability (\" , the probability that the summary is good English. Ideally, the source model should disfavor ungrammatical sentences and documents containing incoherently juxtaposed sentences. The channel model assigns to any document/summary pair a probability )*%(\" . This models the extent to which is a good expansion of . For instance, if is “The mayor is now looking for re-election.”, + is “The mayor is now looking for re-election. He has to secure the vote of the democrats.” and is “The major is now looking for re-election. Sharks have sharp teeth.”, we expect ,-%(\" to be higher than .%&\" because , expands on by elaboration, while shifts to a different topic, yielding an incoherent text. The decoder searches through all possible summaries of a document for the summary that maximizes the posterior probability )*%(\" (\" . Each of these parts is described below. The job of the source model is to assign a score (\" to a compression independent of the original document. That is, the source model should measure how good English a summary is (independent of whether it is a good compression or not). Currently, we use a bigram measure of quality (trigram scores were also tested but failed to make a difference), combined with non-lexicalized context-free syntactic probabilities and context-free discourse probabilities, giving (\"/ 102436587'9&\" :<;>=@?A(\" : <BC;>=@?A )&\" . It would be better to use a lexicalized context free grammar, but that was not possible given the decoder used. The channel model is allowed to add syntactic constituents (through a stochastic operation called constituent-expand) or discourse units (through another stochastic operation called EDU-expand). Both of these operations are performed on a combined discourse/syntax tree called the DS-tree. The DS-tree for Text (1) is shown in Figure 1 for reference. Suppose we start with the summary D “The mayor is looking for re-election.” A constituentexpand operation could insert a syntactic constituent, such as “this year” anywhere in the syntactic tree of . A constituent-expand operation could also add single words: for instance the word “now” could be added between “is” and “looking,” yielding [ “The mayor is now looking for re-election.” The probability of inserting this word is based on the syntactic structure of the node into which it’s inserted. Knight and Marcu (2000) describe in detail a noisy-channel model that explains how short sentences can be expanded into longer ones by inserting and expanding syntactic constituents (and words). Since our constituent-expand stochastic operation simply reimplements Knight and Marcu’s model, we do not focus on them here. We refer the reader to (Knight and Marcu, 2000) for the details. In addition to adding syntactic constituents, our system is also able to add discourse units. Consider the summary \\ “John Doe has already secured the vote of most democrats in his consituency.” Through a sequence of discourse expansions, we can expand upon this summary to reach the original text. A complete discourse expansion process that would occur starting from this initial summary to generate the original document is shown in Figure 2. In this figure, we can follow the sequence of steps required to generate our original text, beginning with our summary . First, through an operation D-Project (“D” for “D”iscourse), we increase the depth of the tree, adding an intermediate NUC=SPAN node. This projection adds a factor of Nuc=Span ] Nuc=Span Nuc=Span\" to the probability of this sequence of operations (as is shown under the arrow). We are now able to perform the second operation, D-Expand, with which we expand on the core message contained in by adding a satellite which evaluates the information presented in . This expansion adds the probability of performing the expansion (called the discourse expansion probabilities, <BC^ . An example discourse expansion probability, written Nuc=Span ] Nuc=Span Sat=Eval Nuc=Span ] Nuc=Span\" , reflects the probability of adding an evaluation satellite onto a nuclear span). The rest of Figure 2 shows some of the remaining steps to produce the original document, each step labeled with the appropriate probability factors. Then, the probability of the entire expansion is the product of all those listed probabilities combined with the appropriate probabilities from the syntax side of things. In order to produce the final score $%&\" for a document/summary pair, we multiply together each of the expansion probabilities in the path leading from to . For estimating the parameters for the discourse models, we used an RST corpus of 385 Wall Street Journal articles from the Penn Treebank, which we obtained from LDC. The documents in the corpus range in size from 31 to 2124 words, with an average of 458 words per document. Each document is paired with a discourse structure that was manually built in the style of RST. (See (Carlson et al., 2001) for details concerning the corpus and the annotation process.) From this corpus, we were able to estimate parameters for a discourse PCFG using standard maximum likelihood methods. Furthermore, 150 document from the same corpus are paired with extractive summaries on the EDU level. Human annotators were asked which EDUs were most important; suppose in the example DStree (Figure 1) the annotators marked the second and fifth EDUs (the starred ones). These stars are propagated up, so that any discourse unit that has a descendent considered important is also considered important. From these annotations, we could deduce that, to compress a NUC=CONTRAST that has two children, NUC=SPAN and SAT=EVALUATION, we can drop the evaluation satellite. Similarly, we can compress a NUC=CONTRAST that has two children, SAT=CONDITION and NUC=SPAN by dropping the first discourse constituent. Finally, we can compress the ROOT deriving into SAT=BACKGROUND NUC=SPAN by dropping the SAT=BACKGROUND constituent. We keep counts of each of these examples and, once collected, we normalize them to get the discourse expansion probabilities. The goal of the decoder is to combine )&\" with $%&\" to get %,\" . There are a vast number of potential compressions of a large DS-tree, but we can efficiently pack them into a shared-forest structure, as described in detail by Knight & Marcu (2000). Each entry in the shared-forest structure has three associated probabilities, one from the source syntax PCFG, one from the source discourse PCFG and one from the expansion-template probabilities described in Section 3.2. Once we have generated a forest representing all possible compressions of the original document, we want to extract the best (or the \u008C -best) trees, taking into account both the expansion probabilities of the channel model and the bigram and syntax and discourse PCFG probabilities of the source model. Thankfully, such a generic extractor has already been built (Langkilde, 2000). For our purposes, the extractor selects the trees with the best combination of LM and expansion scores after performing an exhaustive search over all possible summaries. It returns a list of such trees, one for each possible length."
    tokenizerToTest.tokenize(s)
  }
}
