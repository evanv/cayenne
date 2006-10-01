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

package org.apache.cayenne.exp;

import junit.framework.TestCase;

/**
 * @author Andrus Adamchik
 */
public class ExpressionEqualsTst extends TestCase {

    public void testEquals3() throws Exception {
        Expression e1 = ExpressionFactory.matchExp("aa", "3");
        Expression e2 = ExpressionFactory.matchExp("aa", "3");
        Expression e3 = ExpressionFactory.matchExp("aa", new Integer(3));
        assertEquals(e1, e2);
        assertFalse(e2.equals(e3));
    }

    public void testEquals4() throws Exception {
        Expression e1 = ExpressionFactory.matchExp("aa", "3").andExp(
                ExpressionFactory.matchExp("aa", "4"));
        Expression e2 = ExpressionFactory.matchExp("aa", "3").andExp(
                ExpressionFactory.matchExp("aa", "4"));
        Expression e3 = ExpressionFactory.matchExp("aa", new Integer(3));

        assertEquals(e1, e2);
        assertFalse(e2.equals(e3));
    }
}
