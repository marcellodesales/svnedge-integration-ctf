/*
 * $RCSfile: AbstractIntegrationFault.java,v $
 *
 * SourceForge(r) Enterprise Edition
 * Copyright 2007 CollabNet, Inc.  All rights reserved.
 * http://www.collab.net
 */
package com.vasoftware.sf.externalintegration;

import javax.xml.namespace.QName;

import org.w3c.dom.Element;

import com.vasoftware.sf.common.SfSoapFault;

/**
 * Fault <code>AbstractIntegrationFault</code> is the abstract parent class for faults thrown from the integration
 * server back to the application server. All subclasses of this class should be declared final so that the Faults would
 * work nicely with our integration soap stub generation scheme.
 * 
 * @author Jamie Gray <jgray@vasoftware.com>
 * @author Wei Hsu <whsu@vasoftware.com>
 * @version $Revision: 1.3 $ $Date: 2007/05/24 00:37:30 $
 */
@SuppressWarnings("serial")
public abstract class AbstractIntegrationFault extends SfSoapFault {
    private static final String NAMESPACE = "http://schema.vasoftware.com/soapfault/extint";

    /**
     * return a class's fault code.
     * 
     * @return the fault code
     */
    protected abstract QName getClassFaultCode();

    /**
     * @see super(QName, String, String, Element[])
     */
    public AbstractIntegrationFault(final QName faultCode, final String faultString, final String actor,
                                    final Element[] details) {
        super(faultCode, faultString, actor, details);
    }

    /**
     * @see super(Exception)
     */
    public AbstractIntegrationFault(final Exception e) {
        super(e);
        setFaultCode(getClassFaultCode());
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
