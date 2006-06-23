package org.objectstyle.cayenne.modeler.pref;

/** Class _DataMapDefaults was generated by Cayenne.
  * It is probably a good idea to avoid changing this class manually, 
  * since it may be overwritten next time code is regenerated. 
  * If you need to make any customizations, please use subclass. 
  */
public class _DataMapDefaults extends org.objectstyle.cayenne.pref.PreferenceDetail {

    public static final String GENERATE_PAIRS_PROPERTY = "generatePairs";
    public static final String OUTPUT_PATH_PROPERTY = "outputPath";
    public static final String SUBCLASS_TEMPLATE_PROPERTY = "subclassTemplate";
    public static final String SUPERCLASS_PACKAGE_PROPERTY = "superclassPackage";
    public static final String SUPERCLASS_TEMPLATE_PROPERTY = "superclassTemplate";

    public static final String ID_PK_COLUMN = "id";

    public void setGeneratePairs(Boolean generatePairs) {
        writeProperty("generatePairs", generatePairs);
    }
    public Boolean getGeneratePairs() {
        return (Boolean)readProperty("generatePairs");
    }
    
    
    public void setOutputPath(String outputPath) {
        writeProperty("outputPath", outputPath);
    }
    public String getOutputPath() {
        return (String)readProperty("outputPath");
    }
    
    
    public void setSubclassTemplate(String subclassTemplate) {
        writeProperty("subclassTemplate", subclassTemplate);
    }
    public String getSubclassTemplate() {
        return (String)readProperty("subclassTemplate");
    }
    
    
    public void setSuperclassPackage(String superclassPackage) {
        writeProperty("superclassPackage", superclassPackage);
    }
    public String getSuperclassPackage() {
        return (String)readProperty("superclassPackage");
    }
    
    
    public void setSuperclassTemplate(String superclassTemplate) {
        writeProperty("superclassTemplate", superclassTemplate);
    }
    public String getSuperclassTemplate() {
        return (String)readProperty("superclassTemplate");
    }
    
    
}
