/*
 * This file is generated by jOOQ.
 */
package edu.uci.ics.texera.web.model.jooq.generated.tables.daos;


import edu.uci.ics.texera.web.model.jooq.generated.tables.WorkflowViewCount;
import edu.uci.ics.texera.web.model.jooq.generated.tables.records.WorkflowViewCountRecord;

import java.util.List;

import org.jooq.Configuration;
import org.jooq.impl.DAOImpl;
import org.jooq.types.UInteger;


/**
 * This class is generated by jOOQ.
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class WorkflowViewCountDao extends DAOImpl<WorkflowViewCountRecord, edu.uci.ics.texera.web.model.jooq.generated.tables.pojos.WorkflowViewCount, UInteger> {

    /**
     * Create a new WorkflowViewCountDao without any configuration
     */
    public WorkflowViewCountDao() {
        super(WorkflowViewCount.WORKFLOW_VIEW_COUNT, edu.uci.ics.texera.web.model.jooq.generated.tables.pojos.WorkflowViewCount.class);
    }

    /**
     * Create a new WorkflowViewCountDao with an attached configuration
     */
    public WorkflowViewCountDao(Configuration configuration) {
        super(WorkflowViewCount.WORKFLOW_VIEW_COUNT, edu.uci.ics.texera.web.model.jooq.generated.tables.pojos.WorkflowViewCount.class, configuration);
    }

    @Override
    public UInteger getId(edu.uci.ics.texera.web.model.jooq.generated.tables.pojos.WorkflowViewCount object) {
        return object.getWid();
    }

    /**
     * Fetch records that have <code>wid BETWEEN lowerInclusive AND upperInclusive</code>
     */
    public List<edu.uci.ics.texera.web.model.jooq.generated.tables.pojos.WorkflowViewCount> fetchRangeOfWid(UInteger lowerInclusive, UInteger upperInclusive) {
        return fetchRange(WorkflowViewCount.WORKFLOW_VIEW_COUNT.WID, lowerInclusive, upperInclusive);
    }

    /**
     * Fetch records that have <code>wid IN (values)</code>
     */
    public List<edu.uci.ics.texera.web.model.jooq.generated.tables.pojos.WorkflowViewCount> fetchByWid(UInteger... values) {
        return fetch(WorkflowViewCount.WORKFLOW_VIEW_COUNT.WID, values);
    }

    /**
     * Fetch a unique record that has <code>wid = value</code>
     */
    public edu.uci.ics.texera.web.model.jooq.generated.tables.pojos.WorkflowViewCount fetchOneByWid(UInteger value) {
        return fetchOne(WorkflowViewCount.WORKFLOW_VIEW_COUNT.WID, value);
    }

    /**
     * Fetch records that have <code>view_count BETWEEN lowerInclusive AND upperInclusive</code>
     */
    public List<edu.uci.ics.texera.web.model.jooq.generated.tables.pojos.WorkflowViewCount> fetchRangeOfViewCount(UInteger lowerInclusive, UInteger upperInclusive) {
        return fetchRange(WorkflowViewCount.WORKFLOW_VIEW_COUNT.VIEW_COUNT, lowerInclusive, upperInclusive);
    }

    /**
     * Fetch records that have <code>view_count IN (values)</code>
     */
    public List<edu.uci.ics.texera.web.model.jooq.generated.tables.pojos.WorkflowViewCount> fetchByViewCount(UInteger... values) {
        return fetch(WorkflowViewCount.WORKFLOW_VIEW_COUNT.VIEW_COUNT, values);
    }
}