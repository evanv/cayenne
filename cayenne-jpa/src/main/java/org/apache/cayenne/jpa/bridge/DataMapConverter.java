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

package org.apache.cayenne.jpa.bridge;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;

import javax.persistence.TemporalType;

import org.apache.cayenne.jpa.JpaProviderException;
import org.apache.cayenne.jpa.conf.EntityMapLoaderContext;
import org.apache.cayenne.jpa.map.AccessType;
import org.apache.cayenne.jpa.map.JpaBasic;
import org.apache.cayenne.jpa.map.JpaColumn;
import org.apache.cayenne.jpa.map.JpaEntity;
import org.apache.cayenne.jpa.map.JpaEntityMap;
import org.apache.cayenne.jpa.map.JpaId;
import org.apache.cayenne.jpa.map.JpaJoinColumn;
import org.apache.cayenne.jpa.map.JpaManyToMany;
import org.apache.cayenne.jpa.map.JpaManyToOne;
import org.apache.cayenne.jpa.map.JpaNamedQuery;
import org.apache.cayenne.jpa.map.JpaOneToMany;
import org.apache.cayenne.jpa.map.JpaOneToOne;
import org.apache.cayenne.jpa.map.JpaQueryHint;
import org.apache.cayenne.jpa.map.JpaRelationship;
import org.apache.cayenne.jpa.map.JpaTable;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbJoin;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.map.ObjAttribute;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.ObjRelationship;
import org.apache.cayenne.project.ProjectPath;
import org.apache.cayenne.util.BaseTreeVisitor;
import org.apache.cayenne.util.HierarchicalTreeVisitor;
import org.apache.cayenne.util.TraversalUtil;
import org.apache.cayenne.util.Util;
import org.apache.cayenne.validation.SimpleValidationFailure;

/**
 * A converter between {@link org.apache.cayenne.jpa.map.JpaEntityMap} and Cayenne
 * {@link org.objectstyle.cayenne.map.DataMap}.
 * 
 * @author Andrus Adamchik
 */
public class DataMapConverter {

    protected EntityMapLoaderContext context;

    protected ProjectPath targetPath;
    protected HierarchicalTreeVisitor visitor;

    public synchronized DataMap toDataMap(String name, EntityMapLoaderContext context) {
        this.context = context;

        // reset
        DataMap dataMap = new DataMap(name);
        dataMap.setDefaultPackage(context.getEntityMap().getPackageName());
        dataMap.setDefaultSchema(context.getEntityMap().getSchema());

        this.targetPath = new ProjectPath(dataMap);

        if (visitor == null) {
            visitor = createVisitor();
        }

        TraversalUtil.traverse(context.getEntityMap(), visitor);
        return dataMap;
    }

    protected void recordConflict(ProjectPath path, String message) {
        context.recordConflict(new SimpleValidationFailure(path.getObject(), message));
    }

    /**
     * Creates a stateless instance of the JpaEntityMap traversal visitor. This method is
     * lazily invoked and cached by this object.
     */
    protected HierarchicalTreeVisitor createVisitor() {
        BaseTreeVisitor visitor = new BaseTreeVisitor();
        visitor.addChildVisitor(JpaEntity.class, new JpaEntityVisitor());
        visitor.addChildVisitor(JpaNamedQuery.class, new JpaNamedQueryVisitor());
        return visitor;
    }

    // class JpaMany

    class JpaBasicVisitor extends NestedVisitor {

        JpaBasicVisitor() {
            addChildVisitor(JpaColumn.class, new JpaColumnVisitor());
        }

        @Override
        Object createObject(ProjectPath path) {

            JpaBasic jpaBasic = (JpaBasic) path.getObject();

            ObjEntity parentCayenneEntity = (ObjEntity) targetPath.getObject();

            ObjAttribute cayenneAttribute = new ObjAttribute(jpaBasic.getName());
            cayenneAttribute
                    .setType(getAttributeType(path, jpaBasic.getName()).getName());
            cayenneAttribute.setDbAttributeName(jpaBasic.getColumn().getName());

            parentCayenneEntity.addAttribute(cayenneAttribute);
            return cayenneAttribute;

        }

        Class getAttributeType(ProjectPath path, String name) {
            AccessType access = ((JpaEntityMap) path.getRoot()).getAccess();

            Class objectClass = ((ObjEntity) targetPath.firstInstanceOf(ObjEntity.class))
                    .getJavaClass();

            try {
                if (access == AccessType.FIELD) {
                    return lookupFieldInHierarchy(objectClass, name).getType();
                }
                else {
                    return new PropertyDescriptor(name, objectClass).getPropertyType();
                }
            }
            catch (Exception e) {
                throw new JpaProviderException("Error resolving attribute '"
                        + name
                        + "', access type:"
                        + access, e);
            }
        }

        Field lookupFieldInHierarchy(Class beanClass, String fieldName)
                throws SecurityException, NoSuchFieldException {

            try {
                return beanClass.getDeclaredField(fieldName);
            }
            catch (NoSuchFieldException e) {

                Class superClass = beanClass.getSuperclass();
                if (superClass == null
                        || superClass.getName().equals(Object.class.getName())) {
                    throw e;
                }

                return lookupFieldInHierarchy(superClass, fieldName);
            }
        }
    }

    class JpaColumnVisitor extends BaseTreeVisitor {

        @Override
        public boolean onStartNode(ProjectPath path) {
            JpaColumn jpaColumn = (JpaColumn) path.getObject();
            JpaBasic jpaBasic = (JpaBasic) path.getObjectParent();

            DbAttribute dbAttribute = new DbAttribute(jpaColumn.getName());

            TemporalType ttype = jpaBasic.getTemporal();

            dbAttribute.setType(jpaBasic.getPropertyDescriptor().getJdbcType(ttype));
            dbAttribute.setMandatory(!jpaColumn.isNullable());
            dbAttribute.setMaxLength(jpaColumn.getLength());

            // TODO, andrus, 4/28/2006 - note that Cayenne DbAttribute's precision is
            // really scale (and precision is not defined at all). Fix this in
            // DbAttribute.
            dbAttribute.setPrecision(jpaColumn.getScale());

            if (jpaColumn.getTable() == null) {
                throw new JpaProviderException("No default table defined for JpaColumn "
                        + jpaColumn.getName());
            }

            DbEntity entity = ((DataMap) targetPath.firstInstanceOf(DataMap.class))
                    .getDbEntity(jpaColumn.getTable());

            if (entity == null) {
                throw new JpaProviderException("No DbEntity defined for table  "
                        + jpaColumn.getTable());
            }

            entity.addAttribute(dbAttribute);

            return false;
        }
    }

    class JpaIdColumnVisitor extends BaseTreeVisitor {

        @Override
        public boolean onStartNode(ProjectPath path) {
            JpaColumn jpaColumn = (JpaColumn) path.getObject();

            DbAttribute dbAttribute = new DbAttribute(jpaColumn.getName());

            JpaId jpaId = (JpaId) path.firstInstanceOf(JpaId.class);

            dbAttribute.setType(jpaId.getPropertyDescriptor().getJdbcType(
                    jpaId.getTemporal()));

            dbAttribute.setMaxLength(jpaColumn.getLength());
            dbAttribute.setMandatory(true);
            dbAttribute.setPrimaryKey(true);

            // TODO, andrus, 4/28/2006 - note that Cayenne DbAttribute's precision is
            // really scale (and precision is not defined at all). Fix this in
            // DbAttribute.
            dbAttribute.setPrecision(jpaColumn.getScale());

            if (jpaColumn.getTable() == null) {
                recordConflict(path, "No table defined for JpaColumn '"
                        + jpaColumn.getName());
                return false;
            }

            DbEntity entity = ((DataMap) targetPath.firstInstanceOf(DataMap.class))
                    .getDbEntity(jpaColumn.getTable());

            if (entity == null) {
                recordConflict(path, "Invalid table definition for JpaColumn: "
                        + jpaColumn.getTable());
                return false;
            }

            entity.addAttribute(dbAttribute);
            return false;
        }
    }

    class JpaJoinColumnVisitor extends BaseTreeVisitor {

        @Override
        public boolean onStartNode(ProjectPath path) {

            JpaJoinColumn jpaJoin = (JpaJoinColumn) path.getObject();
            JpaRelationship jpaRelationship = (JpaRelationship) path.getObjectParent();
            JpaEntity targetEntity = context.getEntityMap().entityForClass(
                    jpaRelationship.getTargetEntityName());
            JpaId jpaTargetId = targetEntity.getAttributes().getId(
                    jpaJoin.getReferencedColumnName());

            ObjRelationship objRelationship = (ObjRelationship) targetPath.getObject();
            DataMap dataMap = objRelationship.getSourceEntity().getDataMap();

            // add FK
            DbAttribute src = new DbAttribute(jpaJoin.getName());

            // TODO: andrus, 5/2/2006 - infer this from Jpa relationship
            src.setMandatory(false);

            src.setMaxLength(jpaTargetId.getColumn().getLength());
            src.setType(jpaTargetId.getPropertyDescriptor().getJdbcType(
                    jpaTargetId.getTemporal()));

            DbEntity srcEntity = dataMap.getDbEntity(jpaJoin.getTable());
            srcEntity.addAttribute(src);

            // add join
            DbRelationship dbRelationship = (DbRelationship) objRelationship
                    .getDbRelationships()
                    .get(0);

            DbRelationship reverseRelationship = dbRelationship.getReverseRelationship();
            if (reverseRelationship == null) {
                reverseRelationship = dbRelationship.createReverseRelationship();
            }

            DbJoin join = new DbJoin(dbRelationship, src.getName(), jpaTargetId
                    .getColumn()
                    .getName());
            DbJoin reverseJoin = join.createReverseJoin();
            reverseJoin.setRelationship(reverseRelationship);

            dbRelationship.addJoin(join);
            reverseRelationship.addJoin(reverseJoin);

            return false;
        }
    }

    class JpaEntityVisitor extends NestedVisitor {

        JpaEntityVisitor() {
            BaseTreeVisitor attributeVisitor = new BaseTreeVisitor();
            attributeVisitor.addChildVisitor(
                    JpaManyToOne.class,
                    new JpaRelationshipVisitor());
            attributeVisitor.addChildVisitor(
                    JpaOneToOne.class,
                    new JpaRelationshipVisitor());
            attributeVisitor.addChildVisitor(
                    JpaOneToMany.class,
                    new JpaRelationshipVisitor());
            attributeVisitor.addChildVisitor(
                    JpaManyToMany.class,
                    new JpaRelationshipVisitor());
            attributeVisitor.addChildVisitor(JpaBasic.class, new JpaBasicVisitor());

            // addChildVisitor(JpaAttribute.class, attributeVisitor);

            BaseTreeVisitor idVisitor = new BaseTreeVisitor();
            idVisitor.addChildVisitor(JpaColumn.class, new JpaIdColumnVisitor());
            addChildVisitor(JpaId.class, idVisitor);

            addChildVisitor(JpaTable.class, new JpaTableVisitor());
            addChildVisitor(JpaNamedQuery.class, new JpaNamedQueryVisitor());
        }

        @Override
        Object createObject(ProjectPath path) {
            JpaEntity jpaEntity = (JpaEntity) path.getObject();
            ObjEntity cayenneEntity = new ObjEntity(jpaEntity.getName());
            cayenneEntity.setClassName(jpaEntity.getClassName());

            ((DataMap) targetPath.getObject()).addObjEntity(cayenneEntity);

            return cayenneEntity;
        }
    }

    class JpaRelationshipVisitor extends NestedVisitor {

        JpaRelationshipVisitor() {
            addChildVisitor(JpaJoinColumn.class, new JpaJoinColumnVisitor());
        }

        @Override
        Object createObject(ProjectPath path) {

            JpaRelationship relationship = (JpaRelationship) path.getObject();

            ObjEntity cayenneSrcEntity = (ObjEntity) targetPath.getObject();
            ObjRelationship cayenneRelationship = new ObjRelationship(relationship
                    .getName());

            cayenneSrcEntity.addRelationship(cayenneRelationship);

            JpaEntity jpaTargetEntity = ((JpaEntityMap) path.getRoot())
                    .entityForClass(relationship.getTargetEntityName());

            if (jpaTargetEntity == null) {
                recordConflict(path, "Unknown target entity '"
                        + relationship.getTargetEntityName());
                return null;
            }

            cayenneRelationship.setTargetEntityName(jpaTargetEntity.getName());

            // TODO: db relationship should probably be created when the first join is
            // created...
            DbEntity cayenneSrcDbEntity = cayenneSrcEntity.getDbEntity();

            DbEntity cayenneTargetDbEntity = cayenneSrcEntity.getDataMap().getDbEntity(
                    jpaTargetEntity.getTable().getName());
            if (cayenneTargetDbEntity == null) {
                cayenneTargetDbEntity = new DbEntity(jpaTargetEntity.getTable().getName());
                cayenneSrcEntity.getDataMap().addDbEntity(cayenneTargetDbEntity);
            }

            DbRelationship dbRelationship = new DbRelationship(cayenneRelationship
                    .getName());
            dbRelationship.setTargetEntity(cayenneTargetDbEntity);
            dbRelationship.setToMany(relationship.isToMany());

            cayenneSrcDbEntity.addRelationship(dbRelationship);
            cayenneRelationship.addDbRelationship(dbRelationship);

            return cayenneRelationship;
        }
    }

    class JpaNamedQueryVisitor extends NestedVisitor {

        @Override
        Object createObject(ProjectPath path) {
            JpaNamedQuery jpaQuery = (JpaNamedQuery) path.getObject();
            JpaIndirectQuery cayenneQuery;

            JpaQueryHint hint = jpaQuery.getHint(QueryHints.QUERY_TYPE_HINT);
            if (hint != null && !Util.isEmptyString(hint.getValue())) {
                try {

                    // query class is not enhanced, so use normal class loader
                    Class cayenneQueryClass = Class.forName(hint.getValue(), true, Thread
                            .currentThread()
                            .getContextClassLoader());

                    if (!JpaIndirectQuery.class.isAssignableFrom(cayenneQueryClass)) {
                        recordConflict(path, "Unknown type for Cayenne query '"
                                + jpaQuery.getName()
                                + "': "
                                + cayenneQueryClass.getName());
                        return null;
                    }

                    cayenneQuery = (JpaIndirectQuery) cayenneQueryClass.newInstance();
                }
                catch (Exception e) {
                    recordConflict(path, "Problem while creating Cayenne query '"
                            + jpaQuery.getName()
                            + "', exception"
                            + e.getMessage());
                    return null;
                }
            }
            else {
                // by default use EJBQL query...
                cayenneQuery = new JpaEjbQLQuery();
            }

            cayenneQuery.setName(jpaQuery.getName());
            cayenneQuery.setJpaQuery(jpaQuery);

            DataMap parentMap = (DataMap) targetPath.firstInstanceOf(DataMap.class);

            ObjEntity parentEntity = (ObjEntity) targetPath
                    .firstInstanceOf(ObjEntity.class);
            if (parentEntity != null) {
                cayenneQuery.setParentEntity(parentEntity);
            }
            else {
                cayenneQuery.setParentMap(parentMap);
            }

            parentMap.addQuery(cayenneQuery);

            return cayenneQuery;
        }
    }

    class JpaTableVisitor extends NestedVisitor {

        @Override
        Object createObject(ProjectPath path) {

            JpaTable jpaTable = (JpaTable) path.getObject();
            ObjEntity parentCayenneEntity = (ObjEntity) targetPath.getObject();

            DbEntity cayenneEntity = parentCayenneEntity.getDataMap().getDbEntity(
                    jpaTable.getName());
            if (cayenneEntity == null) {
                cayenneEntity = new DbEntity(jpaTable.getName());
                parentCayenneEntity.getDataMap().addDbEntity(cayenneEntity);
            }

            cayenneEntity.setCatalog(jpaTable.getCatalog());
            cayenneEntity.setSchema(jpaTable.getSchema());

            parentCayenneEntity.setDbEntity(cayenneEntity);
            return cayenneEntity;
        }
    }

    /**
     * A superclass of visitors that need to push/pop processed object from the stack.
     */
    abstract class NestedVisitor extends BaseTreeVisitor {

        abstract Object createObject(ProjectPath path);

        @Override
        public boolean onStartNode(ProjectPath path) {
            Object object = createObject(path);

            if (object != null) {
                targetPath = targetPath.appendToPath(object);
                return true;
            }
            else {
                return false;
            }
        }

        @Override
        public void onFinishNode(ProjectPath path) {
            targetPath = targetPath.subpathWithSize(targetPath.getPath().length - 1);
        }
    }
}
