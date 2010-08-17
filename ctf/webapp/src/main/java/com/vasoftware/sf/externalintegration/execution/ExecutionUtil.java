/*
 * $RCSfile: ExecutionUtil.java,v $
 *
 * SourceForge(r) Enterprise Edition
 * Copyright 2007 CollabNet, Inc.  All rights reserved.
 * http://www.collab.net
 *
 * The source code included in this listing is the confidential
 * and proprietary information of CollabNet, Inc.
 */

package com.vasoftware.sf.externalintegration.execution;

import com.vasoftware.sf.common.logger.Logger;
import com.vasoftware.sf.externalintegration.execution.executors.UnixCommandExecutor;
import com.vasoftware.sf.externalintegration.execution.executors.WindowsCommandExecutor;

/**
 * Utility class that provides helper methods for command execution.
 */
public class ExecutionUtil {
    /**
     * the logger for this class and its subclasses.
     */
    protected static final Logger smLogger = Logger.getLogger(ExecutionUtil.class);

    /**
     * Get the command executor based on the operating system.
     * 
     * @return Return the operating system specific CommandExecutor.
     * @throws CommandExecutorException
     *             thrown if there was a problem getting the executor.
     */
    public static CommandExecutor getCommandExecutor() throws CommandExecutorException {
        /* Right now, there are generic executors for only Unix and Windows. */
        Class<?> executorClass = null;

        if (isWindows()) {
            executorClass = WindowsCommandExecutor.class;
        } else {
            executorClass = UnixCommandExecutor.class;
        }

        CommandExecutor executor;

        try {
            executor = (CommandExecutor) executorClass.newInstance();
        } catch (final InstantiationException e) {
            final String errorMsg = "Could not instantiate class for OS: " + System.getProperty("os.name");

            smLogger.error(errorMsg, e);

            throw new CommandExecutorException(e, errorMsg);
        } catch (final IllegalAccessException e) {
            final String errorMsg = "Illegal access on class for OS: " + System.getProperty("os.name");

            smLogger.error(errorMsg, e);

            throw new CommandExecutorException(e, errorMsg);
        }

        if (smLogger.isDebugEnabled()) {
            smLogger.debug("Instantiated executor: " + executor.getClass().getName());
        }

        return executor;
    }

    /**
     * Returns whether or not TeamForge is running in a Windows environment or not.
     * 
     * @return Returns true if the JVM is running on Windows and false otherwise
     */
    public static boolean isWindows() {
        // TODO: Statically set this so it only happens once per JVM
        return System.getProperty("os.name").toLowerCase().startsWith("windows"); 
    }
}
