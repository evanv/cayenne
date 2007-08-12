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
package org.apache.cayenne.access;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.art.Painting;
import org.apache.cayenne.DataObjectUtils;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.Persistent;
import org.apache.cayenne.query.EJBQLQuery;
import org.apache.cayenne.unit.CayenneCase;

public class DataContextEJBQLConditionsTest extends CayenneCase {

    protected void setUp() throws Exception {
        deleteTestData();
    }

    public void testLike1() throws Exception {
        createTestData("prepareLike");

        String ejbql = "SELECT p FROM Painting p WHERE p.paintingTitle LIKE 'A%C'";

        EJBQLQuery query = new EJBQLQuery(ejbql);
        List objects = createDataContext().performQuery(query);
        assertEquals(1, objects.size());

        Set ids = new HashSet();
        Iterator it = objects.iterator();
        while (it.hasNext()) {
            Object id = DataObjectUtils.pkForObject((Persistent) it.next());
            ids.add(id);
        }

        assertTrue(ids.contains(new Integer(33001)));
    }

    public void testNotLike() throws Exception {
        createTestData("prepareLike");

        String ejbql = "SELECT p FROM Painting p WHERE p.paintingTitle NOT LIKE 'A%C'";

        EJBQLQuery query = new EJBQLQuery(ejbql);
        List objects = createDataContext().performQuery(query);
        assertEquals(4, objects.size());

        Set ids = new HashSet();
        Iterator it = objects.iterator();
        while (it.hasNext()) {
            Object id = DataObjectUtils.pkForObject((Persistent) it.next());
            ids.add(id);
        }

        assertFalse(ids.contains(new Integer(33001)));
    }

    public void testLike2() throws Exception {
        createTestData("prepareLike");

        String ejbql = "SELECT p FROM Painting p WHERE p.paintingTitle LIKE '_DDDD'";

        EJBQLQuery query = new EJBQLQuery(ejbql);
        List objects = createDataContext().performQuery(query);
        assertEquals(3, objects.size());

        Set ids = new HashSet();
        Iterator it = objects.iterator();
        while (it.hasNext()) {
            Object id = DataObjectUtils.pkForObject((Persistent) it.next());
            ids.add(id);
        }

        assertTrue(ids.contains(new Integer(33002)));
        assertTrue(ids.contains(new Integer(33003)));
        assertTrue(ids.contains(new Integer(33005)));
    }

    public void testLikeEscape() throws Exception {
        createTestData("prepareLike");

        String ejbql = "SELECT p FROM Painting p WHERE p.paintingTitle LIKE 'X_DDDD' ESCAPE 'X'";

        EJBQLQuery query = new EJBQLQuery(ejbql);
        List objects = createDataContext().performQuery(query);
        assertEquals(1, objects.size());

        Set ids = new HashSet();
        Iterator it = objects.iterator();
        while (it.hasNext()) {
            Object id = DataObjectUtils.pkForObject((Persistent) it.next());
            ids.add(id);
        }

        assertTrue(ids.contains(new Integer(33005)));
    }

    public void testIn() throws Exception {
        createTestData("prepareIn");

        String ejbql = "SELECT p FROM Painting p WHERE p.paintingTitle IN ('A', 'B')";

        EJBQLQuery query = new EJBQLQuery(ejbql);
        List objects = createDataContext().performQuery(query);
        assertEquals(2, objects.size());

        Set ids = new HashSet();
        Iterator it = objects.iterator();
        while (it.hasNext()) {
            Object id = DataObjectUtils.pkForObject((Persistent) it.next());
            ids.add(id);
        }

        assertTrue(ids.contains(new Integer(33006)));
        assertTrue(ids.contains(new Integer(33007)));
    }

    public void testNotIn() throws Exception {
        createTestData("prepareIn");

        String ejbql = "SELECT p FROM Painting p WHERE p.paintingTitle NOT IN ('A', 'B')";

        EJBQLQuery query = new EJBQLQuery(ejbql);
        List objects = createDataContext().performQuery(query);
        assertEquals(1, objects.size());

        Set ids = new HashSet();
        Iterator it = objects.iterator();
        while (it.hasNext()) {
            Object id = DataObjectUtils.pkForObject((Persistent) it.next());
            ids.add(id);
        }

        assertTrue(ids.contains(new Integer(33008)));
    }

    public void testInSubquery() throws Exception {
        createTestData("prepareIn");

        String ejbql = "SELECT p FROM Painting p WHERE p.paintingTitle IN ("
                + "SELECT a1.artistName FROM Artist a1"
                + ")";

        EJBQLQuery query = new EJBQLQuery(ejbql);
        List objects = createDataContext().performQuery(query);
        assertEquals(2, objects.size());

        Set ids = new HashSet();
        Iterator it = objects.iterator();
        while (it.hasNext()) {
            Object id = DataObjectUtils.pkForObject((Persistent) it.next());
            ids.add(id);
        }

        assertTrue(ids.contains(new Integer(33006)));
        assertTrue(ids.contains(new Integer(33007)));
    }

    public void testCollectionEmpty() throws Exception {
        createTestData("prepareCollection");

        String ejbql = "SELECT a FROM Artist a WHERE a.paintingArray IS EMPTY";

        EJBQLQuery query = new EJBQLQuery(ejbql);
        List objects = createDataContext().performQuery(query);
        assertEquals(1, objects.size());

        Set ids = new HashSet();
        Iterator it = objects.iterator();
        while (it.hasNext()) {
            Object id = DataObjectUtils.pkForObject((Persistent) it.next());
            ids.add(id);
        }

        assertTrue(ids.contains(new Integer(33003)));
    }

    public void testCollectionNotEmpty() throws Exception {
        createTestData("prepareCollection");

        String ejbql = "SELECT a FROM Artist a WHERE a.paintingArray IS NOT EMPTY";

        EJBQLQuery query = new EJBQLQuery(ejbql);
        List objects = createDataContext().performQuery(query);
        assertEquals(2, objects.size());

        Set ids = new HashSet();
        Iterator it = objects.iterator();
        while (it.hasNext()) {
            Object id = DataObjectUtils.pkForObject((Persistent) it.next());
            ids.add(id);
        }

        assertTrue(ids.contains(new Integer(33001)));
        assertTrue(ids.contains(new Integer(33002)));
    }

    public void testCollectionNotEmptyExplicitDistinct() throws Exception {
        createTestData("prepareCollection");

        String ejbql = "SELECT DISTINCT a FROM Artist a WHERE a.paintingArray IS NOT EMPTY";

        EJBQLQuery query = new EJBQLQuery(ejbql);
        List objects = createDataContext().performQuery(query);
        assertEquals(2, objects.size());

        Set ids = new HashSet();
        Iterator it = objects.iterator();
        while (it.hasNext()) {
            Object id = DataObjectUtils.pkForObject((Persistent) it.next());
            ids.add(id);
        }

        assertTrue(ids.contains(new Integer(33001)));
        assertTrue(ids.contains(new Integer(33002)));
    }

    public void testCollectionMemberOfParameter() throws Exception {
        createTestData("prepareCollection");

        String ejbql = "SELECT a FROM Artist a WHERE :x MEMBER OF a.paintingArray";

        ObjectContext context = createDataContext();

        EJBQLQuery query = new EJBQLQuery(ejbql);
        query.setParameter("x", DataObjectUtils.objectForPK(
                context,
                Painting.class,
                33010));
        List objects = context.performQuery(query);
        assertEquals(1, objects.size());

        Set ids = new HashSet();
        Iterator it = objects.iterator();
        while (it.hasNext()) {
            Object id = DataObjectUtils.pkForObject((Persistent) it.next());
            ids.add(id);
        }

        assertTrue(ids.contains(new Integer(33001)));
    }
    
    public void testCollectionNotMemberOfParameter() throws Exception {
        createTestData("prepareCollection");

        String ejbql = "SELECT a FROM Artist a WHERE :x NOT MEMBER a.paintingArray";

        ObjectContext context = createDataContext();

        EJBQLQuery query = new EJBQLQuery(ejbql);
        query.setParameter("x", DataObjectUtils.objectForPK(
                context,
                Painting.class,
                33010));
        List objects = context.performQuery(query);
        assertEquals(2, objects.size());

        Set ids = new HashSet();
        Iterator it = objects.iterator();
        while (it.hasNext()) {
            Object id = DataObjectUtils.pkForObject((Persistent) it.next());
            ids.add(id);
        }
        
        assertTrue(ids.contains(new Integer(33002)));
        assertTrue(ids.contains(new Integer(33003)));
    }

    public void testCollectionMemberOfThetaJoin() throws Exception {
        createTestData("prepareCollection");

        String ejbql = "SELECT p FROM Painting p, Artist a "
                + "WHERE p MEMBER OF a.paintingArray AND a.artistName = 'B'";

        EJBQLQuery query = new EJBQLQuery(ejbql);
        List objects = createDataContext().performQuery(query);
        assertEquals(2, objects.size());

        Set ids = new HashSet();
        Iterator it = objects.iterator();
        while (it.hasNext()) {
            Object id = DataObjectUtils.pkForObject((Persistent) it.next());
            ids.add(id);
        }

        assertTrue(ids.contains(new Integer(33009)));
        assertTrue(ids.contains(new Integer(33010)));
    }
}
