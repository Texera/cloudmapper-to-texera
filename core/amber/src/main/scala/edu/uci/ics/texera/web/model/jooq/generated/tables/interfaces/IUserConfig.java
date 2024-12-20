/*
 * This file is generated by jOOQ.
 */
package edu.uci.ics.texera.web.model.jooq.generated.tables.interfaces;


import org.jooq.types.UInteger;

import java.io.Serializable;


/**
 * This class is generated by jOOQ.
 */
@SuppressWarnings({"all", "unchecked", "rawtypes"})
public interface IUserConfig extends Serializable {

    /**
     * Setter for <code>texera_db.user_config.uid</code>.
     */
    public void setUid(UInteger value);

    /**
     * Getter for <code>texera_db.user_config.uid</code>.
     */
    public UInteger getUid();

    /**
     * Setter for <code>texera_db.user_config.key</code>.
     */
    public void setKey(String value);

    /**
     * Getter for <code>texera_db.user_config.key</code>.
     */
    public String getKey();

    /**
     * Setter for <code>texera_db.user_config.value</code>.
     */
    public void setValue(String value);

    /**
     * Getter for <code>texera_db.user_config.value</code>.
     */
    public String getValue();

    // -------------------------------------------------------------------------
    // FROM and INTO
    // -------------------------------------------------------------------------

    /**
     * Load data from another generated Record/POJO implementing the common interface IUserConfig
     */
    public void from(edu.uci.ics.texera.web.model.jooq.generated.tables.interfaces.IUserConfig from);

    /**
     * Copy data into another generated Record/POJO implementing the common interface IUserConfig
     */
    public <E extends edu.uci.ics.texera.web.model.jooq.generated.tables.interfaces.IUserConfig> E into(E into);
}
