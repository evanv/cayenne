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


package org.apache.cayenne.jpa.cspi;

import org.apache.cayenne.map.EntityDescriptor;
import org.apache.cayenne.map.EntityDescriptorFactory;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.property.ClassDescriptor;

public class CjpaClassDescriptorFactory extends EntityDescriptorFactory {

    public CjpaClassDescriptorFactory(EntityResolver resolver) {
        super(resolver);
    }

    @Override
    protected EntityDescriptor createDescriptor(String entityName) {
        ObjEntity entity = resolver.getObjEntity(entityName);
        if (entity == null) {
            return null;
        }

        String superEntityName = entity.getSuperEntityName();

        ClassDescriptor superDescriptor = (superEntityName != null) ? resolver
                .getClassDescriptor(superEntityName) : null;

        // return uncompiled
        return new CjpaEntityDescriptor(entity, superDescriptor);
    }

}
