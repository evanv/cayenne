/* ====================================================================
 * 
 * The ObjectStyle Group Software License, Version 1.0 
 *
 * Copyright (c) 2002-2003 The ObjectStyle Group 
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
package org.objectstyle.cayenne.wocompat;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.objectstyle.cayenne.CayenneRuntimeException;
import org.objectstyle.cayenne.dba.TypesMapping;
import org.objectstyle.cayenne.exp.Expression;
import org.objectstyle.cayenne.exp.ExpressionFactory;
import org.objectstyle.cayenne.map.DataMap;
import org.objectstyle.cayenne.map.DbAttribute;
import org.objectstyle.cayenne.map.DbAttributePair;
import org.objectstyle.cayenne.map.DbEntity;
import org.objectstyle.cayenne.map.DbRelationship;
import org.objectstyle.cayenne.map.ObjAttribute;
import org.objectstyle.cayenne.map.ObjEntity;
import org.objectstyle.cayenne.map.ObjRelationship;

/**
 *  Class that converts EOModels to org.objectstyle.cayenne.map.DataMap objects.
 */
public class EOModelProcessor {

    /** Performs EOModel loading.  
     * 
     *  @param path A path to ".eomodeld" directory. 
     *  If path doesn't end with ".eomodeld", ".eomodeld"
     *  suffix is automatically assumed. */
    public DataMap loadEOModel(String path) throws Exception {
        EOModelHelper helper = makeHelper(path);

        // create empty map
        DataMap dataMap = helper.getDataMap();

        // process enitities
        Iterator it = helper.modelNames();
        while (it.hasNext()) {
            String name = (String) it.next();

            // create and register entity
            ObjEntity e = makeEntity(helper, name);

            // process entity attributes
            makeAttributes(helper, e);
        }

        // after all entities are loaded, 
        // process relationships
        it = helper.modelNames();
        while (it.hasNext()) {
            String name = (String) it.next();
            makeRelationships(helper, dataMap.getObjEntity(name));
        }

        // after all normal relationships are loaded, 
        // process falttened relationships
        it = helper.modelNames();
        while (it.hasNext()) {
            String name = (String) it.next();
            makeFlatRelationships(helper, dataMap.getObjEntity(name));
        }

        return dataMap;
    }

    /** 
     * Creates an returns new EOModelHelper to process EOModel.
     * Exists mostly for the benefit of subclasses. 
     */
    protected EOModelHelper makeHelper(String path) throws Exception {
        return new EOModelHelper(path);
    }

    /** 
     *  Creates and returns a new ObjEntity linked to a corresponding DbEntity.
     */
    protected ObjEntity makeEntity(EOModelHelper helper, String name) {
        DataMap dataMap = helper.getDataMap();

        // create ObjEntity
        ObjEntity e = new ObjEntity(name);
        e.setClassName(helper.entityClass(name));

        // create DbEntity...since EOF allows the same table to be 
        // associated with multiple EOEntities, check for name duplicates
        String dbEntityName = (String) helper.entityInfo(name).get("externalName");

        if (dbEntityName != null) {
            int i = 0;
            String dbEntityBaseName = dbEntityName;
            while (dataMap.getDbEntity(dbEntityName, false) != null) {
                dbEntityName = dbEntityBaseName + i++;
            }

            DbEntity de = new DbEntity(dbEntityName);
            dataMap.addDbEntity(de);
            e.setDbEntity(de);
        }

        dataMap.addObjEntity(e);

        return e;
    }

    /** 
     *  Create ObjAttributes of the specified entity, as well as 
     *  DbAttributes of the corresponding DbEntity.
     */
    protected void makeAttributes(EOModelHelper helper, ObjEntity e) {
        Map entityMap = helper.entityInfo(e.getName());
        List pks = (List) entityMap.get("primaryKeyAttributes");
        List classProps = (List) entityMap.get("classProperties");
        List attributes = (List) entityMap.get("attributes");
        DbEntity dbEntity = e.getDbEntity();

        if (pks == null) {
            pks = Collections.EMPTY_LIST;
        }

        if (classProps == null) {
            classProps = Collections.EMPTY_LIST;
        }

        if (attributes == null) {
            attributes = Collections.EMPTY_LIST;
        }

        // process attribute list creating both Db and Obj attributes

        if (attributes == null) {
            return;
        }

        Iterator it = attributes.iterator();
        while (it.hasNext()) {
            Map attrMap = (Map) it.next();
            String dbAttrName = (String) attrMap.get("columnName");
            String attrName = (String) attrMap.get("name");
            String attrType = (String) attrMap.get("valueClassName");
            String javaType = helper.javaTypeForEOModelerType(attrType);
            EODbAttribute dbAttr = null;

            if (dbAttrName != null && dbEntity != null) {

                // create DbAttribute...since EOF allows the same column name for 
                // more than one Java attribute, we need to check for name duplicates
                int i = 0;
                String dbAttributeBaseName = dbAttrName;
                while (dbEntity.getAttribute(dbAttrName) != null) {
                    dbAttrName = dbAttributeBaseName + i++;
                }

                dbAttr =
                    new EODbAttribute(
                        dbAttrName,
                        TypesMapping.getSqlTypeByJava(javaType),
                        dbEntity);
                dbAttr.setEoAttributeName(attrName);
                dbEntity.addAttribute(dbAttr);

                Integer width = (Integer) attrMap.get("width");
                if (width != null)
                    dbAttr.setMaxLength(width.intValue());

                if (pks.contains(attrName))
                    dbAttr.setPrimaryKey(true);

                Object allowsNull = attrMap.get("allowsNull");
                dbAttr.setMandatory(!"Y".equals(allowsNull));
            }

            if (classProps.contains(attrName)) {
                ObjAttribute attr = new ObjAttribute(attrName, javaType, e);
                attr.setDbAttribute(dbAttr);
                e.addAttribute(attr);
            }
        }
    }

    /** 
     *  Create ObjRelationships of the specified entity, as well as 
     *  DbRelationships of the corresponding DbEntity.
     */
    protected void makeRelationships(EOModelHelper helper, ObjEntity e) {
        Map info = helper.entityInfo(e.getName());
        List classProps = (List) info.get("classProperties");
        List rinfo = (List) info.get("relationships");

        if (rinfo == null) {
            return;
        }

        if (classProps == null) {
            classProps = Collections.EMPTY_LIST;
        }

        DbEntity dbSrc = e.getDbEntity();
        Iterator it = rinfo.iterator();
        while (it.hasNext()) {
            Map relMap = (Map) it.next();
            String targetName = (String) relMap.get("destination");

            // ignore flattened relationships for now
            if (targetName == null) {
                continue;
            }

            String relName = (String) relMap.get("name");
            boolean toMany = "Y".equals(relMap.get("isToMany"));
            boolean toDependentPK = "Y".equals(relMap.get("propagatesPrimaryKey"));
            ObjEntity target = helper.getDataMap().getObjEntity(targetName);

            if (target == null) {
                continue;
            }

            DbEntity dbTarget = target.getDbEntity();
            DbRelationship dbRel = null;
            // process underlying DbRelationship
            // Note - there is no flattened rel. support here....
            if (dbTarget != null) {
                dbRel = new DbRelationship();
                dbRel.setSourceEntity(dbSrc);
                dbRel.setTargetEntity(dbTarget);
                dbRel.setToMany(toMany);
                dbRel.setName(relName);
                dbRel.setToDependentPK(toDependentPK);
                dbSrc.addRelationship(dbRel);

                List joins = (List) relMap.get("joins");
                Iterator jIt = joins.iterator();
                while (jIt.hasNext()) {
                    Map joinMap = (Map) jIt.next();
                    String srcAttrName = (String) joinMap.get("sourceAttribute");
                    String targetAttrName = (String) joinMap.get("destinationAttribute");

                    DbAttribute srcAttr =
                        EODbAttribute.findForEOAttributeName(dbSrc, srcAttrName);
                    DbAttribute targetAttr =
                        EODbAttribute.findForEOAttributeName(dbTarget, targetAttrName);

                    DbAttributePair join = new DbAttributePair(srcAttr, targetAttr);
                    dbRel.addJoin(join);
                }
            }

            // only create obj relationship if it is a class property

            if (classProps.contains(relName)) {
                ObjRelationship rel = new ObjRelationship();
                rel.setName(relName);
                rel.setSourceEntity(e);
                rel.setTargetEntity(target);
                e.addRelationship(rel);

                if (dbRel != null) {
                    rel.addDbRelationship(dbRel);
                }
            }
        }
    }

    /** 
     *  Create Flattened ObjRelationships of the specified entity.
     */
    protected void makeFlatRelationships(EOModelHelper helper, ObjEntity e) {
        Map info = helper.entityInfo(e.getName());
        List rinfo = (List) info.get("relationships");
        if (rinfo == null) {
            return;
        }

        Iterator it = rinfo.iterator();
        while (it.hasNext()) {
            Map relMap = (Map) it.next();
            String targetPath = (String) relMap.get("definition");

            // ignore normal relationships
            if (targetPath == null) {
                continue;
            }

            Expression exp = ExpressionFactory.unaryExp(Expression.DB_PATH, targetPath);
            Iterator path = e.getDbEntity().resolvePathComponents(exp);

            ObjRelationship flatRel = new ObjRelationship();
            flatRel.setName((String) relMap.get("name"));

            DbRelationship firstRel = null;
            DbRelationship lastRel = null;
            while (path.hasNext()) {
                lastRel = (DbRelationship) path.next();
                flatRel.addDbRelationship(lastRel);

                if (firstRel == null) {
                    firstRel = lastRel;
                }
            }

            if ((firstRel != null) && (lastRel != null)) {
                flatRel.setSourceEntity(e);

                List potentialTargets =
                    e.getDataMap().getMappedEntities(
                        (DbEntity) lastRel.getTargetEntity());

                // sanity check
                if (potentialTargets.size() != 1) {
                    throw new CayenneRuntimeException(
                        "One and only one entity should be mapped"
                            + " to "
                            + lastRel.getTargetEntity().getName()
                            + ". Instead found : "
                            + potentialTargets.size());
                }

                flatRel.setTargetEntity((ObjEntity) potentialTargets.get(0));

                e.addRelationship(flatRel);
            }
            else {
                throw new CayenneRuntimeException("relationship in path was null!");
            }
        }
    }

    /** 
     *  Special DbAttribute subclass that stores extra info needed to work
     *  with EOModels.
     */
    static class EODbAttribute extends DbAttribute {
        protected String eoAttributeName;

        public static DbAttribute findForEOAttributeName(DbEntity e, String name) {
            Iterator it = e.getAttributes().iterator();
            while (it.hasNext()) {
                EODbAttribute attr = (EODbAttribute) it.next();
                if (name.equals(attr.getEoAttributeName())) {
                    return attr;
                }
            }
            return null;
        }

        public EODbAttribute() {
        }

        public EODbAttribute(String name, int type, DbEntity entity) {
            super(name, type, entity);
        }

        public String getEoAttributeName() {
            return eoAttributeName;
        }

        public void setEoAttributeName(String eoAttributeName) {
            this.eoAttributeName = eoAttributeName;
        }
    }
}