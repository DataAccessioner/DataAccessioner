/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.duke.archives.migrator.plugins.adapters;

import edu.duke.archives.interfaces.Adapter;
import edu.harvard.hul.ois.jhove.App;
import edu.harvard.hul.ois.jhove.JhoveBase;
import edu.harvard.hul.ois.jhove.OutputHandler;
import java.io.File;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;
import org.jdom.output.XMLOutputter;
import org.jdom.xpath.*; 

/**
 *
 * @author Seth Shaw
 */
public class JhoveAdapter implements Adapter {

    /** Application name. */
    private static final String NAME = "JhoveTest";
    /** Application build date, YYYY, MM, DD. */
    private static final int[] DATE = {2008, 5, 30};
    /** Application release number. */
    private static final String RELEASE = "0.1";
    /** Application invocation syntax. */
    private static final String USAGE = "N/A";
    /** Copyright information. */
    private static final String RIGHTS =
            "Are you sure you really want to copy this? " +
            "You can do better! I know you can. I believe in you.";
    private JhoveBase je;
    private static App app = new App(NAME, RELEASE, DATE, USAGE, RIGHTS);
    private static final String handlerName = "xml";
    private static String configFile = "jhove/jhove.conf";
    private static String saxClass;
    private static OutputHandler handler;

    public JhoveAdapter() {
        try {
            je = new JhoveBase();
            je.setLogLevel("SEVERE"); //ALL, FINEST, FINER, FINE, 
            //INFO, WARNING, SEVERE
//			if(migrator.isVerbose()){
//				je.setLogLevel("FINER");
//			}
            configFile = System.getProperty("java.io.tmpdir") + File.separator +
                    ".jpf-shadow" + File.separator +
                    "edu.duke.archives.migrator.plugins.adapters.jhove@0.1.0" +
                    File.separator + "jhove" + File.separator + "jhove.conf";
            saxClass = JhoveBase.getSaxClassFromProperties();
            je.init(configFile, saxClass);

            handler = je.getHandler(handlerName);
            if (handler == null && handlerName != null) {
                throw new Exception("Handler '" + handlerName + "' not found");
            }
        } catch (Exception e) {
            System.err.println("Could not Initialize Jhove Adapter: " +
                    e.getLocalizedMessage());
            e.printStackTrace();
        }
    }

    public String identifyFile(String path) {
        Document metadataSet = null;
		String[] dirFileOrUri = {path}; //Only want one at a time.
		try {
			File results = File.createTempFile("jhove",
					".xml");
			je.dispatch (app, null, null, handler, results.getAbsolutePath(), 
					dirFileOrUri);
			/*
			 * The is occasionally a lag between running the analysis and
			 * the results file appearing. Thus, we wait until it shows
			 * up. But not too long.
			 */
			int totalSleep = 0;
			int sleepInterval = 50;
			while(!results.exists()){
				if(totalSleep > 120000){ //Wait for two minutes 
					throw new Exception("Timed out waiting for Jhove: "+
							path);
				}
				totalSleep += sleepInterval;
				Thread.sleep(sleepInterval);
			}
			/*
			 * Sometime Jhove hits a file that results in an error. This wouldn't be so bad
			 * except that it causes all future result xml files to have an indent that the 
			 * SaxReader will not tolerate. 
			 * 
			 * Possible solution: check the results xml for a <message severity="error"> and
			 * reset the JhoveAdapter (or part of it) on these occasions. 
			 */
			metadataSet = new SAXBuilder().build(results);
			//Domj4's SelectSingleNode did not like //message[@severity='error']
            Element error = (Element) (XPath.selectSingleNode(metadataSet,
                    "//*[name()='message' and @severity='error']"));
            if (error != null) {
                System.err.println("Jhove encountered a severe error for " +
                        path + " : " +
                        error.getText());
            }

			//Java has not been deleting when the delete function is used
			//although deleteOnExit seems to work fine.
			if(!results.delete()){
				results.deleteOnExit();
			}
		} catch (InterruptedException e) {
			System.err.println("\tWoken out of our sleep...");
			e.printStackTrace();
		} catch (Exception e) {
			System.err.println("Unknown error:");
			e.printStackTrace();
			metadataSet = null;
		}
		
		/* Reseting seems to be like washing your hands after using the bathroom:
		 * You don't know what germs you are going to pass on and, in this case,
		 * you may accidentally kill the next guy. (I think it may be an artifact
		 * of reusing the saxClass.) Please wash your hands.
		 */
		reset();
        if (metadataSet == null){
            return "";
        }
		return new XMLOutputter().outputString(metadataSet);
    }

    public String identifyDirectory(String path) {
        //identifyDirectory & identifyFile act the same way 
        //and both process directories
        return identifyFile(path);
    }

    public void reset() {
        try {
            configFile = System.getProperty("java.io.tmpdir") + File.separator +
                    ".jpf-shadow" + File.separator +
                    "edu.duke.archives.migrator.plugins.adapters.jhove@0.1.0" +
                    File.separator + "jhove" + File.separator + "jhove.conf";
            saxClass = JhoveBase.getSaxClassFromProperties();
            je.init(configFile, saxClass);

            handler = je.getHandler(handlerName);
            if (handler == null && handlerName != null) {
                throw new Exception("Handler '" + handlerName + "' not found");
            }
        } catch (Exception e) {
            System.err.println("Could not Initialize Jhove Adapter: " +
                    e.getLocalizedMessage());
            e.printStackTrace();
        }
    }

    public String getName() {
        return "Jhove";
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