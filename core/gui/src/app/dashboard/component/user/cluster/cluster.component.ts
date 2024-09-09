import { Component, OnInit } from "@angular/core";
import { Clusters } from "../../../type/clusters";
import { ClusterService } from "../../../../common/service/cluster/cluster.service";
import { Subscription } from "rxjs";
import { FormGroup } from "@angular/forms";
import { HttpErrorResponse } from "@angular/common/http";
import { UntilDestroy, untilDestroyed } from "@ngneat/until-destroy";

@UntilDestroy()
@Component({
  selector: "texera-cluster",
  templateUrl: "./cluster.component.html",
  styleUrls: ["./cluster.component.scss"],
})
export class ClusterComponent implements OnInit {
  private subscriptions: Subscription[] = [];
  private intervalId: any;
  isClusterManagementVisible = false;
  clusterList: Clusters[] = [];

  constructor(private clusterService: ClusterService) {}

  ngOnInit(): void {
    this.getClusters();
    this.startClusterPolling();
  }

  private startClusterPolling(): void {
    this.intervalId = setInterval(() => {
      this.getClusters();
    }, 1000);
  }

  private stopClusterPolling(): void {
    if (this.intervalId) {
      clearInterval(this.intervalId);
    }
  }

  getClusters() {
    this.clusterService
      .getClusters()
      .pipe(untilDestroyed(this))
      .subscribe(
        clusters => (this.clusterList = clusters || []),
        (error: HttpErrorResponse) => console.error("Error fetching clusters", error)
      );
  }

  launchCluster(formData: FormData) {
    this.clusterService
      .launchCluster(formData)
      .pipe(untilDestroyed(this))
      .subscribe(
        response => console.log("Response: ", response),
        (error: HttpErrorResponse) => console.error("Error fetching clusters", error)
      );
  }

  terminateCluster(cluster: Clusters): void {
    this.clusterService
      .terminateCluster(cluster)
      .pipe(untilDestroyed(this))
      .subscribe(
        response => console.log("Response: ", response),
        (error: HttpErrorResponse) => console.error("Error fetching clusters", error)
      );
  }

  stopCluster(cluster: Clusters): void {
    this.clusterService
      .stopCluster(cluster)
      .pipe(untilDestroyed(this))
      .subscribe(
        response => console.log("Response: ", response),
        (error: HttpErrorResponse) => console.error("Error fetching clusters", error)
      );
  }

  startCluster(cluster: Clusters): void {
    this.clusterService
      .startCluster(cluster)
      .pipe(untilDestroyed(this))
      .subscribe(
        response => console.log("Response: ", response),
        (error: HttpErrorResponse) => console.error("Error fetching clusters", error)
      );
  }

  updateCluster(cluster: Clusters): void {
    this.clusterService
      .updateCluster(cluster)
      .pipe(untilDestroyed(this))
      .subscribe(
        response => console.log("Response: ", response),
        (error: HttpErrorResponse) => console.error("Error fetching clusters", error)
      );
  }

  submitCluster(clusterForm: FormGroup): void {
    const formData = new FormData();
    formData.append("Name", clusterForm.value.Name);
    formData.append("machineType", clusterForm.value.machineType);
    formData.append("numberOfMachines", clusterForm.value.numberOfMachines);
    this.launchCluster(formData);
    this.closeClusterManagementModal();
  }

  openClusterManagementModal(): void {
    this.isClusterManagementVisible = true;
  }

  closeClusterManagementModal(): void {
    this.isClusterManagementVisible = false;
  }

  getBadgeStatus(status: string): string[]{
    switch(status){
      case "PENDING":
      case "STARTING":
        return ["play-circle", "orange"];
      case "STOPPING":
        return ["pause-circle", "orange"];
      case "RUNNING":
        return ["check-circle", "green"];
      case "STOPPED":
        return ["pause-circle", "gray"];
      case "SHUTTING_DOWN":
        return ["minus-circle", "orange"];
      case "TERMINATED":
      case "LAUNCH_FAILED":
      case "TERMINATE_FAILED":
      case "STOP_FAILED":
      case "START_FAILED":
        return ["minus-circle", "red"];
      case "LAUNCH_RECEIVED":
      case "TERMINATE_RECEIVED":
      case "STOP_RECEIVED":
      case "START_RECEIVED":
        return ["loading", "black"]
      default:
        return ["exclamation-circle", "gray"];
    }
  }
}
