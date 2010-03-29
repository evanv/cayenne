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
package org.apache.cayenne.access.select;

import java.sql.ResultSet;

import org.apache.cayenne.CayenneException;
import org.apache.cayenne.access.types.ExtendedType;
import org.apache.cayenne.util.Util;

/**
 * @since 3.0
 */
class ScalarRowReader implements RowReader<Object> {

    private ExtendedType converter;
    private int index;
    private int jdbcType;

    ScalarRowReader(ExtendedType converter, int jdbcType) {
        this.converter = converter;
        this.index = 1;
        this.jdbcType = jdbcType;
    }
    
    public void setColumnOffset(int offset) {
        this.index = offset + 1;
    }

    public Object readRow(ResultSet resultSet) throws CayenneException {
        try {
            return converter.materializeObject(resultSet, index, jdbcType);
        }
        catch (CayenneException cex) {
            // rethrow unmodified
            throw cex;
        }
        catch (Exception otherex) {
            throw new CayenneException("Exception materializing column.", Util
                    .unwindException(otherex));
        }
    }
}
