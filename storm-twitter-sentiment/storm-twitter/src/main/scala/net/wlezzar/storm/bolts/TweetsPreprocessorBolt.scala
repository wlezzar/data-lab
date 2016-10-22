package net.wlezzar.storm.bolts

import java.util

import net.wlezzar.storm.Constants
import org.apache.storm.task.{OutputCollector, TopologyContext}
import org.apache.storm.topology.OutputFieldsDeclarer
import org.apache.storm.topology.base.BaseRichBolt
import org.apache.storm.tuple.{Fields, Tuple, Values}
import twitter4j.Status

class TweetsPreprocessorBolt extends BaseRichBolt {

  private var collector: OutputCollector = null

  override def prepare(stormConf: util.Map[_, _], context: TopologyContext, collector: OutputCollector): Unit = {
    this.collector = collector
  }

  override def declareOutputFields(declarer: OutputFieldsDeclarer): Unit = {
    declarer.declare(new Fields("user","createdAt","text","retweetCount","latitude","longitude"))
  }

  override def execute(input: Tuple): Unit = {
    val tweet = input.getValueByField(Constants.FieldNames.TWEET).asInstanceOf[Status]
    collector emit new Values(
      tweet.getUser.getName.replaceAll("\\|",""),
      tweet.getCreatedAt.getTime: java.lang.Long,
      tweet.getText.replaceAll("\n","").replaceAll("\\|",""),
      tweet.getRetweetCount: java.lang.Integer,
      (if (tweet.getGeoLocation == null) 0.0 else tweet.getGeoLocation.getLatitude): java.lang.Double,
      (if (tweet.getGeoLocation == null) 0.0 else tweet.getGeoLocation.getLongitude): java.lang.Double
    )
  }

}
