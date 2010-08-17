/*
 * $RCSfile: ObjectAlreadyExistsFault.java,v $
 *
 * SourceForge(r) Enterprise Edition
 * Copyright 2007 CollabNet, Inc.  All rights reserved.
 * http://www.collab.net
 */
package com.vasoftware.sf.externalintegration;

import javax.xml.namespace.QName;

import org.w3c.dom.Element;

/**
 * Exception <code>ObjectAlreadyExistsFault</code> is thrown if an attempt to create an object fails because an object
 * with the same name already exists.
 * 
 * @author Jamie Gray <jgray@vasoftware.com>
 * @author Wei Hsu <whsu@vasoftware.com>
 * @version $Revision: 1.2 $ $Date: 2007/05/24 00:37:30 $
 */
@SuppressWarnings("serial")
public final class ObjectAlreadyExistsFault extends AbstractIntegrationFault {
    public static final QName FAULT_CODE = getQName(ObjectAlreadyExistsFault.class);

    /**
     * Error message for the exception.
     */
    public static final String MSG_DUPLICATE_OBJECT_EXCEPTION = "Object already exists: ";

    /**
     * Most likely use: to construct from an AxisFault
     * 
     * @see super(javax.xml.namespace.QName, String, String, org.w3c.dom.Element[])
     */
    public ObjectAlreadyExistsFault(final QName faultCode, final String faultString, final String actor,
                                    final Element[] details) {
        super(faultCode, faultString, actor, details);
    }

    /**
     * Constructor for the exception.
     * 
     * @param objectDescription
     *            The object that already exists
     */
    public ObjectAlreadyExistsFault(final String objectDescription) {
        super(FAULT_CODE, MSG_DUPLICATE_OBJECT_EXCEPTION + objectDescription, null, null);
    }

    /**
     * Constructor for the exception that takes a caused by exception.
     * 
     * @param objectDescription
     *            The object that already exists.
     * @param e
     *            caused by exception
     */
    public ObjectAlreadyExistsFault(final String objectDescription, final Exception e) {
        super(FAULT_CODE, MSG_DUPLICATE_OBJECT_EXCEPTION + objectDescription + " - " + e.getMessage(), null, null);
    }

    /**
     * @see super#getClassFaultCode()
     */
    @Override
    protected QName getClassFaultCode() {
        return FAULT_CODE;
    }
}
