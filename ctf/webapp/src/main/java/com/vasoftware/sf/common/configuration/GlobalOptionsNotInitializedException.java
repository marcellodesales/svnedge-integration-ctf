/*
 * CollabNet TeamForge
 * Copyright 2010 CollabNet, Inc.  All rights reserved.
 * http://www.collab.net
 */

package com.vasoftware.sf.common.configuration;

import com.vasoftware.sf.common.SfSystemException;

/**
 * This exception is thrown if a global options object is instantiated, or there is an attempt to access global objects
 * without them being initialized by the SfGlobalOptionsManager class.
 */
@SuppressWarnings("serial")
public class GlobalOptionsNotInitializedException extends SfSystemException {
    private static final String CODE_NOTINITIALIZED = "Global Options Not Initialized";

    /**
     * public constructor.
     */
    public GlobalOptionsNotInitializedException() {
        super(CODE_NOTINITIALIZED);
    }
}