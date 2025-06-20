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

import { Injectable } from "@angular/core";
import { WorkflowActionService } from "../workflow-graph/model/workflow-action.service";
import { isSink } from "../workflow-graph/model/workflow-graph";
import { BehaviorSubject, merge } from "rxjs";
import { CommentBox, OperatorLink, OperatorPredicate, Point } from "../../types/workflow-common.interface";
import { WorkflowUtilService } from "../workflow-graph/util/workflow-util.service";
import { NotificationService } from "src/app/common/service/notification/notification.service";
import { ExecuteWorkflowService } from "../execute-workflow/execute-workflow.service";

type OperatorPositions = {
  [key: string]: Point;
};

// this type associates the old link ID with the new link
type LinkWithID = {
  [key: string]: OperatorLink;
};

// This type represents what the serialized string in the clipboard should look like
type SerializedString = {
  operators: OperatorPredicate[];
  operatorPositions: OperatorPositions;
  links: OperatorLink[];
  commentBoxes: CommentBox[];
};

/**
 * This service provides shared state of menu options related to controlling an operator.
 * This menu state and operations are shared by
 *  - navigation menu
 *  - right-click menu
 *  - keyboard shortcuts
 */
@Injectable({
  providedIn: "root",
})
export class OperatorMenuService {
  public highlightedOperators = new BehaviorSubject([] as readonly string[]);
  public highlightedCommentBoxes = new BehaviorSubject([] as readonly string[]);

  // whether the disable-operator-button should be enabled
  public isDisableOperatorClickable: boolean = false;
  public isDisableOperator: boolean = true;

  public isToViewResult: boolean = false;
  public isToViewResultClickable: boolean = false;

  public isReuseResultClickable: boolean = false;
  public isMarkForReuse: boolean = true;

  public readonly COPY_OFFSET = 20;

  constructor(
    private workflowActionService: WorkflowActionService,
    private workflowUtilService: WorkflowUtilService,
    private notificationService: NotificationService,
    private executeWorkflowService: ExecuteWorkflowService
  ) {
    this.handleDisableOperatorStatusChange();
    this.handleViewResultOperatorStatusChange();
    this.handleReuseOperatorResultStatusChange();

    merge(
      this.workflowActionService.getJointGraphWrapper().getJointOperatorHighlightStream(),
      this.workflowActionService.getJointGraphWrapper().getJointOperatorUnhighlightStream(),
      this.workflowActionService.getJointGraphWrapper().getJointGroupHighlightStream(),
      this.workflowActionService.getJointGraphWrapper().getJointGroupUnhighlightStream()
    ).subscribe(() => {
      this.highlightedOperators.next(
        this.workflowActionService.getJointGraphWrapper().getCurrentHighlightedOperatorIDs()
      );
    });

    merge(
      this.workflowActionService.getJointGraphWrapper().getJointCommentBoxHighlightStream(),
      this.workflowActionService.getJointGraphWrapper().getJointCommentBoxUnhighlightStream()
    ).subscribe(() => {
      this.highlightedCommentBoxes.next(
        this.workflowActionService.getJointGraphWrapper().getCurrentHighlightedCommentBoxIDs()
      );
    });
  }

  /**
   * callback function when user clicks the "disable operator" icon:
   * this.isDisableOperator indicates whether the operators should be disabled or enabled
   */
  public disableHighlightedOperators(): void {
    if (this.isDisableOperator) {
      this.workflowActionService.disableOperators(this.highlightedOperators.value);
    } else {
      this.workflowActionService.enableOperators(this.highlightedOperators.value);
    }
  }

  public viewResultHighlightedOperators(): void {
    const effectiveHighlightedOperatorsExcludeSink = this.highlightedOperators.value.filter(
      op => !isSink(this.workflowActionService.getTexeraGraph().getOperator(op))
    );

    if (this.isToViewResult) {
      this.workflowActionService.setViewOperatorResults(effectiveHighlightedOperatorsExcludeSink);
    } else {
      this.workflowActionService.unsetViewOperatorResults(effectiveHighlightedOperatorsExcludeSink);
    }
  }

  public reuseResultHighlightedOperator(): void {
    const effectiveHighlightedOperatorsExcludeSink = this.highlightedOperators.value.filter(
      op => !isSink(this.workflowActionService.getTexeraGraph().getOperator(op))
    );

    if (this.isMarkForReuse) {
      this.workflowActionService.markReuseResults(effectiveHighlightedOperatorsExcludeSink);
    } else {
      this.workflowActionService.removeMarkReuseResults(effectiveHighlightedOperatorsExcludeSink);
    }
  }

  /**
   * Updates the status of the disable operator icon:
   * If all selected operators are disabled, then click it will re-enable the operators
   * If any of the selected operator is not disabled, then click will disable all selected operators
   */
  handleDisableOperatorStatusChange() {
    merge(
      this.highlightedOperators,
      this.workflowActionService.getTexeraGraph().getDisabledOperatorsChangedStream(),
      this.workflowActionService.getWorkflowModificationEnabledStream()
    ).subscribe(event => {
      const allDisabled = this.highlightedOperators.value.every(op =>
        this.workflowActionService.getTexeraGraph().isOperatorDisabled(op)
      );

      this.isDisableOperator = !allDisabled;
      this.isDisableOperatorClickable =
        this.highlightedOperators.value.length !== 0 && this.workflowActionService.checkWorkflowModificationEnabled();
    });
  }

  handleViewResultOperatorStatusChange() {
    merge(
      this.highlightedOperators,
      this.workflowActionService.getTexeraGraph().getViewResultOperatorsChangedStream(),
      this.workflowActionService.getWorkflowModificationEnabledStream()
    ).subscribe(event => {
      const effectiveHighlightedOperatorsExcludeSink = this.highlightedOperators.value.filter(
        op => !isSink(this.workflowActionService.getTexeraGraph().getOperator(op))
      );

      const allViewing = effectiveHighlightedOperatorsExcludeSink.every(op =>
        this.workflowActionService.getTexeraGraph().isViewingResult(op)
      );

      this.isToViewResult = !allViewing;
      this.isToViewResultClickable =
        effectiveHighlightedOperatorsExcludeSink.length !== 0 &&
        this.workflowActionService.checkWorkflowModificationEnabled();
    });
  }

  handleReuseOperatorResultStatusChange() {
    merge(
      this.highlightedOperators,
      this.workflowActionService.getTexeraGraph().getReuseCacheOperatorsChangedStream(),
      this.workflowActionService.getWorkflowModificationEnabledStream()
    ).subscribe(event => {
      const effectiveHighlightedOperatorsExcludeSink = this.highlightedOperators.value.filter(
        op => !isSink(this.workflowActionService.getTexeraGraph().getOperator(op))
      );

      const allMarkedForReuse = effectiveHighlightedOperatorsExcludeSink.every(op =>
        this.workflowActionService.getTexeraGraph().isMarkedForReuseResult(op)
      );

      this.isMarkForReuse = !allMarkedForReuse;
      this.isReuseResultClickable =
        effectiveHighlightedOperatorsExcludeSink.length !== 0 &&
        this.workflowActionService.checkWorkflowModificationEnabled();
    });
  }

  /**
   * saves highlighted elements to the system clipboard
   */
  public saveHighlightedElements(): void {
    // get all the currently selected operators and links
    const highlightedOperatorIDs = this.workflowActionService.getJointGraphWrapper().getCurrentHighlightedOperatorIDs();

    // initialize the serialized string
    const serializedString: SerializedString = {
      operators: [],
      operatorPositions: {},
      links: [],
      commentBoxes: [],
    };

    // define the copies that will be put in the serialized json string when copying
    const operatorsCopy: OperatorPredicate[] = [];
    const operatorPositionsCopy: OperatorPositions = {};
    const linksCopy: OperatorLink[] = [];
    const commentBoxesCopy: CommentBox[] = [];

    // fill in the operators copy with all the currently highlighted operators for sorting later (the original highlighted operator IDs is a readonly string array, so it can't be sorted)
    highlightedOperatorIDs.forEach(operatorID => {
      operatorsCopy.push(this.workflowActionService.getTexeraGraph().getOperator(operatorID));
    });

    // sort all the highlighted operators by their layer number
    operatorsCopy.sort(
      (first, second) =>
        this.workflowActionService.getJointGraphWrapper().getCellLayer(first.operatorID) -
        this.workflowActionService.getJointGraphWrapper().getCellLayer(second.operatorID)
    );

    operatorsCopy.forEach(op => {
      operatorPositionsCopy[op.operatorID] = this.workflowActionService
        .getJointGraphWrapper()
        .getElementPosition(op.operatorID);
    });

    serializedString.operators = operatorsCopy;
    serializedString.operatorPositions = operatorPositionsCopy;

    // get all the highlighted links, and sort them by their layers
    const highlighghtedLinkIDs = this.workflowActionService.getJointGraphWrapper().getCurrentHighlightedLinkIDs();
    highlighghtedLinkIDs.forEach(linkID => {
      linksCopy.push(this.workflowActionService.getTexeraGraph().getLinkWithID(linkID));
    });
    linksCopy.sort(
      (first, second) =>
        this.workflowActionService.getJointGraphWrapper().getCellLayer(first.linkID) -
        this.workflowActionService.getJointGraphWrapper().getCellLayer(second.linkID)
    );

    serializedString.links = linksCopy;

    //get all the highlighted comment boxes, and sort them by their layers
    const highlightedCommentBoxIDs = this.workflowActionService
      .getJointGraphWrapper()
      .getCurrentHighlightedCommentBoxIDs();
    highlightedCommentBoxIDs.forEach(commentBoxID => {
      commentBoxesCopy.push(this.workflowActionService.getTexeraGraph().getCommentBox(commentBoxID));
    });
    commentBoxesCopy.sort(
      (first, second) =>
        this.workflowActionService.getJointGraphWrapper().getCellLayer(first.commentBoxID) -
        this.workflowActionService.getJointGraphWrapper().getCellLayer(second.commentBoxID)
    );
    serializedString.commentBoxes = commentBoxesCopy;

    // store the stringified copied operators into the clipboard
    navigator.clipboard.writeText(JSON.stringify(serializedString)).catch(() => {
      // if the Promise returned from writeText rejects, it means the write to clipboard permission is not granted
      // although if the current tab is active, permission shouldn't be needed
      this.notificationService.error("Copy failed. You don't have the permission to write to the clipboard.");
    });
  }

  public executeUpToOperator() {
    // get the highlighted operatorId. This feature supports one and only one selected operator.
    const highlightedOperatorIds = this.workflowActionService.getJointGraphWrapper().getCurrentHighlightedOperatorIDs();
    if (highlightedOperatorIds.length !== 1) {
      this.notificationService.error("Can only execute to exactly one target operator.");
      return;
    }

    const targetOperatorId = highlightedOperatorIds[0];
    this.executeWorkflowService.executeWorkflow("", targetOperatorId);
  }

  public performPasteOperation() {
    // by reading from the clipboard, permission needs to be granted
    // a permission prompt automatically shows up by calling readText()
    navigator.clipboard.readText().then(
      text => {
        this.pasteFromText(text);
      },
      // if the Promise returned from readText rejects, the read clipboard permission is not granted, and we send a warning to the user
      () => {
        this.notificationService.error("Paste failed. This site has been blocked from reading the clipboard.");
      }
    );
  }

  public pasteFromText(text: string) {
    try {
      // convert the JSON string in the system clipboard to a JS Map
      var elementsInClipboard: Map<string, any> = new Map(Object.entries(JSON.parse(text)));
      // check if the fields in a normal serialized string exist after converting the JSON string
      // if not, throw an error, which is propagated and produces an alert for the user
      if (
        !elementsInClipboard.has("operators") &&
        !elementsInClipboard.has("operatorPositions") &&
        !elementsInClipboard.has("links") &&
        !elementsInClipboard.has("groups") &&
        !elementsInClipboard.has("commentBoxes")
      ) {
        throw new Error("You haven't copied any element yet.");
      }
    } catch (e) {
      // if the text in the clipboard is not a JSON object, then it means the user hasn't copied an element
      this.notificationService.error("You haven't copied any element yet.");
      return;
    }

    // define the arguments required for actually adding operators and links
    const operatorsAndPositions: { op: OperatorPredicate; pos: Point }[] = [];
    const positions: Point[] = [];
    // calling get() will give either the value or undefined
    // at this point, after checking the existence of fields in the operators in the clipboard,
    // the fields "links" and "operatorPositions" should exist
    const linksInClipboard: OperatorLink[] = elementsInClipboard.get("links") as OperatorLink[];
    const operatorPositionsInClipboard: OperatorPositions = elementsInClipboard.get(
      "operatorPositions"
    ) as OperatorPositions;
    // get all the operators from the clipboard, which are already sorted by their layers
    let copiedOps: OperatorPredicate[] = elementsInClipboard.get("operators") as OperatorPredicate[];

    let linksCopy: LinkWithID = {};
    copiedOps.forEach(copiedOperator => {
      // copyOperator assigns a new randomly generated operator ID to the new operator
      const newOperator = this.copyOperator(copiedOperator);

      for (let link of linksInClipboard) {
        if (linksCopy[link.linkID] === undefined) {
          const newLinkID = this.workflowUtilService.getLinkRandomUUID();
          linksCopy[link.linkID] = {
            linkID: newLinkID,
            source: { operatorID: "", portID: "" },
            target: { operatorID: "", portID: "" },
          };
        }

        if (link.source.operatorID === copiedOperator.operatorID) {
          // if current copied operator is the source operator of current link, we assign the new operator ID to be the source operator for the current link, and the port ID should remain unchanged
          const source = {
            operatorID: newOperator.operatorID,
            portID: link.source.portID,
          };
          const originalLinkProperties = linksCopy[link.linkID];
          linksCopy[link.linkID] = {
            ...originalLinkProperties,
            source: source,
          };
        } else if (link.target.operatorID === copiedOperator.operatorID) {
          // if current copied operator is the target operator of current link, we assign the new operator ID to be the target operator for the current link, and the port ID should remain unchanged
          const target = {
            operatorID: newOperator.operatorID,
            portID: link.target.portID,
          };
          const originalLinkProperties = linksCopy[link.linkID];
          linksCopy[link.linkID] = {
            ...originalLinkProperties,
            target: target,
          };
        }
      }

      const position: Point = operatorPositionsInClipboard[copiedOperator.operatorID] as Point;
      positions.push(position);
      // calculate the new positions for the pasted operators
      const newOperatorPosition = this.calcOperatorPosition(position, positions);
      operatorsAndPositions.push({
        op: newOperator,
        pos: newOperatorPosition,
      });
      positions.push(newOperatorPosition);
    });

    const links = Object.values(linksCopy);

    // actually add all operators and links to the workflow
    try {
      this.workflowActionService.addOperatorsAndLinks(operatorsAndPositions, links);
    } catch (e) {
      this.notificationService.info(
        "Some of the links that you selected don't have operators attached to both ends of them. These links won't be pasted, since links can't exist without operators."
      );
    }

    //add copied comment boxes and calculate new positions for the pasted comment boxes
    let commentBoxesCopy: CommentBox[] = elementsInClipboard.get("commentBoxes") as CommentBox[];
    commentBoxesCopy.forEach(commentBoxCopy => {
      const commentBoxPosition: Point = commentBoxCopy.commentBoxPosition as Point;
      positions.push(commentBoxPosition);
      const newCommentBoxPosition = this.calcOperatorPosition(commentBoxPosition, positions);
      positions.push(newCommentBoxPosition);
      const newCommentBoxID = this.workflowUtilService.getCommentBoxRandomUUID();
      const newCommentBox: CommentBox = {
        commentBoxID: newCommentBoxID,
        comments: commentBoxCopy.comments,
        commentBoxPosition: newCommentBoxPosition,
      };
      this.workflowActionService.addCommentBox(newCommentBox);
    });
  }

  /**
   * Utility function to create a new operator that contains same
   * info as the copied operator.
   * @param operator
   */
  private copyOperator(operator: OperatorPredicate): OperatorPredicate {
    return {
      ...operator,
      operatorID: operator.operatorType + "-" + this.workflowUtilService.getOperatorRandomUUID(),
    };
  }

  /**
   * Utility function to calculate the position to paste the operator.
   * If a previously pasted operator is moved or deleted, the operator will be
   * pasted to the emptied position. Otherwise, it will be pasted to a position
   * that's non-overlapping and calculated according to the copy operator offset.
   * @param pos
   * @param positions
   */
  private calcOperatorPosition(pos: Point, positions: Point[]): Point {
    const position = {
      x: pos.x + this.COPY_OFFSET,
      y: pos.y + this.COPY_OFFSET,
    };
    return this.getNonOverlappingPosition(position, positions);
  }

  /**
   * Utility function to find a non-overlapping position for the pasted operator.
   * The function will check if the current position overlaps with an existing
   * operator. If it does, the function will find a new non-overlapping position.
   * @param position
   * @param positions
   */
  private getNonOverlappingPosition(position: Point, positions: Point[]): Point {
    let overlapped = false;
    const operatorPositions = positions.concat(
      this.workflowActionService
        .getTexeraGraph()
        .getAllCommentBoxes()
        .map(CommentBox => CommentBox.commentBoxPosition)
    );
    do {
      for (const operatorPosition of operatorPositions) {
        if (operatorPosition.x === position.x && operatorPosition.y === position.y) {
          position = {
            x: position.x + this.COPY_OFFSET,
            y: position.y + this.COPY_OFFSET,
          };
          overlapped = true;
          break;
        }
        overlapped = false;
      }
    } while (overlapped);
    return position;
  }
}
