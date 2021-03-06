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

package org.apache.cayenne.conf;

import javax.servlet.FilterConfig;
import javax.servlet.ServletException;

import org.apache.cayenne.configuration.web.CayenneFilter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * This is a legacy Cayenne bootstrapping filter kept for backwards compatibility of
 * web.xml files. It should be replaced with {@link CayenneFilter} instead.
 * 
 * @since 1.2
 * @deprecated since 3.0 {@link CayenneFilter} should be used.
 */
public class WebApplicationContextFilter extends CayenneFilter {

    private Log logger;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        logger = LogFactory.getLog(WebApplicationContextFilter.class);
        logger
                .warn("**** WebApplicationContextFilter is deprecated. Use CayenneFilter instead");

        super.init(filterConfig);
    }
}
