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

import java.io.UnsupportedEncodingException;

import javax.crypto.Cipher;

import org.apache.cayenne.crypto.transformer.value.ValueTransformer;
import org.apache.cayenne.crypto.transformer.value.ValueTransformerFactory;
import org.apache.cayenne.map.DbAttribute;

public class Rot13TransformerFactory implements ValueTransformerFactory {

    private ValueTransformer stringTransformer;

    public static String rotate(String value) {
        if (value == null) {
            return null;
        }

        int length = value.length();
        StringBuilder result = new StringBuilder();

        for (int i = 0; i < length; i++) {
            char c = value.charAt(i);

            // If c is a letter, rotate it by 13. Numbers/symbols are untouched.
            if ((c >= 'a' && c <= 'm') || (c >= 'A' && c <= 'M'))
                c += 13; // The first half of the alphabet goes forward 13
                         // letters
            else if ((c >= 'n' && c <= 'z') || (c >= 'A' && c <= 'Z'))
                c -= 13; // The last half of the alphabet goes backward 13
                         // letters

            result.append(c);
        }

        return result.toString();
    }

    public static byte[] rotate(byte[] value) {
        try {
            String valueString = new String(value, "UTF-8");
            return rotate(valueString).getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException("Can't convert between bytes and String", e);
        }
    }

    public Rot13TransformerFactory() {
        this.stringTransformer = new ValueTransformer() {

            @Override
            public Object transform(Cipher cipher, Object value) {
                return value != null ? rotate(value.toString()) : null;
            }
        };

    }

    @Override
    public ValueTransformer decryptor(DbAttribute a) {
        return stringTransformer;
    }

    @Override
    public ValueTransformer encryptor(DbAttribute a) {
        return stringTransformer;
    }
}
