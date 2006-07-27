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

package org.apache.cayenne.jpa.map;

import java.util.ArrayList;
import java.util.Collection;

import javax.persistence.OneToOne;

import org.apache.cayenne.util.TreeNodeChild;

public class JpaOneToOne extends JpaRelationship {

    protected String mappedBy;
    protected boolean optional;

    protected Collection<JpaPrimaryKeyJoinColumn> primaryKeyJoinColumns;
    protected Collection<JpaJoinColumn> joinColumns;
    protected JpaJoinTable joinTable;

    public JpaOneToOne() {

    }

    public JpaOneToOne(OneToOne annotation) {
        if (!Void.TYPE.equals(annotation.targetEntity())) {
            this.targetEntityName = annotation.targetEntity().getName();
        }

        // resolve internal collection
        getCascades();
        for (int i = 0; i < annotation.cascade().length; i++) {
            cascades.add(annotation.cascade()[i]);
        }

        fetch = annotation.fetch();
        optional = annotation.optional();
        mappedBy = annotation.mappedBy();
    }

    @Override
    public boolean isToMany() {
        return false;
    }

    public boolean isOptional() {
        return optional;
    }

    public void setOptional(boolean optional) {
        this.optional = optional;
    }

    public String getMappedBy() {
        return mappedBy;
    }

    public void setMappedBy(String mappedBy) {
        this.mappedBy = mappedBy;
    }

    @TreeNodeChild(type = JpaPrimaryKeyJoinColumn.class)
    public Collection<JpaPrimaryKeyJoinColumn> getPrimaryKeyJoinColumns() {
        if (primaryKeyJoinColumns == null) {
            primaryKeyJoinColumns = new ArrayList<JpaPrimaryKeyJoinColumn>();
        }
        return primaryKeyJoinColumns;
    }

    @TreeNodeChild(type = JpaJoinColumn.class)
    public Collection<JpaJoinColumn> getJoinColumns() {
        if (joinColumns == null) {
            joinColumns = new ArrayList<JpaJoinColumn>();
        }
        return joinColumns;
    }

    public JpaJoinTable getJoinTable() {
        return joinTable;
    }

    public void setJoinTable(JpaJoinTable joinTable) {
        this.joinTable = joinTable;
    }
}
