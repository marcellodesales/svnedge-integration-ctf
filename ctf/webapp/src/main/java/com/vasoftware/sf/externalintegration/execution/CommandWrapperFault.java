/*
 * $RCSfile: CommandWrapperFault.java,v $
 *
 * SourceForge(r) Enterprise Edition
 * Copyright 2007 CollabNet, Inc.  All rights reserved.
 * http://www.collab.net
 */
package com.vasoftware.sf.externalintegration.execution;

import javax.xml.namespace.QName;

import org.apache.axis.AxisFault;
import org.w3c.dom.Element;

/**
 * Exception <code>CommandWrapperFault</code> is thrown when the specified command could not be executed
 * 
 * @author Richard Lee <rlee@vasoftware.com>
 * @author Priyanka Dhanda <pdhanda@vasoftware.com>
 * @version $Revision: 1.7 $ $Date: 2007/05/24 00:37:29 $
 */
@SuppressWarnings("serial")
public final class CommandWrapperFault extends AxisFault {

    /** Namespace URI for SOAP types (NOTE: must match WSDDGeneratorTask definition) */
    public static final String NAMESPACE_URI = "http://schema.vasoftware.com/sf/integration/type";

    public static final QName FAULT_CODE = getQName(CommandWrapperFault.class);

    /**
     * Error code for the exception.
     */
    public static final String CODE_COMMAND_EXECUTION_ERROR = "Non-zero result code from command: ";

    /**
     * Most likely use: to construct from an AxisFault
     * 
     * @see super(javax.xml.namespace.QName, String, String, org.w3c.dom.Element[])
     */
    public CommandWrapperFault(final QName faultCode, final String faultString, final String actor,
                               final Element[] details) {
        super(faultCode, faultString, actor, details);
    }

    /**
     * Constructor called by the suc classes to psaa theor own fault code and message
     * 
     * @param faultCode
     *            The axis fault code
     * @param message
     *            The error message
     */
    protected CommandWrapperFault(final QName faultCode, final String message) {
        super(faultCode, message, null, null);
    }

    /**
     * Constructor for the exception.
     * 
     * @param command
     *            The command that caused the exception
     * @param message
     *            Whatever strings can be gleaned from the attempted creation.
     */
    public CommandWrapperFault(final String command, final String message) {
        super(FAULT_CODE, cleanseString(CODE_COMMAND_EXECUTION_ERROR + command + " - " + message), null, null);
    }

    /**
     * Constructor for the exception that takes a caused by exception.
     * 
     * @param command
     *            The command that caused the exception
     * @param message
     *            Whatever strings can be gleaned from the attempted creation.
     * @param e
     *            caused by exception
     */
    public CommandWrapperFault(final String command, final String message, final Exception e) {
        super(FAULT_CODE, cleanseString(CODE_COMMAND_EXECUTION_ERROR + command + " - " + message + " - "
                + e.getMessage()), null, null);
    }

    /**
     * Constructor for the exception that takes a caused by exception.
     * 
     * @param message
     *            The error message for this exception
     */
    public CommandWrapperFault(final String message) {
        super(FAULT_CODE, cleanseString(message), null, null);
    }

    /**
     * Constructor for the exception that takes a caused by exception.
     * 
     * @param message
     *            The error message for this exception
     * @param e
     *            caused by exception
     */
    public CommandWrapperFault(final String message, final Exception e) {
        super(FAULT_CODE, cleanseString(message + " - " + e.getMessage()), null, null);
    }

    /**
     * Since these exceptions are going over soap, we are just going to replace less than and greater than symbols with
     * their ampersand equivalents.
     * 
     * @param string
     *            The string being cleansed.
     * @return The string without any funny characters.
     */
    private static String cleanseString(final String string) {
        return string.replaceAll("<", "&lt;").replaceAll(">", "&gt;");
    }

    /**
     * Returns the qualified name for the SOAP type.
     * 
     * @param typeClass
     *            Soap type.
     * @return Qualified name for the SOAP type.
     */
    public static QName getQName(final Class<?> typeClass) {
        String typeName = typeClass.getName();
        final int index = typeName.lastIndexOf(".");

        typeName = typeName.substring(index + 1);

        return new QName(NAMESPACE_URI, typeName);
    }
}
