/*
 * CollabNet TeamForge
 * Copyright 2010 CollabNet, Inc.  All rights reserved.
 * http://www.collab.net
 */

package com.vasoftware.sf.common;

import javax.xml.namespace.QName;

import org.apache.axis.AxisFault;
import org.w3c.dom.Element;

/**
 * The <code>SfSoapFault</code> represents a generic CollabNet TeamForge SOAP fault.
 */
@SuppressWarnings("serial")
public class SfSoapFault extends AxisFault {
    private static final String NAMESPACE = "http://schema.vasoftware.com/soapfault";

    /**
     * Most likely use: to construct from an AxisFault
     * 
     * @see super(javax.xml.namespace.QName, String, String, org.w3c.dom.Element[])
     */
    public SfSoapFault(final QName faultCode, final String faultString, final String actor, final Element[] details) {
        super(faultCode, faultString, actor, details);
    }

    /**
     * Class constructor.
     * 
     * @param cause
     *            Cause of this fault.
     */
    public SfSoapFault(final Throwable cause) {
        super(getQName(cause.getClass()), cause.getMessage(), null, null);
    }

    /**
     * Returns the qualified name for the exception class.
     * 
     * @param exceptionClass
     *            Exception class.
     * @return Qualified name for the exception type.
     */
    public static QName getQName(final Class<? extends Throwable> exceptionClass) {
        String errorType = exceptionClass.getName();
        final int index = errorType.lastIndexOf(".");
        errorType = errorType.substring(index + 1);
        return new QName(getNamespace(), errorType);
    }

    /**
     * Returns the namespace for this fault
     * 
     * @return the namespace
     */
    protected static String getNamespace() {
        return NAMESPACE;
    }
}
