/* ====================================================================
 * 
 * The ObjectStyle Group Software License, Version 1.0 
 *
 * Copyright (c) 2002 The ObjectStyle Group 
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

package org.objectstyle.cayenne.query;

import org.apache.log4j.Level;
import org.objectstyle.cayenne.map.DbEntity;
import org.objectstyle.cayenne.map.ObjEntity;

/** 
 * Superclass of all query classes. 
 * 
 * @author Andrei Adamchik
 */
public abstract class AbstractQuery implements Query {
    /** The root object this query is based on - maybe an entity name , class, ObjEntity or 
     * DbEntity, depending on the specific query and how it was constructed */
    protected Object root;

    protected Level logLevel = DEFAULT_LOG_LEVEL;

    /**
     * Returns the <code>logLevel</code> property of this query.
     * Log level is a hint to QueryEngine that performs this query
     * to log execution with a certain priority.
     */
    public Level getLoggingLevel() {
        return logLevel;
    }

    /**
     * Sets the <code>logLevel</code> property.
     */
    public void setLoggingLevel(Level logLevel) {
        this.logLevel = logLevel;
    }

    /**
     * Returns the root of this query
     * @return Object
     */
    public Object getRoot() {
        return root;
    }

    /**
     * Sets the root of the query
     * @param value The new root
     * @throws IllegalArgumentException if value is not a String, ObjEntity, DbEntity or Class
     */
    public void setRoot(Object value) {
        if (!((value instanceof String)
            || (value instanceof ObjEntity)
            || (value instanceof DbEntity)
            || (value instanceof Class))) {
            String rootClass =
                (value != null) ? value.getClass().getName() : "null";
            throw new IllegalArgumentException(
                getClass().getName()
                    + ".setRoot takes a String, ObjEntity, DbEntity or Class only. It was passed a "
                    + rootClass);
        }
        this.root = value;
    }

}
