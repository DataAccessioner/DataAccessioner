package edu.duke.archives.interfaces;

/**
 *
 * @author Seth Shaw
 */
public interface Adapter {

    String getName();
    String runFile(String path);
    String startDirectory(String path);
    String endCurrentDirectory();
    void reset();
}