package edu.uci.ics.texera.web.resource.dashboard.user.cluster

import edu.uci.ics.amber.core.storage.StorageConfig
import edu.uci.ics.texera.dao.SqlServer
import edu.uci.ics.texera.web.model.jooq.generated.enums.ClusterStatus

import javax.ws.rs.{Consumes, POST, Path}
import javax.ws.rs.core.{MediaType, Response}
import edu.uci.ics.texera.web.model.jooq.generated.tables.daos.{ClusterActivityDao, ClusterDao}
import edu.uci.ics.texera.web.model.jooq.generated.tables.pojos.ClusterActivity
import edu.uci.ics.texera.web.resource.dashboard.user.cluster.ClusterUtils.{
  updateClusterActivityEndTime,
  updateClusterStatus
}
import edu.uci.ics.texera.web.resource.dashboard.user.cluster.ClusterCallbackResource.{
  clusterActivityDao,
  clusterDao,
  context
}
import edu.uci.ics.texera.web.model.jooq.generated.tables.Cluster.CLUSTER
import org.jooq.impl.DSL

import java.sql.Timestamp

object ClusterCallbackResource {
  final private val context = SqlServer
    .getInstance(StorageConfig.jdbcUrl, StorageConfig.jdbcUsername, StorageConfig.jdbcPassword)
    .createDSLContext()
  final private lazy val clusterDao = new ClusterDao(context.configuration)
  final private lazy val clusterActivityDao = new ClusterActivityDao(context.configuration)

  // error messages
  val ERR_USER_HAS_NO_ACCESS_TO_CLUSTER_MESSAGE = "User has no access to this cluster"
}

@Path("/callback")
class ClusterCallbackResource {

  @POST
  @Path("/cluster/created")
  @Consumes(Array(MediaType.APPLICATION_JSON))
  def handleClusterCreatedCallback(callbackPayload: CallbackPayload): Response = {
    val clusterId = callbackPayload.clusterId
    val success = callbackPayload.success

    val cluster = clusterDao.fetchOneByCid(clusterId)
    if (success && cluster != null && cluster.getStatus == ClusterStatus.PENDING) {
      updateClusterStatus(clusterId, ClusterStatus.RUNNING, context)
      insertClusterActivity(cluster.getCid, cluster.getCreationTime)
      Response.ok("Cluster status updated to RUNNING").build()
    } else {
      Response
        .status(Response.Status.NOT_FOUND)
        .entity("Cluster not found or status update not allowed")
        .build()
    }
  }

  @POST
  @Path("/cluster/deleted")
  @Consumes(Array(MediaType.APPLICATION_JSON))
  def handleClusterDeletedCallback(callbackPayload: CallbackPayload): Response = {
    val clusterId = callbackPayload.clusterId
    val success = callbackPayload.success

    val cluster = clusterDao.fetchOneByCid(clusterId)
    if (success && cluster != null && cluster.getStatus == ClusterStatus.SHUTTING_DOWN) {
      updateClusterStatus(clusterId, ClusterStatus.TERMINATED, context)
      updateClusterActivityEndTime(clusterId, context)
      Response
        .ok(s"Cluster with ID $clusterId marked as TERMINATED and activity end time updated")
        .build()
    } else {
      Response
        .status(Response.Status.NOT_FOUND)
        .entity("Cluster not found or status update not allowed")
        .build()
    }
  }

  @POST
  @Path("/cluster/paused")
  @Consumes(Array(MediaType.APPLICATION_JSON))
  def handleClusterPausedCallback(callbackPayload: CallbackPayload): Response = {
    val clusterId = callbackPayload.clusterId
    val success = callbackPayload.success

    val cluster = clusterDao.fetchOneByCid(clusterId)
    if (success && cluster != null && cluster.getStatus == ClusterStatus.STOPPING) {
      updateClusterStatus(clusterId, ClusterStatus.STOPPED, context)
      updateClusterActivityEndTime(clusterId, context)
      Response
        .ok(s"Cluster with ID $clusterId marked as STOPPED and activity end time updated")
        .build()
    } else {
      Response
        .status(Response.Status.NOT_FOUND)
        .entity("Cluster not found or status update not allowed")
        .build()
    }
  }

  @POST
  @Path("/cluster/resumed")
  @Consumes(Array(MediaType.APPLICATION_JSON))
  def handleClusterResumedCallback(callbackPayload: CallbackPayload): Response = {
    val clusterId = callbackPayload.clusterId
    val success = callbackPayload.success
    // Update the cluster status to LAUNCHED in the database
    val cluster = clusterDao.fetchOneByCid(clusterId)
    if (success && cluster != null && cluster.getStatus == ClusterStatus.PENDING) {
      updateClusterStatus(clusterId, ClusterStatus.RUNNING, context)
      insertClusterActivity(cluster.getCid, cluster.getCreationTime)
      Response.ok("Cluster status updated to RUNNING").build()
    } else {
      Response
        .status(Response.Status.NOT_FOUND)
        .entity("Cluster not found or status update not allowed")
        .build()
    }
  }

  @POST
  @Path("/cluster/getid")
  @Consumes(Array(MediaType.APPLICATION_JSON))
  def handleClusterGetIdCallback(callbackPayload: CallbackPayload): Response = {
    val maxCidResult = context
      .select(DSL.max(CLUSTER.CID))
      .from(CLUSTER)
      .fetchOne()

    val maxCid = maxCidResult.getValue(0, classOf[Integer])

    if (maxCid == null) {
      Response.ok("Next cluster ID is 1").entity(1).build()
    } else {
      val nextCid = maxCid + 1
      Response.ok(s"Next cluster ID is $nextCid").entity(nextCid).build()
    }
  }

  /**
    * Inserts a new cluster activity record with the given start time.
    *
    * @param clusterId The ID of the cluster.
    * @param startTime The start time of the activity.
    */
  private def insertClusterActivity(clusterId: Int, startTime: Timestamp): Unit = {
    val clusterActivity = new ClusterActivity()
    clusterActivity.setClusterId(clusterId)
    clusterActivity.setStartTime(startTime)
    clusterActivityDao.insert(clusterActivity)
  }
}

// Define the payload structure expected from the Go service
case class CallbackPayload(clusterId: Int, success: Boolean)
