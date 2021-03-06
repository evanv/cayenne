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
package org.apache.cayenne.maven.plugin.aggregator;

import java.io.File;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

/**
 * A goal to build an aggregated source artifact from multiple source artifacts.
 * 
 * @goal aggregate-sources
 */
public class SourcesAggregatorMojo extends AbstractAggregatorMojo {

    /**
     * Default location used for mojo unless overridden in ArtifactItem
     * 
     * @parameter expression="${unpackDirectory}"
     *            default-value="${project.build.directory}/aggregate/unpack-sources"
     * @required
     */
    private File unpackDirectory;

    public void execute() throws MojoExecutionException, MojoFailureException {
        unpackArtifacts(unpackDirectory, "sources");
        packAggregatedArtifact(unpackDirectory, "sources");
    }
}
