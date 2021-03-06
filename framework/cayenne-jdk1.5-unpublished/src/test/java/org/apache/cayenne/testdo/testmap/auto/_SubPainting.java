package org.apache.cayenne.testdo.testmap.auto;

import org.apache.cayenne.CayenneDataObject;

/**
 * Class _SubPainting was generated by Cayenne.
 * It is probably a good idea to avoid changing this class manually,
 * since it may be overwritten next time code is regenerated.
 * If you need to make any customizations, please use subclass.
 */
public abstract class _SubPainting extends CayenneDataObject {

    public static final String PAINTING_TITLE_PROPERTY = "paintingTitle";

    public static final String PAINTING_ID_PK_COLUMN = "PAINTING_ID";

    public void setPaintingTitle(String paintingTitle) {
        writeProperty(PAINTING_TITLE_PROPERTY, paintingTitle);
    }
    public String getPaintingTitle() {
        return (String)readProperty(PAINTING_TITLE_PROPERTY);
    }

}
