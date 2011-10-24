package edu.duke.archives.migrator.plugins.managers;

import edu.duke.archives.DataAccessioner;
import edu.duke.archives.DefaultMetadataManager;
import edu.duke.archives.metadata.FileWrapper;
import java.io.StringReader;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Map;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.Namespace;
import org.jdom.input.SAXBuilder;

/**
 *
 * @author Seth Shaw
 */
public class PremisManager extends DefaultMetadataManager {

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
                Element premis = currentElement.getChild("object", p1);
                if (premis == null) {
                    premis = new Element("object", p1);
                    currentElement.addContent(premis);
                }
                convert2premis(root, premis);
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

    @Override
    public String getName() {
        return "Duke Premis";
    }

    @Override
    public void init(DataAccessioner migrator) throws Exception {
        super.init(migrator);
        desiredChecksums = migrator.config.getList("DefaultMetadataManager.checksums", Arrays.asList(new String[]{"MD5","SHA-256"}));
        p1 = Namespace.getNamespace("p1",
                "http://www.loc.gov/standards/premis/v1/");
    }

    private void convert2premis(Element root, Element premis) {
        //Try Jhove
        if (root.getName().equalsIgnoreCase("jhove")) {
            Element premisFormat = premis.getChild("format", p1);
            if (premisFormat == null) {
                premisFormat = new Element("format", p1);
                premis.addContent(premisFormat);
            }
            Namespace jhoveNS = root.getNamespace();
            Element jhoveRepInfo = root.getChild("repInfo", jhoveNS);
            if (jhoveRepInfo == null) {
                return;
            }
            Element formatDesignation = new Element("formatDesignation",
                    p1);
            premisFormat.addContent(formatDesignation);
            formatDesignation.setAttribute("source", "JHOVE");

            Element format = jhoveRepInfo.getChild("format", jhoveNS);
            if (format != null) {
                formatDesignation.addContent(new Element("formatName",
                        p1).setText(format.getTextNormalize()));
            }
            Element version = jhoveRepInfo.getChild("version", jhoveNS);
            if (version != null) {
                formatDesignation.addContent(new Element("formatVersion",
                        p1).setText(version.getTextNormalize()));
            }
            Element status = jhoveRepInfo.getChild("status", jhoveNS);
            if (status != null) {
                formatDesignation.addContent(new Element("formatRecognitionStatus",
                        p1).setText(status.getTextNormalize()));
            }
            Element mimeType = jhoveRepInfo.getChild("mimeType", jhoveNS);
            if (mimeType != null) {
                formatDesignation.addContent(new Element("formatType",
                        p1).setText(mimeType.getTextNormalize()));
            }
        } //Try Droid
        else if (root.getName().equalsIgnoreCase("FileCollection")) {
            Element premisFormat = premis.getChild("format", p1);
            if (premisFormat == null) {
                premisFormat = new Element("format", p1);
                premis.addContent(premisFormat);
            }
            Namespace droidNS = root.getNamespace();
            String droidVersion = root.getChild("DROIDVersion",
                    droidNS).
                    getTextNormalize();
            String sigFileVersion = root.getChild("SignatureFileVersion",
                    droidNS).
                    getTextNormalize();
            String note = "DROID Version: V" +
                    droidVersion + ". Signature File Version: " +
                    sigFileVersion;
            Element formatDesignation = null;
            for (Object fileFormatHit : root.getChild("IdentificationFile",
                    droidNS).getChildren("FileFormatHit", droidNS)) {
                formatDesignation = new Element("formatDesignation", p1);
                premisFormat.addContent(formatDesignation);
                formatDesignation.setAttribute("source", "DROID");

                formatDesignation.addContent(new Element("notes", p1).setText(note));

                Element format =
                        ((Element) fileFormatHit).getChild("Name",
                        droidNS);
                if (format != null) {
                    formatDesignation.addContent(new Element("formatName",
                            p1).setText(format.getTextNormalize()));
                }
                Element version =
                        ((Element) fileFormatHit).getChild("Version",
                        droidNS);
                if (version != null) {
                    formatDesignation.addContent(new Element("formatVersion",
                            p1).setText(version.getTextNormalize()));
                }
                Element status =
                        ((Element) fileFormatHit).getChild("Status",
                        droidNS);
                if (status != null) {
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
                }
            }
        }
    }
}
