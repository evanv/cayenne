package org.apache.art.auto;

/** Class _BitTest was generated by Cayenne.
  * It is probably a good idea to avoid changing this class manually, 
  * since it may be overwritten next time code is regenerated. 
  * If you need to make any customizations, please use subclass. 
  */
public class _BitTest extends org.apache.cayenne.CayenneDataObject {

    public static final String BIT_COLUMN_PROPERTY = "bitColumn";

    public static final String ID_PK_COLUMN = "ID";

    public void setBitColumn(Boolean bitColumn) {
        writeProperty("bitColumn", bitColumn);
    }
    public Boolean getBitColumn() {
        return (Boolean)readProperty("bitColumn");
    }
    
    
}
