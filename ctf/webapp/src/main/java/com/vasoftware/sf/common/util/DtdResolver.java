/*
 * CollabNet TeamForge
 * Copyright 2010 CollabNet, Inc.  All rights reserved.
 * http://www.collab.net
 */
package com.vasoftware.sf.common.util;

import java.io.IOException;
import java.io.InputStream;

import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * The <code>DtdResolver</code> class resolves references to SourceForge DTDs.
 */
public class DtdResolver implements EntityResolver {
    /**
     * SourceForge system-id prefix for DTDs.
     */
    public static final String SF_SYSTEM_ID_PREFIX = "http://schema.vasoftware.com/sf/dtd";
    public static final String SFEE50_SYSTEM_ID_PREFIX = "http://schema.open.collab.net/sfee50/dtd";
    public static final String CTF53_SYSTEM_ID_PREFIX = "http://www.open.collab.net/sf/dtd";
    public static final String ETL_SYSTEM_ID_PREFIX = "http://schema.vasoftware.com/etl/dtd";

    /**
     * Empty constructor.
     */
    public DtdResolver() {
        // empty
    }

    /**
     * Resolve references to SourceForge DTD resources.
     * 
     * @param publicId
     *            The public identifier of the external entity being referenced, or null if none was supplied.
     * @param systemId
     *            The system identifier of the external entity being referenced.
     * @return An InputSource object describing the new input source, or null to request that the parser open a regular
     *         URI connection to the system identifier.
     * @exception SAXException
     *                (org.xml.sax.SAXException) Any SAX exception, possibly wrapping another exception.
     * @exception IOException
     *                (java.io.IOException) A Java-specific IO exception, possibly the result of creating a new
     *                InputStream or Reader for the InputSource.
     * @see org.xml.sax.InputSource
     */
    public InputSource resolveEntity(final String publicId, final String systemId) throws SAXException, IOException {
        if (systemId == null) {
            throw new SAXException("System ID must be specified.");
        }

        final int index = systemId.lastIndexOf('/');
        if (index <= 0) {
            throw new SAXException("Invalid System ID: " + systemId);
        }

        // check path
        final String systemIdPrefix = systemId.substring(0, index);
        if (!SF_SYSTEM_ID_PREFIX.equals(systemIdPrefix) && !SFEE50_SYSTEM_ID_PREFIX.equals(systemIdPrefix)
                && !CTF53_SYSTEM_ID_PREFIX.equals(systemIdPrefix) && !ETL_SYSTEM_ID_PREFIX.equals(systemIdPrefix)) {
            throw new SAXException("Invalid System ID: " + systemId);
        }

        // check base name
        final String systemIdBaseName = systemId.substring(index + 1);
        if (systemIdBaseName == null) {
            throw new SAXException("Invalid System ID: " + systemId);
        }

        // compute file-system path
        final String resourcePath = "/META-INF/dtd/" + systemIdBaseName;
        final InputStream is = DtdResolver.class.getResourceAsStream(resourcePath);
        if (is == null) {
            throw new SAXException("Unable to find DTD: " + resourcePath);
        }
        return new InputSource(is);
    }
}
