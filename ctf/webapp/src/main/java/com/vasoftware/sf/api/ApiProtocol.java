/*
 * CollabNet TeamForge
 * Copyright 2010 CollabNet, Inc.  All rights reserved.
 * http://www.collab.net
 */

package com.vasoftware.sf.api;

import java.util.Properties;

/**
 * Represents the protocol over which client-side stubs access server-side APIs.
 */
public interface ApiProtocol {
    /**
     * Initialize protocol.
     * 
     * @param protocolName
     *            Name of the protocol.
     * @param properties
     *            Protocol specific properties.
     */
    void initialize(String protocolName, Properties properties);

    /**
     * Returns the name of the protocol.
     * 
     * @return The protocol name.
     */
    String getProtocolName();
}
