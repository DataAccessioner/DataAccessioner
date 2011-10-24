package edu.duke.archives.interfaces;

import edu.duke.archives.DataMigrator;
import edu.duke.archives.metadata.Metadata;

/**
 *
 * @author Seth Shaw
 */
public interface MetadataManager {
    public void init(DataMigrator aThis) throws Exception;
    String getName();
    void init(String filePath, String collectionName, String accessionNumber) throws Exception;
    void close() throws Exception;
    boolean isRunning();
    void cancel();
    
    void recordFile(Metadata file);
    void recordFile(String path);
    void startFile(Metadata file);
    void startFile(String path);
    void endFile();
    
    void startDirectory(Metadata directory);
    void startDirectory(String path);
    void endDirectory();

    void addXML(String xml);
    void addNote(String note);
}
