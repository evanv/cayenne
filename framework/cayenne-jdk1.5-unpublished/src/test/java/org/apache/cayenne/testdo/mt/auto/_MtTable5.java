package org.apache.cayenne.testdo.mt.auto;

import java.util.List;

import org.apache.cayenne.CayenneDataObject;
import org.apache.cayenne.testdo.mt.MtTable4;

/**
 * Class _MtTable5 was generated by Cayenne.
 * It is probably a good idea to avoid changing this class manually,
 * since it may be overwritten next time code is regenerated.
 * If you need to make any customizations, please use subclass.
 */
public abstract class _MtTable5 extends CayenneDataObject {

    public static final String TABLE4S_PROPERTY = "table4s";

    public static final String ID_PK_COLUMN = "ID";

    public void addToTable4s(MtTable4 obj) {
        addToManyTarget(TABLE4S_PROPERTY, obj, true);
    }
    public void removeFromTable4s(MtTable4 obj) {
        removeToManyTarget(TABLE4S_PROPERTY, obj, true);
    }
    @SuppressWarnings("unchecked")
    public List<MtTable4> getTable4s() {
        return (List<MtTable4>)readProperty(TABLE4S_PROPERTY);
    }


}
