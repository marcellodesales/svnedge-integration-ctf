/*
 * CollabNet TeamForge
 * Copyright 2010 CollabNet, Inc.  All rights reserved.
 * http://www.collab.net
 */

package com.vasoftware.sf.common;

/**
 * The <code>SfException</code> interface is extends by all CollabNet TeamForge exceptions.
 */
public interface SfException {
    /* The prefix used for generating the exception GUID */
    static final String EXCEPTION_GUID_PREFIX = "exid";

    /**
     * Returns the unique exceptionId
     * 
     * @return a string representing the unique id for this Exception.
     */
    String getExceptionId();
}
