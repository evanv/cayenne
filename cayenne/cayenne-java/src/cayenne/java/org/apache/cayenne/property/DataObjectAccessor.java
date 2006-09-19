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

package org.apache.cayenne.property;

import org.apache.cayenne.DataObject;

/**
 * A PropertyAccessor that uses DataObject API to read/write values.
 * 
 * @author Andrus Adamchik
 */
public class DataObjectAccessor implements PropertyAccessor {

    protected String propertyName;

    public DataObjectAccessor(String propertyName) {

        if (propertyName == null) {
            throw new IllegalArgumentException("Null propertyName");
        }

        this.propertyName = propertyName;
    }

    public String getName() {
        return propertyName;
    }

    /**
     * Reads the value without disturbing DataObject state. I.e. no Fault resolving occurs
     * here.
     */
    public Object readPropertyDirectly(Object object) throws PropertyAccessException {
        try {

            DataObject dataObject = (DataObject) object;
            return dataObject.readPropertyDirectly(propertyName);
        }
        catch (ClassCastException e) {
            throw new PropertyAccessException("Object is not a DataObject: '"
                    + object.getClass().getName()
                    + "'", this, object, e);
        }
        catch (Throwable th) {
            throw new PropertyAccessException("Error reading DataObject property: "
                    + propertyName, this, object, th);
        }

        // TODO - see TODO in 'writeValue'
    }

    public void writePropertyDirectly(Object object, Object oldValue, Object newValue)
            throws PropertyAccessException {

        try {
            ((DataObject) object).writePropertyDirectly(propertyName, newValue);
        }
        catch (ClassCastException e) {
            throw new PropertyAccessException("Object is not a DataObject: '"
                    + object.getClass().getName()
                    + "'", this, object, e);
        }
        catch (Throwable th) {
            throw new PropertyAccessException("Error reading DataObject property: "
                    + propertyName, this, object, th);
        }

        // TODO, Andrus, 1/22/2006 - check for the right type? DataObject never did it
        // itself... Doing a check (and a conversion) may be an easy way to fix CAY-399
    }
}
