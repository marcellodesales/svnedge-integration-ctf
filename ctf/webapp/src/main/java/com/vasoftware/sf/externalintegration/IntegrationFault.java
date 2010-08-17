/*
 * $RCSfile: IntegrationFault.java,v $
 *
 * SourceForge(r) Enterprise Edition
 * Copyright 2007 CollabNet, Inc.  All rights reserved.
 * http://www.collab.net
 */
package com.vasoftware.sf.externalintegration;

import javax.xml.namespace.QName;

import org.w3c.dom.Element;

/**
 * Fault <code>IntegrationFault</code> is the generic common purpose fault thrown from the integration server back to
 * the application server.
 * 
 * @author Jamie Gray <jgray@vasoftware.com>
 * @author Wei Hsu <whsu@vasoftware.com>
 * @version $Revision: 1.3 $ $Date: 2007/05/24 00:37:30 $
 */
@SuppressWarnings("serial")
public final class IntegrationFault extends AbstractIntegrationFault {
    public static final QName FAULT_CODE = getQName(IntegrationFault.class);
    /**
     * Error message for the exception.
     */
    public static final String MSG_INTEGRATION_EXCEPTION = "Integration error: ";

    /**
     * Most likely use: to construct from an AxisFault
     * 
     * @see super(javax.xml.namespace.QName, String, String, org.w3c.dom.Element[])
     */
    public IntegrationFault(final QName faultCode, final String faultString, final String actor, final Element[] details) {
        super(faultCode, faultString, actor, details);
    }

    /**
     * Constructor for the exception.
     * 
     * @param description
     *            The problem
     */
    public IntegrationFault(final String description) {
        super(FAULT_CODE, MSG_INTEGRATION_EXCEPTION + description, null, null);
    }

    /**
     * Constructor for the exception that takes a caused by exception.
     * 
     * @param description
     *            The problem
     * @param t
     *            caused by exception
     */
    public IntegrationFault(final String description, final Throwable t) {
        super(FAULT_CODE, MSG_INTEGRATION_EXCEPTION + description + " - " + t.getMessage(), null, null);
    }

    /**
     * Constructor for the exception that takes a caused by exception.
     * 
     * @param t
     *            caused by exception
     */
    public IntegrationFault(final Throwable t) {
        super(FAULT_CODE, t.getMessage(), null, null);
    }

    /**
     * @see super#getClassFaultCode()
     * */
    @Override
    protected QName getClassFaultCode() {
        return FAULT_CODE;
    }
}
