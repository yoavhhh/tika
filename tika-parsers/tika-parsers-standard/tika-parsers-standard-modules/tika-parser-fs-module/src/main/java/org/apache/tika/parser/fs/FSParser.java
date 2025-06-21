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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;

// Sleuth Kit imports - will need to be adjusted based on actual class names and availability
import org.sleuthkit.datamodel.AbstractFile;
import org.sleuthkit.datamodel.FileSystem;
import org.sleuthkit.datamodel.Image;
import org.sleuthkit.datamodel.ReadContentInputStream;
import org.sleuthkit.datamodel.SleuthkitCase;
import org.sleuthkit.datamodel.SleuthkitJNI.CaseDbHandle.AddImageProcess;
import org.sleuthkit.datamodel.TskCoreException;
import org.sleuthkit.datamodel.TskDataException;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

import org.apache.tika.exception.TikaException;
import org.apache.tika.extractor.EmbeddedDocumentExtractor;
import org.apache.tika.extractor.EmbeddedDocumentUtil;
import org.apache.tika.io.TemporaryResources;
import org.apache.tika.io.TikaInputStream;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.metadata.TikaCoreProperties;
import org.apache.tika.mime.MediaType;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.sax.XHTMLContentHandler;


public class FSParser  implements Parser {

    private static final long serialVersionUID = 1L;
    private static final Set<MediaType> SUPPORTED_TYPES =
            Collections.unmodifiableSet(Set.of(
                MediaType.application("x-ntfs-image"), 
                MediaType.application("x-fat32-image"), 
                MediaType.application("x-fat16-image"), 
                MediaType.application("x-fat12-image"),
                MediaType.application("x-exfat-image"),
                MediaType.application("x-mbr-image")));
    
    // Ensure the native TSK library is loaded once
    static {
        try {
            // Adjust this path if your native libraries are elsewhere
            System.loadLibrary("tsk_jni");
            // OR if using -Djava.library.path as JVM arg, you don't need this line.
            // This is more of a safety check.
        } catch (UnsatisfiedLinkError e) {
            // Do nothing
        }
    }


    @Override
    public Set<MediaType> getSupportedTypes(ParseContext context) {
        return SUPPORTED_TYPES;
    }

    @Override
    public void parse(InputStream stream, ContentHandler handler, Metadata metadata, ParseContext context)
            throws IOException, SAXException, TikaException {

        TemporaryResources tmp = new TemporaryResources();
        Path imagePath;
        TikaInputStream tis = TikaInputStream.cast(stream);

        if (tis != null && tis.hasFile()) {
            imagePath = tis.getPath();
        } else {
            // Stream to a temporary file as Sleuth Kit likely needs file access
            Path tmpFile = tmp.createTempFile();
            Files.copy(stream, tmpFile, StandardCopyOption.REPLACE_EXISTING);
            imagePath = tmpFile;
        }

        XHTMLContentHandler xhtml = new XHTMLContentHandler(handler, metadata);
        xhtml.startDocument();

        EmbeddedDocumentExtractor extractor = EmbeddedDocumentUtil.getEmbeddedDocumentExtractor(context);


        SleuthkitCase skCase = null;
        try {
            String caseDbPath = tmp.createTemporaryFile().toPath().toString() + ".db";
            skCase = SleuthkitCase.newCase(caseDbPath);
            
            String acquisitionTimeZone = "UTC";
            String fakeDeviceid = "FS_Device_" + System.currentTimeMillis(); // Unique device ID for the image
            String[] imageArray = new String[] { imagePath.toString() };
            String imageName = metadata.get(TikaCoreProperties.RESOURCE_NAME_KEY); // Use original filename as image name
            if (imageName == null || imageName.isEmpty()) {
                imageName = "FS_Image_" + System.currentTimeMillis();
            }

            AddImageProcess addImageProcess = skCase.makeAddImageProcess(acquisitionTimeZone, false, false, imagePath.toString());
            addImageProcess.run(fakeDeviceid, imageArray);

            List<Image> caseImages = skCase.getImages();
            if (caseImages.isEmpty()) {
                throw new TikaException("No images found in the case");
            }

            for (Image image : caseImages) {

                Collection<FileSystem> fileSystems = skCase.getImageFileSystems(image);
                // Get file systems from the image
                if (fileSystems.isEmpty()) {
                    throw new TikaException("No file systems found in the image: " + imageName);
                }

                //Without this BodyContentHandler does not work
                xhtml.element("div", " ");
                
                // Iterate through each file system
                for (FileSystem fs : fileSystems) {
                    metadata.set("FileSystem", fs.getFsType().name()); 
                    List<AbstractFile> rootObjects = fs.getRootDirectory().listFiles();
                    
                    // Process each root object in the file system
                    for (AbstractFile rootObject : rootObjects) {
                        processFileOrDirectory(rootObject, xhtml, metadata, context, tmp, extractor, handler);
                    }
                }
            }

        } catch (TskCoreException | TskDataException e) {
            throw new TikaException("Sleuth Kit processing error: " + e.getMessage(), e);
        } finally {
            if (skCase != null) {
                skCase.close();
            }
            tmp.dispose(); // Deletes temporary files
        }

        xhtml.endDocument();
    }


    protected static Metadata handleEntryMetadata(String name, Date createAt, Date modifiedAt,
                                                  Long size, XHTMLContentHandler xhtml)
            throws SAXException, IOException, TikaException {
        Metadata entrydata = new Metadata();
        if (createAt != null) {
            entrydata.set(TikaCoreProperties.CREATED, createAt);
        }
        if (modifiedAt != null) {
            entrydata.set(TikaCoreProperties.MODIFIED, modifiedAt);
        }
        if (size != null) {
            entrydata.set(Metadata.CONTENT_LENGTH, Long.toString(size));
        }
        if (name != null && name.length() > 0) {
            name = name.replace("\\", "/");
            entrydata.set(TikaCoreProperties.RESOURCE_NAME_KEY, name);
            AttributesImpl attributes = new AttributesImpl();
            attributes.addAttribute("", "class", "class", "CDATA", "embedded");
            attributes.addAttribute("", "id", "id", "CDATA", name);
            xhtml.startElement("div", attributes);
            xhtml.endElement("div");

            entrydata.set(TikaCoreProperties.EMBEDDED_RELATIONSHIP_ID, name);
        }
        return entrydata;
    }

    

    private void processFileOrDirectory(AbstractFile fileOrDir, XHTMLContentHandler xhtml,
                                        Metadata parentMetadata, ParseContext context, TemporaryResources tmp,
                                        EmbeddedDocumentExtractor extractor,
                                        ContentHandler handler)
            throws IOException, SAXException, TikaException, TskCoreException {

        if (fileOrDir.isDir()) {

            List<AbstractFile> children = fileOrDir.listFiles();
            for (AbstractFile child : children) {
                processFileOrDirectory(child, xhtml, parentMetadata, context, tmp, extractor, handler);
            }

        } else if (fileOrDir.isFile()) {
            Metadata entrydata = FSParser.handleEntryMetadata(fileOrDir.getName(), new Date(fileOrDir.getCrtime()), new Date(fileOrDir.getMtime()), fileOrDir.getSize(), xhtml);
            ReadContentInputStream fileInputStream = new ReadContentInputStream(fileOrDir);

            try (TikaInputStream fileTis = TikaInputStream.get(fileInputStream, entrydata)) {
                if (extractor.shouldParseEmbedded(entrydata)) {
                    extractor.parseEmbedded(fileTis, handler, entrydata, true);
                }
            }
        }
    }
}
