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

package org.apache.cayenne.dba.ingres;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Iterator;
import java.util.List;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.access.DataNode;
import org.apache.cayenne.access.trans.QualifierTranslator;
import org.apache.cayenne.access.trans.QueryAssembler;
import org.apache.cayenne.access.trans.TrimmingQualifierTranslator;
import org.apache.cayenne.access.types.ExtendedType;
import org.apache.cayenne.access.types.ExtendedTypeFactory;
import org.apache.cayenne.access.types.ExtendedTypeMap;
import org.apache.cayenne.configuration.RuntimeProperties;
import org.apache.cayenne.dba.JdbcAdapter;
import org.apache.cayenne.dba.PkGenerator;
import org.apache.cayenne.dba.QuotingStrategy;
import org.apache.cayenne.dba.TypesMapping;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.query.Query;
import org.apache.cayenne.query.SQLAction;

/**
 * DbAdapter implementation for <a href="http://opensource.ca.com/projects/ingres/">Ingres</a>.
 * Sample connection settings to use with Ingres are shown below:
 * 
 * <pre>
 *  ingres.cayenne.adapter = org.apache.cayenne.dba.ingres.IngresAdapter
 *  ingres.jdbc.username = test
 *  ingres.jdbc.password = secret
 *  ingres.jdbc.url = jdbc:ingres://serverhostname:II7/cayenne
 *  ingres.jdbc.driver = ca.ingres.jdbc.IngresDriver
 * </pre>
 */
public class IngresAdapter extends JdbcAdapter {
    public static final String TRIM_FUNCTION = "TRIM";
    
    public IngresAdapter(@Inject RuntimeProperties runtimeProperties,
            @Inject(DEFAULT_EXTENDED_TYPE_LIST) List<ExtendedType> defaultExtendedTypes,
            @Inject(USER_EXTENDED_TYPE_LIST) List<ExtendedType> userExtendedTypes,
            @Inject(EXTENDED_TYPE_FACTORY_LIST) List<ExtendedTypeFactory> extendedTypeFactories) {
        super(runtimeProperties, defaultExtendedTypes, userExtendedTypes, extendedTypeFactories);
        this.setSupportsUniqueConstraints(false);
    }
    
    
    @Override
    public QualifierTranslator getQualifierTranslator(QueryAssembler queryAssembler) {
        return new TrimmingQualifierTranslator(
                queryAssembler,
                IngresAdapter.TRIM_FUNCTION);
    }
    
    /**
     * Returns a SQL string that can be used to create database table corresponding to
     * <code>ent</code> parameter.
     */
    @Override
    public String createTable(DbEntity ent) {       
        QuotingStrategy context = getQuotingStrategy(ent.getDataMap().isQuotingSQLIdentifiers());
        StringBuilder buf = new StringBuilder();
        buf.append("CREATE TABLE ");
        buf.append(context.quoteFullyQualifiedName(ent));
        buf.append(" (");

        // columns
        Iterator<DbAttribute> it = ent.getAttributes().iterator();
        boolean first = true;
        while (it.hasNext()) {
            if (first)
                first = false;
            else
                buf.append(", ");

            DbAttribute at = it.next();

            // attribute may not be fully valid, do a simple check
            if (at.getType() == TypesMapping.NOT_DEFINED) {
                throw new CayenneRuntimeException("Undefined type for attribute '"
                        + ent.getFullyQualifiedName()
                        + "."
                        + at.getName()
                        + "'.");
            }

            String[] types = externalTypesForJdbcType(at.getType());
            if (types == null || types.length == 0) {
                throw new CayenneRuntimeException("Undefined type for attribute '"
                        + ent.getFullyQualifiedName()
                        + "."
                        + at.getName()
                        + "': "
                        + at.getType());
            }

            String type = types[0];
            buf.append(context.quoteString(at.getName())).append(' ').append(type);

            // append size and precision (if applicable)
            if (TypesMapping.supportsLength(at.getType())) {
                int len = at.getMaxLength();
                int scale = TypesMapping.isDecimal(at.getType()) ? at.getScale() : -1;

                // sanity check
                if (scale > len) {
                    scale = -1;
                }

                if (len > 0) {
                    buf.append('(').append(len);

                    if (scale >= 0) {
                        buf.append(", ").append(scale);
                    }

                    buf.append(')');
                }
            }

            // Ingres does not like "null" for non mandatory fields
            if (at.isMandatory()) {
                buf.append(" NOT NULL");
            }
        }

        // primary key clause
        Iterator<DbAttribute> pkit = ent.getPrimaryKeys().iterator();
        if (pkit.hasNext()) {
            if (first)
                first = false;
            else
                buf.append(", ");

            buf.append("PRIMARY KEY (");
            boolean firstPk = true;
            while (pkit.hasNext()) {
                if (firstPk)
                    firstPk = false;
                else
                    buf.append(", ");

                DbAttribute at = pkit.next();
                buf.append(context.quoteString(at.getName()));
            }
            buf.append(')');
        }
        buf.append(')');
        return buf.toString();
    }

    @Override
    protected void configureExtendedTypes(ExtendedTypeMap map) {
        super.configureExtendedTypes(map);
        map.registerType(new IngresCharType());
        
        // configure boolean type to work with numeric columns
        map.registerType(new IngresBooleanType());
    }

    /**
     * @see JdbcAdapter#createPkGenerator()
     */
    @Override
    protected PkGenerator createPkGenerator() {
        return new IngresPkGenerator(this);
    }

    @Override
    public void bindParameter(
            PreparedStatement statement,
            Object object,
            int pos,
            int sqlType,
            int scale) throws SQLException, Exception {

        if (object == null && (sqlType == Types.BOOLEAN || sqlType == Types.BIT)) {
            statement.setNull(pos, Types.VARCHAR);
        } else {
            super.bindParameter(statement, object, pos, sqlType, scale);
        }
    }
}
