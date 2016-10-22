package net.wlezzar.kafka.jobs

import java.util.Properties

import net.wlezzar.tools.Logging
import net.wlezzar.tools.kafka.KafkaConsumersFactory
import org.apache.kafka.common.serialization.StringDeserializer
import org.joda.time.DateTime

import scala.collection.JavaConverters._

case class MortgageDocumentProcessorConfig(topic:String = "UnknownTopic",
                                           consumerProperties:Properties = new Properties(),
                                           esHost:String = "localhost",
                                           esClusterName:String = "elasticsearch",
                                           esIndex:String = "mortgage_documents",
                                           esMapping:String = "processed_documents",
                                           processingTime:Int = 2000)

object MortgageDocumentProcessor extends Logging with App {

  val parser = new scopt.OptionParser[MortgageDocumentProcessorConfig]("Kafka autoscaling example") {

    head("Kafka autoscaling example")

    opt[String]("topic").required().action((x, c) => c.copy(topic = x))

    opt[Map[String, String]]("consumer-properties")
      .required()
      .valueName("k1=v1,k2=v2...")
      .validate { params =>
        if (params.contains("bootstrap.servers")) success
        else failure("bootstrap.servers is required") }
      .action { (x, c) =>
        val props = c.consumerProperties
        x.foreach { case (key, value) => props.put(key, value) }
        c.copy(consumerProperties = props)}

    opt[Int]("processing-time").optional().action((x, c) => c.copy(processingTime = x))
    opt[String]("es-host").optional().action((x, c) => c.copy(esHost = x))
    opt[String]("es-cluster-name").optional().action((x, c) => c.copy(esClusterName = x))
    opt[String]("es-index").optional().action((x, c) => c.copy(esIndex = x))
    opt[String]("es-mapping").optional().action((x, c) => c.copy(esMapping = x))
  }

  parser.parse(args, MortgageDocumentProcessorConfig()) match {
    case Some(config) => {
      logInfo("creating kafka consumer")
      val consumer = KafkaConsumersFactory.get[String, String](config.consumerProperties,
                                                               kSer = new StringDeserializer(),
                                                               vSer = new StringDeserializer())

      val esClient = EsClient.getOrCreateSingleton("myEsClient",config.esClusterName, List(config.esHost)).get

      val topic = config.topic
      val processingTime = config.processingTime

      logInfo(s"consumer group : ${config.consumerProperties.get("group.id")}")
      logInfo(s"subscribing to topic : $topic")
      consumer.subscribe(List(topic).asJava)

      while (true) {
        logInfo(s"polling from : $topic")
        val records = consumer.poll(10000).asScala
        records foreach { document =>
          logInfo(s"processing document $document")
          Thread.sleep(processingTime)
          val date = new DateTime().toString()
          val esResponse = esClient.prepareIndex(config.esIndex,config.esMapping).setSource(s"""{"date":"$date","document":"$document"}""").get()
          println(esResponse)
          logInfo(esResponse.toString)
          logInfo(s"finished processing document $document")
        }
        logInfo("commiting to Kafka")
        consumer.commitSync()
      }
    }

    case None =>
  }

}
