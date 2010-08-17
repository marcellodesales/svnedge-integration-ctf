/*
 * CollabNet TeamForge
 * Copyright 2010 CollabNet, Inc.  All rights reserved.
 * http://www.collab.net
 */

package com.vasoftware.sf.common.configuration;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import com.vasoftware.sf.common.SfSystemException;
import com.vasoftware.sf.common.logger.Logger;

/**
 * This class is a common means of getting at custom site specific information that is provided outside of the
 * sourceforge enterprise archive.
 */
public class SfGlobalOptionsManager {
    /**
     * An array of possible locations where sourceforge global options can be located.
     */
    private static final File[] CONFIG_FILE_LOCATIONS;
    /** The location of the default sourceforge properties file */
    public static final String DEFAULT_CONFIGURATION_FILE = "/etc/sourceforge.properties";

    private static Logger smLogger = Logger.getLogger(SfGlobalOptionsManager.class);
    private static boolean smEnableLogging = false;
    private static String smSourceForgePropertiesPath = null;

    static {
        final List<File> knownLocations = new ArrayList<File>();

        knownLocations.add(new File(DEFAULT_CONFIGURATION_FILE));
        knownLocations.add(new File("/opt/sourceforge/sourceforge.properties"));
        knownLocations.add(new File("/usr/local/sourceforge/sourceforge.properties"));

        String sourceForgePropertiesPath = System.getenv("SOURCEFORGE_PROPERTIES_PATH");

        /* If the SOURCEFORGE_PROPERTIES_PATH environment variable is set, add it to the front of the list. */
        if (sourceForgePropertiesPath != null) {
            knownLocations.add(0, new File(sourceForgePropertiesPath));
        }

        sourceForgePropertiesPath = System.getProperty("sf.sourceForgePropertiesPath");

        /* If the sf.sourceForgePropertiesPath is set, add it to the front of the list. */
        if (sourceForgePropertiesPath != null) {
            knownLocations.add(0, new File(sourceForgePropertiesPath));
        }

        CONFIG_FILE_LOCATIONS = knownLocations.toArray(new File[] {});
    }

    /**
     * Initializes the global options.
     */
    public static void initialize() {
        if (SfGlobalOptions.isInitialized()) {
            return;
        }

        loadFromFiles();
    }

    /**
     * Enable logging
     */
    public static void enableLogging() {
        smEnableLogging = true;
    }

    /**
     * Iterate through all of the static files in order of appearance in the array adding those properties to the
     * properties namespace.
     */
    public static void loadFromFiles() {
        final Properties configProperties = new Properties();

        if (smEnableLogging) {
            smLogger.info("Scanning following properties file in order: ");
            for (int i = 0; i < CONFIG_FILE_LOCATIONS.length; i++) {
                final File configFileLocation = CONFIG_FILE_LOCATIONS[i];

                smLogger.info("\t" + configFileLocation.toString());
            }
        }

        for (int i = 0; i < CONFIG_FILE_LOCATIONS.length; i++) {
            final File fileLocation = CONFIG_FILE_LOCATIONS[i];

            if (fileLocation.isFile() && fileLocation.canRead()) {
                InputStream propertyInputStream = null;

                try {
                    final Properties properties = new Properties();

                    propertyInputStream = new FileInputStream(fileLocation);

                    properties.load(propertyInputStream);

                    configProperties.putAll(properties);

                    if (smEnableLogging) {
                        smLogger.info("Loaded properties from: " + fileLocation.toString());
                    }

                    /* Record the path to the actual properties file loaded */
                    smSourceForgePropertiesPath = fileLocation.getAbsolutePath();

                    break;
                } catch (final FileNotFoundException e) {
                    throw new SfSystemException(e); // If isFile() and canRead() return true, this shouldn't happen.
                } catch (final IOException e) {
                    throw new SfSystemException(e); // this shouldn't happen either.
                } finally {
                    if (propertyInputStream != null) {
                        try {
                            propertyInputStream.close();
                        } catch (final IOException e) {
                            new SfSystemException(e);
                        }
                    }
                }
            }
        }

        if (smEnableLogging) {
            if (configProperties.isEmpty()) {
                smLogger.warn("Could not load properties (or empty properties file)");
            }
        }

        // TODO: get rid of this eventually
        if (!configProperties.containsKey(GlobalOptionKeys.SFMAIN_SOAP_API_SERVER_URL)) {
            configProperties.put(GlobalOptionKeys.SFMAIN_SOAP_API_SERVER_URL, "http://localhost:8080");
        }

        SfGlobalOptions.setOptions(configProperties);
    }

    /**
     * Factory method for SfGlobalOptions instances.
     * 
     * @return a new SfGlobalOptions object.
     */
    public static SfGlobalOptions getOptions() {
        initialize();
        return new SfGlobalOptions();
    }

    /**
     * Returns the path to the sourceforge.properties file used during initialization.
     *
     * @return String The path to the sourceforge.properties
     */
    public static String getSourceForgePropertiesPath() {
        initialize();

        return smSourceForgePropertiesPath;
    }
}
