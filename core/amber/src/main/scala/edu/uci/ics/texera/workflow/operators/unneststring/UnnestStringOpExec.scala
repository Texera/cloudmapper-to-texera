package edu.uci.ics.texera.workflow.operators.unneststring

import edu.uci.ics.amber.engine.common.model.tuple.{Tuple, TupleLike}
import edu.uci.ics.texera.workflow.common.operators.flatmap.FlatMapOpExec

class UnnestStringOpExec(attributeName: String, delimiter: String) extends FlatMapOpExec {

  setFlatMapFunc(splitByDelimiter)
  private def splitByDelimiter(tuple: Tuple): Iterator[TupleLike] = {
    delimiter.r
      .split(tuple.getField(attributeName).toString)
      .filter(_.nonEmpty)
      .iterator
      .map(split => TupleLike(tuple.getFields ++ Seq(split)))
  }
}
