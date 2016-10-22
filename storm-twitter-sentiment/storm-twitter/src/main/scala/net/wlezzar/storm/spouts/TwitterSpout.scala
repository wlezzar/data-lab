package net.wlezzar.storm.spouts

import java.util.concurrent.LinkedBlockingQueue
import java.util.{Map => JavaMap}

import net.wlezzar.storm.Constants
import org.apache.storm.spout.SpoutOutputCollector
import org.apache.storm.task.TopologyContext
import org.apache.storm.topology.OutputFieldsDeclarer
import org.apache.storm.topology.base.BaseRichSpout
import org.apache.storm.tuple.{Fields, Values}
import org.apache.storm.utils.Utils
import twitter4j._
import twitter4j.conf.ConfigurationBuilder

class TwitterSpout(credentials: TwitterCredentials, filterQuery: Option[FilterQuery]) extends BaseRichSpout {

  private var twitterStream:TwitterStream = null
  private var collector:SpoutOutputCollector = null

  val queue = new LinkedBlockingQueue[Status](1000)

  override def open(map: JavaMap[_, _], ctx: TopologyContext, collector: SpoutOutputCollector): Unit = {
    this.collector = collector
    this.twitterStream = {
      val config = new ConfigurationBuilder()
        .setOAuthConsumerKey(credentials.key)
        .setOAuthConsumerSecret(credentials.secret)
        .setOAuthAccessToken(credentials.token)
        .setOAuthAccessTokenSecret(credentials.tokenSecret)
        .build()
      new TwitterStreamFactory(config).getInstance()
    }

    twitterStream addListener new StatusListener {

      override def onStallWarning(stallWarning: StallWarning): Unit = {}

      override def onDeletionNotice(statusDeletionNotice: StatusDeletionNotice): Unit = {}

      override def onScrubGeo(l: Long, l1: Long): Unit = {}

      override def onStatus(status: Status): Unit = queue.offer(status)

      override def onTrackLimitationNotice(i: Int): Unit = {}

      override def onException(e: Exception): Unit = throw e
    }

    filterQuery match {
      case None => twitterStream.sample()
      case Some(query) => twitterStream.filter(query)
    }

  }

  override def nextTuple(): Unit = queue.poll() match {
    case status:Status => collector.emit(new Values(status))
    case _ => Utils.sleep(50)
  }

  override def close() = { twitterStream.shutdown() ; super.close() }

  override def declareOutputFields(declarer: OutputFieldsDeclarer): Unit = declarer.declare(new Fields(Constants.FieldNames.TWEET))

}
