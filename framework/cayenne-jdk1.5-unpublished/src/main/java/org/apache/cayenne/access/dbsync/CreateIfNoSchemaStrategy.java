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
package org.apache.cayenne.access.dbsync;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.access.DataNode;
import org.apache.cayenne.access.DbGenerator;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbEntity;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * @since 3.0
 */
public class CreateIfNoSchemaStrategy extends BaseSchemaUpdateStrategy {

    final Log logObj = LogFactory.getLog(CreateIfNoSchemaStrategy.class);

    @Override
    public void generateUpdateSchema(DataNode dataNode) throws SQLException {

        Map<String, Boolean> nameTables = getNameTablesInDB(dataNode);
        Collection<DbEntity> entities = dataNode.getEntityResolver().getDbEntities();
        boolean generate = true;
        Iterator<DbEntity> it = entities.iterator();
        while (it.hasNext()) {
            if (nameTables.get(it.next().getName()) != null) {
                generate = false;
                break;
            }
        }

        if (generate) {
            generate(dataNode);
        }
        else {
            logObj.info("Full or partial schema is present, skipping schema generation");
        }
    }

    private void generate(DataNode dataNode) {
        Collection<DataMap> map = dataNode.getDataMaps();
        Iterator<DataMap> iterator = map.iterator();
        while (iterator.hasNext()) {
            DbGenerator gen = new DbGenerator(dataNode.getAdapter(), iterator.next());
            gen.setShouldCreateTables(true);
            gen.setShouldDropTables(false);
            gen.setShouldCreateFKConstraints(false);
            gen.setShouldCreatePKSupport(false);
            gen.setShouldDropPKSupport(false);
            try {
                gen.runGenerator(dataNode.getDataSource());
            }
            catch (Exception e) {
                throw new CayenneRuntimeException(e);
            }
        }
    }

    /**
     * Returns all the table names in database.
     * 
     * @throws SQLException
     */
    protected Map<String, Boolean> getNameTablesInDB(DataNode dataNode)
            throws SQLException {
        String tableLabel = dataNode.getAdapter().tableTypeForTable();
        Connection con = null;
        Map<String, Boolean> nameTables = new HashMap<String, Boolean>();
        con = dataNode.getDataSource().getConnection();

        try {
            ResultSet rs = con.getMetaData().getTables(null, null, "%", new String[] {
                tableLabel
            });

            try {

                while (rs.next()) {
                    String name = rs.getString("TABLE_NAME");
                    nameTables.put(name, false);
                }
            }
            finally {
                rs.close();
            }

        }
        finally {

            con.close();
        }
        return nameTables;
    }
}
