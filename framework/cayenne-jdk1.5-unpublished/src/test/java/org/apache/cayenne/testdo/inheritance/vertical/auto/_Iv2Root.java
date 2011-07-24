package org.apache.cayenne.testdo.inheritance.vertical.auto;

import org.apache.cayenne.CayenneDataObject;

/**
 * Class _Iv2Root was generated by Cayenne.
 * It is probably a good idea to avoid changing this class manually,
 * since it may be overwritten next time code is regenerated.
 * If you need to make any customizations, please use subclass.
 */
public abstract class _Iv2Root extends CayenneDataObject {

    public static final String DISCRIMINATOR_PROPERTY = "discriminator";
    public static final String NAME_PROPERTY = "name";

    public static final String ID_PK_COLUMN = "ID";

    public void setDiscriminator(String discriminator) {
        writeProperty("discriminator", discriminator);
    }
    public String getDiscriminator() {
        return (String)readProperty("discriminator");
    }

    public void setName(String name) {
        writeProperty("name", name);
    }
    public String getName() {
        return (String)readProperty("name");
    }

}
