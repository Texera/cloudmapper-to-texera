import { Component, OnInit, OnDestroy } from "@angular/core";
import { Clusters } from "../../../type/clusters";
import { ClusterService } from "../../../../common/service/cluster/cluster.service";
import { FormGroup } from "@angular/forms";
import { HttpErrorResponse } from "@angular/common/http";
import { UntilDestroy, untilDestroyed } from "@ngneat/until-destroy";
import { BehaviorSubject, Observable, timer } from "rxjs";
import { switchMap, distinctUntilChanged, map } from "rxjs/operators";

@UntilDestroy()
@Component({
  selector: "texera-cluster",
  templateUrl: "./cluster.component.html",
  styleUrls: ["./cluster.component.scss"],
})
export class ClusterComponent implements OnInit, OnDestroy {
  isClusterManagementVisible = false;
  clusterList$!: Observable<Clusters[]>;
  private refreshTrigger = new BehaviorSubject<void>(undefined);
  pageSize = 10;
  pageIndex = 1;

  constructor(private clusterService: ClusterService) {}

  ngOnInit(): void {
    this.setupClusterObservable();
    this.refreshClusters();
  }

  ngOnDestroy(): void {
    this.refreshTrigger.complete();
  }

  private setupClusterObservable(): void {
    this.clusterList$ = this.refreshTrigger.pipe(
      switchMap(() => timer(0, 5000)),
      switchMap(() => this.clusterService.getClusters()),
      map(clusters => clusters || []),
      distinctUntilChanged((prev, curr) => JSON.stringify(prev) === JSON.stringify(curr)),
      untilDestroyed(this)
    );
  }

  refreshClusters(): void {
    this.refreshTrigger.next();
  }

  terminateCluster(cluster: Clusters): void {
    this.clusterService
      .terminateCluster(cluster)
      .pipe(untilDestroyed(this))
      .subscribe(
        response => {
          console.log("Response: ", response);
          this.refreshClusters();
        },
        (error: HttpErrorResponse) => console.error("Error terminating cluster", error)
      );
  }

  stopCluster(cluster: Clusters): void {
    this.clusterService
      .stopCluster(cluster)
      .pipe(untilDestroyed(this))
      .subscribe(
        response => {
          console.log("Response: ", response);
          this.refreshClusters();
        },
        (error: HttpErrorResponse) => console.error("Error stopping cluster", error)
      );
  }

  startCluster(cluster: Clusters): void {
    this.clusterService
      .startCluster(cluster)
      .pipe(untilDestroyed(this))
      .subscribe(
        response => {
          console.log("Response: ", response);
          this.refreshClusters();
        },
        (error: HttpErrorResponse) => console.error("Error starting cluster", error)
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
    const clusterConfig = {
      name: clusterForm.value.Name,
      machineType: clusterForm.value.machineType,
      numberOfMachines: clusterForm.value.numberOfMachines,
    };

    this.clusterService
      .launchCluster(clusterConfig)
      .pipe(untilDestroyed(this))
      .subscribe(
        response => {
          console.log("Response: ", response);
          this.refreshClusters();
        },
        (error: HttpErrorResponse) => console.error("Error launching cluster", error)
      );
    this.closeClusterManagementModal();
  }

  openClusterManagementModal(): void {
    this.isClusterManagementVisible = true;
  }

  closeClusterManagementModal(): void {
    this.isClusterManagementVisible = false;
  }

  getBadgeStatus(status: string): string[] {
    switch (status) {
      case "PENDING":
      case "LAUNCH_RECEIVED":
      case "START_RECEIVED":
        return ["loading", "gold"];
      case "STOPPING":
      case "STOP_RECEIVED":
        return ["loading", "orange"];
      case "SHUTTING_DOWN":
      case "TERMINATE_RECEIVED":
        return ["loading", "red"];
      case "RUNNING":
        return ["check-circle", "green"];
      case "STOPPED":
      case "TERMINATED":
        return ["pause-circle", "grey"];
      case "LAUNCH_FAILED":
      case "TERMINATE_FAILED":
      case "STOP_FAILED":
      case "START_FAILED":
        return ["close-circle", "red"];
      default:
        return ["question-circle", "grey"];
    }
  }
  getMachineTypeInfo(machineType: string): string {
    const info: Record<string, string> = {
      "t2.micro":    "1 vCPU, 1 GB RAM, no NVMe, $0.0116/hr",
      "t3.large":    "2 vCPUs, 8 GB RAM, no NVMe, $0.0832/hr",
      "t3.xlarge":   "4 vCPUs, 16 GB RAM, no NVMe, $0.1664/hr",
      "t3.2xlarge":  "8 vCPUs, 32 GB RAM, no NVMe, $0.3328/hr",
      "c5d.2xlarge": "8 vCPUs, 16 GB RAM, 200 GB NVMe, $0.384/hr",
      "c5d.4xlarge": "16 vCPUs, 32 GB RAM, 400 GB NVMe, $0.768/hr",
      "c5d.9xlarge": "36 vCPUs, 72 GB RAM, 900 GB NVMe, $1.728/hr",
      "r5d.2xlarge": "8 vCPUs, 64 GB RAM, 300 GB NVMe, $0.576/hr",
      "r5d.4xlarge": "16 vCPUs, 128 GB RAM, 600 GB NVMe, $1.152/hr",
      "r5d.8xlarge": "32 vCPUs, 256 GB RAM, 1200 GB NVMe, $2.304/hr",
      "m5d.xlarge":  "4 vCPUs, 16 GB RAM, 150 GB NVMe, $0.226/hr",
      "m5d.2xlarge": "8 vCPUs, 32 GB RAM, 300 GB NVMe, $0.452/hr",
      "m5d.4xlarge": "16 vCPUs, 64 GB RAM, 600 GB NVMe, $0.904/hr",
    };
    return info[machineType] ?? "Information not available";
  }

  onPageIndexChange(index: number): void {
    this.pageIndex = index;
  }

  onPageSizeChange(size: number): void {
    this.pageSize = size;
    this.pageIndex = 1;
  }

  isActionDisabled(status: string): boolean {
    const disabledStatuses = [
      "SHUTTING_DOWN",
      "STOPPING",
      "PENDING",
      "LAUNCH_RECEIVED",
      "TERMINATE_RECEIVED",
      "STOP_RECEIVED",
      "START_RECEIVED",
      "LAUNCH_FAILED",
      "TERMINATE_FAILED",
      "STOP_FAILED",
      "START_FAILED",
    ];
    return disabledStatuses.includes(status);
  }
}
