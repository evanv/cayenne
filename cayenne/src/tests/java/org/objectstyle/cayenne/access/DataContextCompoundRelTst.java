/* ====================================================================
 *
 * The ObjectStyle Group Software License, Version 1.0
 *
 * Copyright (c) 2002-2004 The ObjectStyle Group
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
package org.objectstyle.cayenne.access;

import java.util.List;

import org.apache.log4j.Level;
import org.objectstyle.art.CompoundFkTest;
import org.objectstyle.art.CompoundPkTest;
import org.objectstyle.cayenne.exp.Expression;
import org.objectstyle.cayenne.exp.ExpressionFactory;
import org.objectstyle.cayenne.query.SelectQuery;
import org.objectstyle.cayenne.unittest.CayenneTestCase;

/**
 * Testing relationships with compound keys.
 * 
 * @author Andrei Adamchik
 */
public class DataContextCompoundRelTst extends CayenneTestCase {
    protected DataContext ctxt;

    protected void setUp() throws Exception {
        getDatabaseSetup().cleanTableData();
        ctxt = createDataContext();
    }

    public void testInsert() throws Exception {
        CompoundPkTest master =
            (CompoundPkTest) ctxt.createAndRegisterNewObject("CompoundPkTest");
        CompoundFkTest detail =
            (CompoundFkTest) ctxt.createAndRegisterNewObject("CompoundFkTest");
        master.addToCompoundFkArray(detail);
        master.setName("m1");
        master.setKey1("key11");
        master.setKey2("key21");
        detail.setName("d1");

        ctxt.commitChanges(Level.WARN);

        // reset context
        ctxt = createDataContext();

        SelectQuery q = new SelectQuery(CompoundPkTest.class);
        List objs = ctxt.performQuery(q);
        assertEquals(1, objs.size());

        master = (CompoundPkTest) objs.get(0);
        assertEquals("m1", master.getName());

        List details = master.getCompoundFkArray();
        assertEquals(1, details.size());
        detail = (CompoundFkTest) details.get(0);

        assertEquals("d1", detail.getName());
    }

    public void testFetchQualifyingToOne() throws Exception {
        CompoundPkTest master =
            (CompoundPkTest) ctxt.createAndRegisterNewObject("CompoundPkTest");
        CompoundPkTest master1 =
            (CompoundPkTest) ctxt.createAndRegisterNewObject("CompoundPkTest");
        CompoundFkTest detail =
            (CompoundFkTest) ctxt.createAndRegisterNewObject("CompoundFkTest");
        CompoundFkTest detail1 =
            (CompoundFkTest) ctxt.createAndRegisterNewObject("CompoundFkTest");
        master.addToCompoundFkArray(detail);
        master1.addToCompoundFkArray(detail1);

        master.setName("m1");
        master.setKey1("key11");
        master.setKey2("key21");

        master1.setName("m2");
        master1.setKey1("key12");
        master1.setKey2("key22");

        detail.setName("d1");

        detail1.setName("d2");

        ctxt.commitChanges(Level.WARN);

        // reset context
        ctxt = createDataContext();

        Expression qual = ExpressionFactory.matchExp("toCompoundPk", master);
        SelectQuery q = new SelectQuery(CompoundFkTest.class, qual);
        List objs = ctxt.performQuery(q);
        assertEquals(1, objs.size());

        detail = (CompoundFkTest) objs.get(0);
        assertEquals("d1", detail.getName());
    }

	public void testFetchQualifyingToMany() throws Exception {
		   CompoundPkTest master =
			   (CompoundPkTest) ctxt.createAndRegisterNewObject("CompoundPkTest");
		   CompoundPkTest master1 =
			   (CompoundPkTest) ctxt.createAndRegisterNewObject("CompoundPkTest");
		   CompoundFkTest detail =
			   (CompoundFkTest) ctxt.createAndRegisterNewObject("CompoundFkTest");
		   CompoundFkTest detail1 =
			   (CompoundFkTest) ctxt.createAndRegisterNewObject("CompoundFkTest");
		   master.addToCompoundFkArray(detail);
		   master1.addToCompoundFkArray(detail1);

		   master.setName("m1");
		   master.setKey1("key11");
		   master.setKey2("key21");

		   master1.setName("m2");
		   master1.setKey1("key12");
		   master1.setKey2("key22");

		   detail.setName("d1");

		   detail1.setName("d2");

		   ctxt.commitChanges(Level.WARN);

		   // reset context
		   ctxt = createDataContext();

		   Expression qual = ExpressionFactory.matchExp("compoundFkArray", detail1);
		   SelectQuery q = new SelectQuery(CompoundPkTest.class, qual);
		   List objs = ctxt.performQuery(q);
		   assertEquals(1, objs.size());

		   master = (CompoundPkTest) objs.get(0);
		   assertEquals("m2", master.getName());
	   }
}
