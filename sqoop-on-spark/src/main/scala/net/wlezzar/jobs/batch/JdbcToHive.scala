package net.wlezzar.jobs.batch

import net.wlezzar.PrebuiltSparkContext
import net.wlezzar.jobs.utils.JDBCConf
import org.apache.spark.sql.DataFrame
import org.apache.spark.sql.hive.HiveContext


case class JdbcToHiveConfig(sourceTable:String = "",
                            targetTable:Option[String] = None,
                            jdbcConf: JDBCConf = JDBCConf("","","",""),
                            format:String = "parquet",
                            mode:String = "append",
                            sqlContext: Option[HiveContext] = None)


class JdbcToHive(config: JdbcToHiveConfig) extends SparkETLJob {

  private val sqlContext = config.sqlContext.getOrElse(PrebuiltSparkContext.hiveContext)
  private val jdbcConf = config.jdbcConf
  private val sourceTable = config.sourceTable
  private val targetTable = config.targetTable.getOrElse(sourceTable)
  private val format = config.format
  private val mode = config.mode

  override def extract(): DataFrame = {
    logger.info(s"extracting data using JDBC : $jdbcConf")
    logger.info(s"source table : $sourceTable")

    sqlContext.read.format("jdbc")
      .option("url", jdbcConf.url)
      .option("driver", jdbcConf.driver)
      .option("dbtable", sourceTable)
      .option("user", jdbcConf.user)
      .option("password", jdbcConf.password).load()
  }

  override def process(data: DataFrame): DataFrame = data

  override def load(data: DataFrame): Unit = {
    logger.info(s"writing data as : $format")
    logger.info(s"target hive table : $targetTable")
    data.write.format(format).mode(mode).saveAsTable(targetTable)
  }
}


object JdbcToHive extends App {

  val usage = s"spark-submit --class ${classOf[JdbcToHive].getName} <jar>"

  val parser = new scopt.OptionParser[JdbcToHiveConfig](usage) {
    head("JDBC to hive extractor")

    opt[String]("connect").required().action((x, c) => c.copy(jdbcConf = c.jdbcConf.copy(url = x)))
    opt[String]("driver").required().action((x, c) => c.copy(jdbcConf = c.jdbcConf.copy(driver = x)))
    opt[String]("username").required().action((x, c) => c.copy(jdbcConf = c.jdbcConf.copy(user = x)))
    opt[String]("password").required().action((x, c) => c.copy(jdbcConf = c.jdbcConf.copy(password = x)))
    opt[String]("table").required().action((x, c) => c.copy(sourceTable = x))
    opt[String]("format").required().action((x, c) => c.copy(format = x))
    opt[String]("mode").optional().valueName("append,overwrite,...  (default : append)").action((x, c) => c.copy(mode = x))
    opt[String]("hive-table").optional().action((x, c) => c.copy(targetTable = Some(x)))

    help("help").text("print help")
  }

  parser.parse(args, JdbcToHiveConfig()) match {
    case Some(config) => new JdbcToHive(config).run()
    case None =>
  }
}