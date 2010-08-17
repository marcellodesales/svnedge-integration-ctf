/*
 * CollabNet TeamForge
 * Copyright 2010 CollabNet, Inc.  All rights reserved.
 * http://www.collab.net
 *
 * The source code included in this listing is the confidential
 * and proprietary information of CollabNet, Inc.
 */

package com.vasoftware.sf.api;

import com.vasoftware.sf.common.SfSystemException;

/**
 * Exception <code>ProtocolException</code> is thrown when an error is encountered in constructing the
 * <code>RmiProtocol</code> instansce.
 */
@SuppressWarnings("serial")
public class ProtocolException extends SfSystemException {
    /**
     * Error code for the exception.
     */
    private static final String PROTOCOL_EXCEPTION = "Protocol exception: ";

    /**
     * Constructor for the exception class.
     * 
     * @param cause
     *            nested throwable resulting in this exception.
     * @param message
     *            errror message of exception
     */
    public ProtocolException(final String message, final Exception cause) {
        super(PROTOCOL_EXCEPTION + message, cause);
    }

    /**
     * Constructor for the exception class.
     * 
     * @param message
     *            errror message of exception
     */
    public ProtocolException(final String message) {
        super(PROTOCOL_EXCEPTION + message);
    }
}
