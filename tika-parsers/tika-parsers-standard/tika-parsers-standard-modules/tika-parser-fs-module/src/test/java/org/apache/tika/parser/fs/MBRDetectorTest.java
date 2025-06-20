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

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.tika.metadata.Metadata;
import org.apache.tika.mime.MediaType;
import org.junit.jupiter.api.Test;

public class MBRDetectorTest {

    private final MBRDetector detector = new MBRDetector();

    @Test
    public void testDetectValidMBR() throws Exception {
        // Create a byte array representing a minimal MBR structure
        // with the 0x55AA signature at offset 510.
        byte[] mbrData = new byte[512];
        mbrData[510] = (byte) 0x55;
        mbrData[511] = (byte) 0xAA;

        try (InputStream stream = new ByteArrayInputStream(mbrData)) {
            MediaType detected = detector.detect(stream, new Metadata());
            assertEquals(MBRDetector.MBR_IMAGE, detected, "Should detect MBR_IMAGE");
        }
    }

    @Test
    public void testDetectNonMBR() throws Exception {
        // Create a byte array that does not have the MBR signature.
        byte[] nonMbrData = new byte[512];
        // Fill with some other data, ensuring it's not 0x55AA at 510-511
        nonMbrData[510] = (byte) 0x00;
        nonMbrData[511] = (byte) 0x00;


        try (InputStream stream = new ByteArrayInputStream(nonMbrData)) {
            MediaType detected = detector.detect(stream, new Metadata());
            assertEquals(MediaType.OCTET_STREAM, detected, "Should detect OCTET_STREAM for non-MBR data");
        }
    }

    @Test
    public void testDetectEmptyStream() throws Exception {
        try (InputStream stream = new ByteArrayInputStream(new byte[0])) {
            MediaType detected = detector.detect(stream, new Metadata());
            assertEquals(MediaType.OCTET_STREAM, detected, "Should detect OCTET_STREAM for an empty stream");
        }
    }

    @Test
    public void testDetectShortStream() throws Exception {
        // Stream shorter than the required buffer size for MBR detection
        byte[] shortData = new byte[100];
        try (InputStream stream = new ByteArrayInputStream(shortData)) {
            MediaType detected = detector.detect(stream, new Metadata());
            assertEquals(MediaType.OCTET_STREAM, detected, "Should detect OCTET_STREAM for a short stream");
        }
    }

    @Test
    public void testDetectNullStream() throws Exception {
        // Test with a null input stream
        MediaType detected = detector.detect(null, new Metadata());
        assertEquals(MediaType.OCTET_STREAM, detected, "Should detect OCTET_STREAM for a null stream");
    }

    @Test
    public void testDetectWithRealMBRFile() throws Exception {
        Path mbrFilePath = Paths.get("src", "test", "resources", "test-documents", "mbr", "test-mbr.img");
        try (InputStream stream = Files.newInputStream(mbrFilePath)) {
            MediaType detected = detector.detect(stream, new Metadata());
            assertEquals(MBRDetector.MBR_IMAGE, detected, "Should detect MBR_IMAGE from file");
        }
    }
}
