/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.dataaccessioner;

import edu.harvard.hul.ois.fits.Fits;
import edu.harvard.hul.ois.fits.exceptions.FitsException;
import java.io.File;
import java.util.List;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionGroup;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.openide.util.Exceptions;

/**
 *
 * @author sshaw6
 */
public class DataAccessioner {

    private static final String VERSION = "1.0";
    private static Logger logger;
    
    private String collectionName = "";
    private String accessionNumber = "";
    private Fits fits;
    private MetadataManager metadataManager;
    private Migrator migrator;
    
    public DataAccessioner(){
        System.setProperty( "log4j.configuration", Fits.FITS_TOOLS + "log4j.properties" );
        logger = Logger.getLogger( this.getClass() );
        
        //May eventually setup some other configuration stuff here.
    }
    
    /**
     * @param args the command line arguments
     * @throws org.apache.commons.cli.ParseException
     */
    public static void main(String[] args) throws ParseException {
        DataAccessioner da = new DataAccessioner();
        
        Options options = new Options();
        options.addOption("c", true, "Collection Name");
        options.addOption("a", true, "Accession Number");
        options.addOption( "h", false, "print this message" );
        options.addOption( "v", false, "print version information" );
        options.addOption("p", false, "Do not start GUI; requires a source and destination");
        
        OptionGroup fitsOptions = new OptionGroup();
        fitsOptions.addOption(new Option("s", false, "Run FITS on source"));
        fitsOptions.addOption(new Option("x", false, "Don't run FITS; only copy"));
        options.addOptionGroup(fitsOptions);
        
        CommandLineParser parser = new GnuParser();
        CommandLine cmd = parser.parse( options, args );
        
        if (cmd.hasOption("h")) {
            printHelp(options);
            System.exit(0);
        }
        if (cmd.hasOption("v")) {
            System.out.println(DataAccessioner.VERSION);
            System.exit(0);
        }
        if(cmd.hasOption("c")){
            da.collectionName = cmd.getOptionValue("c");
        }
        if (cmd.hasOption("a")){
            da.accessionNumber = cmd.getOptionValue("a");
        }

        try {
            if (cmd.hasOption("x")) {
                da.fits = null;
            } else {
                            logger.info("Starting FITS");
                da.fits = new Fits();
            }
            
            //If 
            
        } catch (FitsException ex) {
            System.err.println("FITS failed to initialize.");
            Exceptions.printStackTrace(ex);
        } catch (Exception ex) {
            System.err.println("MetadataManager failed to initalize.");
            Exceptions.printStackTrace(ex);
        }
    }

    private void autoPilot(File destination, List<File> sources) {
        
        //BLAST, trying to figure out where and when to set options via initializing everything.....
        try {
            File metadata = new File(new File(destination, accessionNumber), accessionNumber + ".xml");
            metadata.mkdirs();
            metadataManager = new MetadataManager(metadata, collectionName, accessionNumber);
            logger.info("Initializing Migrator");
            Migrator migrator = new Migrator(fits, metadataManager);
            migrator.setOptionOverwriteExisting(false);
            migrator.setFailOnPartial(false);
            if (!migrator.setDigestAlgorithm("SHA-1")) {
                System.err.println("Failed to set SHA-1 checksum algorithm.");
            }
            System.out.println("Starting Migrator");
            switch (migrator.run(new File("C:\\Users\\sshaw6\\Dropbox\\Family\\"), new File("C:\\Temp\\"))) {
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
        } catch (Exception ex) {
            logger.error(ex.getMessage());
            Exceptions.printStackTrace(ex);
        }
    }

    private static void printHelp(Options opts) {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("dataAccessioner [options] [destination] [source] [additional sources...]", opts);
    }
    
}
