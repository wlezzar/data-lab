package net.wlezzar.kafka.jobs

import java.util.{Properties, UUID}

import net.wlezzar.tools.Logging
import net.wlezzar.tools.kafka.KafkaProducersFactory
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.StringSerializer


case class MortgageDocumentProducerConfig(kafkaTopic:String = "unknownTopic",
                                          producerProperties:Properties = new Properties(),
                                          interval:Int = 2000)


object MortgageDocumentProducer extends Logging with App  {

  val parser = new scopt.OptionParser[MortgageDocumentProducerConfig]("Kafka autoscaling example") {

    head("Kafka autoscaling example")

    opt[String]("topic").required().action((x, c) => c.copy(kafkaTopic = x))

    opt[Map[String, String]]("producer-properties")
      .required()
      .valueName("k1=v1,k2=v2...")
      .validate { params =>
        if (params.contains("bootstrap.servers")) success
        else failure("bootstrap.servers is required") }
      .action { (x, c) =>
        val props = c.producerProperties
        x.foreach { case (key, value) => props.put(key, value) }
        c.copy(producerProperties = props)}

    opt[Int]("interval").optional().action((x, c) => c.copy(interval = x))
  }

  parser.parse(args, MortgageDocumentProducerConfig()) match {
    case Some(config) => {
      logInfo("creating kafka producer")
      val producer = KafkaProducersFactory.create[String, String](config.producerProperties,
                                                                  kSer = new StringSerializer(),
                                                                  vSer = new StringSerializer())

      val topic = config.kafkaTopic
      val interval = config.interval

      while (true) {
        val document = s"document ${UUID.randomUUID().toString}"
        logInfo(s"sending document : $document")
        val record = new ProducerRecord[String, String](topic, document)
        producer.send(record)
        Thread.sleep(interval)
      }
    }

    case None =>
  }




}
