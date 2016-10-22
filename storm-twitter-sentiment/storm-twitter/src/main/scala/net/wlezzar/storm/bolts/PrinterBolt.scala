package net.wlezzar.storm.bolts

import java.util

import edu.stanford.nlp.simple.{Document, Sentence}
import net.wlezzar.storm.Constants
import org.apache.storm.task.{OutputCollector, TopologyContext}
import org.apache.storm.topology.OutputFieldsDeclarer
import org.apache.storm.topology.base.BaseRichBolt
import org.apache.storm.tuple.Tuple
import twitter4j.Status

import scala.collection.JavaConverters._
class PrinterBolt extends BaseRichBolt {

  private var collector: OutputCollector = null

  override def execute(input: Tuple): Unit = {
    println(input.getValues.asScala.mkString(" | "))
  }

  override def prepare(stormConf: util.Map[_, _], context: TopologyContext, collector: OutputCollector): Unit = {
    println("declared fields : "+context.getThisInputFields.asScala.map{ case (s, m) => (s, m.asScala)})
  }

  override def declareOutputFields(declarer: OutputFieldsDeclarer): Unit = {}
}
