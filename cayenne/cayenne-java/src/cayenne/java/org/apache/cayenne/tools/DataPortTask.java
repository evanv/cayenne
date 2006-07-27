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

package org.apache.cayenne.tools;

import java.io.File;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.cayenne.access.DataDomain;
import org.apache.cayenne.access.DataNode;
import org.apache.cayenne.access.DataPort;
import org.apache.cayenne.conf.Configuration;
import org.apache.cayenne.conf.FileConfiguration;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.util.Util;

/**
 * A "cdataport" Ant task implementing a frontend to DataPort allowing porting database
 * data using Ant build scripts.
 * 
 * @author Andrus Adamchik
 * @since 1.2: Prior to 1.2 DataPort classes were a part of cayenne-examples package.
 */
public class DataPortTask extends CayenneTask {

    protected File projectFile;
    protected String maps;
    protected String srcNode;
    protected String destNode;
    protected String includeTables;
    protected String excludeTables;
    protected boolean cleanDest = true;

    public DataPortTask() {
        // set defaults
        this.cleanDest = true;
    }

    public void execute() throws BuildException {
        configureLogging();
        validateParameters();

        FileConfiguration configuration = new FileConfiguration(projectFile);

        try {
            configuration.initialize();
        }
        catch (Exception ex) {
            throw new BuildException("Error loading Cayenne configuration from "
                    + projectFile, ex);
        }

        // perform project validation
        DataNode source = findNode(configuration, srcNode);
        if (source == null) {
            throw new BuildException("srcNode not found in the project: " + srcNode);
        }

        DataNode destination = findNode(configuration, destNode);
        if (destination == null) {
            throw new BuildException("destNode not found in the project: " + destNode);
        }

        log("Porting from '" + srcNode + "' to '" + destNode + "'.");

        AntDataPortDelegate portDelegate = new AntDataPortDelegate(
                this,
                maps,
                includeTables,
                excludeTables);
        DataPort dataPort = new DataPort(portDelegate);
        dataPort.setEntities(getAllEntities(source, destination));
        dataPort.setCleaningDestination(cleanDest);
        dataPort.setSourceNode(source);
        dataPort.setDestinationNode(destination);

        try {
            dataPort.execute();
        }
        catch (Exception e) {
            Throwable topOfStack = Util.unwindException(e);
            throw new BuildException(
                    "Error porting data: " + topOfStack.getMessage(),
                    topOfStack);
        }
    }

    protected DataNode findNode(Configuration configuration, String name) {
        Iterator domains = configuration.getDomains().iterator();
        while (domains.hasNext()) {
            DataDomain domain = (DataDomain) domains.next();
            DataNode node = domain.getNode(name);
            if (node != null) {
                return node;
            }
        }

        return null;
    }

    protected Collection getAllEntities(DataNode source, DataNode target) {
        // use a set to exclude duplicates, though a valid project will probably have
        // none...
        Collection allEntities = new HashSet();

        Iterator maps = source.getDataMaps().iterator();
        while (maps.hasNext()) {
            DataMap map = (DataMap) maps.next();
            allEntities.addAll(map.getDbEntities());
        }

        maps = target.getDataMaps().iterator();
        while (maps.hasNext()) {
            DataMap map = (DataMap) maps.next();
            allEntities.addAll(map.getDbEntities());
        }

        log("Number of entities: " + allEntities.size(), Project.MSG_VERBOSE);

        if (allEntities.size() == 0) {
            log("No entities found for either source or target.");
        }
        return allEntities;
    }

    protected void validateParameters() throws BuildException {
        if (projectFile == null) {
            throw new BuildException("Required 'projectFile' parameter is missing.");
        }

        if (!projectFile.exists()) {
            throw new BuildException("'projectFile' does not exist: " + projectFile);
        }

        if (srcNode == null) {
            throw new BuildException("Required 'srcNode' parameter is missing.");
        }

        if (destNode == null) {
            throw new BuildException("Required 'destNode' parameter is missing.");
        }
    }

    public void setDestNode(String destNode) {
        this.destNode = destNode;
    }

    public void setExcludeTables(String excludeTables) {
        this.excludeTables = excludeTables;
    }

    public void setIncludeTables(String includeTables) {
        this.includeTables = includeTables;
    }

    public void setMaps(String maps) {
        this.maps = maps;
    }

    public void setProjectFile(File projectFile) {
        this.projectFile = projectFile;
    }

    public void setSrcNode(String srcNode) {
        this.srcNode = srcNode;
    }

    public void setCleanDest(boolean flag) {
        this.cleanDest = flag;
    }
}
