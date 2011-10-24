/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.duke.archives.migrator.plugins.adapters;

import edu.duke.archives.interfaces.Adapter;
import java.io.File;
import java.io.IOException;
import org.jdom.Document;
import org.jdom.input.SAXBuilder;
import org.jdom.output.XMLOutputter;
import uk.gov.nationalarchives.droid.AnalysisController;
/**
 *
 * @author Seth Shaw
 */
public class DroidAdapter implements Adapter {
	private static AnalysisController myAnalysis = new AnalysisController();
	private String exportType = "xml";
    
    public DroidAdapter(){
		reset();
		//myAnalysis.setVerbose(true);
	}
    
    public String identifyFile(String path) {
        myAnalysis.resetFileList();
        myAnalysis.addFile(path);
        return identify(path);
    }

    public String identifyDirectory(String path) {
        myAnalysis.resetFileList();
        myAnalysis.addFile(path, true);
        return identify(path);
    }

    public void reset() {
        myAnalysis = new AnalysisController();
        String basePath =
                System.getProperty("java.io.tmpdir") + File.separator +
                ".jpf-shadow" + File.separator +
                "edu.duke.archives.migrator.plugins.adapters.droid@0.1.0" +
                File.separator;
        String configPath = basePath + AnalysisController.CONFIG_FILE_NAME;
        try {
            myAnalysis.readConfiguration(configPath);
            myAnalysis.readSigFile(basePath + AnalysisController.SIGNATURE_FILE_NAME);
            myAnalysis.setVerbose(false);
        } catch (Exception e) {
            System.err.println("Could not open configuration or signature file:" +
                    configPath);
            e.printStackTrace();
        }
        return;
    }

    private String identify(String path){
        File tmpDir = new File(System.getProperty("java.io.tmpdir")
				+File.separator+"droid");
		tmpDir.mkdirs();
		tmpDir.deleteOnExit();  //Will only delete if all temp files
								// delete successfully.
		Document metadataSet;
        long start;
		try {
			File results = File.createTempFile("droid", 
					"."+exportType, tmpDir);
			start = 0;
			try {
				//XML output, named after file being run.
				//Droid insists on taking the name you provide and attaching the 
				String resultsBaseName = results.getAbsolutePath().substring(0, results.getAbsolutePath().lastIndexOf("."+exportType));
				myAnalysis.runFileFormatAnalysis(exportType, 
						resultsBaseName);
				start = System.nanoTime();
				/*
				 * The is occasionally a lag between running the analysis and
				 * the results file appearing. Thus, we wait until it shows up
				 * But not too long. Since we have a list of files we can't
                 * taylor wait time to total file sizes. Set it at 15 minutes
                 * which should be more than enough.
				 */
				int sleepInterval = 50;
				int timeOut = 900000; //Allow at least 15 min
				while(results.length() == 0){
					if(((System.nanoTime() - start)/1000000) > timeOut){
						throw new Exception("Timed out waiting for Droid.");
					}
					Thread.sleep(sleepInterval);
				}
				metadataSet = new SAXBuilder().build(results);
				results.deleteOnExit();
			} catch (Exception e) {
				System.err.println("Unable to run Droid for "+path
						+": "+e.getLocalizedMessage());
				System.err.println("elapsed: "+((System.nanoTime() - start) / 1000000)+" milliseconds");
				e.printStackTrace();
				results.deleteOnExit();
				// Reset Droid just in case.
				reset();
				return null;
			}
//			System.out.println("elapsed: "+((System.nanoTime() - start) / 1000000)+" milliseconds");
			return new XMLOutputter().outputString(metadataSet);
		} catch (IOException e) {
			System.err.println("Could not create temp file: "
					+e.getLocalizedMessage());
			e.printStackTrace();
			return null;
		}
    }

    public String getName() {
        return "Droid Adapter";
    }

    public String runFile(String path) {
        return identifyFile(path);
    }

    public String startDirectory(String path) {
        return "";
    }

    public String endCurrentDirectory() {
        return "";
    }

}
