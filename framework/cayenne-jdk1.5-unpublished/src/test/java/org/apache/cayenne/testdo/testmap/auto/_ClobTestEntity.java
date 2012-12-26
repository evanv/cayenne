package org.apache.cayenne.testdo.testmap.auto;

import java.util.List;

import org.apache.cayenne.CayenneDataObject;
import org.apache.cayenne.exp.Property;
import org.apache.cayenne.testdo.testmap.ClobTestRelation;

/**
 * Class _ClobTestEntity was generated by Cayenne.
 * It is probably a good idea to avoid changing this class manually,
 * since it may be overwritten next time code is regenerated.
 * If you need to make any customizations, please use subclass.
 */
public abstract class _ClobTestEntity extends CayenneDataObject {

    @Deprecated
    public static final String CLOB_COL_PROPERTY = "clobCol";
    @Deprecated
    public static final String CLOB_VALUE_PROPERTY = "clobValue";

    public static final String CLOB_TEST_ID_PK_COLUMN = "CLOB_TEST_ID";

    public static final Property<String> CLOB_COL = new Property<String>("clobCol");
    public static final Property<List<ClobTestRelation>> CLOB_VALUE = new Property<List<ClobTestRelation>>("clobValue");

    public void setClobCol(String clobCol) {
        writeProperty("clobCol", clobCol);
    }
    public String getClobCol() {
        return (String)readProperty("clobCol");
    }

    public void addToClobValue(ClobTestRelation obj) {
        addToManyTarget("clobValue", obj, true);
    }
    public void removeFromClobValue(ClobTestRelation obj) {
        removeToManyTarget("clobValue", obj, true);
    }
    @SuppressWarnings("unchecked")
    public List<ClobTestRelation> getClobValue() {
        return (List<ClobTestRelation>)readProperty("clobValue");
    }


}
