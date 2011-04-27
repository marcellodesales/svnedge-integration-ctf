/*
 * CollabNet TeamForge
 * Copyright 2010 CollabNet, Inc.  All rights reserved.
 * http://www.collab.net
 */

package com.vasoftware.sf.common.logger;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;

import org.apache.log4j.Level;

import com.vasoftware.sf.externalintegration.BootstrapServlet;

/**
 * The <code>Logger</code> class provides an abstract implementation for logging.
 */
public class Logger {
    /* Private "mWrapped" logger object */
    private final org.apache.log4j.Logger mWrapped;

    /* Hashmap containing all instantiated loggers */
    private static HashMap<String, Logger> smLoggerMap = new HashMap<String, Logger>();

    /* Key under which to set the current profile object root ancestor log id in the logging context. */
    public static final String PROFILE_ROOT_ANCESTOR_KEY = "ProfileRoot";

    /**
     * Private constructor to prevent public one from being created automatically; consumers should only use the static
     * factory methods. Its signature may change if we wrap a different package.
     * 
     * @param loggerName
     *            Name of the Logger that will get created
     */
    private Logger(final String loggerName) {
        mWrapped = org.apache.log4j.Logger.getLogger(loggerName);
    }

    /**
     * Factory method to retrieve a logger based on a string name. This will return the single Logger instance
     * associated with the name.
     * 
     * @param name
     *            Name of the requested Logger
     * @return Logger instance (singleton!)
     */
    public static synchronized Logger getLogger(final String name) {
        Logger logger = null;

        logger = smLoggerMap.get(name);
        if (logger == null) {
            logger = new Logger(name);
            smLoggerMap.put(name, logger);
        }

        return logger;
    }

    /**
     * Factory method to retrieve a logger based on a class. Equivalent to getLogger(clazz.getSuiteName()).
     * 
     * @param clazz
     *            Class object of requested Logger
     * @return Logger instance (Singleton!)
     */
    public static Logger getLogger(final Class<?> clazz) {
        return getLogger(clazz.getName());
    }

    /**
     * Logs the given message in the standard out in a given level with a tag "INTEGRATION" in the place of the
     * running class.
     * @param message is the message to be logged.
     * @param level is the level to be used.
     * @param a given parameter.
     */
    private static void logToSystemOut(String message, Level level, Throwable throwable) {
        System.out.println(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:S").format(new Date()) + " "+ 
                level.toString() + " [INTEGRATION] - " + message);
        if (throwable != null) {
            throwable.printStackTrace(System.out);
        }
    }

    /**
     * Log a message with DEBUG priority
     * 
     * @param message
     *            DEBUG message to be logged
     */
    public void debug(final String message) {
        logToSystemOut(message, Level.DEBUG, null);
    }

    /**
     * Log a message with ERROR priority
     * 
     * @param message
     *            ERROR message to be logged
     */
    public void error(final String message) {
        logToSystemOut(message, Level.ERROR, null);
    }

    /**
     * Log a message with ERROR priority including information about the specified throwable.
     * 
     * @param message
     *            ERROR message to be logged
     * @param t
     *            throwable about which information will be logged
     */
    public void error(final String message, final Throwable t) {
        logToSystemOut(message, Level.ERROR, t);
    }

    /**
     * Log a message with FATAL priority including information
     * about the specified throwable.
     * 
     * @param message FATAL message to be logged
     * @param t throwable about which information will be logged
     */
    public void fatal(String message, Throwable t) {
        logToSystemOut(message, Level.FATAL, null);
    }

    /**
     * Log a message with INFO priority
     * 
     * @param message
     *            INFO message to be logged
     */
    public void info(final String message) {
        logToSystemOut(message, Level.INFO, null);
    }

    /**
     * Log a message with WARNING priority including information about the specified throwable.
     * 
     * @param message
     *            WARNING message to be logged
     */
    public void warn(final String message) {
        logToSystemOut(message, Level.WARN, null);
    }

    /**
     * Log a message with WARNING priority
     * 
     * @param message
     *            WARNING message to be logged
     * @param t
     *            throwable about which information will be logged
     */
    public void warn(final String message, final Throwable t) {
        logToSystemOut(message, Level.WARN, t);
    }

    /**
     * Return true if this logger is enabled for level WARNING and higher. Code should use this test before contructing
     * arguments for a DEBUG-level message.
     * 
     * @return boolean flag if DEBUG-level logging is enabled for this logger
     */
    public boolean isDebugEnabled() {
        return mWrapped.isDebugEnabled();
    }

    /**
     * Return true if this logger is enabled for level INFO and higher. Code should use this test before contructing
     * arguments for an INFO-level message.
     * 
     * @return boolean flag if INFO-level logging is enabled for this logger
     */
    public boolean isInfoEnabled() {
        return mWrapped.isInfoEnabled();
    }
}
