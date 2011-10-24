package edu.duke.archives;

import edu.duke.archives.interfaces.MetadataManager;
import edu.duke.archives.metadata.FileWrapper;
import edu.duke.archives.metadata.QualifiedMetadata;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringReader;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang.NullArgumentException;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.jdom.xpath.XPath;

/**
 *
 * @author Seth Shaw
 */
public class DefaultMetadataManager implements MetadataManager {

    File xmlFile;
    Document document;
    Element accessionElement;
    public Element currentElement;
    boolean isRunning = false;
    public List desiredChecksums;

    private Document startDoc(String collectionName, String accessionNumber) {
        Element collection = new Element("collection");
        collection.setAttribute("name", collectionName);
        document = new Document(collection);
        accessionElement = new Element("accession");
        accessionElement.setAttribute("number", accessionNumber);
        collection.addContent(accessionElement);
        currentElement = accessionElement;
        return document;
    }

    public void close() throws Exception {
        if (isRunning) {
            FileOutputStream stream = new FileOutputStream(xmlFile);
            OutputStreamWriter osw = new OutputStreamWriter(stream, "UTF-8");
            PrintWriter output = new PrintWriter(osw);
            Format format = Format.getRawFormat();
            XMLOutputter outputter = new XMLOutputter(format);
            outputter.output(document, output);

            output.close();
            osw.close();
            stream.close();
        }
        cancel();
    }

    public boolean isRunning() {
        return isRunning;
    }

    public void cancel() {
        isRunning = false;
        // Free up everything.
        xmlFile = null;
        accessionElement = null;
        currentElement = null;
        document.removeContent();
        document = null;
        System.gc();
    }

    public void recordFile(FileWrapper file) {
        startFile(file);
        endFile();
    }

    public void recordFile(String path) {
        startFile(path);
        endFile();
    }

    public void startFile(FileWrapper metadata) {
        Element file = new Element("file");
        currentElement.addContent(file);
        currentElement = file;
        file.setAttribute("name", metadata.getNewName());

        String last_modified = new Timestamp(metadata.lastModified()).toString();
        file.setAttribute("last_modified", last_modified);

        if (metadata.isHidden() == true) {
            file.setAttribute("hidden", "true");
        }
        if (metadata.isFile()) {
            file.setAttribute("size", String.valueOf(metadata.length()));

            //Checksums
            java.util.Map<String, String> checksums = metadata.getChecksums();
            for (String key : checksums.keySet()) {
                String value = (String) checksums.get(key);
                if ((value != null) && (desiredChecksums.contains(key))) {
                    file.setAttribute(key, value);
                }
            }
        }
        addQM(metadata.getQualifiedMetadata());
    }

    public void startFile(String path) {
        startFile(new FileWrapper(path));
    }

    public void endFile() {
        //Does the same as the same thing
        endDirectory();
    }

    public void startDirectory(FileWrapper directory) {
        Element newDir = new Element("folder");
        newDir.setAttribute("name", directory.getNewName());
        String last_modified = new Timestamp(directory.lastModified()).toString();
        newDir.setAttribute("last_modified", last_modified);
        if (directory.isHidden()) {
            newDir.setAttribute("hidden", "true");
        }
        if (currentElement == null) {//Should never happen, but check for sanity
            currentElement = accessionElement;
        }
        currentElement.addContent(newDir);
        currentElement = newDir;
        addQM(directory.getQualifiedMetadata());
    }

    public void startDirectory(String path) {
        startDirectory(new FileWrapper(path));
    }

    public void endDirectory() {
        if (!currentElement.isRootElement()) {
            currentElement = currentElement.getParentElement();
        }
    }

    public void addXML(String xml) {
        if (xml.matches("")) {
            return;
        }
        try {
            Document xmlDoc =
                    new SAXBuilder().build(new StringReader(xml));
            currentElement.addContent(xmlDoc.detachRootElement());
        } catch (Exception ex) {
            System.err.println("Could not convert string into xml: " +
                    ex.getMessage());
            System.err.println(xml);
            ex.printStackTrace();
        }
    }

    public void addQM(List<QualifiedMetadata> qms) {
        if (qms == null || qms.isEmpty()) //Nothing to give
        {
            return;
        }
        for (QualifiedMetadata qm : qms) {
            Element element = new Element(qm.getElement());
            if (qm.getQualifier() != null) {
                element.setAttribute("qualifier", qm.getQualifier());
            }
            element.addContent(qm.getValue());
            currentElement.addContent(element);
        }
    }

    public void addNote(String note) {
        addXML("<note>" + note + "</note>");
    }

    public String getName() {
        return "Default Manager";
    }

    public void init(DataAccessioner migrator) throws Exception {
        String collection = "";
        String accessionNo = "no_accession";
        List<QualifiedMetadata> sourceQM = migrator.getSource().
                getQualifiedMetadata();
        for (QualifiedMetadata qm : sourceQM) {
            if (qm.getElement().equalsIgnoreCase("title") && qm.getQualifier().
                    equalsIgnoreCase("collection")) {
                collection = qm.getValue();
                sourceQM.remove(qm);
            } else if (qm.getElement().equalsIgnoreCase("identifier") && qm.
                    getQualifier().
                    equalsIgnoreCase("accession_no")) {
                accessionNo = qm.getValue();
                sourceQM.remove(qm);
            }
        }
        
        desiredChecksums = migrator.config.getList("DefaultMetadataManager.checksums", Arrays.asList(new String[]{"MD5","CRC32","SHA-256"}));

        /*Setting these properties allows us to interpolate them into the
         writepath whether we use one set by the users or the default one.*/
        migrator.config.setProperty("DefaultMetadataManager.destination", migrator.getDestination().getAbsolutePath());
        migrator.config.setProperty("DefaultMetadataManager.accession.number", accessionNo);
        migrator.config.setProperty("DefaultMetadataManager.collection.name", collection);
        migrator.config.setProperty("DefaultMetadataManager.disk.name", migrator.getSource().getNewName());

        //Set destination for the metadata output file
        String path = migrator.config.getString("DefaultMetadataManager.writepath", "${DefaultMetadataManager.destination}"+File.separator+ "${DefaultMetadataManager.accession.number}" + ".xml");
        try {
            xmlFile = new File(path);
            xmlFile.getAbsoluteFile().getParentFile().mkdirs(); //In case it doesn't exist
            try {
                document = new SAXBuilder().build(xmlFile);
                accessionElement = (Element) (XPath.selectSingleNode(document,
                        "//collection/accession"));
                currentElement = accessionElement;
            } catch (Exception e) {
                if (xmlFile.length() != 0) {
                    System.err.println("Exisiting accession doc contains an " +
                            "error. Results will be saved to a supplementary " +
                            "accession record file.");
                    //Temp file functionality allows creating a unique name
                    xmlFile = File.createTempFile(accessionNo + "_",
                            ".xml", xmlFile.getParentFile());
                } // else the file was empty and we can still safely use it.
                document = startDoc(collection, accessionNo);
            }
            isRunning = true;

        }
        catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }
}
