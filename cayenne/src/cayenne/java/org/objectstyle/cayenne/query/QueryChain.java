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
package org.objectstyle.cayenne.query;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import org.apache.log4j.Level;
import org.objectstyle.cayenne.CayenneRuntimeException;
import org.objectstyle.cayenne.map.EntityResolver;

/**
 * A Query decorator for a collection of other queries.
 * 
 * @since 1.2
 * @author Andrus Adamchik
 */
public class QueryChain implements Query {

    protected String name;
    protected Level loggingLevel;
    protected Collection chain;

    /**
     * Creates an empty QueryChain.
     */
    public QueryChain() {
    }

    /**
     * Creates a new QueryChain with a collection of Queries.
     */
    public QueryChain(Collection queries) {
        if (queries != null && !queries.isEmpty()) {
            this.chain = new ArrayList(queries);
        }
    }

    /**
     * Adds a query to the chain.
     */
    public void addQuery(Query query) {
        if (chain == null) {
            chain = new ArrayList();
        }

        chain.add(query);
    }

    /**
     * Removes a query from the chain, returning true if the query was indeed present in
     * the chain and was removed.
     */
    public boolean removeQuery(Query query) {
        return (chain != null) ? chain.remove(query) : false;
    }

    public boolean isEmpty() {
        return chain == null || chain.isEmpty();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Level getLoggingLevel() {
        return loggingLevel;
    }

    /**
     * Logging level of the QueryChanin can be set, but will be ignored. Keeping for
     * compatibility with Query interface.
     */
    public void setLoggingLevel(Level loggingLevel) {
        this.loggingLevel = loggingLevel;
    }

    /**
     * Throws an exception - chain has no root of its own and each query in a chain is
     * routed individually.
     */
    public Object getRoot() {
        throw new CayenneRuntimeException(
                "Chain doesn't support its own root. Root should be deprecated soon anyway");
    }

    /**
     * Throws an exception - chain has no root of its own and each query in a chain is
     * routed individually.
     */
    public void setRoot(Object root) {
        throw new CayenneRuntimeException(
                "Chain doesn't support its own root. Root should be deprecated soon anyway. An attempt to set it to "
                        + root);
    }

    /**
     * Resolves queries in the chain, creating another QueryChain that contains resolved
     * queries.
     */
    public Query resolve(EntityResolver resolver) {
        if (isEmpty()) {
            return this;
        }

        Collection resolvedChain = new ArrayList(chain.size());

        Iterator it = chain.iterator();
        while (it.hasNext()) {
            Query resolved = ((Query) it.next()).resolve(resolver);
            if (resolved != null) {
                resolvedChain.add(resolved);
            }
        }

        return new QueryChain(resolvedChain);
    }

    /**
     * Delegates routing to each individual query in the chain. If there is no queries,
     * this method does nothing.
     */
    public void route(QueryRouter router, EntityResolver resolver) {
        if (chain != null && !chain.isEmpty()) {
            Iterator it = chain.iterator();
            while (it.hasNext()) {
                Query q = (Query) it.next();
                q.route(router, resolver);
            }
        }
    }

    /**
     * Throws an exception as execution should've been delegated to the queries contained
     * in the chain.
     */
    public SQLAction createSQLAction(SQLActionVisitor visitor) {
        throw new CayenneRuntimeException("Chain doesn't support its own execution "
                + "and should've been split into separate queries during routing phase.");
    }
}
