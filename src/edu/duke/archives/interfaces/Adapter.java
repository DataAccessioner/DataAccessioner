/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.duke.archives.interfaces;

/**
 *
 * @author seths
 */
public interface Adapter {

    String getName();
    String runFile(String path);
    String startDirectory(String path);
    String endCurrentDirectory();
    void reset();
}
