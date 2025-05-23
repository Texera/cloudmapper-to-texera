/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import { Component, inject, OnInit } from "@angular/core";
import { NZ_MODAL_DATA, NzModalRef } from "ng-zorro-antd/modal";
import { UntilDestroy, untilDestroyed } from "@ngneat/until-destroy";
import { DatasetFileNode } from "../../../common/type/datasetVersionFileTree";
import { DatasetVersion } from "../../../common/type/dataset";
import { DashboardDataset } from "../../../dashboard/type/dashboard-dataset.interface";
import { DatasetService } from "../../../dashboard/service/user/dataset/dataset.service";
import { parseFilePathToDatasetFile } from "../../../common/type/dataset-file";

@UntilDestroy()
@Component({
  selector: "texera-file-selection-model",
  templateUrl: "file-selection.component.html",
  styleUrls: ["file-selection.component.scss"],
})
export class FileSelectionComponent implements OnInit {
  readonly selectedFilePath: string = inject(NZ_MODAL_DATA).selectedFilePath;
  private _datasets: ReadonlyArray<DashboardDataset> = [];

  // indicate whether the accessible datasets have been loaded from the backend
  isAccessibleDatasetsLoading = true;

  selectedDataset?: DashboardDataset;
  selectedVersion?: DatasetVersion;
  datasetVersions?: DatasetVersion[];
  suggestedFileTreeNodes: DatasetFileNode[] = [];
  isDatasetSelected: boolean = false;

  constructor(
    private modalRef: NzModalRef,
    private datasetService: DatasetService
  ) {}

  extractVersionPath(currentDisplayedFileName: string): string {
    const pathParts = currentDisplayedFileName.split("/");

    return `/${pathParts[1]}/${pathParts[2]}/${pathParts[3]}`;
  }

  ngOnInit() {
    this.isAccessibleDatasetsLoading = true;

    // retrieve all the accessible datasets from the backend
    this.datasetService
      .retrieveAccessibleDatasets()
      .pipe(untilDestroyed(this))
      .subscribe(datasets => {
        this._datasets = datasets;
        this.isAccessibleDatasetsLoading = false;
        if (!this.selectedFilePath || this.selectedFilePath == "") {
          return;
        }
        // if users already select some file, then ONLY show that selected dataset & related version
        const selectedDatasetFile = parseFilePathToDatasetFile(this.selectedFilePath);
        this.selectedDataset = this.datasets.find(
          d => d.ownerEmail === selectedDatasetFile.ownerEmail && d.dataset.name === selectedDatasetFile.datasetName
        );
        this.isDatasetSelected = !!this.selectedDataset;
        if (this.selectedDataset && this.selectedDataset.dataset.did !== undefined) {
          this.datasetService
            .retrieveDatasetVersionList(this.selectedDataset.dataset.did)
            .pipe(untilDestroyed(this))
            .subscribe(versions => {
              this.datasetVersions = versions;
              this.selectedVersion = this.datasetVersions.find(v => v.name === selectedDatasetFile.versionName);
              this.onVersionChange();
            });
        }
      });
  }

  onDatasetChange() {
    this.selectedVersion = undefined;
    this.suggestedFileTreeNodes = [];
    this.isDatasetSelected = !!this.selectedDataset;
    if (this.selectedDataset && this.selectedDataset.dataset.did !== undefined) {
      this.datasetService
        .retrieveDatasetVersionList(this.selectedDataset.dataset.did)
        .pipe(untilDestroyed(this))
        .subscribe(versions => {
          this.datasetVersions = versions;
          if (this.datasetVersions && this.datasetVersions.length > 0) {
            this.selectedVersion = this.datasetVersions[0];
            this.onVersionChange();
          }
        });
    }
  }

  onVersionChange() {
    this.suggestedFileTreeNodes = [];
    if (
      this.selectedDataset &&
      this.selectedDataset.dataset.did !== undefined &&
      this.selectedVersion &&
      this.selectedVersion.dvid !== undefined
    ) {
      this.datasetService
        .retrieveDatasetVersionFileTree(this.selectedDataset.dataset.did, this.selectedVersion.dvid)
        .pipe(untilDestroyed(this))
        .subscribe(data => {
          this.suggestedFileTreeNodes = data.fileNodes;
        });
    }
  }

  onFileTreeNodeSelected(node: DatasetFileNode) {
    this.modalRef.close(node);
  }

  get datasets(): ReadonlyArray<DashboardDataset> {
    return this._datasets;
  }
}
