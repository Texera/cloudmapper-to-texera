/*
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

package edu.uci.ics.texera.web.resource.dashboard

import edu.uci.ics.texera.dao.jooq.generated.Tables._
import edu.uci.ics.texera.dao.jooq.generated.tables.pojos.Workflow
import edu.uci.ics.texera.web.resource.dashboard.DashboardResource.DashboardClickableFileEntry
import edu.uci.ics.texera.web.resource.dashboard.FulltextSearchQueryUtils._
import edu.uci.ics.texera.web.resource.dashboard.user.workflow.WorkflowResource.DashboardWorkflow
import org.jooq.impl.DSL
import org.jooq.impl.DSL.groupConcatDistinct
import org.jooq.{Condition, GroupField, Record, TableLike}

import scala.jdk.CollectionConverters.CollectionHasAsScala

object WorkflowSearchQueryBuilder extends SearchQueryBuilder {

  override val mappedResourceSchema: UnifiedResourceSchema = {
    UnifiedResourceSchema(
      resourceType = DSL.inline(SearchQueryBuilder.WORKFLOW_RESOURCE_TYPE),
      name = WORKFLOW.NAME,
      description = WORKFLOW.DESCRIPTION,
      creationTime = WORKFLOW.CREATION_TIME,
      wid = WORKFLOW.WID,
      lastModifiedTime = WORKFLOW.LAST_MODIFIED_TIME,
      workflowUserAccess = WORKFLOW_USER_ACCESS.PRIVILEGE,
      uid = WORKFLOW_OF_USER.UID,
      ownerId = WORKFLOW_OF_USER.UID,
      userName = USER.NAME,
      projectsOfWorkflow = groupConcatDistinct(WORKFLOW_OF_PROJECT.PID)
    )
  }

  override protected def constructFromClause(
      uid: Integer,
      params: DashboardResource.SearchQueryParams,
      includePublic: Boolean = false
  ): TableLike[_] = {
    val baseQuery = WORKFLOW
      .leftJoin(WORKFLOW_USER_ACCESS)
      .on(WORKFLOW_USER_ACCESS.WID.eq(WORKFLOW.WID))
      .leftJoin(WORKFLOW_OF_USER)
      .on(WORKFLOW_OF_USER.WID.eq(WORKFLOW.WID))
      .leftJoin(USER)
      .on(USER.UID.eq(WORKFLOW_OF_USER.UID))
      .leftJoin(WORKFLOW_OF_PROJECT)
      .on(WORKFLOW_OF_PROJECT.WID.eq(WORKFLOW.WID))
      .leftJoin(PROJECT_USER_ACCESS)
      .on(PROJECT_USER_ACCESS.PID.eq(WORKFLOW_OF_PROJECT.PID))

    var condition: Condition = DSL.trueCondition()
    if (uid == null) {
      condition = WORKFLOW.IS_PUBLIC.eq(true)
    } else {
      val privateAccessCondition =
        WORKFLOW_USER_ACCESS.UID.eq(uid).or(PROJECT_USER_ACCESS.UID.eq(uid))
      if (includePublic) {
        condition = privateAccessCondition.or(WORKFLOW.IS_PUBLIC.eq(true))
      } else {
        condition = privateAccessCondition
      }
    }

    baseQuery.where(condition)
  }

  override protected def constructWhereClause(
      uid: Integer,
      params: DashboardResource.SearchQueryParams
  ): Condition = {
    val splitKeywords = params.keywords.asScala
      .flatMap(_.split("[+\\-()<>~*@\"]"))
      .filter(_.nonEmpty)
      .toSeq
    getDateFilter(
      params.creationStartDate,
      params.creationEndDate,
      WORKFLOW.CREATION_TIME
    )
      // Apply lastModified_time date filter
      .and(
        getDateFilter(
          params.modifiedStartDate,
          params.modifiedEndDate,
          WORKFLOW.LAST_MODIFIED_TIME
        )
      )
      // Apply workflowID filter
      .and(getContainsFilter(params.workflowIDs, WORKFLOW.WID))
      // Apply owner filter
      .and(getContainsFilter(params.owners, USER.EMAIL))
      // Apply operators filter
      .and(getOperatorsFilter(params.operators, WORKFLOW.CONTENT))
      // Apply projectId filter
      .and(getContainsFilter(params.projectIds, WORKFLOW_OF_PROJECT.PID))
      // Apply fulltext search filter
      .and(
        getFullTextSearchFilter(
          splitKeywords,
          List(WORKFLOW.NAME, WORKFLOW.DESCRIPTION, WORKFLOW.CONTENT)
        )
      )
  }

  override protected def getGroupByFields: Seq[GroupField] = {
    Seq(
      WORKFLOW.NAME,
      WORKFLOW.DESCRIPTION,
      WORKFLOW.CREATION_TIME,
      WORKFLOW.WID,
      WORKFLOW.LAST_MODIFIED_TIME,
      WORKFLOW_USER_ACCESS.PRIVILEGE,
      WORKFLOW_OF_USER.UID,
      USER.NAME
    )
  }

  override def toEntryImpl(
      uid: Integer,
      record: Record
  ): DashboardResource.DashboardClickableFileEntry = {
    val pidField = groupConcatDistinct(WORKFLOW_OF_PROJECT.PID)
    val dw = DashboardWorkflow(
      record.into(WORKFLOW_OF_USER).getUid.eq(uid),
      record
        .get(WORKFLOW_USER_ACCESS.PRIVILEGE)
        .toString,
      record.into(USER).getName,
      record.into(WORKFLOW).into(classOf[Workflow]),
      if (record.get(pidField) == null) {
        List[Integer]()
      } else {
        record
          .get(pidField)
          .asInstanceOf[String]
          .split(',')
          .map(number => Integer.valueOf(number))
          .toList
      },
      record.into(USER).getUid
    )
    DashboardClickableFileEntry(SearchQueryBuilder.WORKFLOW_RESOURCE_TYPE, workflow = Some(dw))
  }
}
