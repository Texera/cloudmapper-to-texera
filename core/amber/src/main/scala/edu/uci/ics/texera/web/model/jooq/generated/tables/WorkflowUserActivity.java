/*
 * This file is generated by jOOQ.
 */
package edu.uci.ics.texera.web.model.jooq.generated.tables;


import edu.uci.ics.texera.web.model.jooq.generated.TexeraDb;
import edu.uci.ics.texera.web.model.jooq.generated.tables.records.WorkflowUserActivityRecord;

import java.sql.Timestamp;

import org.jooq.Field;
import org.jooq.ForeignKey;
import org.jooq.Name;
import org.jooq.Record;
import org.jooq.Row5;
import org.jooq.Schema;
import org.jooq.Table;
import org.jooq.TableField;
import org.jooq.impl.DSL;
import org.jooq.impl.TableImpl;
import org.jooq.types.UInteger;


/**
 * This class is generated by jOOQ.
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class WorkflowUserActivity extends TableImpl<WorkflowUserActivityRecord> {

    private static final long serialVersionUID = 1692655664;

    /**
     * The reference instance of <code>texera_db.workflow_user_activity</code>
     */
    public static final WorkflowUserActivity WORKFLOW_USER_ACTIVITY = new WorkflowUserActivity();

    /**
     * The class holding records for this type
     */
    @Override
    public Class<WorkflowUserActivityRecord> getRecordType() {
        return WorkflowUserActivityRecord.class;
    }

    /**
     * The column <code>texera_db.workflow_user_activity.uid</code>.
     */
    public final TableField<WorkflowUserActivityRecord, UInteger> UID = createField(DSL.name("uid"), org.jooq.impl.SQLDataType.INTEGERUNSIGNED.nullable(false).defaultValue(org.jooq.impl.DSL.inline("0", org.jooq.impl.SQLDataType.INTEGERUNSIGNED)), this, "");

    /**
     * The column <code>texera_db.workflow_user_activity.wid</code>.
     */
    public final TableField<WorkflowUserActivityRecord, UInteger> WID = createField(DSL.name("wid"), org.jooq.impl.SQLDataType.INTEGERUNSIGNED.nullable(false), this, "");

    /**
     * The column <code>texera_db.workflow_user_activity.ip</code>.
     */
    public final TableField<WorkflowUserActivityRecord, String> IP = createField(DSL.name("ip"), org.jooq.impl.SQLDataType.VARCHAR(15), this, "");

    /**
     * The column <code>texera_db.workflow_user_activity.activate</code>.
     */
    public final TableField<WorkflowUserActivityRecord, String> ACTIVATE = createField(DSL.name("activate"), org.jooq.impl.SQLDataType.VARCHAR(10).nullable(false), this, "");

    /**
     * The column <code>texera_db.workflow_user_activity.activity_time</code>.
     */
    public final TableField<WorkflowUserActivityRecord, Timestamp> ACTIVITY_TIME = createField(DSL.name("activity_time"), org.jooq.impl.SQLDataType.TIMESTAMP.defaultValue(org.jooq.impl.DSL.field("CURRENT_TIMESTAMP", org.jooq.impl.SQLDataType.TIMESTAMP)), this, "");

    /**
     * Create a <code>texera_db.workflow_user_activity</code> table reference
     */
    public WorkflowUserActivity() {
        this(DSL.name("workflow_user_activity"), null);
    }

    /**
     * Create an aliased <code>texera_db.workflow_user_activity</code> table reference
     */
    public WorkflowUserActivity(String alias) {
        this(DSL.name(alias), WORKFLOW_USER_ACTIVITY);
    }

    /**
     * Create an aliased <code>texera_db.workflow_user_activity</code> table reference
     */
    public WorkflowUserActivity(Name alias) {
        this(alias, WORKFLOW_USER_ACTIVITY);
    }

    private WorkflowUserActivity(Name alias, Table<WorkflowUserActivityRecord> aliased) {
        this(alias, aliased, null);
    }

    private WorkflowUserActivity(Name alias, Table<WorkflowUserActivityRecord> aliased, Field<?>[] parameters) {
        super(alias, null, aliased, parameters, DSL.comment(""));
    }

    public <O extends Record> WorkflowUserActivity(Table<O> child, ForeignKey<O, WorkflowUserActivityRecord> key) {
        super(child, key, WORKFLOW_USER_ACTIVITY);
    }

    @Override
    public Schema getSchema() {
        return TexeraDb.TEXERA_DB;
    }

    @Override
    public WorkflowUserActivity as(String alias) {
        return new WorkflowUserActivity(DSL.name(alias), this);
    }

    @Override
    public WorkflowUserActivity as(Name alias) {
        return new WorkflowUserActivity(alias, this);
    }

    /**
     * Rename this table
     */
    @Override
    public WorkflowUserActivity rename(String name) {
        return new WorkflowUserActivity(DSL.name(name), null);
    }

    /**
     * Rename this table
     */
    @Override
    public WorkflowUserActivity rename(Name name) {
        return new WorkflowUserActivity(name, null);
    }

    // -------------------------------------------------------------------------
    // Row5 type methods
    // -------------------------------------------------------------------------

    @Override
    public Row5<UInteger, UInteger, String, String, Timestamp> fieldsRow() {
        return (Row5) super.fieldsRow();
    }
}