package edu.duke.archives;

import edu.duke.archives.interfaces.MetadataManager;
import edu.duke.archives.metadata.Metadata;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Seth Shaw
 */
public class BlankMetadataManager implements MetadataManager {

    private boolean isRunning = false;
    
    public void init(DataMigrator aThis) throws Exception {
        isRunning = true;
        return;
    }

    public String getName() {
        return "No Metadata";
    }

    public void init(String filePath, String collectionName,
            String accessionNumber) throws Exception {
        isRunning = true;
        return;
    }

    public void close() throws Exception {
        isRunning = false;
        return;
    }

    public boolean isRunning() {
        return isRunning;
    }

    public void cancel() {
        try {
            close();
            return;
        } catch (Exception ex) {
            Logger.getLogger(BlankMetadataManager.class.getName()).
                    log(Level.SEVERE, null, ex);
        }
    }

    public void recordFile(Metadata file) {
        return;
    }

    public void recordFile(String path) {
        return;
    }

    public void startFile(Metadata file) {
        return;
    }

    public void startFile(String path) {
        return;
    }

    public void endFile() {
        return;
    }

    public void startDirectory(Metadata directory) {
        return;
    }

    public void startDirectory(String path) {
        return;
    }

    public void endDirectory() {
        return;
    }

    public void addXML(String xml) {
        return;
    }

    public void addNote(String note) {
        return;
    }

}
