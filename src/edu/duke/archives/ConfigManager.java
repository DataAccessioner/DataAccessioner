package edu.duke.archives;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.configuration.CompositeConfiguration;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.SystemConfiguration;
import org.apache.commons.configuration.XMLConfiguration;

/**
 *
 * @author Seth Shaw
 */
public class ConfigManager extends CompositeConfiguration {

    static Configuration savedConfig;

    protected ConfigManager(String configPath) {
        this();
        try {
            savedConfig = new XMLConfiguration(configPath);
            addConfiguration(savedConfig);
        } catch (ConfigurationException ex) {
            Logger.getLogger(ConfigManager.class.getName()).
                    log(Level.INFO, "Couldn't build configurations: " +
                    ex.getLocalizedMessage() + "Using default configuration.");
        }
    }

    protected ConfigManager(){
        addConfiguration(new SystemConfiguration());
    }
}
