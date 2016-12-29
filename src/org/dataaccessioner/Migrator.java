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

import com.twmacinta.util.MD5;
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
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
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
    private int optionIDWhich = OPTION_ID_DESTINATION;

    private Fits fits = null;
    private MetadataManager metadataManager = null;
    private HashSet excludedItems = new HashSet(); //Hash key is the file's absolute path

    public Migrator() {
        
    }

    public void setFits(Fits fits) {
        this.fits = fits;
    }

    public void setMetadataManager(MetadataManager metadataManager) {
        this.metadataManager = metadataManager;
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
        if (metadataManager == null){
            //Possibly create a sys.out migrator to use by default?
            warnings.add("Failed to start migrator. No metadata manager set!");
            return STATUS_FAILURE;
        }
        try {
            status = STATUS_RUNNING;
            status = processDirectory(source, destination);
            metadataManager.close();
        } catch (FileNotFoundException ex) {
            setStatusMessage(ex.getLocalizedMessage());
            warnings.add(statusMessage);
            return STATUS_FAILURE;
        } catch (Exception ex) {
            setStatusMessage("Unexpected Failure: " + ex.getLocalizedMessage());
            ex.printStackTrace();
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

    private int processDirectory(File source, File destination) throws FileNotFoundException {
        statusMessage = "Migrating " + source.getPath();
        validateDirectory(source);
        validateDirectory(destination);
        metadataManager.startDirectory(source, destination.getName());
        for (File child : source.listFiles()) {
            if(isExcluded(child)){ // Ensure it shouldn't be skipped. Considering a redundant check at the beginning of both "processing" methods.
                continue;
            }
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
                    switch (processDirectory(child, childDestination)) {
                        case STATUS_FAILURE:
                            return STATUS_FAILURE;
                        case STATUS_CANCELED:
                            return STATUS_CANCELED;
                    }
                }
            } else if (child.isFile()) {
                switch (processFile(child, new File(destination, child.getName()))) {
                    case STATUS_FAILURE:
                        if (optionFailOnPartial) {
                            return STATUS_FAILURE;
                        } else {
                            warnings.add("Failed to process file" + child.getPath());
                        }
                    case STATUS_CANCELED:
                        return STATUS_CANCELED;
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
        metadataManager.closeCurrentElement();
        return STATUS_RUNNING;
    }

    private int processFile(File source, File destination) {
        metadataManager.startFile(source, destination.getName());
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
                //Generate & record Hash
                MD5 inMD5 = new MD5();
                InputStream is = new FileInputStream(source);
                OutputStream os = new FileOutputStream(destination);
                byte[] buffer = new byte[8192];
                int read = 0;

                setStatusMessage("Checksumming & copying: " + source.getName());
                try {
                    while ((read = is.read(buffer)) > -1) {
                        inMD5.Update(buffer, 0, read);
                        os.write(buffer, 0, read);
                    }
                    is.close();
                    os.close();

                    setStatusMessage("Checking migrated checksum: " + source.getName());
                    MD5 destMD5 = new MD5();
                    is = new FileInputStream(destination);
                    while ((read = is.read(buffer)) > -1) {
                        destMD5.Update(buffer, 0, read);
                    }

                    if (!inMD5.asHex().equalsIgnoreCase(destMD5.asHex())) {
                        String message = "Transfer of " + source.getPath() + " to "
                                + "" + destination.getPath()
                                + " failed, the copied file MD5 did not match: "
                                + inMD5.asHex() + " != " + destMD5.asHex();
                        warnings.add(message);
                        if (optionFailOnPartial) {
                            return STATUS_FAILURE;
                        }
                    } else {
                        metadataManager.addAttribute("MD5", inMD5.asHex());
                    }

                } catch (IOException e) {
                    throw new RuntimeException("Unable to process source file "
                            + source.getName(), e);
                } finally {
                    try {
                        is.close();
                        os.close();
                    } catch (IOException e) {
                        throw new RuntimeException("Unable to close input stream.",
                                e);
                    }
                }
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
        File toProcess = null;
        switch (optionIDWhich) {
            case OPTION_ID_DESTINATION:
                toProcess = destination;
                break;
            case OPTION_ID_SOURCE:
                toProcess = source;
                break;
            case OPTION_ID_NONE:
                toProcess = null;
        }
        if (toProcess != null && fits != null) {
            try {
                statusMessage = "Running FITS on " + toProcess.getName();

                FitsOutput fout = fits.examine(toProcess);
                System.out.println(statusMessage);
                metadataManager.addDocumentXSLT(fout.getFitsXml());
            } catch (FitsException ex) {
                Exceptions.printStackTrace(ex);
                String message = "Failed to run FITS on " + toProcess.getPath();
                if (optionFailOnPartial) {
                    statusMessage = message;
                    return STATUS_FAILURE;
                } else {
                    warnings.add(message);
                }
            }
        }
        metadataManager.closeCurrentElement();
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
    
    /**
     * Checks if a file in question has been marked to be skipped
     * during migration.
     * 
     * @param file
     * @return if the file should be skipped.
     */
    public boolean isExcluded(File file){
        return excludedItems.contains(file.getAbsolutePath());
    }
    
    /**
     * Mark a file to be excluded during migration.
     * 
     * @param file the file to be skipped.
     */
    public void addExclusion(File file){
        excludedItems.add(file.getAbsolutePath());
    }
    
    
}
