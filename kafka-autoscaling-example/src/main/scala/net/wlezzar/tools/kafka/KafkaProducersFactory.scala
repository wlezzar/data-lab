package net.wlezzar.tools.kafka

import java.util.Properties

import org.apache.kafka.clients.producer.{KafkaProducer, Producer}
import org.apache.kafka.common.serialization.Serializer

/**
  * Created by wlezzar on 08/03/16.
  */
object KafkaProducersFactory {
  def create[K, V](conf:Properties, kSer:Serializer[K], vSer:Serializer[V]):Producer[K,V] = {
    new KafkaProducer[K,V](conf, kSer, vSer)
  }
  def create[K, V](conf:Properties):Producer[K,V] = {
    new KafkaProducer[K,V](conf)
  }
}