/*
 * This file is generated by jOOQ.
 */
package edu.uci.ics.texera.dao.jooq.generated.tables;


import edu.uci.ics.texera.dao.jooq.generated.Keys;
import edu.uci.ics.texera.dao.jooq.generated.TexeraDb;
import edu.uci.ics.texera.dao.jooq.generated.enums.ClusterStatus;
import edu.uci.ics.texera.dao.jooq.generated.tables.records.ClusterRecord;

import java.sql.Timestamp;
import java.util.Arrays;
import java.util.List;

import org.jooq.Field;
import org.jooq.ForeignKey;
import org.jooq.Identity;
import org.jooq.Name;
import org.jooq.Record;
import org.jooq.Row7;
import org.jooq.Schema;
import org.jooq.Table;
import org.jooq.TableField;
import org.jooq.TableOptions;
import org.jooq.UniqueKey;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;
import org.jooq.impl.TableImpl;


/**
 * This class is generated by jOOQ.
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class Cluster extends TableImpl<ClusterRecord> {

    private static final long serialVersionUID = 1L;

    /**
     * The reference instance of <code>texera_db.cluster</code>
     */
    public static final Cluster CLUSTER = new Cluster();

    /**
     * The class holding records for this type
     */
    @Override
    public Class<ClusterRecord> getRecordType() {
        return ClusterRecord.class;
    }

    /**
     * The column <code>texera_db.cluster.cid</code>.
     */
    public final TableField<ClusterRecord, Integer> CID = createField(DSL.name("cid"), SQLDataType.INTEGER.nullable(false).identity(true), this, "");

    /**
     * The column <code>texera_db.cluster.name</code>.
     */
    public final TableField<ClusterRecord, String> NAME = createField(DSL.name("name"), SQLDataType.VARCHAR(255).nullable(false), this, "");

    /**
     * The column <code>texera_db.cluster.owner_id</code>.
     */
    public final TableField<ClusterRecord, Integer> OWNER_ID = createField(DSL.name("owner_id"), SQLDataType.INTEGER.nullable(false), this, "");

    /**
     * The column <code>texera_db.cluster.machine_type</code>.
     */
    public final TableField<ClusterRecord, String> MACHINE_TYPE = createField(DSL.name("machine_type"), SQLDataType.VARCHAR(255).nullable(false), this, "");

    /**
     * The column <code>texera_db.cluster.number_of_machines</code>.
     */
    public final TableField<ClusterRecord, Integer> NUMBER_OF_MACHINES = createField(DSL.name("number_of_machines"), SQLDataType.INTEGER.nullable(false), this, "");

    /**
     * The column <code>texera_db.cluster.creation_time</code>.
     */
    public final TableField<ClusterRecord, Timestamp> CREATION_TIME = createField(DSL.name("creation_time"), SQLDataType.TIMESTAMP(0).nullable(false).defaultValue(DSL.field("CURRENT_TIMESTAMP", SQLDataType.TIMESTAMP)), this, "");

    /**
     * The column <code>texera_db.cluster.status</code>.
     */
    public final TableField<ClusterRecord, ClusterStatus> STATUS = createField(DSL.name("status"), SQLDataType.VARCHAR.asEnumDataType(edu.uci.ics.texera.dao.jooq.generated.enums.ClusterStatus.class), this, "");

    private Cluster(Name alias, Table<ClusterRecord> aliased) {
        this(alias, aliased, null);
    }

    private Cluster(Name alias, Table<ClusterRecord> aliased, Field<?>[] parameters) {
        super(alias, null, aliased, parameters, DSL.comment(""), TableOptions.table());
    }

    /**
     * Create an aliased <code>texera_db.cluster</code> table reference
     */
    public Cluster(String alias) {
        this(DSL.name(alias), CLUSTER);
    }

    /**
     * Create an aliased <code>texera_db.cluster</code> table reference
     */
    public Cluster(Name alias) {
        this(alias, CLUSTER);
    }

    /**
     * Create a <code>texera_db.cluster</code> table reference
     */
    public Cluster() {
        this(DSL.name("cluster"), null);
    }

    public <O extends Record> Cluster(Table<O> child, ForeignKey<O, ClusterRecord> key) {
        super(child, key, CLUSTER);
    }

    @Override
    public Schema getSchema() {
        return aliased() ? null : TexeraDb.TEXERA_DB;
    }

    @Override
    public Identity<ClusterRecord, Integer> getIdentity() {
        return (Identity<ClusterRecord, Integer>) super.getIdentity();
    }

    @Override
    public UniqueKey<ClusterRecord> getPrimaryKey() {
        return Keys.CLUSTER_PKEY;
    }

    @Override
    public List<ForeignKey<ClusterRecord, ?>> getReferences() {
        return Arrays.asList(Keys.CLUSTER__CLUSTER_OWNER_ID_FKEY);
    }

    private transient User _user;

    /**
     * Get the implicit join path to the <code>texera_db.user</code> table.
     */
    public User user() {
        if (_user == null)
            _user = new User(this, Keys.CLUSTER__CLUSTER_OWNER_ID_FKEY);

        return _user;
    }

    @Override
    public Cluster as(String alias) {
        return new Cluster(DSL.name(alias), this);
    }

    @Override
    public Cluster as(Name alias) {
        return new Cluster(alias, this);
    }

    /**
     * Rename this table
     */
    @Override
    public Cluster rename(String name) {
        return new Cluster(DSL.name(name), null);
    }

    /**
     * Rename this table
     */
    @Override
    public Cluster rename(Name name) {
        return new Cluster(name, null);
    }

    // -------------------------------------------------------------------------
    // Row7 type methods
    // -------------------------------------------------------------------------

    @Override
    public Row7<Integer, String, Integer, String, Integer, Timestamp, ClusterStatus> fieldsRow() {
        return (Row7) super.fieldsRow();
    }
}
