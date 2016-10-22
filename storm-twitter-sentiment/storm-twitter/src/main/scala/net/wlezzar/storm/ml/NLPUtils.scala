package net.wlezzar.storm.ml

import edu.stanford.nlp.ling.IndexedWord
import edu.stanford.nlp.semgraph.SemanticGraph

import scala.collection.JavaConverters._

object NLPUtils {

  def isWordNegated(word:IndexedWord, graph: SemanticGraph):Boolean = {
    val children = graph.getChildList(word).asScala.toList
    children.exists(child => graph.getEdge(word, child).getRelation.getShortName == "neg")
  }

  def areRelated(word:IndexedWord, subject:IndexedWord, graph: SemanticGraph):Boolean = {
    graph.getShortestUndirectedPathEdges(word, subject).size() == 1 || (
      graph.getSiblings(subject).contains(word) &&
        ! graph.getShortestUndirectedPathEdges(graph.getParent(word), word).get(0)
          .getRelation
          .getShortName.matches("conj")
      )
  }

  def buildSubjectPattern(subject:String, canBeCapitalized:Boolean = true, canBeUpperCase:Boolean = true) = {
    Set(
      Some(subject),
      Some(subject.toLowerCase),
      if (canBeCapitalized) Some(subject(0).toUpper + subject.toLowerCase.substring(1)) else None,
      if (canBeUpperCase) Some(subject.toUpperCase) else None
    ).flatten
      .mkString("|")
  }

}
