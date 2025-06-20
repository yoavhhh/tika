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

import org.apache.tika.detect.Detector;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.mime.MediaType;

public class MBRDetector implements Detector {

    private static final long serialVersionUID = 1L;

    public static final MediaType MBR_IMAGE = MediaType.application("x-mbr-image");

    // MBR signature is 0x55AA at offset 510
    private static final int SIGNATURE_OFFSET = 510;
    private static final int SIGNATURE_LENGTH = 2;
    private static final byte[] MBR_SIGNATURE = {(byte) 0x55, (byte) 0xAA};

    // The buffer size should be enough to read the signature
    private static final int BUFFER_SIZE = SIGNATURE_OFFSET + SIGNATURE_LENGTH;


    @Override
    public MediaType detect(InputStream input, Metadata metadata) throws IOException {
        if (input == null) {
            return MediaType.OCTET_STREAM;
        }

        // Check if the input stream supports marking.
        // If not, it's not possible to reset the stream to its original position
        // after reading the header, so we can't safely perform detection.
        if (!input.markSupported()) {
            // Log or handle this case as appropriate for your application
            // System.err.println("Mark/reset not supported");
            return MediaType.OCTET_STREAM;
        }

        input.mark(BUFFER_SIZE);
        try {
            byte[] buffer = new byte[BUFFER_SIZE];
            int bytesRead = readFully(input, buffer);

            if (bytesRead < BUFFER_SIZE) {
                // Not enough bytes to check the signature
                return MediaType.OCTET_STREAM;
            }

            // Check for the MBR signature at offset 510-511
            if ((buffer[SIGNATURE_OFFSET] & 0xFF) == (MBR_SIGNATURE[0] & 0xFF) &&
                (buffer[SIGNATURE_OFFSET + 1] & 0xFF) == (MBR_SIGNATURE[1] & 0xFF)) {
                return MBR_IMAGE;
            }

            return MediaType.OCTET_STREAM;
        } finally {
            input.reset();
        }
    }

    /**
     * Reads {@code buffer.length} bytes from the input stream into the buffer.
     *
     * @param input the input stream to read from.
     * @param buffer the buffer to read the bytes into.
     * @return the total number of bytes read into the buffer.
     * @throws IOException if an I/O error occurs.
     */
    private int readFully(InputStream input, byte[] buffer) throws IOException {
        int bytesRead = 0;
        int offset = 0;
        int length = buffer.length;
        while (bytesRead != -1 && offset < length) {
            bytesRead = input.read(buffer, offset, length - offset);
            if (bytesRead != -1) {
                offset += bytesRead;
            }
        }
        return offset;
    }
}
