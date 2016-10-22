package net.wlezzar

import net.wlezzar.tools.Logging
import org.apache.spark.sql.hive.HiveContext
import org.apache.spark.streaming.{Seconds, StreamingContext}
import org.apache.spark.{SparkConf, SparkContext}

object PrebuiltSparkContext extends Logging {

  val conf = new SparkConf()

  lazy val sc: SparkContext = {

    try {
      conf.get("spark.master")
    } catch {
      case e: NoSuchElementException =>
        logWarn("No master is set, using 'local[*]'")
        conf.setMaster("local[*]")
    }

    new SparkContext(conf.setAppName(this.getClass.getName))
  }

  lazy val hiveContext : HiveContext = new HiveContext(sc)

  lazy val ssc: StreamingContext = new StreamingContext(sc, Seconds(5))

}