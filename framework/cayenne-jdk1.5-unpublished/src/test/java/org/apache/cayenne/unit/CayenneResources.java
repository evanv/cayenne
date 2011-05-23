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

package org.apache.cayenne.unit;

import java.util.Map;

import org.apache.cayenne.access.jdbc.BatchQueryBuilderFactory;
import org.apache.cayenne.access.jdbc.DefaultBatchQueryBuilderFactory;
import org.apache.cayenne.dba.JdbcAdapter;

/**
 * Initializes connections for Cayenne unit tests.
 */
public class CayenneResources {

    protected Map<String, AccessStackAdapter> adapterMap;

    public CayenneResources(Map<String, AccessStackAdapter> adapterMap) {
        this.adapterMap = adapterMap;

        // kludge until we stop using Spring for unit tests and use Cayenne DI
        BatchQueryBuilderFactory factory = new DefaultBatchQueryBuilderFactory();
        for (AccessStackAdapter adapter : adapterMap.values()) {
            ((JdbcAdapter) adapter.getAdapter()).setBatchQueryBuilderFactory(factory);
        }

    }

    /**
     * Returns DB-specific testing adapter.
     */
    public AccessStackAdapter getAccessStackAdapter(String adapterClassName) {
        AccessStackAdapter stackAdapter = adapterMap.get(adapterClassName);

        if (stackAdapter == null) {
            throw new RuntimeException("No AccessStackAdapter for DbAdapter class: "
                    + adapterClassName);
        }

        return stackAdapter;
    }
}
