package net.wlezzar.storm.ml.sentiments

trait SentimentEstimator {

  def evaluateSentiment(text:String):Int

}
