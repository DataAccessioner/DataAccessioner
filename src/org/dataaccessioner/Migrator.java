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

import edu.harvard.hul.ois.fits.Fits;
import edu.harvard.hul.ois.fits.FitsOutput;
import edu.harvard.hul.ois.fits.exceptions.FitsException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import org.openide.util.Exceptions;

/**
 *
 * @author Seth Shaw
 */
public class Migrator {

    public final static int STATUS_FAILURE = -20;
    public final static int STATUS_CANCELED = -10;
    public final static int STATUS_INITIALIZING = 10;
    public final static int STATUS_RUNNING = 20;
    public final static int STATUS_SUCCESS = 30;

    public final static int OPTION_ID_SOURCE = 1001;
    public final static int OPTION_ID_DESTINATION = 1002;
    public final static int OPTION_ID_NONE = 1000;
    
    private static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");

    private String statusMessage = "";
    private int status = STATUS_INITIALIZING;
    private List<String> warnings = new ArrayList<String>();

    private boolean optionFailOnPartial = false;
    private boolean optionOverwriteExisting = false;
    private String digestAlgorithm = "MD5"; //Default MD5
    private int optionIDWhich = OPTION_ID_DESTINATION;    

    private Fits fits = null;
    
    public Migrator() throws FitsException {
        fits = new Fits();
    }

    public Migrator(Fits fits){
        this.fits = fits;
    }
    
    public String getDigestAlgorithm() {
        return digestAlgorithm;
    }

    public boolean setDigestAlgorithm(String digestAlgorithm) {
        try {
            MessageDigest.getInstance(digestAlgorithm);
        } catch (NoSuchAlgorithmException ex) {
            return false;
        }
        this.digestAlgorithm = digestAlgorithm;
        return true;
    }

    public boolean willOverwriteExisting() {
        return optionOverwriteExisting;
    }

    public void setOptionOverwriteExisting(boolean optionOverwriteExisting) {
        this.optionOverwriteExisting = optionOverwriteExisting;
    }
    
    public boolean willFailOnPartial() {
        return this.optionFailOnPartial;
    }
    
    public void setFailOnPartial(boolean optionFailOnPartial) {
        this.optionFailOnPartial = optionFailOnPartial;
    }

    public int getOptionIDWhich() {
        return optionIDWhich;
    }

    public boolean setIDWhich(int optionIDWhich) {
        if (Arrays.asList(
                OPTION_ID_DESTINATION, 
                OPTION_ID_SOURCE, 
                OPTION_ID_NONE).contains(optionIDWhich)) {
            this.optionIDWhich = optionIDWhich;
            return true;
        }
        return false;
    }
    
    

    public List<String> getWarnings() {
        return warnings;
    }

    public int getStatus() {
        return status;
    }

    
    public int run(File source, File destination) {
        status = STATUS_INITIALIZING;
        try {
            status = STATUS_RUNNING;
            status = copyDirectory(source, destination);
        } catch (FileNotFoundException ex) {
            setStatusMessage(ex.getLocalizedMessage());
            return STATUS_FAILURE;
        } catch (Exception ex) {
            setStatusMessage("Unexpected Failure: " + ex.getLocalizedMessage());
            return STATUS_FAILURE;
        }
        return status;
    }

    public String getStatusMessage() {
        return statusMessage;
    }

    private void setStatusMessage(String message) {
        if (message == null) {
            statusMessage = "";
        } else {
            statusMessage = message;
        }

    }

    private int copyDirectory(File source, File destination) throws FileNotFoundException {
        statusMessage = "Migrating " + source.getPath();
        validateDirectory(source);
        validateDirectory(destination);
        for (File child : source.listFiles()) {
            if (child.isDirectory()) {
                File childDestination = new File(destination, child.getName());
                if (!childDestination.mkdirs()) {
                    warnings.add("Skipping " + child.getAbsolutePath()
                            + " (Unable to create destination: "
                            + childDestination.getAbsolutePath() + ")");
                    if (optionFailOnPartial) {
                        return STATUS_FAILURE;
                    }
                } else {
                    switch (copyDirectory(child, childDestination)) {
                        case STATUS_FAILURE:
                            return STATUS_FAILURE;
                        case STATUS_CANCELED:
                            return STATUS_CANCELED;
                    }
                }
            } else if (child.isFile()) {
                switch (copyFile(child, new File(destination, child.getName()))) {
                    case STATUS_FAILURE:
                        if(optionFailOnPartial){
                            return STATUS_FAILURE;
                        } else {
                            warnings.add("Failed to securely copy "+child.getPath());
                        }
                    case STATUS_CANCELED:
                        return STATUS_CANCELED;
                }
                File toProcess = null;
                switch (optionIDWhich){
                    case OPTION_ID_DESTINATION:
                        toProcess = new File(destination, child.getName());
                        break;
                    case OPTION_ID_SOURCE:
                        toProcess = child;
                        break;
                    case OPTION_ID_NONE:
                        toProcess = null;
                }
                if(toProcess != null){
                    runFits(toProcess);
                }
            }
        }
        try {
            destination.setLastModified(source.lastModified());
        } catch (Exception e) {
            warnings.add("Unable to set Last Modified date for "
                    + destination.getAbsolutePath() + " to "
                    + DATE_FORMAT.format(new Date(source.lastModified())));
        }
        return STATUS_RUNNING;
    }

    private int copyFile(File source, File destination) {
        statusMessage = "Migrating " + source.getPath();
        if (!source.canRead()) {
            warnings.add("Unable to copy " + source.getAbsolutePath() + " (unreadable file).");
            if (optionFailOnPartial) {
                return STATUS_FAILURE;
            }
        } else if (!optionOverwriteExisting && destination.exists()) {
            warnings.add("Unable to copy " + source.getAbsolutePath() + " (destination file already exists).");
            if (optionFailOnPartial) {
                return STATUS_FAILURE;
            }
        } else {
            try {
                //TODO: Could fast-md5 be used instead?
                
                //Generate & record Hash
                /*
                 * MD5 code from R.J. Lorimer, "Getting MD5 Sums in Java",
                 * Javalobby.org (accessed 9 May 2008)
                 *
                 * My original approach of using
                 * org.apache.commons.codec.digest.DigestUtils.md5Hex() was
                 * choking on some large files leaving me with an OutOfMemoryError.
                 */
                MessageDigest digest = MessageDigest.getInstance(digestAlgorithm);
                InputStream is = new FileInputStream(source);
                OutputStream os = new FileOutputStream(destination);
                byte[] buffer = new byte[8192];
                int read = 0;
                String inDigest = "";
                setStatusMessage("Checksumming & copying: " + source.getName());
                try {
                    while ((read = is.read(buffer)) > -1) {
                        digest.update(buffer, 0, read);
                        os.write(buffer, 0, read);
                    }
                    byte[] checksumBytes = digest.digest();
                    inDigest = "";
                    for (int i = 0; i < checksumBytes.length; i++) {
                        inDigest += Integer.toString((checksumBytes[i] & 0xff) + 0x100, 16).
                                substring(1);
                    }
                    is.close();
                    os.close();

                    setStatusMessage("Checking migrated checksum: " + source.getName());
                    digest.reset();
                    is = new FileInputStream(destination);
                    String outDigest = "";
                    while ((read = is.read(buffer)) > -1) {
                        digest.update(buffer, 0, read);
                    }
                    checksumBytes = digest.digest();
                    outDigest = "";
                    for (int i = 0; i < checksumBytes.length; i++) {
                        outDigest += Integer.toString((checksumBytes[i] & 0xff) + 0x100, 16).
                                substring(1);
                    }

                    if (inDigest.compareTo(outDigest) != 0) {
                        String message = "Transfer of "+source.getPath()+" to "+destination.getPath()+" failed, the copied file checksum (" + digestAlgorithm
                                + ") did not match: " + inDigest + " != " + outDigest;
                        warnings.add(message);
                        if (optionFailOnPartial) {
                            return STATUS_FAILURE;
                        }
                    }

                } catch (IOException e) {
                    throw new RuntimeException("Unable to process source file "
                            + source.getName() + " for " + digestAlgorithm, e);
                } finally {
                    try {
                        is.close();
                        os.close();
                    } catch (IOException e) {
                        throw new RuntimeException("Unable to close input stream for " + digestAlgorithm + " calculation",
                                e);
                    }
                }
            } catch (NoSuchAlgorithmException ex) {
                warnings.add("Unable to create checksums with the "
                        + digestAlgorithm + " checksum algorithm.");
                return STATUS_FAILURE;
            } catch (FileNotFoundException ex) {
                warnings.add("Unable to copy " + source.getAbsolutePath() + "(unreadable file)");
                if (optionFailOnPartial) {
                    return STATUS_FAILURE;
                }
            }

            try {
                destination.setLastModified(source.lastModified());
            } catch (Exception e) {
                warnings.add("Unable to set Last Modified date for "
                        + destination.getAbsolutePath() + "to "
                        + DATE_FORMAT.format(new Date(source.lastModified())));
            }
        }
        return STATUS_RUNNING;
    }

    static private void validateDirectory(
            File aDirectory) throws FileNotFoundException {
        if (aDirectory == null) {
            throw new IllegalArgumentException("Directory should not be null.");
        }
        if (!aDirectory.exists()) {
            throw new FileNotFoundException("Directory does not exist: "
                    + aDirectory);
        }
        if (!aDirectory.isDirectory()) {
            throw new IllegalArgumentException("Is not a directory: "
                    + aDirectory);
        }
        if (!aDirectory.canRead()) {
            throw new IllegalArgumentException("Directory cannot be read: "
                    + aDirectory);
        }
    }

    private int runFits(File toProcess) {
        try {
            statusMessage = "Running FITS on "+toProcess.getPath();

            FitsOutput fout = fits.examine(toProcess);
            System.out.println(statusMessage);
            //Oddly, using fout.output(System.out) doesn't work on subsequent items. Only the first prints although the other methods do work.
            System.out.println(fout.getFitsXml().toString());
        } catch (Exception ex) {
            Exceptions.printStackTrace(ex);
            String message = "Failed to run FITS on "+toProcess.getPath();
            if(optionFailOnPartial){
                statusMessage = message;
                return STATUS_FAILURE;
            } else {
                warnings.add(message);
            }
        }
        
        return STATUS_RUNNING;
    }
}
