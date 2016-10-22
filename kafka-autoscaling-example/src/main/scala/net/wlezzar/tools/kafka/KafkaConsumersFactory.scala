package net.wlezzar.tools.kafka

import java.util.{Date, Properties}

import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.serialization.Deserializer

import scala.util.Try

object KafkaConsumersFactory /* extends Logging */ {
  def get[K, V](props: Properties): Try[KafkaConsumer[K, V]] =
    util.Try(new KafkaConsumer[K, V](props))

  def get[K, V](props: Properties,
                kSer: Deserializer[K],
                vSer: Deserializer[V]): KafkaConsumer[K, V] = {
    /**
      * if (props.containsKey("client.id")) logWarn("client.id found in consumer properties.
      * Remove it if you want a generated value")
      */
    props.putIfAbsent("client.id", {
      val group = props.getProperty("group.id")
      val timestamp = new Date().getTime
      s"${group}_${timestamp}"
    })
    new KafkaConsumer[K, V](props, kSer, vSer)
  }
}
