/* ====================================================================
 * 
 * The ObjectStyle Group Software License, Version 1.0 
 *
 * Copyright (c) 2002 The ObjectStyle Group 
 * and individual authors of the software.  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer. 
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution, if
 *    any, must include the following acknowlegement:  
 *       "This product includes software developed by the 
 *        ObjectStyle Group (http://objectstyle.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 *
 * 4. The names "ObjectStyle Group" and "Cayenne" 
 *    must not be used to endorse or promote products derived
 *    from this software without prior written permission. For written 
 *    permission, please contact andrus@objectstyle.org.
 *
 * 5. Products derived from this software may not be called "ObjectStyle"
 *    nor may "ObjectStyle" appear in their names without prior written
 *    permission of the ObjectStyle Group.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE OBJECTSTYLE GROUP OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the ObjectStyle Group.  For more
 * information on the ObjectStyle Group, please see
 * <http://objectstyle.org/>.
 *
 */
package org.objectstyle.cayenne.modeler.datamap;

import org.objectstyle.cayenne.map.DbEntity;
import org.objectstyle.cayenne.map.DerivedDbAttribute;
import org.objectstyle.cayenne.map.event.AttributeEvent;
import org.objectstyle.cayenne.modeler.control.EventController;

/**
 * Table model for derived DbAttributes.
 * 
 * @author Andrei Adamchik
 */
public class DerivedDbAttributeTableModel extends DbAttributeTableModel {
	// Column indices
	private static final int DB_ATTRIBUTE_NAME = 0;
	private static final int DB_ATTRIBUTE_SPEC = 1;
	private static final int DB_ATTRIBUTE_TYPE = 2;
	private static final int DB_ATTRIBUTE_GROUPBY = 3;
	private static final int DB_ATTRIBUTE_PRIMARY_KEY = 4;
	private static final int DB_ATTRIBUTE_MANDATORY = 5;

	
	/**
	 * Constructor for DerivedDbAttributeTableModel.
	 * @param entity
	 * @param mediator
	 * @param eventSource
	 */
	public DerivedDbAttributeTableModel(
			DbEntity entity,
			EventController mediator,
			Object eventSource) {
		super(entity, mediator, eventSource);
	}

	/**
	 * @see javax.swing.table.TableModel#getColumnClass(int)
	 */
	public Class getColumnClass(int col) {
		switch (col) {
			case DB_ATTRIBUTE_PRIMARY_KEY :
			case DB_ATTRIBUTE_MANDATORY :
			case DB_ATTRIBUTE_GROUPBY:
				return Boolean.class;
			default :
				return String.class;
		}
	}

    public int mandatoryColumnInd() {
    	return DB_ATTRIBUTE_MANDATORY;
    }
    
    public int nameColumnInd() {
    	return DB_ATTRIBUTE_NAME;
    }
    
    public int typeColumnInd() {
    	return DB_ATTRIBUTE_TYPE;
    }

	/**
	 * @see javax.swing.table.TableModel#getColumnCount()
	 */
	public int getColumnCount() {
		return 6;
	}


	/**
	 * @see javax.swing.table.TableModel#getColumnName(int)
	 */
	public String getColumnName(int col) {
		switch(col) {
			case DB_ATTRIBUTE_NAME: return "Name";
			case DB_ATTRIBUTE_SPEC: return "Spec";
			case DB_ATTRIBUTE_TYPE: return "Type";
			case DB_ATTRIBUTE_GROUPBY: return "Group By";
			case DB_ATTRIBUTE_PRIMARY_KEY: return "PK";
			case DB_ATTRIBUTE_MANDATORY: return "Mandatory";
			default: return "";
		}
	}

	public Object getValueAt(int row, int column) {
		DerivedDbAttribute attr = (DerivedDbAttribute)getAttribute(row);

		if (attr == null) {
			return "";
		}

		switch (column) {
			case DB_ATTRIBUTE_NAME :
				return getAttributeName(attr);
			case DB_ATTRIBUTE_SPEC :
				return getSpec(attr);
			case DB_ATTRIBUTE_TYPE :
				return getAttributeType(attr);
			case DB_ATTRIBUTE_GROUPBY :
				return isGroupBy(attr);
			case DB_ATTRIBUTE_PRIMARY_KEY :
				return isPrimaryKey(attr);
			case DB_ATTRIBUTE_MANDATORY :
				return isMandatory(attr);
			default :
				return "";
		}
	}
	
	public void setUpdatedValueAt(Object newVal, int row, int col) {
		DerivedDbAttribute attr = (DerivedDbAttribute)getAttribute(row);
		if (attr == null) {
			return;
		}

		AttributeEvent e = new AttributeEvent(eventSource, attr, entity);

		switch (col) {
			case DB_ATTRIBUTE_NAME :
				e.setOldName(attr.getName());
				setAttributeName((String) newVal, attr);
				fireTableCellUpdated(row, col);
				break;
			case DB_ATTRIBUTE_SPEC :
				setSpec((String) newVal, attr);
				break;
			case DB_ATTRIBUTE_TYPE :
				setAttributeType((String) newVal, attr);
				break;
			case DB_ATTRIBUTE_GROUPBY :
				setGroupBy((Boolean) newVal, attr);
				break;
			case DB_ATTRIBUTE_PRIMARY_KEY :
				setPrimaryKey((Boolean) newVal, attr);
				fireTableCellUpdated(row, DB_ATTRIBUTE_MANDATORY);
				break;
			case DB_ATTRIBUTE_MANDATORY :
				setMandatory((Boolean) newVal, attr);
				break;
		}

		mediator.fireDbAttributeEvent(e);
	}


	
	public String getSpec(DerivedDbAttribute attr) {
		return attr.getExpressionSpec();
	}

	public void setSpec(String newVal, DerivedDbAttribute attr) {
		attr.setExpressionSpec(newVal);
	}
	
	public Boolean isGroupBy(DerivedDbAttribute attr) {
		return attr.isGroupBy() ? Boolean.TRUE : Boolean.FALSE;
	}
	
	public void setGroupBy(Boolean newVal, DerivedDbAttribute attr) {
		attr.setGroupBy(newVal.booleanValue());
	}
	
	
	/**
	 * @see org.objectstyle.cayenne.modeler.util.CayenneTableModel#getElementsClass()
	 */
	public Class getElementsClass() {
		return DerivedDbAttribute.class;
	}
}

