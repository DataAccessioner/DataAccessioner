/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.duke.archives.interfaces;

import edu.duke.archives.DataAccessioner;
import edu.duke.archives.metadata.FileWrapper;

/**
 *
 * @author seths
 */
public interface MetadataManager {
    public void init(DataAccessioner aThis) throws Exception;
    String getName();
    void close() throws Exception;
    boolean isRunning();
    void cancel();
    
    void recordFile(FileWrapper file);
    void recordFile(String path);
    void startFile(FileWrapper file);
    void startFile(String path);
    void endFile();
    
    void startDirectory(FileWrapper directory);
    void startDirectory(String path);
    void endDirectory();

    void addXML(String xml);
    void addNote(String note);
}
