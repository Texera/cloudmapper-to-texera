package edu.uci.ics.amber.engine.architecture.sendsemantics.partitioners

import edu.uci.ics.amber.core.marker.Marker
import edu.uci.ics.amber.core.tuple.Tuple
import edu.uci.ics.amber.engine.architecture.messaginglayer.NetworkOutputGateway
import edu.uci.ics.amber.engine.common.AmberConfig
import edu.uci.ics.amber.engine.common.ambermessage.{DataFrame, MarkerFrame}
import edu.uci.ics.amber.core.virtualidentity.ActorVirtualIdentity

import scala.collection.mutable.ArrayBuffer

trait Partitioner extends Serializable {
  def getBucketIndex(tuple: Tuple): Iterator[Int]

  def allReceivers: Seq[ActorVirtualIdentity]
}

class NetworkOutputBuffer(
    val to: ActorVirtualIdentity,
    val dataOutputPort: NetworkOutputGateway,
    val batchSize: Int = AmberConfig.defaultDataTransferBatchSize
) {

  var buffer = new ArrayBuffer[Tuple]()

  def addTuple(tuple: Tuple): Unit = {
    buffer.append(tuple)
    if (buffer.size >= batchSize) {
      flush()
    }
  }

  def sendMarker(marker: Marker): Unit = {
    flush()
    dataOutputPort.sendTo(to, MarkerFrame(marker))
    flush()
  }

  def flush(): Unit = {
    if (buffer.nonEmpty) {
      dataOutputPort.sendTo(to, DataFrame(buffer.toArray))
      buffer = new ArrayBuffer[Tuple]()
    }
  }

}
