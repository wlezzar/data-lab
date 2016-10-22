package net.wlezzar.tools

import org.slf4j.LoggerFactory

abstract class Logging {

  val logger = LoggerFactory.getLogger(this.getClass)

  def logDebug(msg:String) = logger.debug(msg)
  def logInfo(msg:String) = logger.info(msg)
  def logWarn(msg:String) = logger.warn(msg)
  def logError(msg:String) = logger.error(msg)
}
