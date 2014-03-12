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

package org.apache.cayenne.dba.openbase;

import java.sql.Connection;

import org.apache.cayenne.access.jdbc.RowReaderFactory;
import org.apache.cayenne.access.jdbc.SelectAction;
import org.apache.cayenne.access.trans.SelectTranslator;
import org.apache.cayenne.dba.JdbcActionBuilder;
import org.apache.cayenne.dba.JdbcAdapter;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.query.SQLAction;
import org.apache.cayenne.query.SelectQuery;

/**
 * @since 1.2
 */
class OpenBaseActionBuilder extends JdbcActionBuilder {

    OpenBaseActionBuilder(JdbcAdapter adapter, EntityResolver resolver, RowReaderFactory rowReaderFactory) {
        super(adapter, resolver, rowReaderFactory);
    }

    @Override
    public <T> SQLAction objectSelectAction(SelectQuery<T> query) {
        return new SelectAction(query, adapter, entityResolver, rowReaderFactory) {

            @Override
            protected SelectTranslator createTranslator(Connection connection) {
                SelectTranslator translator = new OpenBaseSelectTranslator();
                translator.setQuery(query);
                translator.setAdapter(adapter);
                translator.setEntityResolver(getEntityResolver());
                translator.setConnection(connection);
                translator.setJdbcEventLogger(logger);
                return translator;
            }
        };
    }
}
