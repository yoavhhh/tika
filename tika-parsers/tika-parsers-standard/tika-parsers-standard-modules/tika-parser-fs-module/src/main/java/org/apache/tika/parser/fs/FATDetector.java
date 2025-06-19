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

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import org.apache.tika.detect.Detector;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.mime.MediaType;

public class FATDetector implements Detector {

    private static final MediaType FAT12 = MediaType.application("x-fat12-image");
    private static final MediaType FAT16 = MediaType.application("x-fat16-image");
    private static final MediaType FAT32 = MediaType.application("x-fat32-image");

    @Override
    public MediaType detect(InputStream input, Metadata metadata) throws IOException {
        if (!input.markSupported()) {
            return MediaType.OCTET_STREAM;
        }

        input.mark(512);
        byte[] header = new byte[512];
        int read = input.read(header);
        input.reset();

        if (read < 90) {
            return MediaType.OCTET_STREAM;
        }

        // Validate boot signature at offset 510–511: should be 0x55AA
        if ((header[510] & 0xFF) != 0x55 || (header[511] & 0xFF) != 0xAA) {
            return MediaType.OCTET_STREAM;
        }

        // Extract FAT12/16 type string (offset 54–61)
        String fatType = new String(Arrays.copyOfRange(header, 54, 62), 
                StandardCharsets.US_ASCII).trim();

        // Extract FAT32 type string (offset 82–90)
        String fat32Type = new String(Arrays.copyOfRange(header, 82, 90), 
                StandardCharsets.US_ASCII).trim();

        if ("FAT12".equalsIgnoreCase(fatType)) {
            return FAT12;
        } else if ("FAT16".equalsIgnoreCase(fatType)) {
            return FAT16;
        } else if ("FAT32".equalsIgnoreCase(fat32Type)) {
            return FAT32;
        }

        return MediaType.OCTET_STREAM;
    }
}
