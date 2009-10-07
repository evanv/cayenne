/*****************************************************************
 *   Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 ****************************************************************/
package org.apache.cayenne.modeler.editor;

import java.awt.Component;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EventObject;
import java.util.Hashtable;

import javax.swing.DefaultCellEditor;
import javax.swing.JComboBox;
import javax.swing.JTable;
import javax.swing.event.CellEditorListener;
import javax.swing.table.TableCellEditor;

import org.apache.cayenne.map.Embeddable;
import org.apache.cayenne.map.EmbeddableAttribute;
import org.apache.cayenne.map.event.EmbeddableAttributeEvent;
import org.apache.cayenne.modeler.ProjectController;
import org.apache.cayenne.modeler.util.CayenneTable;
import org.apache.cayenne.modeler.util.CayenneTableModel;
import org.apache.cayenne.modeler.util.CayenneWidgetFactory;
import org.apache.cayenne.modeler.util.ProjectUtil;
import org.apache.cayenne.util.Util;

public class EmbeddableAttributeTableModel extends CayenneTableModel {

    private Embeddable embeddable;

    // Columns
    static final int INHERITED = 0;
    static final int OBJ_ATTRIBUTE = 1;
    static final int OBJ_ATTRIBUTE_TYPE = 2;
    static final int DB_ATTRIBUTE = 3;

    private CellEditor cellEditor;

    private CayenneTable table;

    public EmbeddableAttributeTableModel(Embeddable embeddable,
            ProjectController mediator, Object eventSource) {
        super(mediator, eventSource, new ArrayList<EmbeddableAttribute>(embeddable
                .getAttributes()));
        this.embeddable = embeddable;

        // order using local comparator
        Collections.sort(objectList, new EmbeddableAttributeComparator());
    }

    public EmbeddableAttribute getEmbeddableAttribute(int row) {
        return (row >= 0 && row < objectList.size()) ? (EmbeddableAttribute) objectList
                .get(row) : null;
    }

    @Override
    public Class<?> getElementsClass() {
        return EmbeddableAttribute.class;
    }

    @Override
    public void setUpdatedValueAt(Object value, int row, int col) {
        EmbeddableAttribute attribute = getEmbeddableAttribute(row);
        EmbeddableAttributeEvent event = new EmbeddableAttributeEvent(eventSource, embeddable, attribute);
        String path = null;
        Collection<String> nameAttr = null;
        
        if (col == OBJ_ATTRIBUTE) {
            event.setOldName(attribute.getName());
            ProjectUtil.setEmbeddableAttributeName(attribute, value != null ? value
             .toString()
             .trim() : null);

            fireTableCellUpdated(row, col);
        }
        else if (col == OBJ_ATTRIBUTE_TYPE) {
            attribute.setType(value != null ? value.toString() : null);
            fireTableCellUpdated(row, col);
        }
        else if (col == DB_ATTRIBUTE) {
            attribute.setDbAttributeName(value != null ? value.toString() : null);
            fireTableCellUpdated(row, col);
        }
        
        mediator.fireEmbeddableAttributeEvent(event);
    }

    public int getColumnCount() {
        return 4;
    }

    public String getColumnName(int column) {
        switch (column) {
            case INHERITED:
                return "In";
            case OBJ_ATTRIBUTE:
                return "ObjAttribute";
            case OBJ_ATTRIBUTE_TYPE:
                return "Java Type";
            case DB_ATTRIBUTE:
                return "DbAttribute";
            default:
                return "";
        }
    }

    public Object getValueAt(int row, int column) {
        EmbeddableAttribute attribute = getEmbeddableAttribute(row);

        if (column == INHERITED) {
            return null;
        }
        else if (column == OBJ_ATTRIBUTE) {
            return attribute.getName();
        }
        else if (column == OBJ_ATTRIBUTE_TYPE) {
            return attribute.getType();
        }
        else if (column == DB_ATTRIBUTE) {
            return attribute.getDbAttributeName();
        }
        else {
            return null;
        }
    }

    public CellEditor setCellEditor(Collection<String> nameAttr, CayenneTable table) {
        this.cellEditor = new CellEditor(table, CayenneWidgetFactory.createComboBox(
                nameAttr,
                true));
        this.table = table;
        return cellEditor;
    }

    public CellEditor getCellEditor() {
        return cellEditor;
    }

    public boolean isCellEditable(int row, int col) {
        return true;
    }

    final class EmbeddableAttributeComparator implements Comparator {

        public int compare(Object o1, Object o2) {
            EmbeddableAttribute a1 = (EmbeddableAttribute) o1;
            EmbeddableAttribute a2 = (EmbeddableAttribute) o2;

            int delta = getWeight(a1) - getWeight(a2);

            return (delta != 0) ? delta : Util.nullSafeCompare(true, a1.getName(), a2
                    .getName());
        }

        private int getWeight(EmbeddableAttribute a) {
            return a.getEmbeddable() == embeddable ? 1 : -1;
        }
    }

    final class CellEditor implements TableCellEditor {

        protected Hashtable editors;
        protected TableCellEditor editor, defaultEditor;
        JTable table;

        public CellEditor(JTable table, JComboBox combo) {
            this.table = table;
            editors = new Hashtable();
            if (combo != null) {
                defaultEditor = new DefaultCellEditor(combo);
            }
            else {
                defaultEditor = new DefaultCellEditor(new JComboBox());
            }
        }

        public void setEditorAt(int row, TableCellEditor editor) {
            editors.put(new Integer(row), editor);
        }

        public Component getTableCellEditorComponent(
                JTable table,
                Object value,
                boolean isSelected,
                int row,
                int column) {

            return editor.getTableCellEditorComponent(
                    table,
                    value,
                    isSelected,
                    row,
                    column);
        }

        public Object getCellEditorValue() {
            return editor.getCellEditorValue();
        }

        public boolean stopCellEditing() {
            return editor.stopCellEditing();
        }

        public void cancelCellEditing() {
            editor.cancelCellEditing();
        }

        public boolean isCellEditable(EventObject anEvent) {
            selectEditor((MouseEvent) anEvent);
            return editor.isCellEditable(anEvent);
        }

        public void addCellEditorListener(CellEditorListener l) {
            editor.addCellEditorListener(l);
        }

        public void removeCellEditorListener(CellEditorListener l) {
            editor.removeCellEditorListener(l);
        }

        public boolean shouldSelectCell(EventObject anEvent) {
            selectEditor((MouseEvent) anEvent);
            return editor.shouldSelectCell(anEvent);
        }

        protected void selectEditor(MouseEvent e) {
            int row;
            if (e == null) {
                row = table.getSelectionModel().getAnchorSelectionIndex();
            }
            else {
                row = table.rowAtPoint(e.getPoint());
            }
            editor = (TableCellEditor) editors.get(new Integer(row));
            if (editor == null) {
                editor = defaultEditor;
            }
        }
    }
}