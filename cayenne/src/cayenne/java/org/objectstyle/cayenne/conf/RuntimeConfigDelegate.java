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
package org.objectstyle.cayenne.conf;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.objectstyle.cayenne.ConfigException;
import org.objectstyle.cayenne.access.DataDomain;
import org.objectstyle.cayenne.access.DataNode;
import org.objectstyle.cayenne.dba.DbAdapter;
import org.objectstyle.cayenne.dba.JdbcAdapter;
import org.objectstyle.cayenne.map.DataMap;
import org.objectstyle.cayenne.map.DataMapException;
import org.objectstyle.cayenne.map.MapLoader;
import org.xml.sax.InputSource;

/**
 * Implementation of ConfigLoaderDelegate that creates Cayenne access objects
 * stack.
 * 
 * @author Andrei Adamchik
 */
public class RuntimeConfigDelegate implements ConfigLoaderDelegate {
    private static Logger logObj =
        Logger.getLogger(RuntimeConfigDelegate.class);

    protected Map domains = new HashMap();
    protected ConfigStatus status = new ConfigStatus();
    protected Configuration config;
    protected Level logLevel = Level.DEBUG;
    protected long startTime;

    public RuntimeConfigDelegate(Configuration config, Level logLevel) {
        this.config = config;
        this.logLevel = logLevel;
    }

    protected DataDomain findDomain(String name) {
        DataDomain domain = (DataDomain) domains.get(name);
        if (domain == null) {
            throw new ConfigException("Can't find DataDomain: " + name);
        }

        return domain;
    }

    protected DataMap findMap(String domainName, String mapName) {
        DataDomain domain = findDomain(domainName);
        DataMap map = domain.getMap(mapName);
        if (map == null) {
            throw new ConfigException("Can't find DataMap: " + mapName);
        }

        return map;
    }

    protected DataNode findNode(String domainName, String nodeName) {
        DataDomain domain = findDomain(domainName);
        DataNode node = domain.getNode(nodeName);
        if (node == null) {
            throw new ConfigException("Can't find DataNode: " + nodeName);
        }

        return node;
    }

    public boolean loadError(Throwable th) {
        logObj.log(logLevel, "Parser Exception.", th);
        status.getOtherFailures().add(th.getMessage());
        return false;
    }

    public void shouldLoadDataDomain(String domainName) {
        if (domainName == null) {
            logObj.log(logLevel, "error: unnamed <domain>.");
            throw new ConfigException("Domain 'name' attribute must be not null.");
        }

        logObj.log(logLevel, "loaded domain: " + domainName);
        domains.put(domainName, new DataDomain(domainName));
    }

    public void shouldLoadDataMap(
        String domainName,
        String mapName,
        String location,
        List depMapNames) {

        if (mapName == null) {
            throw new ConfigException("error: <map> without 'name'.");
        }

        if (location == null) {
            throw new ConfigException(
                "error: map '" + mapName + "' without 'location'.");
        }

        List depMaps = new ArrayList();
        if (depMapNames != null && depMapNames.size() > 0) {
            for (int i = 0; i < depMapNames.size(); i++) {
                String depMapName = (String) depMapNames.get(i);
                if (depMapName == null) {
                    logObj.log(
                        logLevel,
                        "error: missing dependent map name for map: "
                            + mapName);
                    getStatus().getFailedMaps().put(mapName, location);
                    return;
                }

                logObj.log(
                    logLevel,
                    "info: linking map to dependent map: " + depMapName);
                depMaps.add(findMap(domainName, depMapName));
            }
        }

        InputStream mapIn = config.getMapConfig(location);

        if (mapIn == null) {
            logObj.log(logLevel, "warning: map location not found.");
            getStatus().getFailedMaps().put(mapName, location);
            return;
        }

        try {
            DataMap map =
                new MapLoader().loadDataMap(new InputSource(mapIn), depMaps);

            logObj.log(
                logLevel,
                "loaded <map name='"
                    + mapName
                    + "' location='"
                    + location
                    + "'>.");

            map.setName(mapName);
            map.setLocation(location);
            findDomain(domainName).addMap(map);
        } catch (DataMapException dmex) {
            logObj.log(logLevel, "Warning: map loading failed.", dmex);
            getStatus().getFailedMaps().put(mapName, location);
        }
    }

    public void shouldLoadDataNode(
        String domainName,
        String nodeName,
        String dataSource,
        String adapter,
        String factory) {

        logObj.log(
            logLevel,
            "loading <node name='"
                + nodeName
                + "' datasource='"
                + dataSource
                + "' factory='"
                + factory
                + "'>.");

        if (nodeName == null) {
            throw new ConfigException("error: <node> without 'name'.");
        }

        if (dataSource == null) {
            logObj.log(
                logLevel,
                "warning: <node> '" + nodeName + "' has no 'datasource'.");
        }

        if (factory == null) {
            if (config.getOverrideFactory() != null) {
                logObj.log(
                    logLevel,
                    "warning: <node> '" + nodeName + "' without 'factory'.");
            } else {
                throw new ConfigException(
                    "error: <node> '" + nodeName + "' without 'factory'.");
            }
        }

        DataNode node = new DataNode(nodeName);
        node.setDataSourceFactory(factory);
        node.setDataSourceLocation(dataSource);

        // load DataSource
        try {
            // use DomainHelper factory if it exists, if not - use factory specified
            // in configuration data
            DataSourceFactory confFactory = config.getOverrideFactory();
            DataSourceFactory localFactory =
                (confFactory != null)
                    ? confFactory
                    : (DataSourceFactory) Class.forName(factory).newInstance();

            logObj.log(
                logLevel,
                "using factory: " + localFactory.getClass().getName());

            localFactory.setParentConfig(config);
            DataSource ds = localFactory.getDataSource(dataSource, logLevel);
            if (ds != null) {
                logObj.log(logLevel, "loaded datasource.");
                node.setDataSource(ds);
            } else {
                logObj.log(logLevel, "warning: null datasource.");
                getStatus().getFailedDataSources().put(nodeName, dataSource);
            }
        } catch (Exception ex) {
            logObj.log(logLevel, "error: DataSource load failed", ex);
            getStatus().getFailedDataSources().put(nodeName, dataSource);
        }

        // load DbAdapter
        if (adapter == null) {
            adapter = JdbcAdapter.class.getName();
        }

        try {
            node.setAdapter((DbAdapter) Class.forName(adapter).newInstance());
        } catch (Exception ex) {
            logObj.log(logLevel, "instantiating adapter failed.", ex);
            getStatus().getFailedAdapters().put(nodeName, adapter);
        }

        findDomain(domainName).addNode(node);
    }

    public void shouldLinkDataMap(
        String domainName,
        String nodeName,
        String mapName) {

        if (mapName == null) {
            logObj.log(logLevel, "<map-ref> has no 'name'.");
            throw new ConfigException("<map-ref> has no 'name'.");
        }

        logObj.log(logLevel, "loaded map-ref: " + mapName + ".");
        DataMap map = findMap(domainName, mapName);
        DataNode node = findNode(domainName, nodeName);

        node.addDataMap(map);
    }

    /**
     * Returns the domains.
     * @return List
     */
    public Map getDomains() {
        return domains;
    }

    /**
     * Returns the status.
     * @return ConfigStatus
     */
    public ConfigStatus getStatus() {
        return status;
    }

    /**
     * Returns the config.
     * @return Configuration
     */
    public Configuration getConfig() {
        return config;
    }

    /**
     * Sets the config.
     * @param config The config to set
     */
    public void setConfig(Configuration config) {
        this.config = config;
    }

    /**
     * Returns the logLevel.
     * @return Level
     */
    public Level getLogLevel() {
        return logLevel;
    }

    /**
     * Sets the logLevel.
     * @param logLevel The logLevel to set
     */
    public void setLogLevel(Level logLevel) {
        this.logLevel = logLevel;
    }

    /**
     * @see org.objectstyle.cayenne.conf.ConfigLoaderDelegate#finishedLoading()
     */
    public void finishedLoading() {
        // check for failures
        if (status.hasFailures()) {
            if (!config.isIgnoringLoadFailures()) {
                StringBuffer msg = new StringBuffer();
                msg.append("[").append(config.getClass().getName()).append(
                    "] : Failed to load domain and/or its maps/nodes.");

                throw new ConfigException(msg.toString());
            }
        }

        // update configuration object
        Iterator it = getDomains().values().iterator();
        while (it.hasNext()) {
            config.addDomain((DataDomain) it.next());
        }
        logObj.log(
            logLevel,
            "Finished configuration loading. Took "
                + (System.currentTimeMillis() - startTime)
                + " ms.");
    }

    /**
     * @see org.objectstyle.cayenne.conf.ConfigLoaderDelegate#startedLoading()
     */
    public void startedLoading() {
        startTime = System.currentTimeMillis();
        logObj.log(logLevel, "Started configuration loading.");
    }
}
