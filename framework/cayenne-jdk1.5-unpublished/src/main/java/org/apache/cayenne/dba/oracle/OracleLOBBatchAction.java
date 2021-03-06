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

package org.apache.cayenne.dba.oracle;

import java.io.OutputStream;
import java.io.Writer;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Collections;
import java.util.List;

import org.apache.cayenne.CayenneException;
import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.access.OperationObserver;
import org.apache.cayenne.access.trans.LOBBatchQueryBuilder;
import org.apache.cayenne.access.trans.LOBBatchQueryWrapper;
import org.apache.cayenne.access.trans.LOBInsertBatchQueryBuilder;
import org.apache.cayenne.access.trans.LOBUpdateBatchQueryBuilder;
import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.dba.JdbcAdapter;
import org.apache.cayenne.log.JdbcEventLogger;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.query.BatchQuery;
import org.apache.cayenne.query.InsertBatchQuery;
import org.apache.cayenne.query.SQLAction;
import org.apache.cayenne.query.UpdateBatchQuery;
import org.apache.cayenne.util.Util;

/**
 * @since 1.2
 */
class OracleLOBBatchAction implements SQLAction {

    BatchQuery query;
    DbAdapter adapter;

    protected JdbcEventLogger logger;

    OracleLOBBatchAction(BatchQuery query, JdbcAdapter adapter) {
        this.adapter = adapter;
        this.query = query;
        this.logger = adapter.getJdbcEventLogger();
    }

    DbAdapter getAdapter() {
        return adapter;
    }

    public void performAction(Connection connection, OperationObserver observer)
            throws SQLException, Exception {

        LOBBatchQueryBuilder queryBuilder;
        if (query instanceof InsertBatchQuery) {
            queryBuilder = new LOBInsertBatchQueryBuilder(getAdapter());
        }
        else if (query instanceof UpdateBatchQuery) {
            queryBuilder = new LOBUpdateBatchQueryBuilder(getAdapter());
        }
        else {
            throw new CayenneException(
                    "Unsupported batch type for special LOB processing: " + query);
        }

        queryBuilder.setTrimFunction(OracleAdapter.TRIM_FUNCTION);
        queryBuilder.setNewBlobFunction(OracleAdapter.NEW_BLOB_FUNCTION);
        queryBuilder.setNewClobFunction(OracleAdapter.NEW_CLOB_FUNCTION);

        // no batching is done, queries are translated
        // for each batch set, since prepared statements
        // may be different depending on whether LOBs are NULL or not..

        LOBBatchQueryWrapper selectQuery = new LOBBatchQueryWrapper(query);
        List<DbAttribute> qualifierAttributes = selectQuery
                .getDbAttributesForLOBSelectQualifier();

        boolean isLoggable = logger.isLoggable();

        query.reset();
        while (selectQuery.next()) {
            int updated = 0;
            String updateStr = queryBuilder.createSqlString(query);

            // 1. run row update
            logger.logQuery(updateStr, Collections.EMPTY_LIST);
            PreparedStatement statement = connection.prepareStatement(updateStr);
            try {

                if (isLoggable) {
                    List bindings = queryBuilder.getValuesForLOBUpdateParameters(query);
                    logger.logQueryParameters(
                            "bind",
                            null,
                            bindings,
                            query instanceof InsertBatchQuery);
                }

                queryBuilder.bindParameters(statement, query);
                updated = statement.executeUpdate();
                logger.logUpdateCount(updated);
            }
            finally {
                try {
                    statement.close();
                }
                catch (Exception e) {
                }
            }

            // 2. run row LOB update (SELECT...FOR UPDATE and writing out LOBs)
            processLOBRow(connection, queryBuilder, selectQuery, qualifierAttributes);

            // finally, notify delegate that the row was updated
            observer.nextCount(query, updated);
        }
    }

    void processLOBRow(
            Connection con,
            LOBBatchQueryBuilder queryBuilder,
            LOBBatchQueryWrapper selectQuery,
            List<DbAttribute> qualifierAttributes) throws SQLException, Exception {

        List<DbAttribute> lobAttributes = selectQuery
                .getDbAttributesForUpdatedLOBColumns();
        if (lobAttributes.size() == 0) {
            return;
        }

        boolean isLoggable = logger.isLoggable();

        List qualifierValues = selectQuery.getValuesForLOBSelectQualifier();
        List lobValues = selectQuery.getValuesForUpdatedLOBColumns();
        int parametersSize = qualifierValues.size();
        int lobSize = lobAttributes.size();

        String selectStr = queryBuilder.createLOBSelectString(
                selectQuery.getQuery(),
                lobAttributes,
                qualifierAttributes);

        if (isLoggable) {
            logger.logQuery(selectStr, qualifierValues);
            logger.logQueryParameters("write LOB", null, lobValues, false);
        }

        PreparedStatement selectStatement = con.prepareStatement(selectStr);
        try {
            for (int i = 0; i < parametersSize; i++) {
                Object value = qualifierValues.get(i);
                DbAttribute attribute = qualifierAttributes.get(i);

                adapter.bindParameter(
                        selectStatement,
                        value,
                        i + 1,
                        attribute.getType(),
                        attribute.getScale());
            }

            ResultSet result = selectStatement.executeQuery();

            try {
                if (!result.next()) {
                    throw new CayenneRuntimeException("Missing LOB row.");
                }

                // read the only expected row

                for (int i = 0; i < lobSize; i++) {
                    DbAttribute attribute = lobAttributes.get(i);
                    int type = attribute.getType();

                    if (type == Types.CLOB) {
                        Clob clob = result.getClob(i + 1);
                        Object clobVal = lobValues.get(i);

                        if (clobVal instanceof char[]) {
                            writeClob(clob, (char[]) clobVal);
                        }
                        else {
                            writeClob(clob, clobVal.toString());
                        }
                    }
                    else if (type == Types.BLOB) {
                        Blob blob = result.getBlob(i + 1);

                        Object blobVal = lobValues.get(i);
                        if (blobVal instanceof byte[]) {
                            writeBlob(blob, (byte[]) blobVal);
                        }
                        else {
                            String className = (blobVal != null) ? blobVal
                                    .getClass()
                                    .getName() : null;
                            throw new CayenneRuntimeException(
                                    "Unsupported class of BLOB value: " + className);
                        }
                    }
                    else {
                        throw new CayenneRuntimeException(
                                "Only BLOB or CLOB is expected here, got: " + type);
                    }
                }

                if (result.next()) {
                    throw new CayenneRuntimeException("More than one LOB row found.");
                }
            }
            finally {
                try {
                    result.close();
                }
                catch (Exception e) {
                }
            }
        }
        finally {
            try {
                selectStatement.close();
            }
            catch (Exception e) {
            }
        }
    }

    /**
     * Writing of LOBs is not supported prior to JDBC 3.0 and has to be done using Oracle
     * driver utilities, using reflection.
     */
    protected void writeBlob(Blob blob, byte[] value) {

        try {
            OutputStream out = blob.setBinaryStream(0);
            try {
                out.write(value);
                out.flush();
            }
            finally {
                out.close();
            }
        }
        catch (Exception e) {
            throw new CayenneRuntimeException(
                    "Error processing BLOB.",
                    Util.unwindException(e));
        }
    }

    /**
     * Writing of LOBs is not supported prior to JDBC 3.0 and has to be done using Oracle
     * driver utilities.
     */
    protected void writeClob(Clob clob, char[] value) {
        try {

            Writer out = clob.setCharacterStream(0);
            try {
                out.write(value);
                out.flush();
            }
            finally {
                out.close();
            }

        }
        catch (Exception e) {
            throw new CayenneRuntimeException(
                    "Error processing CLOB.",
                    Util.unwindException(e));
        }
    }

    /**
     * Writing of LOBs is not supported prior to JDBC 3.0 and has to be done using Oracle
     * driver utilities.
     */
    protected void writeClob(Clob clob, String value) {
        try {

            Writer out = clob.setCharacterStream(0);
            try {
                out.write(value);
                out.flush();
            }
            finally {
                out.close();
            }
        }
        catch (Exception e) {
            throw new CayenneRuntimeException(
                    "Error processing CLOB.",
                    Util.unwindException(e));
        }
    }
}
