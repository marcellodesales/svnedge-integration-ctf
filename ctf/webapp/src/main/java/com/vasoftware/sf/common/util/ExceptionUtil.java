/*
 * CollabNet TeamForge
 * Copyright 2010 CollabNet, Inc.  All rights reserved.
 * http://www.collab.net
 */

package com.vasoftware.sf.common.util;

/**
 * The <code>ExceptionUtil</code> class provides static methods for working with exceptions.
 */
public class ExceptionUtil {
    /**
     * Walk down the causal chain to find first exception
     * 
     * @param throwable
     *            Exception to start with
     * @return the root cause exception
     */
    public static Throwable fetchRootCause(Throwable throwable) {
        Throwable follower = null;

        // Walk down the causal linked list
        while (throwable != null) {
            follower = throwable;
            throwable = throwable.getCause();
        }

        return follower;
    }
}
