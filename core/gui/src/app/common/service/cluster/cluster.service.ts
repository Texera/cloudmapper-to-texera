import { Injectable } from "@angular/core";
import { HttpClient } from "@angular/common/http";
import { Observable } from "rxjs";
import { Clusters } from "../../../dashboard/type/clusters";
import { AppSettings } from "../../app-setting";

@Injectable({
  providedIn: "root",
})
export class ClusterService {
  public CLUSTER_BASE_URL = "cluster";
  public CLUSTER_CREATE_URL = this.CLUSTER_BASE_URL + "/launch";
  public CLUSTER_DELETE_URL = this.CLUSTER_BASE_URL + "/terminate";
  public CLUSTER_PAUSE_URL = this.CLUSTER_BASE_URL + "/stop";
  public CLUSTER_RESUME_URL = this.CLUSTER_BASE_URL + "/start";
  public CLUSTER_UPDATE_URL = this.CLUSTER_BASE_URL + "/update/name";

  constructor(private http: HttpClient) {}

  getClusters(): Observable<Clusters[]> {
    return this.http.get<Clusters[]>(`${AppSettings.getApiEndpoint()}/${this.CLUSTER_BASE_URL}`, {});
  }

  launchCluster(formData: FormData): Observable<Response> {
    return this.http.post<Response>(`${AppSettings.getApiEndpoint()}/${this.CLUSTER_CREATE_URL}`, formData);
  }

  terminateCluster(cluster: Clusters): Observable<any> {
    return this.http.post(`${AppSettings.getApiEndpoint()}/${this.CLUSTER_DELETE_URL}`, cluster);
  }

  stopCluster(cluster: Clusters): Observable<Response> {
    return this.http.post<Response>(`${AppSettings.getApiEndpoint()}/${this.CLUSTER_PAUSE_URL}`, cluster);
  }

  startCluster(cluster: Clusters): Observable<Response> {
    return this.http.post<Response>(`${AppSettings.getApiEndpoint()}/${this.CLUSTER_RESUME_URL}`, cluster);
  }

  updateCluster(cluster: Clusters): Observable<Response> {
    return this.http.post<Response>(`${AppSettings.getApiEndpoint()}/${this.CLUSTER_UPDATE_URL}`, cluster);
  }
}
