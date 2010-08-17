/*
 * $RCSfile: UserAlreadyExistsFault.java,v $
 *
 * SourceForge(r) Enterprise Edition
 * Copyright 2007 CollabNet, Inc.  All rights reserved.
 * http://www.collab.net
 */
package com.vasoftware.sf.externalintegration.execution;

import javax.xml.namespace.QName;

import org.w3c.dom.Element;

import com.vasoftware.sf.externalintegration.AbstractIntegrationFault;

/**
 * Exception <code>UserAlreadyExistsFault</code> is thrown if an attempt to create a user fails because a user with the
 * same name already exists.
 * 
 * @author Richard Lee <rlee@vasoftware.com>
 * @author Priyanka Dhanda <pdhanda@vasoftware.com>
 * @version $Revision: 1.5 $ $Date: 2007/05/24 00:37:29 $
 */
@SuppressWarnings("serial")
public class UserAlreadyExistsFault extends AbstractIntegrationFault {
    public static final QName FAULT_CODE = getQName(UserAlreadyExistsFault.class);

    /**
     * Error code for the exception.
     */
    public static final String CODE_DUPLICATE_USER_EXCEPTION = "Username already exists: ";

    /**
     * Most likely use: to construct from an AxisFault
     * 
     * @see super(javax.xml.namespace.QName, String, String, org.w3c.dom.Element[])
     */
    public UserAlreadyExistsFault(final QName faultCode, final String faultString, final String actor,
                                  final Element[] details) {
        super(faultCode, faultString, actor, details);
    }

    /**
     * Constructor for the exception.
     * 
     * @param username
     *            The username that failed creation
     */
    public UserAlreadyExistsFault(final String username) {
        super(FAULT_CODE, CODE_DUPLICATE_USER_EXCEPTION + username, null, null);
    }

    /**
     * Constructor for the exception that takes a caused by exception.
     * 
     * @param username
     *            The username that failed creation
     * @param e
     *            caused by exception
     */
    public UserAlreadyExistsFault(final String username, final Exception e) {
        super(FAULT_CODE, CODE_DUPLICATE_USER_EXCEPTION + username + " - " + e.getMessage(), null, null);
    }

    /**
     * @see super#getClassFaultCode()
     */
    @Override
    protected QName getClassFaultCode() {
        return FAULT_CODE;
    }

}
