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

import org.apache.cayenne.unit.CayenneTestCase;

/**
 * @deprecated since 1.2
 */
public class UnaryExpressionTst extends CayenneTestCase {
    // non-existent type
    private static final int defaultType = -34;
    protected UnaryExpression expr;
    
    
    protected void setUp() throws java.lang.Exception {
        expr = new UnaryExpression(defaultType);
    }
    
    
    public void testGetType() throws java.lang.Exception {
        assertEquals(defaultType, expr.getType());
    }
    
    
    public void testGetOperandCount() throws java.lang.Exception {
        assertEquals(1, expr.getOperandCount());
    }
    
    
    public void testGetOperandAtIndex() throws java.lang.Exception {
        expr.getOperand(0);
        
        try {
            expr.getOperand(1);
            fail();
        }
        catch(Exception ex) {
            // exception expected..
        }
    }
    
    
    public void testSetOperandAtIndex() throws java.lang.Exception {
        Object obj = new Object();
        expr.setOperand(0, obj);
        assertSame(obj, expr.getOperand(0));
        
        try {
            expr.setOperand(1, obj);
            fail();
        }
        catch(Exception ex) {
            // exception expected..
        }
    }
    
    
}
