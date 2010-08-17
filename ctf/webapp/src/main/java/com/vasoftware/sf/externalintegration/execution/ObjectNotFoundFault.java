/*
 * $RCSfile: ObjectNotFoundFault.java,v $
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
 * Exception <code>ObjectNotExistFault</code> is thrown if an attempt to delete an object fails because the object with
 * the same name already exists.
 * 
 * @author Anne Rosset <arosset@vasoftware.com>
 * @version $Revision: 1.2 $ $Date: 2007/05/24 00:37:29 $
 */
@SuppressWarnings("serial")
public class ObjectNotFoundFault extends AbstractIntegrationFault {
    public static final QName FAULT_CODE = getQName(ObjectNotFoundFault.class);

    /**
     * Error code for the exception.
     */
    public static final String OBJECT_NOT_EXIST_EXCEPTION = "Object doesn't exist: ";

    /**
     * Most likely use: to construct from an AxisFault
     * 
     * @see super(javax.xml.namespace.QName, String, String, org.w3c.dom.Element[])
     */
    public ObjectNotFoundFault(final QName faultCode, final String faultString, final String actor,
                               final Element[] details) {
        super(faultCode, faultString, actor, details);
    }

    /**
     * Constructor for the exception.
     * 
     * @param id
     *            The id that failed creation
     */
    public ObjectNotFoundFault(final String id) {
        super(FAULT_CODE, OBJECT_NOT_EXIST_EXCEPTION + id, null, null);
    }

    /**
     * Constructor for the exception that takes a caused by exception.
     * 
     * @param id
     *            The object that failed creation
     * @param e
     *            caused by exception
     */
    public ObjectNotFoundFault(final String id, final Exception e) {
        super(FAULT_CODE, OBJECT_NOT_EXIST_EXCEPTION + id + " - " + e.getMessage(), null, null);
    }

    /**
     * @see super#getClassFaultCode()
     */
    @Override
    protected QName getClassFaultCode() {
        return FAULT_CODE;
    }

}
