/*
 * DataAccessioner.java
 */

package edu.duke.archives;

import edu.duke.archives.interfaces.MetadataManager;
import java.util.ArrayList;
import org.jdesktop.application.Application;
import org.jdesktop.application.SingleFrameApplication;

/**
 * The main class of the application.
 */
public class DataAccessioner extends SingleFrameApplication {

    protected static DataMigrator migrator;
    protected static MetadataManager manager;
    protected static ArrayList tools;
    
    /**
     * At startup create and show the main frame of the application.
     */
    @Override protected void startup() {
        DataAccessionerView dav = new DataAccessionerView(this);
        dav.setMigrator(migrator);
        show(dav);
    }

    /**
     * This method is to initialize the specified window by injecting resources.
     * Windows shown in our application come fully initialized from the GUI
     * builder, so this additional configuration is not needed.
     */
    @Override protected void configureWindow(java.awt.Window root) {
    }

    /**
     * A convenient static getter for the application instance.
     * @return the instance of DataAccessioner
     */
    public static DataAccessioner getApplication() {
        return Application.getInstance(DataAccessioner.class);
    }

    /**
     * Main method launching the application.
     */
    public static void main(String[] args) {
        launch(DataAccessioner.class, new DataMigrator(), args);
    }
    
    public static synchronized<T extends Application> void launch(Class<T> arg1, DataMigrator dm, String[] args){
        migrator = dm;
        launch(arg1, args);
    }
}