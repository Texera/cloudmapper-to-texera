import { TestBed, fakeAsync, tick } from "@angular/core/testing";
import { WorkflowResultExportService } from "./workflow-result-export.service";
import { HttpClientTestingModule } from "@angular/common/http/testing";
import { WorkflowWebsocketService } from "../workflow-websocket/workflow-websocket.service";
import { WorkflowActionService } from "../workflow-graph/model/workflow-action.service";
import { NotificationService } from "../../../common/service/notification/notification.service";
import { ExecuteWorkflowService } from "../execute-workflow/execute-workflow.service";
import {
  WorkflowResultService,
  OperatorPaginationResultService,
  OperatorResultService,
} from "../workflow-result/workflow-result.service";
import { FileSaverService } from "../../../dashboard/service/user/file/file-saver.service";
import { of, EMPTY } from "rxjs";
import { PaginatedResultEvent } from "../../types/workflow-websocket.interface";
import { ExecutionState } from "../../types/execute-workflow.interface";
import * as JSZip from "jszip";
import { DownloadService } from "src/app/dashboard/service/user/download/download.service";

describe("WorkflowResultExportService", () => {
  let service: WorkflowResultExportService;
  let workflowWebsocketServiceSpy: jasmine.SpyObj<WorkflowWebsocketService>;
  let workflowActionServiceSpy: jasmine.SpyObj<WorkflowActionService>;
  let notificationServiceSpy: jasmine.SpyObj<NotificationService>;
  let executeWorkflowServiceSpy: jasmine.SpyObj<ExecuteWorkflowService>;
  let workflowResultServiceSpy: jasmine.SpyObj<WorkflowResultService>;
  let downloadServiceSpy: jasmine.SpyObj<DownloadService>;

  let jointGraphWrapperSpy: jasmine.SpyObj<any>;
  let texeraGraphSpy: jasmine.SpyObj<any>;

  beforeEach(() => {
    // Create spies for the required services
    jointGraphWrapperSpy = jasmine.createSpyObj("JointGraphWrapper", [
      "getCurrentHighlightedOperatorIDs",
      "getJointOperatorHighlightStream",
      "getJointOperatorUnhighlightStream",
    ]);
    jointGraphWrapperSpy.getCurrentHighlightedOperatorIDs.and.returnValue([]);
    jointGraphWrapperSpy.getJointOperatorHighlightStream.and.returnValue(of());
    jointGraphWrapperSpy.getJointOperatorUnhighlightStream.and.returnValue(of());

    texeraGraphSpy = jasmine.createSpyObj("TexeraGraph", ["getAllOperators"]);
    texeraGraphSpy.getAllOperators.and.returnValue([]);

    const wsSpy = jasmine.createSpyObj("WorkflowWebsocketService", ["subscribeToEvent", "send"]);
    wsSpy.subscribeToEvent.and.returnValue(of()); // Return an empty observable
    const waSpy = jasmine.createSpyObj("WorkflowActionService", [
      "getJointGraphWrapper",
      "getTexeraGraph",
      "getWorkflow",
    ]);
    waSpy.getJointGraphWrapper.and.returnValue(jointGraphWrapperSpy);
    waSpy.getTexeraGraph.and.returnValue(texeraGraphSpy);
    waSpy.getWorkflow.and.returnValue({ wid: "workflow1", name: "Test Workflow" });

    const ntSpy = jasmine.createSpyObj("NotificationService", ["success", "error", "loading"]);
    const ewSpy = jasmine.createSpyObj("ExecuteWorkflowService", ["getExecutionStateStream", "getExecutionState"]);
    ewSpy.getExecutionStateStream.and.returnValue(of({ previous: {}, current: { state: ExecutionState.Completed } }));
    ewSpy.getExecutionState.and.returnValue({ state: ExecutionState.Completed });

    const wrSpy = jasmine.createSpyObj("WorkflowResultService", [
      "hasAnyResult",
      "getResultService",
      "getPaginatedResultService",
    ]);
    const downloadSpy = jasmine.createSpyObj("DownloadService", ["downloadOperatorsResult"]);
    downloadSpy.downloadOperatorsResult.and.returnValue(of(new Blob()));

    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [
        WorkflowResultExportService,
        { provide: WorkflowWebsocketService, useValue: wsSpy },
        { provide: WorkflowActionService, useValue: waSpy },
        { provide: NotificationService, useValue: ntSpy },
        { provide: ExecuteWorkflowService, useValue: ewSpy },
        { provide: WorkflowResultService, useValue: wrSpy },
        { provide: DownloadService, useValue: downloadSpy },
      ],
    });

    // Inject the service and spies
    service = TestBed.inject(WorkflowResultExportService);
    workflowWebsocketServiceSpy = TestBed.inject(WorkflowWebsocketService) as jasmine.SpyObj<WorkflowWebsocketService>;
    workflowActionServiceSpy = TestBed.inject(WorkflowActionService) as jasmine.SpyObj<WorkflowActionService>;
    notificationServiceSpy = TestBed.inject(NotificationService) as jasmine.SpyObj<NotificationService>;
    executeWorkflowServiceSpy = TestBed.inject(ExecuteWorkflowService) as jasmine.SpyObj<ExecuteWorkflowService>;
    workflowResultServiceSpy = TestBed.inject(WorkflowResultService) as jasmine.SpyObj<WorkflowResultService>;
    downloadServiceSpy = TestBed.inject(DownloadService) as jasmine.SpyObj<DownloadService>;
  });

  it("should be created", () => {
    expect(service).toBeTruthy();
  });

  it("should export paginated results as CSV for highlighted operators", fakeAsync(() => {
    // Arrange
    jointGraphWrapperSpy.getCurrentHighlightedOperatorIDs.and.returnValue(["operator1"]);

    const paginatedResultServiceSpy = jasmine.createSpyObj("OperatorPaginationResultService", ["selectPage"]);

    // Mock the paginated result service for 'operator1'
    workflowResultServiceSpy.getPaginatedResultService.and.callFake(operatorId => {
      if (operatorId === "operator1") {
        return paginatedResultServiceSpy;
      }
      return undefined;
    });
    workflowResultServiceSpy.getResultService.and.returnValue(undefined);

    // Mock paginated results for multiple pages
    const paginatedResults: PaginatedResultEvent[] = [
      {
        requestID: "request1",
        operatorID: "operator1",
        pageIndex: 0,
        table: Array.from({ length: 10 }, (_, i) => ({ column1: `value${i}`, column2: `value${i}` })),
        schema: [
          { attributeName: "column1", attributeType: "string" },
          { attributeName: "column2", attributeType: "string" },
        ],
      },
      {
        requestID: "request1",
        operatorID: "operator1",
        pageIndex: 1,
        table: Array.from({ length: 10 }, (_, i) => ({ column1: `value${i + 10}`, column2: `value${i + 10}` })),
        schema: [
          { attributeName: "column1", attributeType: "string" },
          { attributeName: "column2", attributeType: "string" },
        ],
      },
      {
        requestID: "request1",
        operatorID: "operator1",
        pageIndex: 2,
        table: [{ column1: "value20", column2: "value20" }],
        schema: [
          { attributeName: "column1", attributeType: "string" },
          { attributeName: "column2", attributeType: "string" },
        ],
      },
    ];

    paginatedResultServiceSpy.selectPage.and.callFake((page: number, size: any) => {
      const index = page - 1;
      if (index < paginatedResults.length) {
        return of(paginatedResults[index]);
      } else {
        return EMPTY;
      }
    });

    // Act
    service.exportOperatorsResultToLocal(false);

    // Simulate asynchronous operations
    tick();

    // Assert
    expect(downloadServiceSpy.downloadOperatorsResult).toHaveBeenCalled();
    const args = downloadServiceSpy.downloadOperatorsResult.calls.mostRecent().args;
    expect(args[0]).toEqual(jasmine.any(Array));
    expect(args[1]).toEqual(jasmine.objectContaining({ wid: jasmine.any(String) }));

    const resultObservables = args[0];
    resultObservables[0].subscribe(files => {
      expect(files[0].filename).toBe("result_operator1.csv");
      expect(files[0].blob).toEqual(jasmine.any(Blob));
    });
  }));

  it("should export a single visualization result as an HTML file when there is only one result", done => {
    // Arrange
    jointGraphWrapperSpy.getCurrentHighlightedOperatorIDs.and.returnValue(["operator2"]);

    const resultServiceSpy = jasmine.createSpyObj("OperatorResultService", ["getCurrentResultSnapshot"]);

    // Mock the result service for 'operator2'
    workflowResultServiceSpy.getResultService.and.callFake(operatorId => {
      if (operatorId === "operator2") {
        return resultServiceSpy;
      }
      return undefined;
    });
    workflowResultServiceSpy.getPaginatedResultService.and.returnValue(undefined);

    // Mock the result snapshot with one result
    const resultSnapshot = [{ "html-content": "<html><body><p>Visualization</p></body></html>" }];

    resultServiceSpy.getCurrentResultSnapshot.and.returnValue(resultSnapshot);

    downloadServiceSpy.downloadOperatorsResult.and.returnValue(of(new Blob()));

    // Act
    service.exportOperatorsResultToLocal(false);

    expect(downloadServiceSpy.downloadOperatorsResult).toHaveBeenCalled();
    const args = downloadServiceSpy.downloadOperatorsResult.calls.mostRecent().args;
    expect(args[0]).toEqual(jasmine.any(Array));
    expect(args[1]).toEqual(jasmine.objectContaining({ wid: jasmine.any(String) }));

    const resultObservables = args[0];
    resultObservables[0].subscribe(files => {
      expect(files[0].filename).toBe("result_operator2_1.html");
      expect(files[0].blob).toEqual(jasmine.any(Blob));

      const reader = new FileReader();
      reader.onload = () => {
        const content = reader.result as string;
        expect(content).toBe("<html><body><p>Visualization</p></body></html>");
        done();
      };
      reader.readAsText(files[0].blob);
    });
  });

  it("should export multiple visualization results as a zip file when there are multiple results", done => {
    // Arrange
    jointGraphWrapperSpy.getCurrentHighlightedOperatorIDs.and.returnValue(["operator2"]);

    const resultServiceSpy = jasmine.createSpyObj("OperatorResultService", ["getCurrentResultSnapshot"]);

    // Mock the result service for 'operator2'
    workflowResultServiceSpy.getResultService.and.callFake(operatorId => {
      if (operatorId === "operator2") {
        return resultServiceSpy;
      }
      return undefined;
    });
    workflowResultServiceSpy.getPaginatedResultService.and.returnValue(undefined);

    // Mock the result snapshot with multiple results
    const resultSnapshot = [
      { "html-content": "<html><body><p>Visualization 1</p></body></html>" },
      { "html-content": "<html><body><p>Visualization 2</p></body></html>" },
    ];

    resultServiceSpy.getCurrentResultSnapshot.and.returnValue(resultSnapshot);

    // Spy on the 'downloadOperatorsResult' method
    downloadServiceSpy.downloadOperatorsResult.and.returnValue(of(new Blob()));

    // Act
    service.exportOperatorsResultToLocal(false);

    // Assert
    expect(downloadServiceSpy.downloadOperatorsResult).toHaveBeenCalled();
    const args = downloadServiceSpy.downloadOperatorsResult.calls.mostRecent().args;
    expect(args[0]).toEqual(jasmine.any(Array)); // Check if the first argument is an array
    expect(args[1]).toEqual(jasmine.objectContaining({ wid: jasmine.any(String) })); // Check if the second argument is a workflow object

    // Check the content of the observables
    const resultObservables = args[0];
    resultObservables[0].subscribe(files => {
      expect(files[0].filename).toBe("result_operator2_1.html");
      expect(files[0].blob).toEqual(jasmine.any(Blob));

      const reader = new FileReader();
      reader.onload = () => {
        const content = reader.result as string;
        expect(content).toBe("<html><body><p>Visualization 1</p></body></html>");
        done();
      };
      reader.readAsText(files[0].blob);
    });
  });
});
