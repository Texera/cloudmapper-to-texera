/*
 * This file is generated by jOOQ.
 */
package edu.uci.ics.texera.web.model.jooq.generated.tables.daos;


import edu.uci.ics.texera.web.model.jooq.generated.tables.FileOfWorkflow;
import edu.uci.ics.texera.web.model.jooq.generated.tables.records.FileOfWorkflowRecord;

import java.util.List;

import org.jooq.Configuration;
import org.jooq.Record2;
import org.jooq.impl.DAOImpl;
import org.jooq.types.UInteger;


/**
 * This class is generated by jOOQ.
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class FileOfWorkflowDao extends DAOImpl<FileOfWorkflowRecord, edu.uci.ics.texera.web.model.jooq.generated.tables.pojos.FileOfWorkflow, Record2<UInteger, UInteger>> {

    /**
     * Create a new FileOfWorkflowDao without any configuration
     */
    public FileOfWorkflowDao() {
        super(FileOfWorkflow.FILE_OF_WORKFLOW, edu.uci.ics.texera.web.model.jooq.generated.tables.pojos.FileOfWorkflow.class);
    }

    /**
     * Create a new FileOfWorkflowDao with an attached configuration
     */
    public FileOfWorkflowDao(Configuration configuration) {
        super(FileOfWorkflow.FILE_OF_WORKFLOW, edu.uci.ics.texera.web.model.jooq.generated.tables.pojos.FileOfWorkflow.class, configuration);
    }

    @Override
    public Record2<UInteger, UInteger> getId(edu.uci.ics.texera.web.model.jooq.generated.tables.pojos.FileOfWorkflow object) {
        return compositeKeyRecord(object.getFid(), object.getWid());
    }

    /**
     * Fetch records that have <code>fid BETWEEN lowerInclusive AND upperInclusive</code>
     */
    public List<edu.uci.ics.texera.web.model.jooq.generated.tables.pojos.FileOfWorkflow> fetchRangeOfFid(UInteger lowerInclusive, UInteger upperInclusive) {
        return fetchRange(FileOfWorkflow.FILE_OF_WORKFLOW.FID, lowerInclusive, upperInclusive);
    }

    /**
     * Fetch records that have <code>fid IN (values)</code>
     */
    public List<edu.uci.ics.texera.web.model.jooq.generated.tables.pojos.FileOfWorkflow> fetchByFid(UInteger... values) {
        return fetch(FileOfWorkflow.FILE_OF_WORKFLOW.FID, values);
    }

    /**
     * Fetch records that have <code>wid BETWEEN lowerInclusive AND upperInclusive</code>
     */
    public List<edu.uci.ics.texera.web.model.jooq.generated.tables.pojos.FileOfWorkflow> fetchRangeOfWid(UInteger lowerInclusive, UInteger upperInclusive) {
        return fetchRange(FileOfWorkflow.FILE_OF_WORKFLOW.WID, lowerInclusive, upperInclusive);
    }

    /**
     * Fetch records that have <code>wid IN (values)</code>
     */
    public List<edu.uci.ics.texera.web.model.jooq.generated.tables.pojos.FileOfWorkflow> fetchByWid(UInteger... values) {
        return fetch(FileOfWorkflow.FILE_OF_WORKFLOW.WID, values);
    }
}