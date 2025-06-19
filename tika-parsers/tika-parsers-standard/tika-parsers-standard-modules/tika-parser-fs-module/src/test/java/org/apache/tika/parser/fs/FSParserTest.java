/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.tika.parser.fs;

import org.junit.jupiter.api.Test;

import org.apache.tika.TikaTest;


public class FSParserTest extends TikaTest {

    @Test
    public void testNTFS() throws Exception {
        XMLResult xml = getXML("raw_ntfs_image.img");
        System.out.println(xml.xml);
        assert xml.xml.contains("testWORD.doc");
    }

    @Test
    public void testFAT32() throws Exception {
        XMLResult xml = getXML("raw_fat32_image.img");
        System.out.println(xml.xml);
        assert xml.xml.contains("testWORD.doc");
    }

    @Test
    public void testFAT16() throws Exception {
        XMLResult xml = getXML("raw_fat16_image.img");
        System.out.println(xml.xml);
        assert xml.xml.contains("testWORD.doc");
    }

    @Test
    public void testFAT12() throws Exception {
        XMLResult xml = getXML("raw_fat12_image.img");
        System.out.println(xml.xml);
        assert xml.xml.contains("testWORD.doc");
    }


}
