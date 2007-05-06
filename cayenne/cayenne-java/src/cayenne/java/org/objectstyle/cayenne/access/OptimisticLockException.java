/* ====================================================================
 *
 * The ObjectStyle Group Software License, version 1.1
 * ObjectStyle Group - http://objectstyle.org/
 * 
 * Copyright (c) 2002-2005, Andrei (Andrus) Adamchik and individual authors
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
package org.objectstyle.cayenne.access;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.objectstyle.cayenne.CayenneRuntimeException;
import org.objectstyle.cayenne.exp.Expression;
import org.objectstyle.cayenne.exp.ExpressionFactory;
import org.objectstyle.cayenne.map.DbAttribute;
import org.objectstyle.cayenne.map.DbEntity;
import org.objectstyle.cayenne.query.SelectQuery;

/**
 * An exception thrown on optimistic lock failure.
 * 
 * @since 1.1
 * @author Andrei Adamchik
 */
public class OptimisticLockException extends CayenneRuntimeException {

    protected String querySQL;
    protected DbEntity rootEntity;
    protected Map qualifierSnapshot;

    public OptimisticLockException(DbEntity rootEntity, String querySQL,
            Map qualifierSnapshot) {
        super("Optimistic Lock Failure");

        this.rootEntity = rootEntity;
        this.querySQL = querySQL;
        this.qualifierSnapshot = (qualifierSnapshot != null)
                ? qualifierSnapshot
                : Collections.EMPTY_MAP;
    }

    public Map getQualifierSnapshot() {
        return qualifierSnapshot;
    }

    public String getQuerySQL() {
        return querySQL;
    }

    /**
     * Retrieves fresh snapshot for the failed row. Null row indicates that it was
     * deleted.
     */
    // TODO: andrus, 5/30/2006 - use DataChannel instead of QE as a parameter after 1.2
    public Map getFreshSnapshot(QueryEngine engine) {

        // extract PK from the qualifierSnapshot and fetch a row
        // for PK, ignoring other locking attributes...

        Expression qualifier = null;
        Iterator it = rootEntity.getPrimaryKey().iterator();
        while (it.hasNext()) {
            DbAttribute attribute = (DbAttribute) it.next();
            Expression attributeQualifier = ExpressionFactory.matchDbExp(attribute
                    .getName(), qualifierSnapshot.get(attribute.getName()));

            qualifier = (qualifier != null)
                    ? qualifier.andExp(attributeQualifier)
                    : attributeQualifier;
        }

        SelectQuery query = new SelectQuery(rootEntity, qualifier);
        query.setFetchingDataRows(true);
        QueryResult observer = new QueryResult();
        engine.performQueries(Collections.singletonList(query), observer);
        List results = observer.getFirstRows(query);

        if (results == null || results.isEmpty()) {
            return null;
        }
        else if (results.size() > 1) {
            throw new CayenneRuntimeException("More than one row for ObjectId.");
        }
        else {
            return (Map) results.get(0);
        }
    }

    /**
     * Returns descriptive message for this exception.
     */
    public String getMessage() {
        StringBuffer buffer = new StringBuffer(super.getMessage());

        if (querySQL != null) {
            buffer.append(", SQL: [").append(querySQL.trim()).append("]");
        }

        if (!qualifierSnapshot.isEmpty()) {
            buffer.append(", WHERE clause bindings: [");
            Iterator it = qualifierSnapshot.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry entry = (Map.Entry) it.next();
                buffer.append(entry.getKey()).append("=");
                QueryLogger.sqlLiteralForObject(buffer, entry.getValue());

                if (it.hasNext()) {
                    buffer.append(", ");
                }
            }
            buffer.append("]");
        }

        return buffer.toString();
    }
}
