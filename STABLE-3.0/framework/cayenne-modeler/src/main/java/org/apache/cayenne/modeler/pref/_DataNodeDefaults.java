package org.apache.cayenne.modeler.pref;

import org.apache.cayenne.pref.PreferenceDetail;

/**
 * Class _DataNodeDefaults was generated by Cayenne.
 * It is probably a good idea to avoid changing this class manually,
 * since it may be overwritten next time code is regenerated.
 * If you need to make any customizations, please use subclass.
 */
public abstract class _DataNodeDefaults extends PreferenceDetail {

    public static final String LOCAL_DATA_SOURCE_PROPERTY = "localDataSource";

    public static final String ID_PK_COLUMN = "id";

    public void setLocalDataSource(String localDataSource) {
        writeProperty("localDataSource", localDataSource);
    }
    public String getLocalDataSource() {
        return (String)readProperty("localDataSource");
    }

}
