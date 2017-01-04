package org.dataaccessioner.fits.tools;

import uk.gov.nationalarchives.droid.core.BinarySignatureIdentifier;
import uk.gov.nationalarchives.droid.core.SignatureParseException;
import uk.gov.nationalarchives.droid.core.interfaces.IdentificationResultCollection;
import uk.gov.nationalarchives.droid.core.interfaces.RequestIdentifier;
import uk.gov.nationalarchives.droid.core.interfaces.resource.FileSystemIdentificationRequest;
import uk.gov.nationalarchives.droid.core.interfaces.resource.RequestMetaData;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * Created by sprater on 1/4/17.
 */
public class DADroidQuery {

    private BinarySignatureIdentifier sigIdentifier = new BinarySignatureIdentifier();;

    /** Create a DroidQuery object. This can be retained for any number of
     *  different queries.
     *
     *  @param sigFile   File object for a Droid signature file
     *
     *   @throws SignatureParseException
     */
    public DADroidQuery (File sigFile)  throws SignatureParseException, FileNotFoundException {
        if (!sigFile.exists()) {
            throw new FileNotFoundException ("Signature file " + sigFile.getAbsolutePath() + " not found");
        }
        sigIdentifier.setSignatureFile (sigFile.getAbsolutePath());
        sigIdentifier.init ();
    }

    /** Query a file and get back an XML response. */
    public IdentificationResultCollection queryFile (File fil)
            throws IOException {
        RequestMetaData metadata = new RequestMetaData(fil.length(), fil.lastModified(), fil.getName());
        RequestIdentifier identifier = new RequestIdentifier (fil.toURI());
        FileInputStream in = null;
        FileSystemIdentificationRequest req = null;
        try {
            req = new FileSystemIdentificationRequest(metadata, identifier);
            in = new FileInputStream (fil);
            req.open(in);
            IdentificationResultCollection results;
            results = sigIdentifier.matchBinarySignatures(req);
            if (results.getResults().size() > 1) {
                sigIdentifier.removeLowerPriorityHits(results);
            }

            if(results.getResults().size() == 0) {
                results = sigIdentifier.matchExtensions(req,false);
            }
            if (results.getResults().size() > 1) {
                sigIdentifier.removeLowerPriorityHits(results);
            }

            //        List<IdentificationResult> resultsList = results.getResults();
            // This gives us an unfiltered list of matching signatures
            sigIdentifier.checkForExtensionsMismatches(results, req.getExtension());
            return results;
        }
        finally {
            if (req != null) {
                req.close ();
            }
            if (in != null) {
                in.close();
            }
        }
    }

}
