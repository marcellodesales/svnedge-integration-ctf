/*
 * CollabNet TeamForge
 * Copyright 2010 CollabNet, Inc.  All rights reserved.
 * http://www.collab.net
 */

package com.vasoftware.sf.common.configuration;

import java.util.Properties;

/**
 * The <code>SfGlobalOptions</code> class provides access to CollabNet TeamForge global options.
 */
public class SfGlobalOptions {
    /* A static singleton style property map. */
    private static Properties smConfigProperties = null;

    /**
     * Check to see if the options manager is initialized.
     * 
     * @return true if this manager class has been initialized, false otherwise.
     */
    static boolean isInitialized() {
        return smConfigProperties != null;
    }

    /**
     * Retrieve a global property from the property map. Returns null if the key doesn't exist.
     * 
     * @param optionKey
     *            the name of the option to look up.
     * @return The value of the option as a string. null if it does not exist.
     */
    public String getOption(final String optionKey) {
        if (!isInitialized()) {
            throw new GlobalOptionsNotInitializedException();
        }

        return smConfigProperties.getProperty(optionKey);
    }

    /**
     * package protected method which sets the properties map
     * 
     * @param properties
     *            the map of values to set properties to.
     */
    static void setOptions(final Properties properties) {
        smConfigProperties = properties;
    }
}
