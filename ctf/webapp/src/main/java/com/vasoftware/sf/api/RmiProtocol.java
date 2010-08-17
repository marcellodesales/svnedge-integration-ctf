/*
 * CollabNet TeamForge
 * Copyright 2010 CollabNet, Inc.  All rights reserved.
 * http://www.collab.net
 */

package com.vasoftware.sf.api;

import java.util.Properties;

import javax.naming.InitialContext;
import javax.naming.NamingException;

/**
 * The <code>RmiProtocol</code> represents the RMI protocol.
 */
public class RmiProtocol implements ApiProtocol {
    /**
     * Name of the protocol.
     */
    private String mProtocolName;

    /**
     * Initial context for JNDI lookup.
     */
    private InitialContext mInitialContext;

    /**
     * Properties for initial context
     */
    private Properties mContextProperties;

    /**
     * Constructor for the <code>RmiProtocol</code> class.
     */
    public RmiProtocol() {
        // empty
    }

    /**
     * Initialize protocol.
     * 
     * @param protocolName
     *            Name of the protocol.
     * @param properties
     *            Protocol specific properties.
     */
    public void initialize(final String protocolName, final Properties properties) {
        this.mContextProperties = (Properties) properties.clone();
        this.mProtocolName = protocolName;
    }

    /**
     * Gets the intial context for JNDI lookup.
     * 
     * @return initial context for JNDI lookup.
     */
    public InitialContext getInitialContext() {
        if (mInitialContext == null) {
            try {
                mInitialContext = new InitialContext(mContextProperties);
            } catch (final NamingException e) {
                throw new ProtocolException("RmiProtocol: Error constructing InitialContext", e);
            }
        }

        return mInitialContext;
    }

    /**
     * Converts this <code>ApiProtocol</code> object to a string.
     * 
     * @return a string representation of this protocol type.
     */
    @Override
    public String toString() {
        return mProtocolName;
    }

    /**
     * @see ApiProtocol#getProtocolName
     */
    public String getProtocolName() {
        return mProtocolName;
    }

    /**
     * @see Object#equals
     */
    @Override
    public boolean equals(final Object obj) {
        if (obj instanceof RmiProtocol && mProtocolName.equals(obj.toString())) {
            return true;
        }
        return false;
    }

    /**
     * @see Object#hashCode
     */
    @Override
    public int hashCode() {
        return mProtocolName.hashCode();
    }
}
