/*
 * $RCSfile: GroupAlreadyExistsFault.java,v $
 *
 * SourceForge(r) Enterprise Edition
 * Copyright 2007 CollabNet, Inc.  All rights reserved.
 * http://www.collab.net
 */
package com.vasoftware.sf.externalintegration.execution;

import javax.xml.namespace.QName;

import com.vasoftware.sf.externalintegration.AbstractIntegrationFault;

/**
 * Exception <code>GroupAlreadyExistsFault</code> is thrown if an attempt to create a group fails because a group with
 * the same name already exists.
 * 
 * @author Richard Lee <rlee@vasoftware.com>
 * @author Priyanka Dhanda <pdhanda@vasoftware.com>
 * @version $Revision: 1.4 $ $Date: 2007/05/24 00:37:29 $
 */
@SuppressWarnings("serial")
public class GroupAlreadyExistsFault extends AbstractIntegrationFault {
    public static final QName FAULT_CODE = getQName(GroupAlreadyExistsFault.class);
    /**
     * Error code for the exception.
     */
    public static final String CODE_DUPLICATE_GROUP_EXCEPTION = "Group already exists: ";

    /**
     * Constructor for the exception.
     * 
     * @param groupname
     *            The name of the group that failed creation.
     */
    public GroupAlreadyExistsFault(final String groupname) {
        super(FAULT_CODE, CODE_DUPLICATE_GROUP_EXCEPTION + groupname, null, null);
    }

    /**
     * Constructor for the exception that takes a caused by exception.
     * 
     * @param groupname
     *            Whatever strings can be gleaned from the attempted creation.
     * @param e
     *            caused by exception
     */
    public GroupAlreadyExistsFault(final String groupname, final Exception e) {
        super(FAULT_CODE, CODE_DUPLICATE_GROUP_EXCEPTION + groupname + " - " + e.getMessage(), null, null);
    }

    /**
     * @see super#getClassFaultCode()
     */
    @Override
    protected QName getClassFaultCode() {
        return FAULT_CODE;
    }

}
