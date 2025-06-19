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
package org.apache.tika.parser.ntfs;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import org.apache.tika.detect.Detector;
import org.apache.tika.io.IOUtils;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.mime.MediaType;

public class FileSystemDetector implements Detector {

    private static final long serialVersionUID = 1L;

    // Signatures and offsets
    private static final int NTFS_SIGNATURE_OFFSET = 0x03;
    private static final byte[] NTFS_SIGNATURE = "NTFS    ".getBytes(StandardCharsets.US_ASCII);

    private static final int FAT12_SIGNATURE_OFFSET = 0x36;
    private static final byte[] FAT12_SIGNATURE = "FAT12   ".getBytes(StandardCharsets.US_ASCII);

    private static final int FAT16_SIGNATURE_OFFSET = 0x36;
    private static final byte[] FAT16_SIGNATURE = "FAT16   ".getBytes(StandardCharsets.US_ASCII);

    private static final int FAT32_SIGNATURE_OFFSET = 0x52;
    private static final byte[] FAT32_SIGNATURE = "FAT32   ".getBytes(StandardCharsets.US_ASCII);

    private static final int EXFAT_SIGNATURE_OFFSET = 0x03;
    private static final byte[] EXFAT_SIGNATURE = "EXFAT   ".getBytes(StandardCharsets.US_ASCII); // Standard exFAT signature starts at offset 3 with "EXFAT   "

    // Max offset + signature length needed for checking.
    // FAT32 offset 0x52 (82) + "FAT32   " (8 bytes) = 90
    // NTFS offset 0x03 (3) + "NTFS    " (8 bytes) = 11
    // EXFAT offset 0x03 (3) + "EXFAT   " (8 bytes) = 11
    // FAT12/16 offset 0x36 (54) + "FAT12/16   " (8 bytes) = 62
    // So, the max buffer size needed is 90.
    private static final int MAX_BUFFER_SIZE = 90;

    @Override
    public MediaType detect(InputStream input, Metadata metadata) throws IOException {
        if (input == null) {
            return MediaType.OCTET_STREAM;
        }

        input.mark(MAX_BUFFER_SIZE + 1);
        try {
            byte[] buffer = new byte[MAX_BUFFER_SIZE];
            // Use IOUtils.readFully to ensure the buffer is filled if possible
            int n = IOUtils.readFully(input, buffer, 0, MAX_BUFFER_SIZE);

            // Check for NTFS
            // Ensure enough bytes were read for this specific signature
            if (n >= NTFS_SIGNATURE_OFFSET + NTFS_SIGNATURE.length) {
                if (Arrays.equals(Arrays.copyOfRange(buffer, NTFS_SIGNATURE_OFFSET, NTFS_SIGNATURE_OFFSET + NTFS_SIGNATURE.length), NTFS_SIGNATURE)) {
                    return MediaType.application("x-ntfs-image");
                }
            }

            // Check for FAT12
            if (n >= FAT12_SIGNATURE_OFFSET + FAT12_SIGNATURE.length) {
                if (Arrays.equals(Arrays.copyOfRange(buffer, FAT12_SIGNATURE_OFFSET, FAT12_SIGNATURE_OFFSET + FAT12_SIGNATURE.length), FAT12_SIGNATURE)) {
                    return MediaType.application("x-fat12-image");
                }
            }

            // Check for FAT16
            if (n >= FAT16_SIGNATURE_OFFSET + FAT16_SIGNATURE.length) {
                if (Arrays.equals(Arrays.copyOfRange(buffer, FAT16_SIGNATURE_OFFSET, FAT16_SIGNATURE_OFFSET + FAT16_SIGNATURE.length), FAT16_SIGNATURE)) {
                    return MediaType.application("x-fat16-image");
                }
            }

            // Check for FAT32
            if (n >= FAT32_SIGNATURE_OFFSET + FAT32_SIGNATURE.length) {
                if (Arrays.equals(Arrays.copyOfRange(buffer, FAT32_SIGNATURE_OFFSET, FAT32_SIGNATURE_OFFSET + FAT32_SIGNATURE.length), FAT32_SIGNATURE)) {
                    return MediaType.application("x-fat32-image");
                }
            }

            // Check for exFAT
            // Ensure enough bytes were read for this specific signature
            if (n >= EXFAT_SIGNATURE_OFFSET + EXFAT_SIGNATURE.length) {
                 // Check if the first 3 bytes are 0xEB 0x76 0x90 (common for exFAT boot sectors)
                if (buffer[0] == (byte)0xEB && buffer[1] == (byte)0x76 && buffer[2] == (byte)0x90) {
                    // Then check for "EXFAT" at offset 3
                   if (Arrays.equals(Arrays.copyOfRange(buffer, EXFAT_SIGNATURE_OFFSET, EXFAT_SIGNATURE_OFFSET + EXFAT_SIGNATURE.length), EXFAT_SIGNATURE)) {
                        return MediaType.application("x-exfat-image");
                    }
                } else if (Arrays.equals(Arrays.copyOfRange(buffer, EXFAT_SIGNATURE_OFFSET, EXFAT_SIGNATURE_OFFSET + EXFAT_SIGNATURE.length), EXFAT_SIGNATURE)) {
                    // Fallback for exFAT images that might not have the typical boot sector jump code, but do have "EXFAT   " at offset 3
                    // This is less specific, so it's a fallback.
                    // A more robust exFAT detection might involve checking filesystem structures.
                    // For now, per instructions, we check "EXFAT" at offset 0x03
                     return MediaType.application("x-exfat-image");
                }
            }

            return MediaType.OCTET_STREAM;

        } finally {
            input.reset();
        }
    }
}
