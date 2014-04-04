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
package org.apache.cayenne.crypto.unit;

import static org.junit.Assert.assertArrayEquals;

import org.apache.cayenne.crypto.transformer.bytes.BytesEncryptor;
import org.junit.Test;

public class SwapBytesTransformerTest {

    @Test
    public void testEncrypt_Odd() {

        BytesEncryptor instance = SwapBytesTransformer.encryptor();

        byte[] input = { 1, 3, 5 };
        byte[] output = { 8, 11, 13, 0, 0, 0, 5, 6 };

        instance.encrypt(input, output, 3);

        assertArrayEquals(new byte[] { 8, 11, 13, 5, 3, 1, 5, 6 }, output);
    }
    
    @Test
    public void testEncrypt_Even() {

        BytesEncryptor instance = SwapBytesTransformer.encryptor();

        byte[] input = { 1, 3, 5, 8 };
        byte[] output = { 8, 11, 13, 0, 0, 0, 0};

        instance.encrypt(input, output, 3);

        assertArrayEquals(new byte[] { 8, 11, 13, 8, 5, 3, 1}, output);
    }
}
