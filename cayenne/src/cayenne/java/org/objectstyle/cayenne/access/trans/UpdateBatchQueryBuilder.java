/* ====================================================================
 *
 * The ObjectStyle Group Software License, version 1.1
 * ObjectStyle Group - http://objectstyle.org/
 * 
 * Copyright (c) 2002-2004, Andrei (Andrus) Adamchik and individual authors
 * of the software. All rights reserved.
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
 * 3. The end-user documentation included with the redistribution, if any,
 *    must include the following acknowlegement:
 *    "This product includes software developed by independent contributors
 *    and hosted on ObjectStyle Group web site (http://objectstyle.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 * 
 * 4. The names "ObjectStyle Group" and "Cayenne" must not be used to endorse
 *    or promote products derived from this software without prior written
 *    permission. For written permission, email
 *    "andrus at objectstyle dot org".
 * 
 * 5. Products derived from this software may not be called "ObjectStyle"
 *    or "Cayenne", nor may "ObjectStyle" or "Cayenne" appear in their
 *    names without prior written permission.
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
 * individuals and hosted on ObjectStyle Group web site.  For more
 * information on the ObjectStyle Group, please see
 * <http://objectstyle.org/>.
 */

package org.objectstyle.cayenne.access.trans;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.objectstyle.cayenne.access.QueryLogger;
import org.objectstyle.cayenne.dba.DbAdapter;
import org.objectstyle.cayenne.map.DbAttribute;
import org.objectstyle.cayenne.query.BatchQuery;
import org.objectstyle.cayenne.query.UpdateBatchQuery;

/**
 * A translator for UpdateBatchQueries that produces parameterized SQL.
 *  
 * @author Andriy Shapochka, Andrei Adamchik, Mike Kienenberger
 */

public class UpdateBatchQueryBuilder extends BatchQueryBuilder {
    private static Logger logObj = Logger.getLogger(UpdateBatchQueryBuilder.class);

    public UpdateBatchQueryBuilder(DbAdapter adapter) {
        super(adapter);
    }

    public String createSqlString(BatchQuery batch) {
        UpdateBatchQuery updateBatch = (UpdateBatchQuery) batch;
        String table = batch.getDbEntity().getFullyQualifiedName();
        List idDbAttributes = updateBatch.getIdDbAttributes();
        List updatedDbAttributes = updateBatch.getUpdatedDbAttributes();
        StringBuffer query = new StringBuffer("UPDATE ");
        query.append(table).append(" SET ");

        int len = updatedDbAttributes.size();
        for (int i = 0; i < len; i++) {
            if (i > 0) {
                query.append(", ");
            }

            DbAttribute attribute = (DbAttribute) updatedDbAttributes.get(i);
            query.append(attribute.getName()).append(" = ?");
        }

        query.append(" WHERE ");
        int parameterIndex = len;
        Iterator i = idDbAttributes.iterator();
        while (i.hasNext()) {
            DbAttribute attribute = (DbAttribute) i.next();
            appendDbAttribute(query, attribute);
            if (updateBatch.isUsingOptimisticLocking()) {
                Object value = batch.getObject(parameterIndex++);
                if (null == value)
                    query.append(" IS NULL");
                else
                    query.append(" = ?");
            }
            else
                query.append(" = ?");
            if (i.hasNext()) {
                query.append(" AND ");
            }
        }
        return query.toString();
    }

    /**
     * Binds BatchQuery parameters to the PreparedStatement. 
     */
    public void bindParameters(
        PreparedStatement statement,
        BatchQuery query,
        List dbAttributes)
        throws SQLException, Exception {

        UpdateBatchQuery updateBatch = (UpdateBatchQuery) query;
        List idDbAttributes = updateBatch.getIdDbAttributes();
        List updatedDbAttributes = updateBatch.getUpdatedDbAttributes();

        int len = updatedDbAttributes.size();
        int parameterIndex = 0;
        for (int i = 0; i < len; i++) {
            Object value = query.getObject(i);

            DbAttribute attribute = (DbAttribute) updatedDbAttributes.get(i);
            adapter.bindParameter(
                statement,
                value,
                parameterIndex + 1,
                attribute.getType(),
                attribute.getPrecision());

            logQueryParameterInDetail(
                Level.DEBUG,
                "binding set",
                parameterIndex + 1,
                attribute.getType(),
                attribute.getName(),
                value);

            ++parameterIndex;
        }

        for (int i = 0; i < idDbAttributes.size(); i++) {
            Object value = query.getObject(len + i);
            if (null == value)
                continue;

            DbAttribute attribute = (DbAttribute) idDbAttributes.get(i);
            adapter.bindParameter(
                statement,
                value,
                parameterIndex + 1,
                attribute.getType(),
                attribute.getPrecision());

            logQueryParameterInDetail(
                Level.DEBUG,
                "binding id/lock",
                parameterIndex + 1,
                attribute.getType(),
                attribute.getName(),
                value);

            ++parameterIndex;
        }
    }

    // utility method to log batch bindings
    static void logQueryParameterInDetail(
        Level logLevel,
        String label,
        int parameterIndex,
        int attributeSqlType,
        String attributeName,
        Object value) {

        if (logObj.isEnabledFor(logLevel)) {
            StringBuffer buf = new StringBuffer("[");
            buf.append(label).append(": ");

            buf.append("parameter=");
            buf.append(parameterIndex);

            buf.append(", type=");
            buf.append(attributeSqlType);

            buf.append(", name=");
            buf.append(attributeName);

            buf.append(", value=");
            QueryLogger.sqlLiteralForObject(buf, value);

            buf.append(']');

            logObj.log(logLevel, buf.toString());
        }
    }

}