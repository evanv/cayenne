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

import java.util.Collection;

import org.apache.cayenne.access.DataDomain;
import org.apache.cayenne.configuration.CayenneRuntime;
import org.apache.cayenne.configuration.rop.client.ClientRuntime;
import org.apache.cayenne.di.Module;

/**
 * An object representing Cayenne server-stack that connects directly to the database via
 * JDBC. This is an entry point for user applications to access Cayenne, which
 * encapsulates the dependency injection internals. The term "server" is used as opposed
 * to ROP "client" (see {@link ClientRuntime}). Any application, desktop, server, etc.
 * that has a direct JDBC connection should be using this runtime.
 * 
 * @since 3.1
 */
public class ServerRuntime extends CayenneRuntime {

    private static Module mainModule(String configurationLocation) {
        return new ServerModule(configurationLocation);
    }

    /**
     * Creates a server runtime configuring it with a standard set of services contained
     * in {@link ServerModule}. CayenneServerModule is created with provided
     * 'configurationLocation'. An optional array of extra modules may contain service
     * overrides and/or user services.
     */
    public ServerRuntime(String configurationLocation, Module... extraModules) {
        super(mergeModules(mainModule(configurationLocation), extraModules));
    }

    /**
     * Creates a server runtime configuring it with a standard set of services contained
     * in {@link ServerModule}. CayenneServerModule is created with provided
     * 'configurationLocation'. An optional array of extra modules may contain service
     * overrides and/or user services.
     */
    public ServerRuntime(String configurationLocation, Collection<Module> extraModules) {
        super(mergeModules(mainModule(configurationLocation), extraModules));
    }

    /**
     * Returns the main runtime DataDomain. Note that by default the returned DataDomain
     * is the same as the main DataChannel returned by {@link #getChannel()}. Although
     * users may redefine DataChannel provider in the DI registry, for instance to
     * decorate this DataDomain with a custom wrapper.
     */
    public DataDomain getDataDomain() {
        return injector.getInstance(DataDomain.class);
    }

}
