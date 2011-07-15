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
package org.apache.cayenne.configuration.server;

import org.apache.cayenne.BaseContext;
import org.apache.cayenne.DataChannel;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.access.DataContext;
import org.apache.cayenne.access.DataDomain;
import org.apache.cayenne.access.DataRowStore;
import org.apache.cayenne.access.ObjectMapRetainStrategy;
import org.apache.cayenne.access.ObjectStore;
import org.apache.cayenne.cache.QueryCache;
import org.apache.cayenne.configuration.ObjectContextFactory;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.di.Injector;
import org.apache.cayenne.di.Key;
import org.apache.cayenne.event.EventManager;

/**
 * @since 3.1
 */
public class DataContextFactory implements ObjectContextFactory {

    @Inject
    protected DataDomain dataDomain;

    @Inject
    protected EventManager eventManager;

    @Inject
    protected Injector injector;
    
    @Inject
    protected ObjectMapRetainStrategy retainStrategy;

    public ObjectContext createContext() {
        return createdFromDataDomain(dataDomain);
    }

    public ObjectContext createContext(DataChannel parent) {

        // this switch may go away once we figure out clean property configuration...
        if (parent instanceof DataDomain) {
            return createdFromDataDomain((DataDomain) parent);
        }
        else if (parent instanceof DataContext) {
            return createFromDataContext((DataContext) parent);
        }
        else {
            return createFromGenericChannel(parent);
        }
    }

    protected ObjectContext createFromGenericChannel(DataChannel parent) {

        // for new dataRowStores use the same name for all stores
        // it makes it easier to track the event subject
        DataRowStore snapshotCache = (dataDomain.isSharedCacheEnabled()) ? dataDomain
                .getSharedSnapshotCache() : new DataRowStore(
                dataDomain.getName(),
                dataDomain.getProperties(),
                eventManager);

        DataContext context = new DataContext(parent, new ObjectStore(snapshotCache, 
                retainStrategy.createObjectMap()));
        context.setValidatingObjectsOnCommit(dataDomain.isValidatingObjectsOnCommit());
        context.setQueryCache(injector.getInstance(Key.get(
                QueryCache.class,
                BaseContext.QUERY_CACHE_INJECTION_KEY)));
        return context;
    }

    protected ObjectContext createFromDataContext(DataContext parent) {
        // child ObjectStore should not have direct access to snapshot cache, so do not
        // pass it in constructor.
        ObjectStore objectStore = new ObjectStore(null, retainStrategy.createObjectMap());

        DataContext context = new DataContext(parent, objectStore);

        context.setValidatingObjectsOnCommit(parent.isValidatingObjectsOnCommit());
        context.setUsingSharedSnapshotCache(parent.isUsingSharedSnapshotCache());
        context.setQueryCache(injector.getInstance(Key.get(
                QueryCache.class,
                BaseContext.QUERY_CACHE_INJECTION_KEY)));

        return context;
    }

    protected ObjectContext createdFromDataDomain(DataDomain parent) {

        // for new dataRowStores use the same name for all stores
        // it makes it easier to track the event subject
        DataRowStore snapshotCache = (parent.isSharedCacheEnabled()) ? parent
                .getSharedSnapshotCache() : new DataRowStore(parent.getName(), parent
                .getProperties(), eventManager);

        DataContext context = new DataContext(parent, new ObjectStore(snapshotCache, 
                retainStrategy.createObjectMap()));
        context.setValidatingObjectsOnCommit(parent.isValidatingObjectsOnCommit());
        context.setQueryCache(injector.getInstance(Key.get(
                QueryCache.class,
                BaseContext.QUERY_CACHE_INJECTION_KEY)));
        return context;
    }
}
