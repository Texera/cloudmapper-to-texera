package edu.uci.ics.amber.engine.common.executor

import edu.uci.ics.amber.engine.common.model.tuple.{Tuple, TupleLike}
import edu.uci.ics.amber.engine.common.workflow.PortIdentity

trait SinkOperatorExecutor extends OperatorExecutor {

  override def processTupleMultiPort(
      tuple: Tuple,
      port: Int
  ): Iterator[(Tuple, Option[PortIdentity])] = {
    consumeTuple(tuple, port)
    Iterator.empty
  }

  override def onFinishMultiPort(port: Int): Iterator[(TupleLike, Option[PortIdentity])] =
    Iterator.empty

  def consumeTuple(tuple: Tuple, input: Int): Unit

  override def open(): Unit = {}

  override def close(): Unit = {}
}