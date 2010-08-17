/*
 * CollabNet TeamForge
 * Copyright 2010 CollabNet, Inc.  All rights reserved.
 * http://www.collab.net
 */
package com.vasoftware.sf.common;

import com.vasoftware.sf.common.util.ExceptionUtil;
import com.vasoftware.sf.common.util.GuidGenerator;

/**
 * The <code>SfSystemException</code> class is the base exception class for all SourceForge runtime exceptions.
 */
@SuppressWarnings("serial")
public class SfSystemException extends RuntimeException implements SfException {

    /** The error code used to retrieve the error message */
    private static final String ERROR_MESSAGE = "An unexpected system error has occurred";

    /** Unique exception id associated with each Exception. */
    private String mExceptionId;

    /** Error code used to look up the error essage. */
    private final String mErrorMsg;

    /**
     * Constructor for a generic System Exception
     */
    public SfSystemException() {
        this(ERROR_MESSAGE, null);
    }

    /**
     * Constructor for a SystemException which wraps another Exception.
     * 
     * @param ex
     *            The exception being mWrapped.
     */
    public SfSystemException(final Exception ex) {
        this(ERROR_MESSAGE, ex);
    }

    /**
     * Initializes an Exception that does not wrap an Exception.
     * 
     * @param msg
     *            The message associated with this exception.
     */
    public SfSystemException(final String msg) {
        this(msg, null);
    }

    /**
     * Initializes an Exception with an mExceptionId and an mErrorMsg.
     * 
     * @param msg
     *            The message associated with this exception.
     * @param cause
     *            The cause of this exception.
     */
    public SfSystemException(final String msg, final Throwable cause) {
        super(msg, cause);

        // Search through the cause exception and its causes for a pre-existing exception ID
        Throwable t = cause;
        while (t != null) {
            if (t instanceof SfException) {
                this.mExceptionId = ((SfException) t).getExceptionId();
                break;
            }
            t = t.getCause();
        }
        if (this.mExceptionId == null) {
            this.mExceptionId = GuidGenerator.newGuid(SfException.EXCEPTION_GUID_PREFIX);
        }
        this.mErrorMsg = msg;
    }

    /**
     * Returns the unique mExceptionId
     * 
     * @return a string representing the unique id for this Exception.
     */
    public String getExceptionId() {
        return this.mExceptionId;
    }

    /**
     * Returns the error code used to look up the error message.
     * 
     * @return A string representing the error code.
     */
    public String getErrorMessage() {
        return this.mErrorMsg;
    }

    /**
     * Override toString() to include the exception ID
     * 
     * @return a string describing this exception
     */
    @Override
    public String toString() {
        return super.toString() + " [" + getExceptionId() + "]";
    }

    /**
     * @see SfException#fetchRootCause()
     */
    public Throwable fetchRootCause() {
        return ExceptionUtil.fetchRootCause(this);
    }

    /**
     * @see SfException#fetchRootCauseMessage()
     */
    public String fetchRootCauseMessage() {
        return fetchRootCause().getMessage();
    }
}
