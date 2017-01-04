package org.dataaccessioner.fits.tools;

import edu.harvard.hul.ois.fits.Fits;
import edu.harvard.hul.ois.fits.exceptions.FitsToolException;
import edu.harvard.hul.ois.fits.tools.ToolBase;
import edu.harvard.hul.ois.fits.tools.ToolInfo;
import edu.harvard.hul.ois.fits.tools.ToolOutput;
import edu.harvard.hul.ois.fits.tools.droid.DroidQuery;
import org.apache.log4j.Logger;
import org.jdom.input.SAXBuilder;
import uk.gov.nationalarchives.droid.command.action.VersionCommand;
import uk.gov.nationalarchives.droid.core.BinarySignatureIdentifier;
import uk.gov.nationalarchives.droid.core.SignatureFileParser;
import uk.gov.nationalarchives.droid.core.SignatureParseException;
import uk.gov.nationalarchives.droid.core.interfaces.IdentificationResultCollection;

import java.io.*;

/**
 * Created by prater on 1/3/2017.
 */
public class DADroid extends ToolBase {

    private boolean enabled = true;
    private DroidQuery droidQuery;
    private static final Logger logger = Logger.getLogger(edu.harvard.hul.ois.fits.tools.droid.Droid.class);
    private static String sigFileVersion;
    private static BinarySignatureIdentifier sigIdentifier = new BinarySignatureIdentifier();
    private static SignatureFileParser sigFileParser = new SignatureFileParser();

    public DADroid() throws FitsToolException {
        logger.debug("Initializing Droid");
        info = new ToolInfo("Droid", getDroidVersion(), null);

        try {
            String droid_conf = Fits.FITS_TOOLS_DIR + "droid" + File.separator;
            File sigFile = new File(droid_conf + Fits.config.getString("droid_sigfile"));
            try {
                droidQuery = new DroidQuery(sigFile);
                if (sigFileVersion == null) {
                    setSigFileVersion(sigFile);
                }
            } catch (SignatureParseException e) {
                throw new FitsToolException("Problem with DROID signature file");
            }
        } catch (Exception e) {
            throw new FitsToolException("Error initilizing DROID", e);
        }
    }

    @Override
    public ToolOutput extractInfo(File file) throws FitsToolException {
        logger.debug("Droid.extractInfo starting on " + file.getName());
        long startTime = System.currentTimeMillis();
        IdentificationResultCollection results;
        try {
            results = droidQuery.queryFile(file);
        } catch (IOException e) {
            throw new FitsToolException("DROID can't query file " + file.getAbsolutePath(),
                    e);
        }
        DADroidToolOutputter outputter = new DADroidToolOutputter(this, results);
        ToolOutput output = outputter.toToolOutput();

        duration = System.currentTimeMillis() - startTime;
        runStatus = RunStatus.SUCCESSFUL;
        logger.debug("Droid.extractInfo finished on " + file.getName());
        return output;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean value) {
        enabled = value;
    }

    /**
     * Make the SAXBuilder available to helper class
     */
    protected SAXBuilder getSaxBuilder() {
        return saxBuilder;
    }

    /* Get the version of DROID. This is about the cleanest I can manage. */
    private String getDroidVersion() {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        VersionCommand vcmd = new VersionCommand(pw);
        try {
            vcmd.execute();
        } catch (Exception e) {
            return "(Version unknown)";
        }
        return sw.toString().trim();
    }

    private static String getSigFileVersion() {
        return sigFileVersion;
    }

    /* Read the sigFile to get its version */
    private void setSigFileVersion(File sigFile) throws SignatureParseException, FileNotFoundException {
        if (!sigFile.exists()) {
            throw new FileNotFoundException("Signature file " + sigFile.getAbsolutePath() + " not found");
        }
    }
}
