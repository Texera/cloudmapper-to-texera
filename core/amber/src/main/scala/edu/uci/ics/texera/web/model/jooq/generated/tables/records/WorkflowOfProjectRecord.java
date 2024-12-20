/*
 * This file is generated by jOOQ.
 */
package edu.uci.ics.texera.web.model.jooq.generated.tables.records;


import edu.uci.ics.texera.web.model.jooq.generated.tables.WorkflowOfProject;
import edu.uci.ics.texera.web.model.jooq.generated.tables.interfaces.IWorkflowOfProject;
import org.jooq.Field;
import org.jooq.Record2;
import org.jooq.Row2;
import org.jooq.impl.UpdatableRecordImpl;
import org.jooq.types.UInteger;


/**
 * This class is generated by jOOQ.
 */
@SuppressWarnings({"all", "unchecked", "rawtypes"})
public class WorkflowOfProjectRecord extends UpdatableRecordImpl<WorkflowOfProjectRecord> implements Record2<UInteger, UInteger>, IWorkflowOfProject {

    private static final long serialVersionUID = 919631497;

    /**
     * Setter for <code>texera_db.workflow_of_project.wid</code>.
     */
    @Override
    public void setWid(UInteger value) {
        set(0, value);
    }

    /**
     * Getter for <code>texera_db.workflow_of_project.wid</code>.
     */
    @Override
    public UInteger getWid() {
        return (UInteger) get(0);
    }

    /**
     * Setter for <code>texera_db.workflow_of_project.pid</code>.
     */
    @Override
    public void setPid(UInteger value) {
        set(1, value);
    }

    /**
     * Getter for <code>texera_db.workflow_of_project.pid</code>.
     */
    @Override
    public UInteger getPid() {
        return (UInteger) get(1);
    }

    // -------------------------------------------------------------------------
    // Primary key information
    // -------------------------------------------------------------------------

    @Override
    public Record2<UInteger, UInteger> key() {
        return (Record2) super.key();
    }

    // -------------------------------------------------------------------------
    // Record2 type implementation
    // -------------------------------------------------------------------------

    @Override
    public Row2<UInteger, UInteger> fieldsRow() {
        return (Row2) super.fieldsRow();
    }

    @Override
    public Row2<UInteger, UInteger> valuesRow() {
        return (Row2) super.valuesRow();
    }

    @Override
    public Field<UInteger> field1() {
        return WorkflowOfProject.WORKFLOW_OF_PROJECT.WID;
    }

    @Override
    public Field<UInteger> field2() {
        return WorkflowOfProject.WORKFLOW_OF_PROJECT.PID;
    }

    @Override
    public UInteger component1() {
        return getWid();
    }

    @Override
    public UInteger component2() {
        return getPid();
    }

    @Override
    public UInteger value1() {
        return getWid();
    }

    @Override
    public UInteger value2() {
        return getPid();
    }

    @Override
    public WorkflowOfProjectRecord value1(UInteger value) {
        setWid(value);
        return this;
    }

    @Override
    public WorkflowOfProjectRecord value2(UInteger value) {
        setPid(value);
        return this;
    }

    @Override
    public WorkflowOfProjectRecord values(UInteger value1, UInteger value2) {
        value1(value1);
        value2(value2);
        return this;
    }

    // -------------------------------------------------------------------------
    // FROM and INTO
    // -------------------------------------------------------------------------

    @Override
    public void from(IWorkflowOfProject from) {
        setWid(from.getWid());
        setPid(from.getPid());
    }

    @Override
    public <E extends IWorkflowOfProject> E into(E into) {
        into.from(this);
        return into;
    }

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    /**
     * Create a detached WorkflowOfProjectRecord
     */
    public WorkflowOfProjectRecord() {
        super(WorkflowOfProject.WORKFLOW_OF_PROJECT);
    }

    /**
     * Create a detached, initialised WorkflowOfProjectRecord
     */
    public WorkflowOfProjectRecord(UInteger wid, UInteger pid) {
        super(WorkflowOfProject.WORKFLOW_OF_PROJECT);

        set(0, wid);
        set(1, pid);
    }
}
