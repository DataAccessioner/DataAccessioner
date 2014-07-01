/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.dataaccessioner;

import edu.harvard.hul.ois.fits.Fits;
import edu.harvard.hul.ois.fits.exceptions.FitsException;
import java.io.File;
import org.openide.util.Exceptions;

/**
 *
 * @author sshaw6
 */
public class DataAccessioner {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        
        try {
            System.out.println("Starting Fits");
            Fits fits = new Fits();
            System.out.println("Initializing Migrator");
            Migrator migrator = new Migrator(fits);
            migrator.setOptionOverwriteExisting(false);
            migrator.setFailOnPartial(false);
            if(!migrator.setDigestAlgorithm("SHA-1")){
                System.err.println("Failed to set SHA-1 checksum algorithm.");
            }
            System.out.println("Starting Migrator");
            migrator.run(new File("C:\\Users\\sshaw6\\Dropbox\\Family\\"), new File("C:\\Temp\\"));
            for(String warning: migrator.getWarnings()){
                System.err.println(warning);
            }
            System.out.println("Done!");
        } catch (FitsException ex) {
            System.err.println("FITS failed to initialize.");
            Exceptions.printStackTrace(ex);
        }
    }
    
}
