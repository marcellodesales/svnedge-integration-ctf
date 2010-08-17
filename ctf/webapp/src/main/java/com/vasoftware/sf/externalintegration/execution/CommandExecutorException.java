/*
 * $RCSfile: CommandExecutorException.java,v $
 *
 * SourceForge(r) Enterprise Edition
 * Copyright 2007 CollabNet, Inc.  All rights reserved.
 * http://www.collab.net
 */

package com.vasoftware.sf.externalintegration.execution;

/**
 * CommandExecutorException. This is thrown if the act of creating or using command executors causes unexpected errors.
 * 
 * @author Richard Lee <rlee@vasoftware.com>
 * @version $Revision: 1.3 $ $Date: 2007/05/24 00:37:29 $
 */
@SuppressWarnings("serial")
public class CommandExecutorException extends Exception {

    /**
     * This is the constructor used when an attempt at creating a command executor occurs for an unknown or unset
     * operating system type.
     * 
     * @param operatingSystemType
     *            the operating system whose executor was being retrieved.
     */
    public CommandExecutorException(final String operatingSystemType) {
        super("Unsupported Operating System type: " + operatingSystemType);
    }

    /**
     * This is the generic constructor used when errors occur while trying to create or use executors.
     * 
     * @param e
     *            an exception to wrap.
     * @param message
     *            the message to pass up along with the exception
     */
    public CommandExecutorException(final Exception e, final String message) {
        super("Error while initializing Command Executor: " + message, e);
    }
}
