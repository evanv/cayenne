/* ====================================================================
 *
 * The ObjectStyle Group Software License, Version 1.0
 *
 * Copyright (c) 2002-2003 The ObjectStyle Group
 * and individual authors of the software.  All rights reserved.
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
 * 3. The end-user documentation included with the redistribution, if
 *    any, must include the following acknowlegement:
 *       "This product includes software developed by the
 *        ObjectStyle Group (http://objectstyle.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 *
 * 4. The names "ObjectStyle Group" and "Cayenne"
 *    must not be used to endorse or promote products derived
 *    from this software without prior written permission. For written
 *    permission, please contact andrus@objectstyle.org.
 *
 * 5. Products derived from this software may not be called "ObjectStyle"
 *    nor may "ObjectStyle" appear in their names without prior written
 *    permission of the ObjectStyle Group.
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
 * individuals on behalf of the ObjectStyle Group.  For more
 * information on the ObjectStyle Group, please see
 * <http://objectstyle.org/>.
 *
 */

package org.objectstyle.cayenne.access;

import java.util.Collection;
import java.util.List;

import org.objectstyle.cayenne.map.ObjEntity;
import org.objectstyle.cayenne.query.Query;


/**
 * Defines methods used to run Cayenne queries.
 *
 * <p><i>For more information see <a href="../../../../../../userguide/index.html"
 * target="_top">Cayenne User Guide.</a></i></p>
 *
 * @author Andrei Adamchik
 */
public interface QueryEngine {

    /** Executes a list of queries. Will notify <code>resultConsumer</code>
     * about queries progress and results.
     *
     * @see org.objectstyle.cayenne.access.OperationObserver
     */
    public void performQueries(List queries, OperationObserver resultConsumer);

    /** Executes a single query. Will notify <code>resultConsumer</code>
     * about query progress and results.
     *
     * @see org.objectstyle.cayenne.access.OperationObserver
     */
    public void performQuery(Query query, OperationObserver resultConsumer);

   	/** Returns DataNode that should handle database operations for
      * a specified <code>objEntity</code>. */
    public DataNode dataNodeForObjEntity(ObjEntity objEntity);

    /**
     * Returns a resolver for this query engine that is capable of resolving
     * between classes, entity names, and obj/db entities
     */
    public EntityResolver getEntityResolver();

	/** 
	 * Returns a list of DataMap objects associated with this QueryEngine.
	 * @deprecated Since 1.0 beta 1; use #getDataMaps() instead.
	 */
	public List getDataMapsAsList();

	/** 
	 * Returns an unmodifiable collection of DataMap objects associated
	 * with this QueryEngine.
	 */
	public Collection getDataMaps();

}

