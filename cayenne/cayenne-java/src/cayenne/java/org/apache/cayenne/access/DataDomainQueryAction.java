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

package org.apache.cayenne.access;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.cayenne.CayenneException;
import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.DataRow;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.ObjectId;
import org.apache.cayenne.QueryResponse;
import org.apache.cayenne.cache.QueryCache;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.ObjRelationship;
import org.apache.cayenne.query.InvalidateListCacheQuery;
import org.apache.cayenne.query.ObjectIdQuery;
import org.apache.cayenne.query.PrefetchSelectQuery;
import org.apache.cayenne.query.PrefetchTreeNode;
import org.apache.cayenne.query.Query;
import org.apache.cayenne.query.QueryMetadata;
import org.apache.cayenne.query.QueryRouter;
import org.apache.cayenne.query.RelationshipQuery;
import org.apache.cayenne.util.GenericResponse;
import org.apache.cayenne.util.ListResponse;
import org.apache.cayenne.util.Util;
import org.apache.commons.collections.Transformer;

/**
 * Performs query routing and execution. During execution phase intercepts callbacks to
 * the OperationObserver, remapping results to the original pre-routed queries.
 * 
 * @since 1.2
 * @author Andrus Adamchik
 */
class DataDomainQueryAction implements QueryRouter, OperationObserver {

    static final boolean DONE = true;

    DataContext context;
    DataDomain domain;
    DataRowStore cache;
    Query query;
    QueryMetadata metadata;

    QueryResponse response;
    GenericResponse fullResponse;
    Map prefetchResultsByPath;
    Map queriesByNode;
    Map queriesByExecutedQueries;

    /*
     * A constructor for the "new" way of performing a query via 'execute' with
     * QueryResponse created internally.
     */
    DataDomainQueryAction(ObjectContext context, DataDomain domain, Query query) {
        if (context != null && !(context instanceof DataContext)) {
            throw new IllegalArgumentException(
                    "DataDomain can only work with DataContext. "
                            + "Unsupported context type: "
                            + context);
        }

        this.domain = domain;
        this.query = query;
        this.metadata = query.getMetaData(domain.getEntityResolver());
        this.context = (DataContext) context;

        // cache may be shared or unique for the ObjectContext
        if (context != null) {
            this.cache = this.context.getObjectStore().getDataRowCache();
        }

        if (this.cache == null) {
            this.cache = domain.getSharedSnapshotCache();
        }
    }

    QueryResponse execute() {

        // run chain...
        if (interceptOIDQuery() != DONE) {
            if (interceptRelationshipQuery() != DONE) {
                if (interceptInvalidateQuery() != DONE) {
                    if (interceptSharedCache() != DONE) {
                        runQueryInTransaction();
                    }
                }
            }
        }

        // turn results to objects
        interceptObjectConversion();

        return response;
    }

    private boolean interceptOIDQuery() {
        if (query instanceof ObjectIdQuery) {

            ObjectIdQuery oidQuery = (ObjectIdQuery) query;

            DataRow row = null;

            if (!oidQuery.isFetchMandatory()) {
                row = cache.getCachedSnapshot(oidQuery.getObjectId());
            }

            // refresh is forced or not found in cache
            if (row == null) {

                if (oidQuery.isFetchAllowed()) {

                    runQueryInTransaction();

                    List result = response.firstList();
                    if (result != null && !result.isEmpty()) {

                        if (result.size() > 1) {
                            throw new CayenneRuntimeException(
                                    "More than 1 row found for ObjectId "
                                            + oidQuery.getObjectId()
                                            + ". Fetch matched "
                                            + result.size()
                                            + " rows.");
                        }

                        // cache for future use
                        cache.snapshots.put(oidQuery.getObjectId(), result.get(0));
                    }
                }
                else {
                    response = new ListResponse();
                }
            }
            else {
                response = new ListResponse(row);
            }

            return DONE;
        }

        return !DONE;
    }

    private boolean interceptRelationshipQuery() {

        if (query instanceof RelationshipQuery) {

            RelationshipQuery relationshipQuery = (RelationshipQuery) query;
            if (relationshipQuery.isRefreshing()) {
                return !DONE;
            }

            ObjRelationship relationship = relationshipQuery.getRelationship(domain
                    .getEntityResolver());

            // check if we can derive target PK from FK... this implies that the
            // relationship is to-one
            if (relationship.isSourceIndependentFromTargetChange()) {
                return !DONE;
            }

            DataRow sourceRow = cache.getCachedSnapshot(relationshipQuery.getObjectId());

            if (sourceRow == null) {
                return !DONE;
            }

            // we can assume that there is one and only one DbRelationship as
            // we previously checked that
            // "!isSourceIndependentFromTargetChange"
            DbRelationship dbRelationship = (DbRelationship) relationship
                    .getDbRelationships()
                    .get(0);

            ObjectId targetId = sourceRow.createTargetObjectId(relationship
                    .getTargetEntityName(), dbRelationship);

            // null id means that FK is null...
            if (targetId == null) {
                this.response = new GenericResponse(Collections.EMPTY_LIST);
                return DONE;
            }

            DataRow targetRow = cache.getCachedSnapshot(targetId);

            DataRow resultRow;

            if (targetRow != null) {
                resultRow = targetRow;
            }
            // if no inheritance involved, we can return a valid partial row made from
            // the target Id alone...
            else if (domain.getEntityResolver().lookupInheritanceTree(
                    (ObjEntity) relationship.getTargetEntity()) == null) {

                resultRow = new DataRow(targetId.getIdSnapshot());
            }
            else {
                // can't guess the right target...
                return !DONE;
            }

            this.response = new GenericResponse(Collections.singletonList(resultRow));
            return DONE;
        }

        return !DONE;
    }

    /**
     * @since 3.0
     */
    private boolean interceptInvalidateQuery() {
        if(domain.getQueryCacheInternal() == null) {
            return !DONE;
        }
        
        if (query instanceof InvalidateListCacheQuery) {
            InvalidateListCacheQuery invalidateQuery = (InvalidateListCacheQuery) query;

            QueryCache queryCache = domain.getQueryCache();

            if (invalidateQuery.getQueryNameKey() != null) {
                queryCache.remove(invalidateQuery.getQueryNameKey());
            }

            String[] groupKeys = invalidateQuery.getGroupKeys();
            if (groupKeys != null && groupKeys.length > 0) {
                for (int i = 0; i < groupKeys.length; i++) {
                    queryCache.removeGroup(groupKeys[i]);
                }
            }

            // ignore 'cascade' setting - we are at the bottom of the stack already...
            GenericResponse response = new GenericResponse();
            response.addUpdateCount(1);
            this.response = response;
            return DONE;
        }

        return !DONE;
    }

    /*
     * Wraps execution in shared cache checks
     */
    private final boolean interceptSharedCache() {

        if (metadata.getCacheKey() == null) {
            return !DONE;
        }

        boolean cache = QueryMetadata.SHARED_CACHE.equals(metadata.getCachePolicy());
        boolean cacheOrCacheRefresh = cache
                || QueryMetadata.SHARED_CACHE_REFRESH.equals(metadata.getCachePolicy());

        if (!cacheOrCacheRefresh) {
            return !DONE;
        }

        QueryCache queryCache = domain.getQueryCache();
        
        if (cache) {
            List cachedRows = queryCache.get(metadata);

            if (cachedRows != null) {
                // decorate result immutable list to avoid messing up the cache
                this.response = new ListResponse(Collections.unmodifiableList(cachedRows));

                if (cachedRows instanceof ListWithPrefetches) {
                    this.prefetchResultsByPath = ((ListWithPrefetches) cachedRows)
                            .getPrefetchResultsByPath();
                }

                return DONE;
            }
        }

        runQueryInTransaction();

        List list = response.firstList();
        if (list != null) {

            // include prefetches in the cached result
            if (prefetchResultsByPath != null) {
                list = new ListWithPrefetches(list, prefetchResultsByPath);
            }

            queryCache.put(metadata, list);
        }

        return DONE;
    }

    /*
     * Gets response from the underlying DataNodes.
     */
    void runQueryInTransaction() {

        domain.runInTransaction(new Transformer() {

            public Object transform(Object input) {
                runQuery();
                return null;
            }
        });
    }

    private void runQuery() {
        // reset
        this.fullResponse = new GenericResponse();
        this.response = this.fullResponse;
        this.queriesByNode = null;
        this.queriesByExecutedQueries = null;

        // whether this is null or not will driver further decisions on how to process
        // prefetched rows
        this.prefetchResultsByPath = metadata.getPrefetchTree() != null
                && !metadata.isFetchingDataRows() ? new HashMap() : null;

        // categorize queries by node and by "executable" query...
        query.route(this, domain.getEntityResolver(), null);

        // run categorized queries
        if (queriesByNode != null) {
            Iterator nodeIt = queriesByNode.entrySet().iterator();
            while (nodeIt.hasNext()) {
                Map.Entry entry = (Map.Entry) nodeIt.next();
                QueryEngine nextNode = (QueryEngine) entry.getKey();
                Collection nodeQueries = (Collection) entry.getValue();
                nextNode.performQueries(nodeQueries, this);
            }
        }
    }

    private void interceptObjectConversion() {

        if (context != null && !metadata.isFetchingDataRows()) {

            List mainRows = response.firstList();
            if (mainRows != null && !mainRows.isEmpty()) {

                List objects;
                ObjEntity entity = metadata.getObjEntity();
                PrefetchTreeNode prefetchTree = metadata.getPrefetchTree();

                // take a shortcut when no prefetches exist...
                if (prefetchTree == null) {
                    objects = new ObjectResolver(context, entity, metadata
                            .isRefreshingObjects(), metadata.isResolvingInherited())
                            .synchronizedObjectsFromDataRows(mainRows);
                }
                else {

                    ObjectTreeResolver resolver = new ObjectTreeResolver(
                            context,
                            metadata);
                    objects = resolver.synchronizedObjectsFromDataRows(
                            prefetchTree,
                            mainRows,
                            prefetchResultsByPath);
                }

                if (response instanceof GenericResponse) {
                    ((GenericResponse) response).replaceResult(mainRows, objects);
                }
                else if (response instanceof ListResponse) {
                    this.response = new ListResponse(objects);
                }
                else {
                    throw new IllegalStateException("Unknown response object: "
                            + this.response);
                }
            }
        }
    }

    public void route(QueryEngine engine, Query query, Query substitutedQuery) {

        List queries = null;
        if (queriesByNode == null) {
            queriesByNode = new HashMap();
        }
        else {
            queries = (List) queriesByNode.get(engine);
        }

        if (queries == null) {
            queries = new ArrayList(5);
            queriesByNode.put(engine, queries);
        }

        queries.add(query);

        // handle case when routing resuled in an "exectable" query different from the
        // original query.
        if (substitutedQuery != null && substitutedQuery != query) {

            if (queriesByExecutedQueries == null) {
                queriesByExecutedQueries = new HashMap();
            }

            queriesByExecutedQueries.put(query, substitutedQuery);
        }
    }

    public QueryEngine engineForDataMap(DataMap map) {
        if (map == null) {
            throw new NullPointerException("Null DataMap, can't determine DataNode.");
        }

        QueryEngine node = domain.lookupDataNode(map);

        if (node == null) {
            throw new CayenneRuntimeException("No DataNode exists for DataMap " + map);
        }

        return node;
    }

    public void nextCount(Query query, int resultCount) {
        fullResponse.addUpdateCount(resultCount);
    }

    public void nextBatchCount(Query query, int[] resultCount) {
        fullResponse.addBatchUpdateCount(resultCount);
    }

    public void nextDataRows(Query query, List dataRows) {

        // exclude prefetched rows in the main result
        if (prefetchResultsByPath != null && query instanceof PrefetchSelectQuery) {
            PrefetchSelectQuery prefetchQuery = (PrefetchSelectQuery) query;
            prefetchResultsByPath.put(prefetchQuery.getPrefetchPath(), dataRows);
        }
        else {
            fullResponse.addResultList(dataRows);
        }
    }

    public void nextDataRows(Query q, ResultIterator it) {
        throw new CayenneRuntimeException("Invalid attempt to fetch a cursor.");
    }

    public void nextGeneratedDataRows(Query query, ResultIterator keysIterator) {
        if (keysIterator != null) {
            try {
                nextDataRows(query, keysIterator.dataRows(true));
            }
            catch (CayenneException ex) {
                // don't throw here....
                nextQueryException(query, ex);
            }
        }
    }

    public void nextQueryException(Query query, Exception ex) {
        throw new CayenneRuntimeException("Query exception.", Util.unwindException(ex));
    }

    public void nextGlobalException(Exception e) {
        throw new CayenneRuntimeException("Global exception.", Util.unwindException(e));
    }

    public boolean isIteratedResult() {
        return false;
    }
}
