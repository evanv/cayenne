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
package org.objectstyle.cayenne.map;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.SortedMap;

import org.apache.log4j.Logger;
import org.objectstyle.cayenne.util.CayenneMap;

/**
 * DataMap is a central class in Cayenne Object Relational mapping design.
 * It contains a set of mapping descriptors, such as entities, attributes
 * and relationships, providing O/R mapping of a set of database tables
 * to a set of Java classes.
 *
 * @author Michael Shengaout
 * @author Andrei Adamchik  
 * @author Craig Miskell
 */
public class DataMap {
    private static Logger logObj = Logger.getLogger(DataMap.class);

    protected String name;
    protected String location;

    /** 
     * Contains a list of DataMaps that are used by this map.
     */
	protected List dependencies = new ArrayList();
	protected List dependenciesRef = Collections.unmodifiableList(dependencies);

    /** 
     * ObjEntities representing the data object classes.
     * The name of ObjEntity serves as a key. 
     */
    private CayenneMap objEntityMap = new CayenneMap(this);
	private SortedMap objEntityMapRef = Collections.unmodifiableSortedMap(objEntityMap);
	private Set objEntityNamesRef = Collections.unmodifiableSet(objEntityMap.keySet());
	private Collection objEntityValuesRef = Collections.unmodifiableCollection(objEntityMap.values());

    /**
     * DbEntities representing metadata for individual database tables.
     * The name of DbEntity (which is also a database table name) serves
     * as a key.
     */
    private CayenneMap dbEntityMap = new CayenneMap(this);
    private SortedMap dbEntityMapRef = Collections.unmodifiableSortedMap(dbEntityMap);

    /**
     * Sorts an array of DataMaps in the right save order to satisfy
     * inter-map dependencies.
	 * @deprecated Since 1.0 Beta1; use Collections.sort(List, Comparator) instead.
     */
    public static void sortMaps(List maps) {
        Collections.sort(maps, new MapComparator());
    }

    /** Creates an empty DataMap */
    public DataMap() {
    	this("unnamed");
    }

    /**
     * Creates an empty DataMap and assigns it a <code>name</code>.
     */
    public DataMap(String mapName) {
    	super();
        this.setName(mapName);
    }

    /**
     * Adds a data map that has entities used by this map.
     * 
     * @throws IllegalArgumentException if a <code>map</code> argument
     * already depends on thsi map directly or indirectly.
     */
    public void addDependency(DataMap map) throws IllegalArgumentException {
        if (map.isDependentOn(this)) {
            StringBuffer buf = new StringBuffer(128);
			buf.append("Attempt to create circular dependency. ")
				.append('\'')
				.append(map.getName())
				.append("' already depends on '")
				.append(this.getName())
				.append("'.");
            throw new IllegalArgumentException(buf.toString());
        }
        dependencies.add(map);
        logObj.debug("added dependency: '"+ map.getName() + "'");
    }

    public void removeDependency(DataMap map) {
        dependencies.remove(map);
		logObj.debug("removed dependency: '"+ map.getName() + "'");
    }

    public List getDependencies() {
        return dependenciesRef;
    }

    /**
     * Returns <code>true</code> if this map
     * depends on DataMap supplied as a <code>map</code>
     * parameter. Checks for nested dependencies as well.
     * For instance if the following dependency exists:
     * map1 -> map2 -> map3, calling <code>map1.isDependentOn(map3)</code>
     * will return <code>true</code>.
     */
    public boolean isDependentOn(DataMap map) {
        if (dependencies.size() == 0) {
            return false;
        }

		if (dependencies.contains(map)) {
			return true;
		}

        Iterator it = dependencies.iterator();
        while (it.hasNext()) {
            // check dependencies recursively
            DataMap dep = (DataMap) it.next();
            if (dep.isDependentOn(map)) {
                return true;
            }
        }

        return false;
    }

    public void clearDependencies() {
        dependencies.clear();
    }

    /** Returns "name" property value. */
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = (name != null ? name : "unnamed");
    }

    /** 
     * Adds all Object and DB entities from another map
     * to this map. Overwrites all existing entities with the
     * new ones.
     * 
     * <p><i>FIXME: will need to implement advanced merge
     * that allows different policies for overwriting entities.
     * </i></p>
     */
    public void mergeWithDataMap(DataMap map) {
        Iterator dbs = map.getDbEntitiesAsList().iterator();
        while (dbs.hasNext()) {
            DbEntity ent = (DbEntity) dbs.next();
            this.removeDbEntity(ent.getName());
            this.addDbEntity(ent);
        }

        Iterator objs = map.getObjEntitiesAsList().iterator();
        while (objs.hasNext()) {
            ObjEntity ent = (ObjEntity) objs.next();
            this.removeObjEntity(ent.getName());
            this.addObjEntity(ent);
        }
    }

    /**
     * Returns "location" property value.
     * Location is abstract and treated differently
     * by different loaders. See Configuration class
     * for more details on how location is resolved.
     */
    public String getLocation() {
        return location;
    }

    /**
     * Sets "location" property. Usually location is set by
     * map loader when map is created from a XML file,
     * or by a GUI tool when such a tool wants to change where 
     * the map should be saved.
     */
    public void setLocation(String location) {
        this.location = location;
    }

    public SortedMap getObjEntityMap() {
        return objEntityMapRef;
    }

    /**
     * Returns sorted unmodifiable map of DbEntities
     * contained in this DataMap. Keys are DbEntity
     * names (same as database table names).
     */
    public SortedMap getDbEntityMap() {
    	return dbEntityMapRef;
    }

    /** 
     * Adds ObjEntity to the list of map entities.
     */
    public void addObjEntity(ObjEntity objEntity) {
        if (objEntity.getName() == null) {
            throw new NullPointerException("Attempt to add ObjEntity with no name.");
        }

        objEntityMap.put(objEntity.getName(), objEntity);
    }

    /** 
     * Adds DbEntity to the list of map entities.
     * If there is another entity registered under the same name,
     * throws an IllegalArgumentException.
     */
    public void addDbEntity(DbEntity dbEntity) {
        if (dbEntity.getName() == null) {
            throw new NullPointerException("Attempt to add DbEntity with no name.");
        }

        dbEntityMap.put(dbEntity.getName(), dbEntity);
    }

	/**
	 * Returns a list of ObjEntities stored in this DataMap.
	 * @!deprecated Since 1.0 Beta1; use #getObjEntities() instead.
	 */
	public List getObjEntitiesAsList() {
		return this.getObjEntitiesAsList(false);
	}

	/**
	 * Returns all ObjEntities in this DataMap, including entities
	 * from dependent maps if <code>includeDeps</code> is <code>true</code>.
	 * @!deprecated Since 1.0 Beta1; use #getObjEntities(boolean) instead.
	 */
	public List getObjEntitiesAsList(boolean includeDeps) {
		List ents = new ArrayList(objEntityMap.values());

		if (includeDeps) {
			Iterator it = this.getDependencies().iterator();
			while (it.hasNext()) {
				DataMap dep = (DataMap) it.next();
				// using "false" to avoid problems with circular dependencies
				ents.addAll(dep.getObjEntitiesAsList(false));
			}
		}

		return ents;
	}

	/**
	 * Returns a list of ObjEntities stored in this DataMap.
	 */
	public Collection getObjEntities() {
		return this.getObjEntities(false);
	}

	/**
	 * Returns all ObjEntities in this DataMap, including entities
	 * from dependent maps if <code>includeDeps</code> is <code>true</code>.
	 */
	public Collection getObjEntities(boolean includeDeps) {
		throw new UnsupportedOperationException("NYI!");
	}

	/**
	 * Returns all DbEntities in this DataMap.
	 * @!deprecated Since 1.0 Beta1; use #getDbEntities() instead.
	 */
	public List getDbEntitiesAsList() {
		return this.getDbEntitiesAsList(false);
	}

	/**
	 * Returns all DbEntities in this DataMap, including entities
	 * from dependent maps if <code>includeDeps</code> is <code>true</code>.
	 * @!deprecated Since 1.0 Beta1; use #getDbEntities(boolean) instead.
	 */
	public List getDbEntitiesAsList(boolean includeDeps) {
		List ents = new ArrayList(dbEntityMap.values());

		if (includeDeps) {
			Iterator it = this.getDependencies().iterator();
			while (it.hasNext()) {
				DataMap dep = (DataMap) it.next();
				// using "false" to avoid problems with circular dependencies
				ents.addAll(dep.getDbEntitiesAsList(false));
			}
		}
		return ents;
	}

	/**
	 * Returns all DbEntities in this DataMap.
	 */
	public Collection getDbEntities() {
		return this.getDbEntities(false);
	}

	/**
	 * Returns all DbEntities in this DataMap, including entities
	 * from dependent maps if <code>includeDeps</code> is <code>true</code>.
	 */
	public Collection getDbEntities(boolean includeDeps) {
		throw new UnsupportedOperationException("NYI!");
	}

    /**
     * Returns a list of DbEntity names.
     */
    public List getDbEntityNames(boolean includeDeps) {
        List ents = new ArrayList(dbEntityMap.keySet());

        if (includeDeps) {
            Iterator it = this.getDependencies().iterator();
            while (it.hasNext()) {
                DataMap dep = (DataMap) it.next();
                // using "false" to avoid problems with circular dependencies
                ents.addAll(dep.getDbEntityNames(false));
            }
        }
        return ents;
    }

    /** 
     * Returns DbEntity matching the <code>name</code> parameter. No
     * dependencies will be searched.
     */
    public DbEntity getDbEntity(String dbEntityName) {
        return this.getDbEntity(dbEntityName, false);
    }

    public DbEntity getDbEntity(String dbEntityName, boolean searchDependencies) {
        DbEntity ent = (DbEntity)dbEntityMap.get(dbEntityName);
        if (ent != null || !searchDependencies) {
            return ent;
        }

        Iterator it = this.getDependencies().iterator();
        while (it.hasNext()) {
            DataMap dep = (DataMap)it.next();
            // using "false" to avoid problems with circular dependencies
            DbEntity e = dep.getDbEntity(dbEntityName, false);
            if (e != null) {
                return e;
            }
        }
        return null;
    }

    /**
     * Get ObjEntity by its name.
     */
    public ObjEntity getObjEntity(String objEntityName) {
        return (ObjEntity) objEntityMap.get(objEntityName);
    }

    public ObjEntity getObjEntity(String objEntityName, boolean searchDependencies) {
        ObjEntity ent = (ObjEntity) objEntityMap.get(objEntityName);
        if (ent != null || !searchDependencies) {
            return ent;
        }

        Iterator it = this.getDependencies().iterator();
        while (it.hasNext()) {
            DataMap dep = (DataMap) it.next();
            // using "false" to avoid problems with circular dependencies
            ObjEntity e = dep.getObjEntity(objEntityName, false);
            if (e != null) {
                return e;
            }
        }
        return null;
    }

    /** 
     * Returns a list of ObjEntities mapped to the given DbEntity.
     */
    public List getMappedEntities(DbEntity dbEntity) {
        if (dbEntity == null) {
            return null;
        }

		List result = new ArrayList();
        Iterator iter = this.getObjEntitiesAsList(true).iterator();
        while (iter.hasNext()) {
            ObjEntity objEnt = (ObjEntity) iter.next();
            if (objEnt.getDbEntity() == dbEntity) {
                result.add(objEnt);
            }
        }

        return result;
    }

    /** "Dirty" remove of the DbEntity from the data map. */
    public void removeDbEntity(String dbEntityName) {
        dbEntityMap.remove(dbEntityName);
    }

    /**
     * Clean remove of the DbEntity from the data map.
     * If there are any ObjEntities or ObjRelationship entities
     * referencing given entity, removes them as well.
     * No relationships are re-established. Also, if the ObjEntity is removed,
     * all the ObjRelationships referencing it are removed as well.
     */
    public void deleteDbEntity(String dbEntityName) {
        DbEntity dbEntityToDelete = getDbEntity(dbEntityName);
        // No db entity to remove? return.
        if (null == dbEntityToDelete) {
            return;
        }

        dbEntityMap.remove(dbEntityName);

        Iterator dbEnts = this.getDbEntitiesAsList().iterator();
        while (dbEnts.hasNext()) {
        	DbEntity dbEnt = (DbEntity)dbEnts.next();
            Iterator rels = dbEnt.getRelationships().iterator();
            while (rels.hasNext()) {
                DbRelationship rel = (DbRelationship)rels.next();
                if (rel.getTargetEntity() == dbEntityToDelete) {
                    dbEnt.removeRelationship(rel.getName());
                }
            }
        }

        // Remove all obj relationships referencing removed DbRelationships.
        Iterator objEnts = this.getObjEntityMap().values().iterator();
        while (objEnts.hasNext()) {
            ObjEntity objEnt = (ObjEntity)objEnts.next();
            if (objEnt.getDbEntity() == dbEntityToDelete) {
                objEnt.clearDbMapping();
            } else {
                Iterator iter = objEnt.getRelationships().iterator();
                while (iter.hasNext()) {
                    ObjRelationship rel = (ObjRelationship) iter.next();
                    Iterator dbRels = rel.getDbRelationships().iterator();
                    while (dbRels.hasNext()) {
                        DbRelationship dbRel = (DbRelationship) dbRels.next();
                        if (dbRel.getTargetEntity() == dbEntityToDelete) {
                            rel.clearDbRelationships();
                            break;
                        }
                    }
                }
            }
        }
    }

    /** 
     * Clean remove of the ObjEntity from the data map.
     * Removes all ObjRelationships referencing this ObjEntity
     */
    public void deleteObjEntity(String objEntityName) {
        ObjEntity entity = (ObjEntity) objEntityMap.get(objEntityName);
        if (null == entity) {
            return;
        }

        objEntityMap.remove(objEntityName);

        Iterator entities = this.getObjEntityMap().values().iterator();
        while (entities.hasNext()) {
        	ObjEntity ent = (ObjEntity)entities.next();
        	// take a copy since we're going to modifiy the entity
			Iterator rels = new ArrayList(ent.getRelationships()).iterator();
            while (rels.hasNext()) {
                ObjRelationship rel = (ObjRelationship)rels.next();
                if (objEntityName.equals(rel.getTargetEntityName())
                	|| objEntityName.equals(rel.getTargetEntityName())) {
	                	ent.removeRelationship(rel.getName());
                }
            }
        }
    }

    /** "Dirty" remove of the ObjEntity from the data map.*/
    public void removeObjEntity(String objEntityName) {
        ObjEntity objEntity = (ObjEntity) objEntityMap.get(objEntityName);
        if (objEntity != null) {
            objEntityMap.remove(objEntityName);
        }
    }

    /**
     * Used as a comparator to infer DataMap ordering.
     */
    public static final class MapComparator implements Comparator {

        public final int compare(Object o1, Object o2) {
            DataMap m1 = (DataMap) o1;
            DataMap m2 = (DataMap) o2;
            int result = compareMaps(m1, m2);

            // resort to very bad and dumb alphabetic ordering
            if (result == 0) {
                result = m1.getName().compareTo(m2.getName());
            }

            return result;
        }

        /**
         * Checks if these 2 DataMaps have a dependency on each other.
         *
         * @return
         *  <ul>
         *  <li> -1 when m2 depends on m1
         *  <li> 1 when m1 depends on m2
         *  <li> 0 when dependency is undefined
         * </ul>
         */
        private final int compareMaps(DataMap m1, DataMap m2) {

            boolean hasDependent1 = m1.isDependentOn(m2);
            boolean hasDependent2 = m2.isDependentOn(m1);

            // ok if 1 map has a dependency and another does not,
            // the first one goes first
            if (hasDependent1 && !hasDependent2)
                return 1;

            if (!hasDependent1 && hasDependent2)
                return -1;

            // do not know what to do...
            return 0;
        }
    }
}