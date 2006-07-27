package org.apache.cayenne.testdo.inherit.auto;

/** Class _CustomerRepresentative was generated by Cayenne.
  * It is probably a good idea to avoid changing this class manually, 
  * since it may be overwritten next time code is regenerated. 
  * If you need to make any customizations, please use subclass. 
  */
public class _CustomerRepresentative extends org.apache.cayenne.testdo.inherit.AbstractPerson {

    public static final String CLIENT_CONTACT_TYPE_PROPERTY = "clientContactType";
    public static final String TO_CLIENT_COMPANY_PROPERTY = "toClientCompany";

    public static final String PERSON_ID_PK_COLUMN = "PERSON_ID";

    public void setClientContactType(String clientContactType) {
        writeProperty("clientContactType", clientContactType);
    }
    public String getClientContactType() {
        return (String)readProperty("clientContactType");
    }
    
    
    public void setToClientCompany(org.apache.cayenne.testdo.inherit.ClientCompany toClientCompany) {
        setToOneTarget("toClientCompany", toClientCompany, true);
    }

    public org.apache.cayenne.testdo.inherit.ClientCompany getToClientCompany() {
        return (org.apache.cayenne.testdo.inherit.ClientCompany)readProperty("toClientCompany");
    } 
    
    
}
