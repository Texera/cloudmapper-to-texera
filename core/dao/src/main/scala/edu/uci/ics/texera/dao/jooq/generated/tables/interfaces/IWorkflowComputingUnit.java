/*
 * This file is generated by jOOQ.
 */
package edu.uci.ics.texera.dao.jooq.generated.tables.interfaces;


import java.io.Serializable;
import java.sql.Timestamp;


/**
 * This class is generated by jOOQ.
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public interface IWorkflowComputingUnit extends Serializable {

    /**
     * Setter for <code>texera_db.workflow_computing_unit.uid</code>.
     */
    public void setUid(Integer value);

    /**
     * Getter for <code>texera_db.workflow_computing_unit.uid</code>.
     */
    public Integer getUid();

    /**
     * Setter for <code>texera_db.workflow_computing_unit.name</code>.
     */
    public void setName(String value);

    /**
     * Getter for <code>texera_db.workflow_computing_unit.name</code>.
     */
    public String getName();

    /**
     * Setter for <code>texera_db.workflow_computing_unit.cuid</code>.
     */
    public void setCuid(Integer value);

    /**
     * Getter for <code>texera_db.workflow_computing_unit.cuid</code>.
     */
    public Integer getCuid();

    /**
     * Setter for <code>texera_db.workflow_computing_unit.creation_time</code>.
     */
    public void setCreationTime(Timestamp value);

    /**
     * Getter for <code>texera_db.workflow_computing_unit.creation_time</code>.
     */
    public Timestamp getCreationTime();

    /**
     * Setter for <code>texera_db.workflow_computing_unit.terminate_time</code>.
     */
    public void setTerminateTime(Timestamp value);

    /**
     * Getter for <code>texera_db.workflow_computing_unit.terminate_time</code>.
     */
    public Timestamp getTerminateTime();

    // -------------------------------------------------------------------------
    // FROM and INTO
    // -------------------------------------------------------------------------

    /**
     * Load data from another generated Record/POJO implementing the common
     * interface IWorkflowComputingUnit
     */
    public void from(IWorkflowComputingUnit from);

    /**
     * Copy data into another generated Record/POJO implementing the common
     * interface IWorkflowComputingUnit
     */
    public <E extends IWorkflowComputingUnit> E into(E into);
}
