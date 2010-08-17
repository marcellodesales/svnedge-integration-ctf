/*
 * CollabNet TeamForge
 * Copyright 2010 CollabNet, Inc.  All rights reserved.
 * http://www.collab.net
 *
 * The source code included in this listing is the confidential
 * and proprietary information of CollabNet, Inc.
 */

package com.vasoftware.sf.common.util;

import com.vasoftware.sf.common.SfApplicationException;

/**
 * The <code>XmlException</code> represents XML helper related errors.
 */
@SuppressWarnings("serial")
public class XmlException extends SfApplicationException {
    /**
     * Error code for the exception.
     */
    public static final String XML_ERROR = "XML Error: ";

    /**
     * Basic constructor for exception class.
     * 
     * @param msg
     *            Error message associated with this exception.
     */
    public XmlException(final String msg) {
        super(XML_ERROR + msg);
    }

    /**
     * Constructor with a nested exception.
     * 
     * @param msg
     *            Error message associated with this exception.
     * @param cause
     *            Nested exception which is the cause of this exception
     */
    public XmlException(final String msg, final Exception cause) {
        super(XML_ERROR + msg, cause);
    }
}
