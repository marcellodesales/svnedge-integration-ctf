/*
 * $RCSfile: ScmLimitationFault.java,v $
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
 * Exception <code>GroupAlreadyExistsFault</code> is thrown if an attempt to create a group fails because a group with
 * the same name already exists.
 * 
 * @author Richard Lee <rlee@vasoftware.com>
 * @author Priyanka Dhanda <pdhanda@vasoftware.com>
 * @version $Revision: 1.2 $ $Date: 2007/05/24 00:37:29 $
 */
@SuppressWarnings("serial")
public class ScmLimitationFault extends AbstractIntegrationFault {
    public static final QName FAULT_CODE = getQName(ScmLimitationFault.class);
    /**
     * Error code for the exception.
     */
    public static final String SCM_LIMITATION_EXCEPTION = "Scm limitation: ";

    /**
     * Constructor for the exception.
     * 
     * @param limitation
     *            The name of the group that failed creation.
     */
    public ScmLimitationFault(final String limitation) {
        super(FAULT_CODE, SCM_LIMITATION_EXCEPTION + limitation, null, null);
    }

    /**
     * Constructor for the exception that takes a caused by exception.
     * 
     * @param limitation
     *            Whatever strings can be gleaned from the attempted creation.
     * @param e
     *            caused by exception
     */
    public ScmLimitationFault(final String limitation, final Exception e) {
        super(FAULT_CODE, SCM_LIMITATION_EXCEPTION + limitation + " - " + e.getMessage(), null, null);
    }

    /**
     * Most likely use: to construct from an AxisFault
     * 
     * @see super(javax.xml.namespace.QName, String, String, org.w3c.dom.Element[])
     */
    public ScmLimitationFault(final QName faultCode, final String faultString, final String actor,
                              final Element[] details) {
        super(faultCode, faultString, actor, details);
    }

    /**
     * @see super#getClassFaultCode()
     */
    @Override
    protected QName getClassFaultCode() {
        return FAULT_CODE;
    }

}
