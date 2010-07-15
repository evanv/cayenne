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

import java.math.BigDecimal;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.cayenne.Cayenne;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.Persistent;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.query.EJBQLQuery;
import org.apache.cayenne.query.SelectQuery;
import org.apache.cayenne.test.jdbc.DBHelper;
import org.apache.cayenne.test.jdbc.TableHelper;
import org.apache.cayenne.testdo.testmap.Artist;
import org.apache.cayenne.testdo.testmap.Painting;
import org.apache.cayenne.unit.di.server.ServerCase;
import org.apache.cayenne.unit.di.server.UseServerRuntime;

@UseServerRuntime(ServerCase.TESTMAP_PROJECT)
public class DataContextEJBQLConditionsTest extends ServerCase {

    @Inject
    protected ObjectContext context;

    @Inject
    protected DBHelper dbHelper;

    protected TableHelper tArtist;
    protected TableHelper tPainting;

    @Override
    protected void setUpAfterInjection() throws Exception {
        dbHelper.deleteAll("PAINTING_INFO");
        dbHelper.deleteAll("PAINTING");
        dbHelper.deleteAll("ARTIST_EXHIBIT");
        dbHelper.deleteAll("ARTIST");

        tArtist = new TableHelper(dbHelper, "ARTIST");
        tArtist.setColumns("ARTIST_ID", "ARTIST_NAME");

        tPainting = new TableHelper(dbHelper, "PAINTING");
        tPainting.setColumns(
                "PAINTING_ID",
                "ARTIST_ID",
                "PAINTING_TITLE",
                "ESTIMATED_PRICE");
    }

    protected void createCollectionDataSet() throws Exception {
        tArtist.insert(33001, "B");
        tArtist.insert(33002, "A");
        tArtist.insert(33003, "D");

        tPainting.insert(33009, 33001, "X", 5000);
        tPainting.insert(33010, 33001, "Y", 5000);
        tPainting.insert(33011, 33002, "Z", 5000);
    }

    protected void createLikeDataSet() throws Exception {
        tPainting.insert(33001, null, "ABAAC", 3000);
        tPainting.insert(33002, null, "ADDDD", 4000);
        tPainting.insert(33003, null, "BDDDD", 5000);
        tPainting.insert(33004, null, "BBDDDD", 5000);
        tPainting.insert(33005, null, "_DDDD", 5000);
    }

    protected void createGreaterThanDataSet() throws Exception {
        createLikeDataSet();
    }

    protected void createInDataSet() throws Exception {
        tPainting.insert(33006, null, "A", 5000);
        tPainting.insert(33007, null, "B", 5000);
        tPainting.insert(33008, null, "C", 5000);
    }

    protected void createInSubqueryDataSet() throws Exception {
        tPainting.insert(33012, null, "C", 5000);
        tPainting.insert(33013, null, "D", 5000);
        tPainting.insert(33014, null, "C", 5000);
    }

    public void testDateParameter() throws Exception {
        createCollectionDataSet();

        SelectQuery q = new SelectQuery(Artist.class);
        List<Artist> allArtists = context.performQuery(q);

        Date dob = new Date();
        allArtists.get(0).setDateOfBirth(dob);
        context.commitChanges();

        String ejbql = "SELECT a FROM Artist a WHERE a.dateOfBirth = :x";

        EJBQLQuery query = new EJBQLQuery(ejbql);
        query.setParameter("x", dob);
        List<?> objects = context.performQuery(query);
        assertEquals(1, objects.size());

        assertSame(allArtists.get(0), objects.get(0));
    }

    public void testArithmetics() throws Exception {
        createLikeDataSet();

        // TODO: andrus 02/25/2008 - fails on HSQLDB / succeeds on MySQL. HSQLDB error is
        // "Unresolved parameter type : as both operands of aritmetic operator in

        // statement"
        // String ejbql = "SELECT p FROM Painting p WHERE p.estimatedPrice < (1 + - 4.0 *
        // - 1000.0)";
        //
        // EJBQLQuery query = new EJBQLQuery(ejbql);
        // List<?> objects = createDataContext().performQuery(query);
        // assertEquals(2, objects.size());
        //
        // Set<Object> ids = new HashSet<Object>();
        // Iterator<?> it = objects.iterator();
        // while (it.hasNext()) {
        // Object id = Cayenne.pkForObject((Persistent) it.next());
        // ids.add(id);
        // }
        //
        // assertTrue(ids.contains(new Integer(33001)));
        // assertTrue(ids.contains(new Integer(33002)));
    }

    public void testLike1() throws Exception {
        createLikeDataSet();

        String ejbql = "SELECT p FROM Painting p WHERE p.paintingTitle LIKE 'A%C'";

        EJBQLQuery query = new EJBQLQuery(ejbql);
        List<?> objects = context.performQuery(query);
        assertEquals(1, objects.size());

        Set<Object> ids = new HashSet<Object>();
        Iterator<?> it = objects.iterator();
        while (it.hasNext()) {
            Object id = Cayenne.pkForObject((Persistent) it.next());
            ids.add(id);
        }

        assertTrue(ids.contains(new Integer(33001)));
    }

    public void testNotLike() throws Exception {
        createLikeDataSet();

        String ejbql = "SELECT p FROM Painting p WHERE p.paintingTitle NOT LIKE 'A%C'";

        EJBQLQuery query = new EJBQLQuery(ejbql);
        List<?> objects = context.performQuery(query);
        assertEquals(4, objects.size());

        Set<Object> ids = new HashSet<Object>();
        Iterator<?> it = objects.iterator();
        while (it.hasNext()) {
            Object id = Cayenne.pkForObject((Persistent) it.next());
            ids.add(id);
        }

        assertFalse(ids.contains(new Integer(33001)));
    }

    public void testLike2() throws Exception {
        createLikeDataSet();

        String ejbql = "SELECT p FROM Painting p WHERE p.paintingTitle LIKE '_DDDD'";

        EJBQLQuery query = new EJBQLQuery(ejbql);
        List<?> objects = context.performQuery(query);
        assertEquals(3, objects.size());

        Set<Object> ids = new HashSet<Object>();
        Iterator<?> it = objects.iterator();
        while (it.hasNext()) {
            Object id = Cayenne.pkForObject((Persistent) it.next());
            ids.add(id);
        }

        assertTrue(ids.contains(new Integer(33002)));
        assertTrue(ids.contains(new Integer(33003)));
        assertTrue(ids.contains(new Integer(33005)));
    }

    public void testLikeEscape() throws Exception {
        createLikeDataSet();

        String ejbql = "SELECT p FROM Painting p WHERE p.paintingTitle LIKE 'X_DDDD' ESCAPE 'X'";

        EJBQLQuery query = new EJBQLQuery(ejbql);
        List<?> objects = context.performQuery(query);
        assertEquals(1, objects.size());

        Set<Object> ids = new HashSet<Object>();
        Iterator<?> it = objects.iterator();
        while (it.hasNext()) {
            Object id = Cayenne.pkForObject((Persistent) it.next());
            ids.add(id);
        }

        assertTrue(ids.contains(new Integer(33005)));
    }

    public void testLikeEscape_LikeParameter() throws Exception {
        createLikeDataSet();

        // test for CAY-1426
        String ejbql = "SELECT p FROM Painting p WHERE p.paintingTitle LIKE ?1 ESCAPE 'X'";

        EJBQLQuery query = new EJBQLQuery(ejbql);
        query.setParameter(1, "X_DDDD");
        List<?> objects = context.performQuery(query);
        assertEquals(1, objects.size());

        Set<Object> ids = new HashSet<Object>();
        Iterator<?> it = objects.iterator();
        while (it.hasNext()) {
            Object id = Cayenne.pkForObject((Persistent) it.next());
            ids.add(id);
        }

        assertTrue(ids.contains(new Integer(33005)));
    }

    public void testIn() throws Exception {
        createInDataSet();

        String ejbql = "SELECT p FROM Painting p WHERE p.paintingTitle IN ('A', 'B')";

        EJBQLQuery query = new EJBQLQuery(ejbql);
        List<?> objects = context.performQuery(query);
        assertEquals(2, objects.size());

        Set<Object> ids = new HashSet<Object>();
        Iterator<?> it = objects.iterator();
        while (it.hasNext()) {
            Object id = Cayenne.pkForObject((Persistent) it.next());
            ids.add(id);
        }

        assertTrue(ids.contains(new Integer(33006)));
        assertTrue(ids.contains(new Integer(33007)));
    }

    public void testNotIn() throws Exception {
        createInDataSet();

        String ejbql = "SELECT p FROM Painting p WHERE p.paintingTitle NOT IN ('A', 'B')";

        EJBQLQuery query = new EJBQLQuery(ejbql);
        List<?> objects = context.performQuery(query);
        assertEquals(1, objects.size());

        Set<Object> ids = new HashSet<Object>();
        Iterator<?> it = objects.iterator();
        while (it.hasNext()) {
            Object id = Cayenne.pkForObject((Persistent) it.next());
            ids.add(id);
        }

        assertTrue(ids.contains(new Integer(33008)));
    }

    public void testInSubquery() throws Exception {
        createInSubqueryDataSet();

        String ejbql = "SELECT p FROM Painting p WHERE p.paintingTitle IN ("
                + "SELECT p1.paintingTitle FROM Painting p1 WHERE p1.paintingTitle = 'C'"
                + ")";

        EJBQLQuery query = new EJBQLQuery(ejbql);
        List<?> objects = context.performQuery(query);
        assertEquals(2, objects.size());

        Set<Object> ids = new HashSet<Object>();
        Iterator<?> it = objects.iterator();
        while (it.hasNext()) {
            Object id = Cayenne.pkForObject((Persistent) it.next());
            ids.add(id);
        }

        assertTrue(ids.contains(new Integer(33012)));
        assertTrue(ids.contains(new Integer(33014)));
    }

    public void testCollectionEmpty() throws Exception {
        createCollectionDataSet();

        String ejbql = "SELECT a FROM Artist a WHERE a.paintingArray IS EMPTY";

        EJBQLQuery query = new EJBQLQuery(ejbql);
        List<?> objects = context.performQuery(query);
        assertEquals(1, objects.size());

        Set<Object> ids = new HashSet<Object>();
        Iterator<?> it = objects.iterator();
        while (it.hasNext()) {
            Object id = Cayenne.pkForObject((Persistent) it.next());
            ids.add(id);
        }

        assertTrue(ids.contains(new Long(33003)));
    }

    public void testCollectionNotEmpty() throws Exception {
        createCollectionDataSet();

        String ejbql = "SELECT a FROM Artist a WHERE a.paintingArray IS NOT EMPTY";

        EJBQLQuery query = new EJBQLQuery(ejbql);
        List<?> objects = context.performQuery(query);
        assertEquals(2, objects.size());

        Set<Object> ids = new HashSet<Object>();
        Iterator<?> it = objects.iterator();
        while (it.hasNext()) {
            Object id = Cayenne.pkForObject((Persistent) it.next());
            ids.add(id);
        }

        assertTrue(ids.contains(33001l));
        assertTrue(ids.contains(33002l));
    }

    public void testCollectionNotEmptyExplicitDistinct() throws Exception {
        createCollectionDataSet();

        String ejbql = "SELECT DISTINCT a FROM Artist a WHERE a.paintingArray IS NOT EMPTY";

        EJBQLQuery query = new EJBQLQuery(ejbql);
        List<?> objects = context.performQuery(query);
        assertEquals(2, objects.size());

        Set<Object> ids = new HashSet<Object>();
        Iterator<?> it = objects.iterator();
        while (it.hasNext()) {
            Object id = Cayenne.pkForObject((Persistent) it.next());
            ids.add(id);
        }

        assertTrue(ids.contains(33001l));
        assertTrue(ids.contains(33002l));
    }

    public void testCollectionMemberOfParameter() throws Exception {
        createCollectionDataSet();

        String ejbql = "SELECT a FROM Artist a WHERE :x MEMBER OF a.paintingArray";

        EJBQLQuery query = new EJBQLQuery(ejbql);
        query.setParameter("x", Cayenne.objectForPK(context, Painting.class, 33010));
        List<?> objects = context.performQuery(query);
        assertEquals(1, objects.size());

        Set<Object> ids = new HashSet<Object>();
        Iterator<?> it = objects.iterator();
        while (it.hasNext()) {
            Object id = Cayenne.pkForObject((Persistent) it.next());
            ids.add(id);
        }

        assertTrue(ids.contains(33001l));
    }

    public void testGreaterOrEquals() throws Exception {
        createGreaterThanDataSet();

        String ejbql = "SELECT p FROM Painting p WHERE p.estimatedPrice >= :estimatedPrice";

        EJBQLQuery query = new EJBQLQuery(ejbql);
        query.setParameter("estimatedPrice", new BigDecimal(4000));
        List<?> objects = context.performQuery(query);
        assertEquals(4, objects.size());
    }

    public void testLessOrEquals() throws Exception {
        createGreaterThanDataSet();

        String ejbql = "SELECT p FROM Painting p WHERE p.estimatedPrice <= :estimatedPrice";

        EJBQLQuery query = new EJBQLQuery(ejbql);
        query.setParameter("estimatedPrice", new BigDecimal(4000));
        List<?> objects = context.performQuery(query);
        assertEquals(2, objects.size());
    }

    public void testCollectionNotMemberOfParameter() throws Exception {
        createCollectionDataSet();

        String ejbql = "SELECT a FROM Artist a WHERE :x NOT MEMBER a.paintingArray";

        EJBQLQuery query = new EJBQLQuery(ejbql);
        query.setParameter("x", Cayenne.objectForPK(context, Painting.class, 33010));
        List<?> objects = context.performQuery(query);
        assertEquals(2, objects.size());

        Set<Object> ids = new HashSet<Object>();
        Iterator<?> it = objects.iterator();
        while (it.hasNext()) {
            Object id = Cayenne.pkForObject((Persistent) it.next());
            ids.add(id);
        }

        assertTrue(ids.contains(33002l));
        assertTrue(ids.contains(33003l));
    }

    public void testCollectionMemberOfThetaJoin() throws Exception {
        createCollectionDataSet();

        String ejbql = "SELECT p FROM Painting p, Artist a "
                + "WHERE p MEMBER OF a.paintingArray AND a.artistName = 'B'";

        EJBQLQuery query = new EJBQLQuery(ejbql);
        List<?> objects = context.performQuery(query);
        assertEquals(2, objects.size());

        Set<Object> ids = new HashSet<Object>();
        Iterator<?> it = objects.iterator();
        while (it.hasNext()) {
            Object id = Cayenne.pkForObject((Persistent) it.next());
            ids.add(id);
        }

        assertTrue(ids.contains(new Integer(33009)));
        assertTrue(ids.contains(new Integer(33010)));
    }
    
}
