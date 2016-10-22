package net.wlezzar.jobs.batch

import net.wlezzar.tools.Logging
import org.apache.spark.sql.DataFrame

trait SparkETLJob extends Logging {
  def extract():DataFrame
  def process(data:DataFrame):DataFrame
  def load(data:DataFrame):Unit

  def run():Unit = {
    val data = extract()
    val processed = process(data)
    load(processed)
  }
}
