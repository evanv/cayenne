package org.apache.art.auto;

/** Class _BigIntegerEntity was generated by Cayenne.
  * It is probably a good idea to avoid changing this class manually, 
  * since it may be overwritten next time code is regenerated. 
  * If you need to make any customizations, please use subclass. 
  */
public abstract class _BigIntegerEntity extends org.apache.cayenne.CayenneDataObject {

    public static final String BIG_INTEGER_FIELD_PROPERTY = "bigIntegerField";

    public static final String ID_PK_COLUMN = "ID";

    public void setBigIntegerField(java.math.BigInteger bigIntegerField) {
        writeProperty("bigIntegerField", bigIntegerField);
    }
    public java.math.BigInteger getBigIntegerField() {
        return (java.math.BigInteger)readProperty("bigIntegerField");
    }
    
    
}
