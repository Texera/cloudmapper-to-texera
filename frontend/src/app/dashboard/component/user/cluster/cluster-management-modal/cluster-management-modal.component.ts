import { Component, Input, Output, EventEmitter } from "@angular/core";
import { FormBuilder, FormGroup, Validators } from "@angular/forms";

@Component({
  selector: "app-cluster-management-modal",
  templateUrl: "./cluster-management-modal.component.html",
  styleUrls: ["./cluster-management-modal.component.scss"],
})
export class ClusterManagementModalComponent {
  @Input() isVisible: boolean = false;
  @Output() closeModal = new EventEmitter<void>();
  @Output() submitClusterEvent = new EventEmitter<FormGroup>();
  clusterForm!: FormGroup;

  // Instance types grouped by storage tier.
  // NVMe instances are recommended for large genome indices (t-series have no
  // instance storage and rely entirely on EFS for index I/O).
  machineOptionGroups = [
    {
      label: "General Purpose — No local NVMe (dev / small genomes)",
      options: [
        { value: "t2.micro",   label: "t2.micro   —  1 vCPU,   1 GB RAM,  no NVMe,  $0.0116/hr" },
        { value: "t3.large",   label: "t3.large   —  2 vCPUs,  8 GB RAM,  no NVMe,  $0.0832/hr" },
        { value: "t3.xlarge",  label: "t3.xlarge  —  4 vCPUs, 16 GB RAM,  no NVMe,  $0.1664/hr" },
        { value: "t3.2xlarge", label: "t3.2xlarge —  8 vCPUs, 32 GB RAM,  no NVMe,  $0.3328/hr" },
      ],
    },
    {
      label: "Compute Optimised + NVMe (recommended for alignment)",
      options: [
        { value: "c5d.2xlarge", label: "c5d.2xlarge —  8 vCPUs,  16 GB RAM,  200 GB NVMe,  $0.384/hr" },
        { value: "c5d.4xlarge", label: "c5d.4xlarge — 16 vCPUs,  32 GB RAM,  400 GB NVMe,  $0.768/hr" },
        { value: "c5d.9xlarge", label: "c5d.9xlarge — 36 vCPUs,  72 GB RAM,  900 GB NVMe,  $1.728/hr" },
      ],
    },
    {
      label: "Memory Optimised + NVMe (large genomes / multi-sample)",
      options: [
        { value: "r5d.2xlarge", label: "r5d.2xlarge —  8 vCPUs,  64 GB RAM,  300 GB NVMe,  $0.576/hr" },
        { value: "r5d.4xlarge", label: "r5d.4xlarge — 16 vCPUs, 128 GB RAM,  600 GB NVMe,  $1.152/hr" },
        { value: "r5d.8xlarge", label: "r5d.8xlarge — 32 vCPUs, 256 GB RAM, 1200 GB NVMe,  $2.304/hr" },
      ],
    },
    {
      label: "Balanced + NVMe (general production workloads)",
      options: [
        { value: "m5d.xlarge",  label: "m5d.xlarge  —  4 vCPUs,  16 GB RAM,  150 GB NVMe,  $0.226/hr" },
        { value: "m5d.2xlarge", label: "m5d.2xlarge —  8 vCPUs,  32 GB RAM,  300 GB NVMe,  $0.452/hr" },
        { value: "m5d.4xlarge", label: "m5d.4xlarge — 16 vCPUs,  64 GB RAM,  600 GB NVMe,  $0.904/hr" },
      ],
    },
  ];

  machineNumbers = [1, 2, 3, 4, 5, 6, 7, 8];

  constructor(private fb: FormBuilder) {}

  ngOnInit(): void {
    this.clusterForm = this.fb.group({
      Name: [null, [Validators.required]],
      machineType: [null, [Validators.required]],
      numberOfMachines: [null, [Validators.required]],
    });
  }

  closeClusterManagementModal() {
    this.closeModal.emit();
  }

  submitCluster() {
    if (this.clusterForm.valid) {
      this.submitClusterEvent.emit(this.clusterForm);
    } else {
      Object.values(this.clusterForm.controls).forEach(control => {
        if (control.invalid) {
          control.markAsDirty();
          control.updateValueAndValidity({ onlySelf: true });
        }
      });
    }
  }
}
