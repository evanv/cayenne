package org.objectstyle.cayenne.modeler.model;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.objectstyle.cayenne.map.DataMap;
import org.objectstyle.cayenne.map.ObjEntity;
import org.scopemvc.core.Selector;
import org.scopemvc.model.basic.BasicModel;

/**
 * @author Andrei Adamchik
 */
public class ClassGeneratorModel extends BasicModel {
    protected DataMap map;
    protected String outputDir;
    protected boolean pairs;
    protected List entities;

    public ClassGeneratorModel(DataMap map) {
        this.map = map;
        prepareEntities();
    }

    protected void prepareEntities() {
        List tmp = new ArrayList();
        Iterator it = map.getObjEntitiesAsList().iterator();
        while (it.hasNext()) {
            ObjEntity ent = (ObjEntity) it.next();
            tmp.add(new ClassGeneratorEntityWrapper(ent, true));
        }
        entities = tmp;
    }
    
    public List getSelectedEntities() {
    	Iterator it = entities.iterator();
        List selected = new ArrayList();
        while(it.hasNext()) {
        	ClassGeneratorEntityWrapper wrapper = (ClassGeneratorEntityWrapper)it.next();
        	if(wrapper.isSelected()) {
        	    selected.add(wrapper.getEntity());
        	}
        }
        
    	return selected;
    }
    
    
    /**
     * Returns the map.
     * @return DataMap
     */
    public DataMap getMap() {
        return map;
    }

    /**
     * Sets the map.
     * @param map The map to set
     */
    public void setMap(DataMap map) {
        this.map = map;
        prepareEntities();
    }

    /**
     * Returns the outputDir.
     * @return File
     */
    public File getOutputDirectory() {
        return (outputDir != null) ? new File(outputDir) : null;
    }


    /**
     * Returns the pairs.
     * @return boolean
     */
    public boolean isPairs() {
        return pairs;
    }

    /**
     * Sets the pairs.
     * @param pairs The pairs to set
     */
    public void setPairs(boolean pairs) {
        this.pairs = pairs;
    }

    /**
     * Returns the entities.
     * @return List
     */
    public List getEntities() {
        return entities;
    }
    /**
     * Returns the outputDir.
     * @return String
     */
    public String getOutputDir() {
        return outputDir;
    }

    /**
     * Sets the outputDir.
     * @param outputDir The outputDir to set
     */
    public void setOutputDir(String outputDir) {
        this.outputDir = outputDir;
        fireModelChange(VALUE_CHANGED, Selector.fromString("outputDir"));
    }
}
