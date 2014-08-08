/*
 * Copyright (C) 2014 Seth Shaw.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301  USA
 */

package org.dataaccessioner;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import org.apache.tika.metadata.DublinCore;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.metadata.Property;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.Namespace;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.jdom.transform.XSLTransformException;
import org.jdom.transform.XSLTransformer;

/**
 *
 * @author Seth Shaw
 */
public class MetadataManager {

    public final static int STATUS_FAILURE = -20;
    public final static int STATUS_SUCCESS = 30;
    
    private static File xmlFile;
    private static Document document;
    private static Element currentElement;
    private static final Namespace DEFAULT_NAMESPACE = Namespace.getNamespace("http://dataaccessioner.org/schema/dda-0-3-1");
    private static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
    
    private static final Namespace DC_NAMESPACE = Namespace.getNamespace("dc","http://purl.org/dc/elements/1.1/");
    private static final Namespace DC_XML = Namespace.getNamespace("dcx","http://purl.org/dc/xml/");
    protected static final List<Property> DC_ELEMENTS = Arrays.asList(
        DublinCore.CONTRIBUTOR,
        DublinCore.COVERAGE,
        DublinCore.CREATED,
        DublinCore.CREATOR,
        DublinCore.DATE,
        DublinCore.DESCRIPTION,
        DublinCore.FORMAT,
        DublinCore.IDENTIFIER,
        DublinCore.LANGUAGE,
        DublinCore.MODIFIED,
        DublinCore.PUBLISHER,
        DublinCore.RELATION,
        DublinCore.RIGHTS,
        DublinCore.SOURCE,
        DublinCore.SUBJECT,
        DublinCore.TITLE,
        DublinCore.TYPE
    );

    private String DEFAULT_XSLT = "xml/metadataManager.xsl";
    
    private File metadataFile = null;
    private String collectionName = "";
    private String accessionNumber = "";
    private HashMap<String, Metadata> annotatedFiles = new HashMap<String, Metadata>(); //Hash key is the file's absolute path
    
    public static String getName() {
        return "Full Metadata";
    }

    public MetadataManager(File metadataFile, String collectionName, String accessionNumber) {
        this.metadataFile = metadataFile;
        this.collectionName = collectionName;
        this.accessionNumber = accessionNumber;
    }
    
    public MetadataManager() {
        
    }
 
    public void open() throws Exception {
        xmlFile = metadataFile;
        xmlFile.getParentFile().mkdirs(); //In case it doesn't exist
        if (!xmlFile.exists() || xmlFile.length() == 0) {
            Element collection = new Element("collection", DEFAULT_NAMESPACE);
            collection.setAttribute("name", collectionName);
            document = new Document(collection);
            Element accessionElement = new Element("accession", DEFAULT_NAMESPACE);
            accessionElement.setAttribute("number", accessionNumber);
            collection.addContent(accessionElement);
        } else {
            try {
                document = new SAXBuilder().build(xmlFile);
            } catch (JDOMException ex) {
                throw new Exception("Could not read the metadata file "
                        + xmlFile.getPath() + ": " + ex.getLocalizedMessage());
            }
        }

        currentElement = document.getRootElement()
                .getChild("accession", DEFAULT_NAMESPACE);
    }
    
    public void close() {
        FileOutputStream stream = null;
        OutputStreamWriter osw = null;
        PrintWriter output = null;
        try {
            stream = new FileOutputStream(xmlFile);
            osw = new OutputStreamWriter(stream, "UTF-8");
            output = new PrintWriter(osw);
            Format format = Format.getPrettyFormat();
            XMLOutputter outputter = new XMLOutputter(format);
            outputter.output(document, output);
        } catch (IOException ex) {
            System.err.println("Could not write to metadata file " 
                    + xmlFile.getPath() + ": " + ex.getMessage());
        } finally {
            try {
                output.close();
                osw.close();
                stream.close();
            } catch (Exception ex) {
                System.err.println("Failed to close output streams when writting out " 
                        + xmlFile.getName() + ": " + ex.getMessage());
            }
        }
    }

    public void cancel() {
        // Free up everything.
        xmlFile = null;
        currentElement = null;
        document.removeContent();
        document = null;
        System.gc();
    }

    public void startFile(File file) {
        Element fileNode = new Element("file", DEFAULT_NAMESPACE);
        currentElement.addContent(fileNode);
        fileNode.setAttribute("name", file.getName());

        String last_modified = DATE_FORMAT.format(new Date(file.lastModified()));
        fileNode.setAttribute("last_modified", last_modified);

        if (file.isHidden() == true) {
            fileNode.setAttribute("hidden", "true");
        }
        fileNode.setAttribute("size", String.valueOf(file.length()));
        currentElement = fileNode;
        processFileAnnotations(file);
    }

    public void startDirectory(File directory) {
        Element newDir = new Element("folder", DEFAULT_NAMESPACE);
        newDir.setAttribute("name", directory.getName());

        String last_modified = DATE_FORMAT.format(new Date(directory.lastModified()));
        newDir.setAttribute("last_modified", last_modified);

        if (directory.isHidden()) {
            newDir.setAttribute("hidden", "true");
        }
        
        currentElement.addContent(newDir);
        currentElement = newDir;
        processFileAnnotations(directory);
    }

    public void closeCurrentElement() {
        //TODO: Add Additional Metadata
        currentElement = currentElement.getParentElement();
    }

    public void addAttribute(String name, String value){
        currentElement.setAttribute(name, value);
    }
    public void addElement(Element element){
        currentElement.addContent(element);
    }

    public boolean addDocumentXSLT(Document document){
        try {
            XSLTransformer transformer = new XSLTransformer("xml/metadataManager.xsl");
            Document premis = transformer.transform(document);
            addElement(premis.detachRootElement());
        } catch (XSLTransformException ex) {
            System.err.println("Failed to transform document xml using xml/metadataManager.xsl");
            addElement(document.detachRootElement());
            return false;
        }
        return true;
    }
    
       
    public Metadata getFileAnnotation(File file){
        if(annotatedFiles.containsKey(file.getAbsolutePath())){
            return annotatedFiles.get(file.getAbsolutePath());
        } else {
            return new Metadata();
        }
    }
    
    public void setFileAnnotation(File file, Metadata metadata){
        annotatedFiles.put(file.getAbsolutePath(), metadata);
    }
    
    private void processFileAnnotations(File file){
        Metadata metadata = getFileAnnotation(file);
        if(metadata.size() < 1){return;} //Nothing to do here.
        Element dcDescription = new Element("description", DC_XML);
        for(Property property: DC_ELEMENTS){
            for(String value: metadata.getValues(property)){
                
                Element dcStatement = new Element(property.getName().substring(property.getName().indexOf(":")+1), DC_NAMESPACE);
                dcStatement.addContent(value);
                dcDescription.addContent(dcStatement);
            }
        }
        currentElement.addContent(dcDescription);
    }
}