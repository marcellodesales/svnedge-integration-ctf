/*
 * CollabNet TeamForge
 * Copyright 2010 CollabNet, Inc.  All rights reserved.
 * http://www.collab.net
 */

package com.vasoftware.sf.common.configuration;

import java.io.File;

import com.vasoftware.sf.common.SfSystemException;
import com.vasoftware.sf.common.util.StringUtil;

/**
 * The <code>SfPaths</code> provides methods for getting access to CollabNet TeamForge file/directory paths.
 */
public class SfPaths {
    /* System property key for source forge home directory */
    private static final String SOURCEFORGE_HOME = "sourceforge.home";

    /* System property key for data directory */
    private static final String DATA_DIR = "app.data";

    /**
     * Subdirectory under SOURCEFORGE_HOME to where integration server scripts are stored,
     * unless an override location is specified.
     */
    private static final String SCRIPTS_SUBDIRECTORY = "/integration";

    /**
     * Helper function to look for the specified file/directory in two locations: <sourceforge_home>/etc and
     * <dataDir>/etc
     * 
     * @param filename
     *            name of properties or config file
     * @return path to file
     */
    public static String dataEtcPath(final String filename) {
        String path = sourceforgeHome() + File.separator + "etc" + File.separator + filename; // sandbox location
        File file = new File(path);
        if (!file.exists()) {
            final String dataEtcDir = dataEtcDir();
            file = new File(dataEtcDir);
            if (!file.isDirectory()) {
                file.mkdirs();
            }
            path = dataEtcDir + File.separator + filename; // installer location
        }
        return path;
    }

    /**
     * Gets the path for SourceForge Home.
     * 
     * @return the directory path
     */
    public static String sourceforgeHome() {
        String dir = System.getProperty(SOURCEFORGE_HOME);
        if (!StringUtil.isEmpty(dir)) {
            return dir;
        }

        // See if it is in /etc/sourceforge.properties
        final SfGlobalOptions options = SfGlobalOptionsManager.getOptions();
        dir = options.getOption(GlobalOptionKeys.SFMAIN_SOURCEFORGE_HOME);
        if (!StringUtil.isEmpty(dir)) {
            return dir;
        } else {
            throw new SfSystemException("System property '" + SOURCEFORGE_HOME + "' must be defined!");
        }
    }

    /**
     * Gets the path for the data etc directory
     * 
     * @return the directory path
     */
    private static String dataEtcDir() {
        return dataDir() + File.separator + "etc";
    }

    /**
     * Gets the path for data directory.
     * 
     * @return the directory path
     */
    public static String dataDir() {
        return dirProperty(DATA_DIR, "var");
    }

    /**
     * Determine property value by first check if it is specified by -D, then if it is in /etc/sourceforge.properties,
     * then it will return it as directoryName in the same level as sourceforge_home.
     * 
     * @param propertyName name of property
     * @param directoryName if we can't find the property, use this directoryName in site.dir
     * @return directory path of the specified property
     */
    private static String dirProperty(final String propertyName, final String directoryName) {
        // See if it is defined as -D
        String dir = System.getProperty(propertyName);
        if (!StringUtil.isEmpty(dir)) {
            return dir;
        }

        // See if it is in /etc/sourceforge.properties
        final SfGlobalOptions options = SfGlobalOptionsManager.getOptions();
        dir = options.getOption(propertyName);
        if (!StringUtil.isEmpty(dir)) {
            return dir;
        }

        // Make it as directory in site.dir
        final File sfHome = new File(sourceforgeHome());
        final File dirFile = new File(sfHome.getParentFile(), directoryName);
        return dirFile.getAbsolutePath();
    }

    /**
     * Either returns the path specific in sourceforge.properties (sfmain.integration.scripts-root)
     * or SfPaths.sourceforgeHome()/integration.
     * 
     * @return Returns the full path to the directory containing the integration server scripts.
     */
    public static String getIntegrationScriptsRootPath() {
        final SfGlobalOptions options = SfGlobalOptionsManager.getOptions();

        String scriptsRoot = options.getOption(GlobalOptionKeys.SFMAIN_INTEGRATION_SCRIPTS_ROOT);

        if (scriptsRoot == null) {
            scriptsRoot = SfPaths.sourceforgeHome();
            scriptsRoot += SCRIPTS_SUBDIRECTORY;
        }

        return scriptsRoot;
    }
}
