package net.wlezzar.storm.ml.sentiments

import java.util.Properties

import edu.stanford.nlp.ling.CoreAnnotations
import edu.stanford.nlp.neural.rnn.RNNCoreAnnotations
import edu.stanford.nlp.pipeline.StanfordCoreNLP
import edu.stanford.nlp.sentiment.SentimentCoreAnnotations

import scala.collection.JavaConverters._

class SimpleSentimentEstimator extends SentimentEstimator {

  val sentimentPipeline = {
    val props = new Properties()
    props.setProperty("annotators", "tokenize, ssplit, parse, sentiment")
    new StanfordCoreNLP(props)
  }

  override def evaluateSentiment(text:String): Int = {

    case class Sentiment(sentence:String, sentiment:Int)

    // submit text to the sentiment pipeline
    val annotation = sentimentPipeline.process(text)

    val sentimentsBySentence:List[Sentiment] = annotation
      .get(classOf[CoreAnnotations.SentencesAnnotation]).asScala
      .map { sentence =>
        val tree = sentence.get(classOf[SentimentCoreAnnotations.SentimentAnnotatedTree])
        val sentiment = RNNCoreAnnotations.getPredictedClass(tree)
        Sentiment(sentence.toString, sentiment) }
      .toList

    // We take the non neutral sentiment of the longest sentence and consider it as the sentiment of the tweet
    sentimentsBySentence.filter(_.sentiment != 2) match {
      case Nil => 2
      case nonNeutralSentiments => nonNeutralSentiments.maxBy(_.sentence.length).sentiment
    }
  }

}
