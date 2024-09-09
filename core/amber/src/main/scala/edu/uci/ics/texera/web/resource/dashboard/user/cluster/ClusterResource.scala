package edu.uci.ics.texera.web.resource.dashboard.user.cluster

import edu.uci.ics.texera.web.SqlServer
import edu.uci.ics.texera.web.auth.SessionUser
import edu.uci.ics.texera.web.model.jooq.generated.enums.ClusterStatus
import edu.uci.ics.texera.web.model.jooq.generated.tables.Cluster.CLUSTER
import edu.uci.ics.texera.web.model.jooq.generated.tables.daos.ClusterDao
import edu.uci.ics.texera.web.resource.dashboard.user.cluster.ClusterResource.{
  ERR_USER_HAS_NO_ACCESS_TO_CLUSTER_MESSAGE,
  clusterDao,
  context
}
import io.dropwizard.auth.Auth
import org.glassfish.jersey.media.multipart.FormDataParam
import edu.uci.ics.texera.web.model.jooq.generated.tables.pojos.Cluster
import edu.uci.ics.texera.web.resource.dashboard.user.cluster.ClusterServiceClient.{
  callCreateClusterAPI,
  callDeleteClusterAPI
}
import edu.uci.ics.texera.web.resource.dashboard.user.cluster.ClusterUtils.{
  updateClusterActivityEndTime,
  updateClusterStatus
}

import java.util
import javax.annotation.security.RolesAllowed
import javax.ws.rs.{Consumes, ForbiddenException, GET, POST, Path}
import javax.ws.rs.core.{MediaType, Response}

object ClusterResource {
  final private lazy val context = SqlServer.createDSLContext()
  final private lazy val clusterDao = new ClusterDao(context.configuration)

  // error messages
  val ERR_USER_HAS_NO_ACCESS_TO_CLUSTER_MESSAGE = "User has no access to this cluster"
}

@RolesAllowed(Array("REGULAR", "ADMIN"))
@Path("/cluster")
class ClusterResource {

  /**
    * Launches a new cluster and records the start time in cluster_activity.
    *
    * @param user The authenticated user creating the cluster.
    * @param name The name of the cluster.
    * @param machineType The type of machines in the cluster.
    * @param numberOfMachines The number of machines in the cluster.
    * @return The created Cluster object.
    */
  @POST
  @Path("/launch")
  @Consumes(Array(MediaType.MULTIPART_FORM_DATA))
  def launchCluster(
      @Auth user: SessionUser,
      @FormDataParam("Name") name: String,
      @FormDataParam("machineType") machineType: String,
      @FormDataParam("numberOfMachines") numberOfMachines: Integer
  ): Response = {
    val cluster = new Cluster()
    cluster.setName(name)
    cluster.setOwnerId(user.getUid)
    cluster.setMachineType(machineType)
    cluster.setNumberOfMachines(numberOfMachines)
    cluster.setStatus(ClusterStatus.LAUNCH_RECEIVED)
    clusterDao.insert(cluster)

    // Call Go microservice to actually create the cluster
    callCreateClusterAPI(cluster.getCid, machineType, numberOfMachines) match {
      case Right(goResponse) =>
        updateClusterStatus(cluster.getCid, ClusterStatus.PENDING, context)
        Response.ok(clusterDao.fetchOneByCid(cluster.getCid)).build()

      case Left(errorMessage) =>
        updateClusterStatus(cluster.getCid, ClusterStatus.LAUNCH_FAILED, context)
        Response
          .status(Response.Status.INTERNAL_SERVER_ERROR)
          .entity(s"Cluster creation failed: $errorMessage")
          .build()
    }
  }

  /**
    * Terminates a cluster and records the termination time in cluster_activity.
    *
    * @param user The authenticated user requesting the deletion.
    * @param cluster The cluster to be deleted.
    * @return A Response indicating the result of the operation.
    */
  @POST
  @Path("/terminate")
  def terminateCluster(@Auth user: SessionUser, cluster: Cluster): Response = {
    val clusterId = cluster.getCid
    validateClusterOwnership(user, clusterId)

    updateClusterStatus(clusterId, ClusterStatus.TERMINATE_RECEIVED, context)

    // Call Go microservice to actually delete the cluster
    callDeleteClusterAPI(clusterId) match {
      case Right(goResponse) =>
        updateClusterStatus(clusterId, ClusterStatus.SHUTTING_DOWN, context)
        Response.ok(goResponse).build()

      case Left(errorMessage) =>
        updateClusterStatus(
          clusterId,
          ClusterStatus.TERMINATE_FAILED,
          context
        )
        Response
          .status(Response.Status.INTERNAL_SERVER_ERROR)
          .entity(s"Cluster deletion failed: $errorMessage")
          .build()
    }
  }

  /**
    * Stops a cluster and records the pause time in cluster_activity.
    *
    * @param user The authenticated user requesting the pause.
    * @param cluster The cluster to be paused.
    * @return A Response indicating the result of the operation.
    */
  @POST
  @Path("/stop")
  def stopCluster(@Auth user: SessionUser, cluster: Cluster): Response = {
    validateClusterOwnership(user, cluster.getCid)

    updateClusterStatus(cluster.getCid, ClusterStatus.STOP_RECEIVED, context)

    // TODO: Call the Go Microservice
    updateClusterStatus(cluster.getCid, ClusterStatus.STOPPING, context)
    // TODO: need to consider if the pause fails

    updateClusterStatus(cluster.getCid, ClusterStatus.STOPPED, context)

    updateClusterActivityEndTime(cluster.getCid, context)

    Response.ok().build()
  }

  /**
    * Starts a stopped cluster and records the resume time in cluster_activity.
    *
    * @param user The authenticated user requesting the resume.
    * @param cluster The cluster to be resumed.
    * @return A Response indicating the result of the operation.
    */
  @POST
  @Path("/start")
  def startCluster(@Auth user: SessionUser, cluster: Cluster): Response = {
    validateClusterOwnership(user, cluster.getCid)

    updateClusterStatus(cluster.getCid, ClusterStatus.START_RECEIVED, context)

    // TODO: Call the Go Microservice
    updateClusterStatus(cluster.getCid, ClusterStatus.PENDING, context)
    // TODO: need to consider if the resume fails

    updateClusterStatus(cluster.getCid, ClusterStatus.RUNNING, context)

    Response.ok().build()
  }

  /**
    * Updates the name of a cluster.
    *
    * @param user The authenticated user requesting the update.
    * @param cluster The cluster with the new name.
    * @return A Response indicating the result of the operation.
    */
  @POST
  @Consumes(Array(MediaType.APPLICATION_JSON))
  @Path("/update/name")
  def updateClusterName(@Auth user: SessionUser, cluster: Cluster): Response = {
    validateClusterOwnership(user, cluster.getCid)

    context
      .update(CLUSTER)
      .set(CLUSTER.NAME, cluster.getName)
      .where(CLUSTER.CID.eq(cluster.getCid))
      .execute()

    Response.ok().build()
  }

  /**
    * Lists all clusters owned by the authenticated user.
    *
    * @param user The authenticated user.
    * @return A list of Clusters owned by the user.
    */
  @GET
  @Path("")
  def listClusters(@Auth user: SessionUser): util.List[Cluster] = {
    clusterDao.fetchByOwnerId(user.getUid)
    context
      .select(CLUSTER.asterisk())
      .from(CLUSTER)
      .where(CLUSTER.OWNER_ID.eq(user.getUid))
      .and(CLUSTER.STATUS.ne(ClusterStatus.TERMINATED))
      .fetchInto(classOf[Cluster])
  }

  /**
    * Validates that the authenticated user has ownership of the cluster.
    *
    * @param user The authenticated user.
    * @param clusterId The ID of the cluster to validate ownership.
    */
  private def validateClusterOwnership(user: SessionUser, clusterId: Int): Unit = {
    val clusterOwnerId = clusterDao.fetchOneByCid(clusterId).getOwnerId
    if (clusterOwnerId != user.getUid) {
      throw new ForbiddenException(ERR_USER_HAS_NO_ACCESS_TO_CLUSTER_MESSAGE)
    }
  }
}
