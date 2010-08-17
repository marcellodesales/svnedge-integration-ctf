/*
 * CollabNet TeamForge
 * Copyright 2010 CollabNet, Inc.  All rights reserved.
 * http://www.collab.net
 */

package com.vasoftware.sf.common;

import com.vasoftware.sf.common.util.ExceptionUtil;
import com.vasoftware.sf.common.util.GuidGenerator;

/**
 * The <code>SfApplicationException</code> the abstract class that all SourceForge Application Exceptions extend. This
 * is a checked exception.
 */
@SuppressWarnings("serial")
public abstract class SfApplicationException extends Exception implements SfException {

    /** Unique exception id associated with each Exception. */
    private String mExceptionId;

    /** Error code used to look up the error essage. */
    private final String mErrorCode;

    /**
     * Class constructor that a sub class may call or override. Initializes an Exception that does not wrap an
     * Exception.
     * 
     * @param errorCode
     *            The error code associated with this exception. Used to look up the error message.
     */
    protected SfApplicationException(final String errorCode) {
        super(errorCode);
        this.mErrorCode = errorCode;
    }

    /**
     * Class constructor that may be called only from within this class. Initializes an Exception with an mExceptionId,
     * an mErrorCode, a logged flag and a mCategory.
     * 
     * @param errorCode
     *            The error code associated with this exception. Used to look up the error message.
     * @param wrapped
     *            The mWrapped throwable.
     */
    protected SfApplicationException(final String errorCode, final Exception wrapped) {
        super(errorCode, wrapped);

        // Search through the wrapped exception and its causes for a pre-existing exception ID
        Throwable t = wrapped;
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
        this.mErrorCode = errorCode;

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
        return this.mErrorCode;
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
