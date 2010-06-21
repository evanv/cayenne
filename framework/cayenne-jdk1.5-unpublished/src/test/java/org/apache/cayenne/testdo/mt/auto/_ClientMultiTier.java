package org.apache.cayenne.testdo.mt.auto;

import java.util.List;

import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.query.NamedQuery;
import org.apache.cayenne.testdo.mt.ClientMtTable1;

/**
 * This class was generated by Cayenne.
 * It is probably a good idea to avoid changing this class manually,
 * since it may be overwritten next time code is regenerated.
 * If you need to make any customizations, please use subclass.
 */
public class _ClientMultiTier {

    public static final String ALL_MT_TABLE1_QUERYNAME = "AllMtTable1";

    public static final String MT_QUERY_WITH_LOCAL_CACHE_QUERYNAME = "MtQueryWithLocalCache";

    public static final String PARAMETERIZED_MT_QUERY_WITH_LOCAL_CACHE_QUERYNAME = "ParameterizedMtQueryWithLocalCache";

    public List<ClientMtTable1> performAllMtTable1(ObjectContext context ) {
        return context.performQuery(new NamedQuery("AllMtTable1"));
    }

    public List<ClientMtTable1> performMtQueryWithLocalCache(ObjectContext context ) {
        return context.performQuery(new NamedQuery("MtQueryWithLocalCache"));
    }

    public List<ClientMtTable1> performParameterizedMtQueryWithLocalCache(ObjectContext context , String g) {
        String[] parameters = {
            "g",
        };

        Object[] values = {
            g,
        };

        return context.performQuery(new NamedQuery("ParameterizedMtQueryWithLocalCache", parameters, values));
    }
}
