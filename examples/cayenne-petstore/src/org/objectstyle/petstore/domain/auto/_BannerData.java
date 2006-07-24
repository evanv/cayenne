package org.objectstyle.petstore.domain.auto;

/** Class _BannerData was generated by Cayenne.
  * It is probably a good idea to avoid changing this class manually, 
  * since it may be overwritten next time code is regenerated. 
  * If you need to make any customizations, please use subclass. 
  */
public class _BannerData extends org.objectstyle.cayenne.CayenneDataObject {

    public static final String BANNER_NAME_PROPERTY = "bannerName";
    public static final String CATEGORY_PROPERTY = "category";

    public static final String FAVCATEGORY_PK_COLUMN = "favcategory";

    public void setBannerName(String bannerName) {
        writeProperty("bannerName", bannerName);
    }
    public String getBannerName() {
        return (String)readProperty("bannerName");
    }
    
    
    public void setCategory(org.objectstyle.petstore.domain.Category category) {
        setToOneTarget("category", category, true);
    }

    public org.objectstyle.petstore.domain.Category getCategory() {
        return (org.objectstyle.petstore.domain.Category)readProperty("category");
    } 
    
    
}
