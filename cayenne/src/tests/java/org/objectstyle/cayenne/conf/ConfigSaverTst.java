/* ====================================================================
 * 
 * The ObjectStyle Group Software License, Version 1.0 
 *
 * Copyright (c) 2002-2003 The ObjectStyle Group 
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

import java.io.PrintWriter;
import java.io.StringWriter;

import org.objectstyle.cayenne.conn.DataSourceInfo;
import org.objectstyle.cayenne.project.Project;
import org.objectstyle.cayenne.unittest.CayenneTestCase;

/**
 * Test cases for DomainHelper class.
 * 
 * @author Andrei Adamchik
 */
public class ConfigSaverTst extends CayenneTestCase {
	protected ConfigSaver saver;

	public void testStoreFullDataNode() throws Exception {
		DataSourceInfo info = new DataSourceInfo();
		info.setDataSourceUrl("s1");
		info.setJdbcDriver("s2");
		info.setPassword("s3");
		info.setUserName("s4");
		
		info.setMaxConnections(35);
		info.setMinConnections(22);
		assertSaved(info);
	}
	
	public void testStoreDataNodeNoUserName() throws Exception {
		DataSourceInfo info = new DataSourceInfo();
		info.setDataSourceUrl("s1");
		info.setJdbcDriver("s2");
		info.setPassword("s3");
		
		info.setMaxConnections(35);
		info.setMinConnections(22);
		assertSaved(info);
	}
	
	public void testStoreDataNodeNoPassword() throws Exception {
		DataSourceInfo info = new DataSourceInfo();
		info.setDataSourceUrl("s1");
		info.setJdbcDriver("s2");
		info.setUserName("s4");
		
		info.setMaxConnections(35);
		info.setMinConnections(22);
		assertSaved(info);
	}


	protected void assertSaved(DataSourceInfo info) throws Exception {
		StringWriter str = new StringWriter();
		PrintWriter out = new PrintWriter(str);

	    saver.storeDataNode(out, info);

		out.close();
		str.close();

		StringBuffer buf = str.getBuffer();

		// perform assertions
		if (info.getDataSourceUrl() != null) {
			assertTrue(
				"URL not saved: " + info.getDataSourceUrl(),
				buf.toString().indexOf("<url value=\"" + info.getDataSourceUrl() + "\"/>")
					>= 0);
		}
		
		if (info.getJdbcDriver() != null) {
			assertTrue(
				"Driver not saved: " + info.getJdbcDriver(),
				buf.toString().indexOf("<driver project-version=\"" + Project.CURRENT_PROJECT_VERSION + "\" class=\"" + info.getJdbcDriver() + "\">")
					>= 0);
		}
		
		if (info.getUserName() != null) {
			assertTrue(
				"User name not saved: " + info.getUserName(),
				buf.toString().indexOf("userName=\"" + info.getUserName() + "\"")
					>= 0);
		}
		
		if (info.getPassword() != null) {
			assertTrue(
				"Password not saved: " + info.getPassword(),
				buf.toString().indexOf("password=\"" + info.getPassword() + "\"")
					>= 0);
		}
	}
	
    /**
     * @see junit.framework.TestCase#setUp()
     */
    protected void setUp() throws Exception {
        saver = new ConfigSaver();
    }
}
