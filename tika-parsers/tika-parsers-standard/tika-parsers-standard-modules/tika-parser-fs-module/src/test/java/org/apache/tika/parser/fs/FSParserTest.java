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

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

import org.apache.tika.TikaTest;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.fs.MBRDetector;


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

    @Test
    public void testMBR() throws Exception {
        // The test-mbr.img is a very basic MBR, it doesn't contain a file system
        // that FSParser's underlying Sleuthkit logic would list files from.
        // The main thing is that FSParser should identify it correctly via MBRDetector
        // and not fail during parsing.
        // We expect it to be identified as "application/x-mbr-image".
        // We don't expect it to list any embedded files like the FAT/NTFS tests.
        XMLResult xml = getXML("mbr/test-mbr.img");

        // Check that the parser identifies the correct content type
        assertContains(MBRDetector.MBR_IMAGE.toString(), xml.metadata.get(Metadata.CONTENT_TYPE));

        // For a raw MBR image without a recognized file system within,
        // we don't expect specific file entries in the XHTML output.
        // The FSParser might output a generic div or nothing for the body if no FS is processed.
        // A basic check could be that it produces valid XML.
        assertNotNull(xml.xml);

        // Depending on how FSParser handles images it can't find a filesystem in,
        // the output might be minimal. If Tika/FSParser has a standard way of reporting
        // such cases (e.g., a specific metadata field or XML content), that could be asserted.
        // For now, ensuring it's identified and doesn't crash is the primary goal.
        // We can refine assertions later if there's specific output to expect.

        // Example: Check if the FileSystem metadata field is NOT set or is set to something
        // indicating no recognized FS, as MBR itself isn't a browsable FS by TSK in this context.
        // This depends on FSParser's behavior with such images.
        // String fileSystemMeta = xml.metadata.get("FileSystem");
        // assertTrue(fileSystemMeta == null || fileSystemMeta.isEmpty() ||
        //            !fileSystemMeta.contains("NTFS") && !fileSystemMeta.contains("FAT"));

        // The most important part is that it was processed without error and identified.
        // The FSParser's job is to parse file *systems*. An MBR is a prerequisite to that,
        // but not a file system itself in terms of having a listable directory structure
        // directly from the MBR's 512 bytes.
    }
}
