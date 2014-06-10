package edu.duke.archives;

import edu.duke.archives.interfaces.MetadataManager;
<<<<<<< HEAD
import edu.duke.archives.metadata.FileWrapper;
import edu.duke.archives.metadata.QualifiedMetadata;
=======
import edu.duke.archives.metadata.Metadata;
>>>>>>> origin/0.3
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringReader;
<<<<<<< HEAD
import java.sql.Timestamp;
import java.util.Arrays;
=======
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
>>>>>>> origin/0.3
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
<<<<<<< HEAD
public class DefaultMetadataManager implements MetadataManager {
=======
public class DefaultMetadataManager implements MetadataManager{
    private static File xmlFile;
    private static Document document;
    private static Element accessionElement;
    private static Element currentElement;
    private static final Namespace DEFAULT_NAMESPACE = Namespace.getNamespace("http://dataaccessioner.org/schema/dda-0-3-1");
    private static boolean isRunning = false;
    private static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
               
    public void init(String filePath,
            String collectionName,
            String accessionNumber) throws Exception {
        try {
            xmlFile = new File(filePath);
            xmlFile.getParentFile().mkdirs(); //In case it doesn't exist
            try {
                document = new SAXBuilder().build(xmlFile);
                currentElement = document.getRootElement()
                        .getChild("accession", DEFAULT_NAMESPACE);
            } catch (Exception e) {
                if (xmlFile.length() == 0) {
                    System.err.println("File was probably empty");
                } else {
                    System.err.println("Exisiting accession doc contains an " +
                            "error. Results will be saved to a supplementary " +
                            "accession record file.");
                    //Temp file functionality allows creating a unique name
                    xmlFile = File.createTempFile(accessionNumber + "_",
                            ".xml", xmlFile.getParentFile());
                }
                document = startDoc(collectionName, accessionNumber);
            }
            isRunning = true;
>>>>>>> origin/0.3

    File xmlFile;
    Document document;
    Element accessionElement;
    public Element currentElement;
    boolean isRunning = false;
    public List desiredChecksums;

<<<<<<< HEAD
    private Document startDoc(String collectionName, String accessionNumber) {
        Element collection = new Element("collection");
=======
    private static Document startDoc(String collectionName,
            String accessionNumber) {
        System.out.println("\tCreating the document");
        Element collection = new Element("collection", DEFAULT_NAMESPACE);
>>>>>>> origin/0.3
        collection.setAttribute("name", collectionName);
        document = new Document(collection);
        accessionElement = new Element("accession", DEFAULT_NAMESPACE);
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

<<<<<<< HEAD
    public void startFile(FileWrapper metadata) {
        Element file = new Element("file");
=======
    public void startFile(Metadata metadata) {
        Element file = new Element("file", DEFAULT_NAMESPACE);
>>>>>>> origin/0.3
        currentElement.addContent(file);
        currentElement = file;
        file.setAttribute("name", metadata.getNewName());

        String last_modified = DATE_FORMAT.format(new Date(metadata.lastModified()));
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

<<<<<<< HEAD
    public void startDirectory(FileWrapper directory) {
        Element newDir = new Element("folder");
        newDir.setAttribute("name", directory.getNewName());
        String last_modified = new Timestamp(directory.lastModified()).toString();
=======
    public void startDirectory(Metadata directory) {
        Element newDir = new Element("folder", DEFAULT_NAMESPACE);
        newDir.setAttribute("name", directory.getNewName());

        String last_modified = DATE_FORMAT.format(new Date(directory.lastModified()));
>>>>>>> origin/0.3
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

<<<<<<< HEAD
    public void addQM(List<QualifiedMetadata> qms) {
=======
    public boolean implementsMetadataManager() {
        return true;
    }

    private void addQM(List<Element> qms) {
>>>>>>> origin/0.3
        if (qms == null || qms.isEmpty()) //Nothing to give
        {
            return;
        }
        for (Element qm : qms) {
            currentElement.addContent(qm);
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
<<<<<<< HEAD
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
=======
        List<Element> sourceQM = migrator.getSource().getQualifiedMetadata();
        Iterator<Element> sqmI = sourceQM.iterator();
        while(sqmI.hasNext()){
            Element qm = sqmI.next();
            
            if(qm.getName().equalsIgnoreCase("title") 
                    && qm.getAttribute("qualifier") != null 
                    && qm.getAttributeValue("qualifier").equalsIgnoreCase("collection")){
                collection = qm.getValue();
                sqmI.remove();
            }
            else if(qm.getName().equalsIgnoreCase("identifier") 
                    && qm.getAttribute("qualifier") != null 
                    && qm.getAttributeValue("qualifier").equalsIgnoreCase("accession_no")){
>>>>>>> origin/0.3
                accessionNo = qm.getValue();
                sqmI.remove();
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
