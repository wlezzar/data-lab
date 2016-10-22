package net.wlezzar.storm.ml.sentiments

import edu.stanford.nlp.semgraph.SemanticGraph
import edu.stanford.nlp.simple.Document
import net.wlezzar.storm.ml.NLPUtils

import scala.collection.JavaConverters._

class AdvancedSentimentEstimator(subject:String) extends SentimentEstimator {


  lazy val postiveWords = {
    val stream = getClass.getResourceAsStream("/sentiment/positive-words.txt")
    scala.io.Source.fromInputStream(stream).getLines.toSet
  }

  lazy val negativeWords = {
    val stream = getClass.getResourceAsStream("/sentiment/negative-words.txt")
    scala.io.Source.fromInputStream(stream).getLines.toSet
  }

  override def evaluateSentiment(text: String): Int = {
    val cleanedText = clean(text)
    val sentences = new Document(cleanedText).sentences().asScala
    val sentiments = sentences.map(s => evaluateSentiment(s.dependencyGraph))
    sentiments.sum
  }

  private def clean(text:String):String = {
    val urlRegex = """https?:\/\/\S+""".r
    val cleanedFromUrls = urlRegex.replaceAllIn(text, "")
    val cleanedFromHashtags = cleanedFromUrls.replaceAll("#","").replaceAll("@","")
    cleanedFromHashtags
  }

  /**
    * Sentiments are expressed mainly by verbs and adjectives. For example : 'I <b>love</b> this technology'. 'this is
    * <b>amazing</b>', etc. This algorithm makes use of this assumption to evaluate the sentiment expressed by a sentence
    * relatively to a subject (the subject is given at the class instanciation). It extracts all the adjectives and
    * verbs and checks if they are related to the studied subject. If so, it evaluates the positiveness or negativeness
    * of these related words and affects that to the global evaliation.
    * For example, if the subject we are studying is Hortonworks, here are the sentiments that should be evaluated from
    * the following sentences :
    * <ul>
    *   <li>I love Hortonworks and hate Cloudera | sentiment : 1 (positive) </li>
    *   <li>I love cloudera and hate Hortonworks | sentiment : -1 (negative) </li>
    *   <li>Hortonworks is the best actor in the big data market | sentiment : 1 (positive) </li>
    *   <li>Hortonworks released a product. I love something | sentiment : 0 (neutral) </li>
    * </ul>
    * The advantage of this algorithm is that it takes into account the semantic relationships between words. So even
    * if negative/positive words are present in the text, if they are not related to the subject, they will be
    * discarded and will not affect the global evaluation.
    * @param dependencyGraph the semantic dependecy graph of the sentence.
    * @return the sentiment related to the subject ( > 0 => positive, < 0 => negative, = 0 => neutral)
    */
  private def evaluateSentiment(dependencyGraph: SemanticGraph): Int = {
    /*
     We will rely exculsively on them to evaluate sentiment. So the first step is to extract them.
     */
    val adjectivesAndVerbs = dependencyGraph.getAllNodesByPartOfSpeechPattern("JJ|JJ(S|R)|VB|VB(D|G|N|P|Z)").asScala
    /*
    Next, we extract all the instances of the subject in the text. We will check if any of these instances are related
    to the previously extracted adjectives and verbs.
     */
    val subjectInstances = dependencyGraph.getAllNodesByWordPattern(subject).asScala

    if (subjectInstances.isEmpty || adjectivesAndVerbs.isEmpty) 0
    else {
      // for each subject word instance, we evaluate the sentiment related to it.
      val sentiments = subjectInstances map { subjectInstance =>
        val wordsRelatedToSubject = adjectivesAndVerbs.filter(word => NLPUtils.areRelated(word, subjectInstance, dependencyGraph))

        // Evaluate sentiments of words related to the subject
        wordsRelatedToSubject
          .map(word => annotateWord(word.originalText, NLPUtils.isWordNegated(word, dependencyGraph)))
          .sum
      }
      // Keep only words that have a direct relationship with the subject
      sentiments.sum
    }
  }

  /**
    * Gives the sentiment related to the word.
    * @param word The word for which we want to evaluate the sentiment
    * @param negated If this parameter is true, the sentiment value will be inversed (-1 instead of 1, etc.)
    * @return The sentiment expressed by the word
    */
  private def annotateWord(word:String, negated:Boolean = false):Int = {
    val sentiment = {
      if (postiveWords.contains(word)) 1
      else if (negativeWords.contains(word)) -1
      else 0
    }
    sentiment * (if (negated) -1 else 1)
  }
}

object AdvancedSentimentEstimator {

  def main(args: Array[String]) {
    var subject:String = null
    var testSentences:List[String] = null

    if (args.length == 0) {
      println("No parameters given ! Working with defaults")
      subject = "docker"
      testSentences = List(
        "I hate something but I love docker",
        "I hate docker but I love something",
        "I hate docker but that makes me love something",
        "docker is the best technology of this last decade !",
        "I am crazy about Docker"
      )
    } else if (args.length == 2) {
      subject = args(0)
      testSentences = List(args(1))
    } else {
      println("Wrong number of parameters given !")
      println("Usage : arg1 -> subject | arg2 -> sentence")
    }

    val pattern = NLPUtils.buildSubjectPattern(subject, true, true)
    val estimator = new AdvancedSentimentEstimator(pattern)

    println(s"Testing the estimator about $subject")
    testSentences foreach { sentence =>
      print(s"\n$sentence | sentiment : ${estimator.evaluateSentiment(sentence)}")
    }

    println("\n")
    println("If you want to try your own sentences and subjects : ")
    println("Try : arg1 -> subject | arg2 -> sentence")
  }
}
