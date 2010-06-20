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
package org.apache.cayenne.unit.di.server;

import javax.sql.DataSource;

import org.apache.cayenne.ConfigurationException;
import org.apache.cayenne.access.DataDomain;
import org.apache.cayenne.configuration.server.ServerRuntime;
import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.di.Binder;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.di.Module;
import org.apache.cayenne.di.Provider;
import org.apache.cayenne.unit.CayenneResources;

public class ServerRuntimeProvider implements Provider<ServerRuntime> {

    @Inject
    protected ServerCaseProperties properties;

    protected CayenneResources resources;

    public ServerRuntimeProvider(CayenneResources resources) {
        this.resources = resources;
    }

    public ServerRuntime get() throws ConfigurationException {

        String configurationLocation = properties.getConfigurationLocation();
        if (configurationLocation == null) {
            throw new NullPointerException("Null 'configurationLocation', "
                    + "annotate your test case with @UseServerRuntime");
        }

        return new ServerRuntime(configurationLocation, new ServerExtraModule());
    }

    class ServerExtraModule implements Module {

        public void configure(Binder binder) {

            // these are the objects overriding standard ServerModule definitions or
            // dependencies needed by such overrides

            binder.bind(DbAdapter.class).toProviderInstance(
                    new CayenneResourcesDbAdapterProvider(resources));
            binder.bind(DataDomain.class).toProvider(ServerCaseDataDomainProvider.class);
            binder.bind(DataSource.class).toProviderInstance(
                    new CayenneResourcesDataSourceProvider(resources));
        }
    }
}
