package edu.duke.archives.migrator.plugins.managers;

<<<<<<< HEAD:plugins/duke_premis/src/edu/duke/archives/migrator/plugins/managers/PremisManager.java
import edu.duke.archives.DataAccessioner;
import edu.duke.archives.DefaultMetadataManager;
import edu.duke.archives.metadata.FileWrapper;
import java.io.StringReader;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Map;
=======
import edu.duke.archives.DataMigrator;
import edu.duke.archives.interfaces.MetadataManager;
import edu.duke.archives.metadata.Metadata;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringReader;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
>>>>>>> origin/0.3:plugins/da_premis2/src/edu/duke/archives/migrator/plugins/managers/PremisManager.java
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.Namespace;
import org.jdom.input.SAXBuilder;

/**
 *
 * @author Seth Shaw
 */
public class PremisManager extends DefaultMetadataManager {

<<<<<<< HEAD:plugins/duke_premis/src/edu/duke/archives/migrator/plugins/managers/PremisManager.java
    Namespace p1;

    @Override
    public void startFile(FileWrapper metadata) {
        super.startFile(metadata);
        // Start Premis
        Element object = new Element("object", p1);
        currentElement.addContent(object);
        Element name = new Element("originalName", p1);
        object.addContent(name);
        name.setText(metadata.getNewName());
        Element significantProperties = new Element("significantProperties", p1);
        object.addContent(significantProperties);
        Element dateLastModified = new Element("dateLastModified", p1);
        significantProperties.addContent(dateLastModified);
        dateLastModified.setAttribute("source", "DataAccessioner");
        dateLastModified.setText(new Timestamp(metadata.lastModified()).toString());
        if (metadata.isFile()) {
            Map<String, String> checksums = metadata.getChecksums();
            for (String key : checksums.keySet()) {
                String value = (String) checksums.get(key);
                if ((value != null) && (desiredChecksums.contains(key))) {
                    Element fixity = new Element("fixity", p1);
                    object.addContent(fixity);
                    fixity.setAttribute("source", "DataAccessioner");
                    Element messageDigestAlgorithm =
                            new Element("messageDigestAlgorithm", p1);
                    fixity.addContent(messageDigestAlgorithm);
                    messageDigestAlgorithm.setText(key);
                    Element messageDigest = new Element("messageDigest", p1);
                    fixity.addContent(messageDigest);
                    messageDigest.setText(value);
                    Element messageDigestOriginator =
                            new Element("messageDigestOriginator", p1);
                    fixity.addContent(messageDigestOriginator);
                    messageDigestOriginator.setText("DataAccessioner");
                }
            }

=======
    private static File xmlFile;
    private static Document document;
    private static Element accessionElement;
    private static Element currentElement;
    private static boolean isRunning = false;
    
    private static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS"); 
    private static final Namespace DEFAULT_NAMESPACE = Namespace.getNamespace("http://dataaccessioner.org/schema/dda-0-3-1");
    private static final Namespace p2 = Namespace.getNamespace("premis",
            "info:lc/xmlns/premis-v2");
    private static final Namespace xsi = Namespace.getNamespace("xsi", 
            "http://www.w3.org/2001/XMLSchema-instance");

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

        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    private static Document startDoc(String collectionName,
            String accessionNumber) {
        System.out.println("\tCreating the document");
        Element collection = new Element("collection", DEFAULT_NAMESPACE);
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

    public void recordFile(Metadata file) {
        startFile(file);
        endFile();
    }

    public void recordFile(String path) {
        startFile(path);
        endFile();
    }

    public void startFile(Metadata metadata) {
        //Create the file element and add non-premis attributes
        Element file = new Element("file", DEFAULT_NAMESPACE);
        currentElement.addContent(file);
        currentElement = file;
        
        //Set the file name attribute
        file.setAttribute("name", metadata.getNewName());

        //Set file element attribute last_modified
        String last_modified = DATE_FORMAT.format(new Date(metadata.lastModified())); 
        file.setAttribute("last_modified", last_modified);

        //Set hidden attribute
        if (metadata.isHidden() == true) {
            file.setAttribute("hidden", "true");
        }
        
        // Start Premis
        Element premisObject = new Element("object", p2);
        premisObject.setAttribute("type", p2.getPrefix()+":file", xsi);
        file.addContent(premisObject);
        
        //Add UUID premis identifier
        Element objID = new Element("objectIdentifier", p2);
        Element objIDType = new Element("objectIdentifierType", p2);
        objIDType.addContent("uuid");
        Element objIDValue = new Element("objectIdentifierValue", p2);
        objIDValue.addContent(UUID.randomUUID().toString());
        objID.addContent(objIDType);
        objID.addContent(objIDValue);
        premisObject.addContent(objID);
        
        //Note: Most PREMIS metadata goes in an objectCharacteristics element
        Element premisObjCharacteristics = new Element("objectCharacteristics",p2);
        premisObject.addContent(premisObjCharacteristics);
        
        //Add PREMIS originalName AFTER objectCharacteristics
        premisObject.addContent(
                new Element("originalName", p2).setText(metadata.getName()));
        
        //PREMIS composition level
        premisObjCharacteristics.addContent(
                new Element("compositionLevel", p2).addContent("0"));
        
        if (metadata.isFile()) {
            //Fixity
            if ((metadata.getMD5() != null) && !(metadata.getMD5().equals(""))) {
                file.setAttribute("MD5", metadata.getMD5());
                
                //PREMIS fixity
                Element fixity = new Element("fixity", p2);
                premisObjCharacteristics.addContent(fixity);
                Element messageDigestAlgorithm =
                        new Element("messageDigestAlgorithm", p2);
                fixity.addContent(messageDigestAlgorithm);
                messageDigestAlgorithm.setText("MD5");
                Element messageDigest = new Element("messageDigest", p2);
                fixity.addContent(messageDigest);
                messageDigest.setText(metadata.getMD5());
                Element messageDigestOriginator =
                        new Element("messageDigestOriginator", p2);
                fixity.addContent(messageDigestOriginator);
                messageDigestOriginator.setText("DataAccessioner");
            }
            //Size
            file.setAttribute("size", String.valueOf(metadata.length()));
            premisObjCharacteristics.addContent(
                    new Element("size",p2).addContent(
                            String.valueOf(metadata.length())));
        }
        addQM(metadata.getQualifiedMetadata());
    }

    public void startFile(String path) {
        startFile(new Metadata(path));
    }

    public void endFile() {
        try{
            //Ensure PREMIS output has at least one format designation
            Element premisObjCharacteristics = currentElement
                    .getChild("object", p2)
                    .getChild("objectCharacteristics", p2);
            if(premisObjCharacteristics.getChildren("format", p2).isEmpty()){
                premisObjCharacteristics
                        .addContent(new Element("format", p2)
                                .addContent(new Element("formatDesignation",p2)
                                        .addContent(new Element("formatName",p2)
                                                .setText("bytestream"))));
            }
        } catch (Exception ex) {
            System.err.println("Trying to close PREMIS object record for \""
                    +currentElement.getAttributeValue("name")+"\": "
                    + ex.getMessage());
            ex.printStackTrace();
        }
        //Does the same thing
        endDirectory();
    }

    public void startDirectory(Metadata directory) {
        Element newDir = new Element("folder", DEFAULT_NAMESPACE);
        newDir.setAttribute("name", directory.getNewName());
        String last_modified = DATE_FORMAT.format(new Date(directory.lastModified())); 
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
        startDirectory(new Metadata(path));
    }

    public void endDirectory() {
        if (!currentElement.isRootElement()) {
            currentElement = currentElement.getParentElement();
>>>>>>> origin/0.3:plugins/da_premis2/src/edu/duke/archives/migrator/plugins/managers/PremisManager.java
        }
        addQM(metadata.getQualifiedMetadata());
    }

    @Override
    public void addXML(String xml) {
        if (xml.equalsIgnoreCase("")) {
            return;
        }
        try {
            Document xmlDoc =
                    new SAXBuilder().build(new StringReader(xml));
            Element root = xmlDoc.detachRootElement();
            if (currentElement.getName().equalsIgnoreCase("file")) {
                if (currentElement.getChild("object", p2) == null) {
                    currentElement.addContent(
                            new Element("object", p2).addContent(
                                    new Element("objectCharacteristics",p2)));
                }
                try{
                convert2premis(root, 
                        currentElement.getChild("object", p2)
                                .getChild("objectCharacteristics", p2));
                } catch (Exception ex) {
                    System.err.println("Could translate plugin xml to PREMIS v2: "
                            + ex.getMessage());
                    System.err.println(xml);
                    ex.printStackTrace();
                }
            } else {
                currentElement.addContent(root);
            }
        } catch (Exception ex) {
            System.err.println("Could not convert string into xml: " +
                    ex.getMessage());
            System.err.println(xml);
            ex.printStackTrace();
        }
    }

<<<<<<< HEAD:plugins/duke_premis/src/edu/duke/archives/migrator/plugins/managers/PremisManager.java
    @Override
=======
    public boolean implementsMetadataManager() {
        return true;
    }

    private void addQM(List<Element> qms) {
        if (qms == null || qms.isEmpty()) //Nothing to give
        {
            return;
        }
        for (Element qm : qms) {
            currentElement.addContent(qm);
        }
    }

    public void addNote(String note) {
        addXML("note>" + note + "</note>");
    }

>>>>>>> origin/0.3:plugins/da_premis2/src/edu/duke/archives/migrator/plugins/managers/PremisManager.java
    public String getName() {
        return "DA Premis 2.2";
    }

<<<<<<< HEAD:plugins/duke_premis/src/edu/duke/archives/migrator/plugins/managers/PremisManager.java
    @Override
    public void init(DataAccessioner migrator) throws Exception {
        super.init(migrator);
        desiredChecksums = migrator.config.getList("DefaultMetadataManager.checksums", Arrays.asList(new String[]{"MD5","SHA-256"}));
        p1 = Namespace.getNamespace("p1",
                "http://www.loc.gov/standards/premis/v1/");
=======
    public void init() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void init(DataMigrator migrator) throws Exception {
        String collection = "";
        String accessionNo = "no_accession";
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
                accessionNo = qm.getValue();
                sqmI.remove();
            }
        }
        
        File destination_xml = new File(migrator.getDestination(), accessionNo +
                ".xml");
        init(destination_xml.getAbsolutePath(), collection, accessionNo);
>>>>>>> origin/0.3:plugins/da_premis2/src/edu/duke/archives/migrator/plugins/managers/PremisManager.java
    }

    private void convert2premis(Element root, Element premisObjCharacteristics) {
        //Try Jhove, but not if the designation is the generic bytestream
        if (root.getName().equalsIgnoreCase("jhove")) {
            Namespace jhoveNS = root.getNamespace();
            
            if (root.getChild("repInfo",jhoveNS).getChild("format", jhoveNS).getText().equalsIgnoreCase("bytestream")) {
                System.out.println("Skipping Jhove default \"bytestream\" format identification.");
                return;
            }
            Element premisFormat = new Element("format", p2);
            premisObjCharacteristics.addContent(premisFormat);

            Element jhoveRepInfo = root.getChild("repInfo", jhoveNS);
            if (jhoveRepInfo == null) {
                return;
            }
            Element formatDesignation = new Element("formatDesignation",
                    p2);
            premisFormat.addContent(formatDesignation);
//            formatDesignation.setAttribute("source", "JHOVE");

            Element format = jhoveRepInfo.getChild("format", jhoveNS);
            if (format != null) {
                formatDesignation.addContent(new Element("formatName",
                        p2).setText(format.getTextNormalize()));
            }
            Element version = jhoveRepInfo.getChild("version", jhoveNS);
            if (version != null) {
                formatDesignation.addContent(new Element("formatVersion",
                        p2).setText(version.getTextNormalize()));
            }
            Element status = jhoveRepInfo.getChild("status", jhoveNS);
            if (status != null) {
                premisFormat.addContent(new Element("formatNote",
                        p2).setText("Format status is: "+status.getTextNormalize()));
            }
            
            Element reportingModule = jhoveRepInfo.getChild("reportingModule", jhoveNS);
            if (reportingModule != null){
                String note = "Jhove Reporting Module: "+reportingModule.getTextNormalize();
                String release = reportingModule.getAttributeValue("release", jhoveNS);
                if(release != null && release.length()>0){
                    note += ", release "+release;
                }
                premisFormat.addContent(new Element("formatNote",p2).setText(note));
            }
            
        } //Try Droid
        else if (root.getName().equalsIgnoreCase("FileCollection")) {
            Namespace droidNS = root.getNamespace();
            //Setup PREMIS formatNote for each object
            String droidVersion = root.getChild("DROIDVersion",
                    droidNS).
                    getTextNormalize();
            String sigFileVersion = root.getChild("SignatureFileVersion",
                    droidNS).
                    getTextNormalize();
            String note = "DROID Version: V" + droidVersion 
                        + ". Signature File Version: " + sigFileVersion;
            //Multiple format hits possible
            for (Object fileFormatHit : root.getChild("IdentificationFile",
                    droidNS).getChildren("FileFormatHit", droidNS)) {
                //New Format element for each hit
                Element premisFormat = new Element("format", p2);
                premisObjCharacteristics.addContent(premisFormat);
                
                //Format Designation
                Element formatDesignation = new Element("formatDesignation", p2);
                premisFormat.addContent(formatDesignation);

                Element formatName =
                        ((Element) fileFormatHit).getChild("Name",
                        droidNS);
                if (formatName != null) {
                    formatDesignation.addContent(new Element("formatName",
                            p2).setText(formatName.getTextNormalize()));
                }
                Element formatVersion =
                        ((Element) fileFormatHit).getChild("Version",
                        droidNS);
                if (formatVersion != null) {
                    formatDesignation.addContent(new Element("formatVersion",
                            p2).setText(formatVersion.getTextNormalize()));
                }
                
//                Element mimeType =
//                        ((Element) fileFormatHit).getChild("MimeType",
//                        droidNS);
//                if (mimeType != null) {
//                    formatDesignation.addContent(new Element("formatType",
//                            p2).setText(mimeType.getTextNormalize()));
//                }
                //Format Registry
                Element droidPUID = ((Element) fileFormatHit).getChild("PUID", droidNS);
                if (droidPUID != null) {
                    Element formatRegistry = new Element("formatRegistry", p2);
                    premisFormat.addContent(formatRegistry);
                    formatRegistry.addContent(new Element("formatRegistryName", p2).setText("http://www.nationalarchives.gov.uk/pronom"));
                    formatRegistry.addContent(new Element("formatRegistryKey",p2).setText(droidPUID.getTextNormalize()));
                }

                //Format Notes
                Element status =
                        ((Element) fileFormatHit).getChild("Status",
                        droidNS);
                if (status != null) {
<<<<<<< HEAD:plugins/duke_premis/src/edu/duke/archives/migrator/plugins/managers/PremisManager.java
                    formatDesignation.addContent(new Element("formatRecognitionStatus",
                            p1).setText(status.getTextNormalize()));
                }
                Element mimeType =
                        ((Element) fileFormatHit).getChild("MimeType",
                        droidNS);
                if (mimeType != null) {
                    String mime = mimeType.getTextNormalize();
                    if (mime.trim().length() != 0) {
                        formatDesignation.addContent(new Element("formatType",
                                p1).setText(mimeType.getTextNormalize()));
                    }
=======
                    premisFormat.addContent(new Element("formatNote",
                            p2).setText(status.getTextNormalize()));
>>>>>>> origin/0.3:plugins/da_premis2/src/edu/duke/archives/migrator/plugins/managers/PremisManager.java
                }
                premisFormat.addContent(new Element("formatNote", p2).setText(note));
            }
        }
    }
}
