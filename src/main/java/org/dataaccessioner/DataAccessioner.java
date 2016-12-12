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
import edu.harvard.hul.ois.fits.exceptions.FitsException;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionGroup;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.openide.util.Exceptions;

/**
 *
 * @author sshaw6
 */
public class DataAccessioner {

    private static final String NAME = "DataAccessioner";
    private static final String VERSION = "1.0";
    private static Logger logger;

    private Fits fits;
    private MetadataManager metadataManager;
    private Migrator migrator = new Migrator();

    public DataAccessioner() {
        System.setProperty("log4j.configuration", Fits.FITS_TOOLS + "log4j.properties");
        logger = Logger.getLogger(this.getClass());
        BasicConfigurator.configure();
        logger.setLevel(Level.INFO);

        //May eventually setup some other configuration stuff here.
        //FITS is not initialized here because it takes some time to start
        //and the user may opt not to use it.
    }

    /**
     * @param args the command line arguments
     * @throws org.apache.commons.cli.ParseException
     */
    public static void main(String[] args) throws ParseException {
        DataAccessioner da = new DataAccessioner();
        //Default settings
        String collectionName = "";
        String accessionNumber = "";

        Options options = new Options();
        options.addOption("c", true, "Collection Name");
        options.addOption("a", true, "Accession Number");
        options.addOption("h", false, "print this message");
        options.addOption("v", false, "print version information");
        options.addOption("u", false, "Do not start GUI; requires a source and destination");

        OptionGroup fitsOptions = new OptionGroup();
        fitsOptions.addOption(new Option("s", false, "Run FITS on source"));
        fitsOptions.addOption(new Option("x", false, "Don't run FITS; only copy"));
        options.addOptionGroup(fitsOptions);

        CommandLineParser parser = new GnuParser();
        CommandLine cmd = parser.parse(options, args);

        if (cmd.hasOption("h")) {
            printHelp(options);
            System.exit(0);
        }
        if (cmd.hasOption("v")) {
            System.out.println(DataAccessioner.VERSION);
            System.exit(0);
        }
        if (cmd.hasOption("c")) {
            collectionName = cmd.getOptionValue("c");
        }
        if (cmd.hasOption("a")) {
            accessionNumber = cmd.getOptionValue("a");
        }

        try {
            if (cmd.hasOption("x")) {
                da.fits = null;
            } else {
                logger.info("Starting FITS");
                da.fits = new Fits();
            }
        } catch (FitsException ex) {
            System.err.println("FITS failed to initialize.");
            Exceptions.printStackTrace(ex);
        }
        //Get the destination & source
        File destination = null;
        List<File> sources = new ArrayList<File>();
        if (!cmd.getArgList().isEmpty()) {
            destination = new File((String) cmd.getArgList().remove(0));

            //validate sources or reject them
            for (Object sourceObj : cmd.getArgList()) {
                File source = new File(sourceObj.toString());
                if (source.canRead()) {
                    sources.add(source);
                }
            }
        }

        if (cmd.hasOption("u")) {//Unattended
            if (collectionName.isEmpty() || accessionNumber.isEmpty()) {
                System.err.println("Both a collection name and an accession number must be provided if not using the GUI.");
                printHelp(options);
            } else if (destination == null || !(destination.isDirectory() && destination.canWrite())) {
                String destinationStr = "<blank>";
                if (destination != null) {
                    destinationStr = destination.toString();
                }
                System.err.println("Cannot run automatically. The destination (" + destinationStr + ") is either not a valid or writable directory.");
                printHelp(options);
            } else if (sources.isEmpty()) {
                System.err.println("Cannot run automatically. At least one valid source is required.");
                printHelp(options);
            } else {
                da.runUnattended(destination, sources, accessionNumber, collectionName);
            }

        } else { //Start GUI
            try {
                // Set cross-platform Java L&F (also called "Metal")
                UIManager.setLookAndFeel(
                        UIManager.getSystemLookAndFeelClassName());
            } catch (UnsupportedLookAndFeelException e) {
                // handle exception
            } catch (ClassNotFoundException e) {
                // handle exception
            } catch (InstantiationException e) {
                // handle exception
            } catch (IllegalAccessException e) {
                // handle exception
            }
            DASwingView view = new DASwingView(da);
            view.pack();
            view.setVisible(true);
        }

    }

    void run(File source, File destination, MetadataManager mm, Set<File> excludedFiles) throws Exception {
        mm.open();
        migrator.setMetadataManager(mm);
        migrator.setFits(fits);
        for (File excluded : excludedFiles) {
            migrator.addExclusion(excluded);
        }
        int status = migrator.run(source, destination);
        switch(status){
            case Migrator.STATUS_CANCELED:
            case Migrator.STATUS_FAILURE: throw new Exception(migrator.getStatusMessage());
        }
    }

    private void runUnattended(File destination, List<File> sources, String accessionNumber, String collectionName) {

        try {
            File accnDir = new File(destination, accessionNumber);
            accnDir.mkdirs();
            File metadata = new File(accnDir, accessionNumber + ".xml");

            metadataManager = new MetadataManager(metadata, collectionName, accessionNumber);
            metadataManager.open();
            migrator.setMetadataManager(metadataManager);
            migrator.setFits(fits);
            System.out.println("Starting Migrator");
            for (File source : sources) {
                File sourceDestination = new File(accnDir, source.getName());
                sourceDestination.mkdirs();
                switch (migrator.run(source, sourceDestination)) {
                    case Migrator.STATUS_FAILURE:
                        System.err.println("Migration failed! "
                                + migrator.getStatusMessage());
                        break;
                    case Migrator.STATUS_SUCCESS:
                        System.out.println("Migration completed successfully!");
                }

                for (String warning : migrator.getWarnings()) {
                    logger.warn(warning);
                }
            }
        } catch (Exception ex) {
            logger.error(ex.getMessage());
            Exceptions.printStackTrace(ex);
        }
    }

    private static void printHelp(Options opts) {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("dataAccessioner [options] [destination] [source] [additional sources...]", opts);
    }

    public void startFits() {
        if (fits == null) {
            try {
                fits = new Fits();
            } catch (FitsException ex) {
                logger.warn(ex.getMessage());
            }
        }
    }

    public Fits getFits() {
        return fits;
    }

    public Migrator getMigrator() {
        return migrator;
    }

    String getName() {
        return NAME;
    }

    String getVersion() {
        return VERSION;
    }
}
