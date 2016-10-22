package net.wlezzar.storm.bolts

import java.util
import java.util.{Collections, Properties}

import edu.stanford.nlp.ling.CoreAnnotations
import edu.stanford.nlp.neural.rnn.RNNCoreAnnotations
import edu.stanford.nlp.pipeline.StanfordCoreNLP
import edu.stanford.nlp.sentiment.SentimentCoreAnnotations
import net.wlezzar.storm.ml.sentiments.{AdvancedSentimentEstimator, SentimentEstimator}
import org.apache.storm.task.{OutputCollector, TopologyContext}
import org.apache.storm.topology.OutputFieldsDeclarer
import org.apache.storm.topology.base.BaseRichBolt
import org.apache.storm.tuple.{Fields, Tuple}

import scala.collection.JavaConverters._

class SentimentBolt(subject:String) extends BaseRichBolt {

  var collector: OutputCollector = null
  var sentimentEstimator: SentimentEstimator = null

  override def prepare(stormConf: util.Map[_, _], context: TopologyContext, collector: OutputCollector): Unit = {
    this.collector = collector
    this.sentimentEstimator = new AdvancedSentimentEstimator(subject)
  }

  override def declareOutputFields(declarer: OutputFieldsDeclarer): Unit = {
    declarer.declare(new Fields("user", "createdAt","text","retweetCount","latitude","longitude","sentiment"))
  }

  override def execute(input: Tuple): Unit = {
    // extract text
    val text = input.getStringByField("text")
    val sentiment = sentimentEstimator.evaluateSentiment(text)
    val output = input.getValues
    output.add(sentiment: java.lang.Integer)
    collector.emit(output)
  }
}
