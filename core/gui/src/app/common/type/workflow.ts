import { WorkflowMetadata } from "../../dashboard/type/workflow-metadata.interface";
import { OperatorLink, OperatorPredicate, Point, CommentBox } from "../../workspace/types/workflow-common.interface";

export interface WorkflowSettings {
  dataTransferBatchSize: number;
}

/**
 * WorkflowContent is used to store the information of the workflow
 *  1. all existing operators and their properties
 *  2. operator's position on the JointJS paper
 *  3. operator link predicates
 *
 * When the user refreshes the browser, the CachedWorkflow interface will be
 *  automatically cached and loaded once the refresh completes. This information
 *  will then be used to reload the entire workflow.
 *
 */

export interface WorkflowContent
  extends Readonly<{
    operators: OperatorPredicate[];
    operatorPositions: { [key: string]: Point };
    links: OperatorLink[];
    commentBoxes: CommentBox[];
    settings: WorkflowSettings;
  }> {}

export type Workflow = { content: WorkflowContent } & WorkflowMetadata;
