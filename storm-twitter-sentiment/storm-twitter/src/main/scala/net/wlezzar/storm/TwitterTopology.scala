package net.wlezzar.storm

import net.wlezzar.storm.bolts.{PrinterBolt, SentimentBolt, TweetsPreprocessorBolt}
import net.wlezzar.storm.ml.NLPUtils
import net.wlezzar.storm.spouts.{TwitterCredentials, TwitterSpout}
import net.wlezzar.tools.Logging
import org.apache.storm.hdfs.bolt.HdfsBolt
import org.apache.storm.hdfs.bolt.format.{DefaultFileNameFormat, DelimitedRecordFormat}
import org.apache.storm.hdfs.bolt.rotation.FileSizeRotationPolicy
import org.apache.storm.hdfs.bolt.rotation.FileSizeRotationPolicy.Units
import org.apache.storm.hdfs.bolt.sync.CountSyncPolicy
import org.apache.storm.topology.TopologyBuilder
import org.apache.storm.{Config, LocalCluster, StormSubmitter}
import twitter4j.FilterQuery


case class TwitterTopologyConfig(topologyName:String = "TwitterSentimentAnalysis",
                                 twitterCredentials: TwitterCredentials = TwitterCredentials("","","",""),
                                 keywords:List[String] = List(),
                                 sentimentAnalysisSubjectPattern:String = "",
                                 fsUrl:String = "hdfs://localhost:8020",
                                 fsPath:String = "/trump/",
                                 outputFileformatDelimiter:String = "|",
                                 fileRotationSize:Float = 5.0f,
                                 messagesBeforeHsync:Int = 100,
                                 localMode:Boolean = false,
                                 debug:Boolean = false)

class TwitterTopology(config: TwitterTopologyConfig) {

  val topologyName = config.topologyName
  val twitterCredentials = config.twitterCredentials
  val keywords = config.keywords
  val localMode:Boolean = config.localMode

  val twitterFilterQuery = new FilterQuery()
    .language("en")
    .track(keywords:_*)

  val hdfsBolt = new HdfsBolt()
    .withFsUrl(config.fsUrl)
    .withFileNameFormat(new DefaultFileNameFormat().withPath(config.fsPath))
    .withRecordFormat(new DelimitedRecordFormat().withFieldDelimiter(config.outputFileformatDelimiter))
    .withRotationPolicy(new FileSizeRotationPolicy(config.fileRotationSize, Units.MB))
    .withSyncPolicy(new CountSyncPolicy(config.messagesBeforeHsync))

  val topologyBuilder = new TopologyBuilder()
  topologyBuilder.setSpout("TwitterSpout", new TwitterSpout(twitterCredentials, Some(twitterFilterQuery)))
  topologyBuilder.setBolt("TweetsPreprocessorBolt", new TweetsPreprocessorBolt())
    .localOrShuffleGrouping("TwitterSpout")
  topologyBuilder.setBolt("SentimentBolt", new SentimentBolt(config.sentimentAnalysisSubjectPattern))
    .localOrShuffleGrouping("TweetsPreprocessorBolt")
  topologyBuilder.setBolt("HdfsBolt", hdfsBolt)
    .localOrShuffleGrouping("SentimentBolt")
  if (config.debug) topologyBuilder.setBolt("PrinterBolt", new PrinterBolt()).localOrShuffleGrouping("SentimentBolt")

  def start():Unit = {
    val stormConf = new Config()
    stormConf.setMessageTimeoutSecs(120)

    if (config.localMode) {
      val cluster = new LocalCluster()

      Runtime.getRuntime.addShutdownHook(new Thread() {
        override def run(): Unit = {
          cluster.killTopology(topologyName)
          cluster.shutdown()
        }
      })

      cluster.submitTopology(topologyName, stormConf, topologyBuilder.createTopology())
    } else {
      StormSubmitter.submitTopology(topologyName, stormConf, topologyBuilder.createTopology())
    }
  }

}

object TwitterTopology extends Logging with App {


  val parser = new scopt.OptionParser[TwitterTopologyConfig]("Twitter Sentiment analysis Storm Topology") {

    head(s"${this.getClass.toString}")

    opt[String]("topology-name").required().action((x, c) => c.copy(topologyName = x))

    opt[String]("twitter-api-key").required().action((x, c) => c.copy(twitterCredentials = c.twitterCredentials.copy(key = x)))
    opt[String]("twitter-secret-key").required().action((x, c) => c.copy(twitterCredentials = c.twitterCredentials.copy(secret = x)))
    opt[String]("twitter-token").required().action((x, c) => c.copy(twitterCredentials = c.twitterCredentials.copy(token = x)))
    opt[String]("twitter-token-secret").required().action((x, c) => c.copy(twitterCredentials = c.twitterCredentials.copy(tokenSecret = x)))

    opt[String]("keywords").required().valueName("k1,k2,...").action((x, c) => c.copy(keywords = x.split(",").toList))
    opt[String]("subject").required().valueName("docker").action((x, c) => c.copy(sentimentAnalysisSubjectPattern = NLPUtils.buildSubjectPattern(x)))
    opt[String]("fs-url").optional().valueName("hdfs://localhost:8020").action((x, c) => c.copy(fsUrl = x))
    opt[String]("fs-path").optional().valueName("/foo/").action((x, c) => c.copy(fsPath = x))
    opt[String]("file-delimiter").optional().action((x, c) => c.copy(outputFileformatDelimiter = x))
    opt[Double]("file-rotation-limit-size").optional().action((x, c) => c.copy(fileRotationSize = x.toFloat))
    opt[Int]("hsync-threshold").optional().action((x, c) => c.copy(messagesBeforeHsync= x))
    opt[Unit]("local").optional().action((x, c) => c.copy(localMode = true))
    opt[Unit]("debug").optional().action((x, c) => c.copy(debug = true))
  }

  parser.parse(args, TwitterTopologyConfig()) match {
    case Some(config) => {
      logInfo("creating the topology")
      new TwitterTopology(config).start()
    }

    case None =>
  }


}
