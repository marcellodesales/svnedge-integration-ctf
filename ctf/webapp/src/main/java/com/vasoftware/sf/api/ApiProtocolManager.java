/*
 * CollabNet TeamForge
 * Copyright 2010 CollabNet, Inc.  All rights reserved.
 * http://www.collab.net
 *
 * The source code included in this listing is the confidential
 * and proprietary information of CollabNet, Inc.
 */

package com.vasoftware.sf.api;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.vasoftware.sf.common.SfSystemException;
import com.vasoftware.sf.common.logger.Logger;
import com.vasoftware.sf.common.util.SfNameHelper;
import com.vasoftware.sf.common.util.StringUtil;
import com.vasoftware.sf.common.util.XmlException;
import com.vasoftware.sf.common.util.XmlHelper;

/**
 * The <code>ApiProtocolManager</code> is a singleton class managing the API apiProtocols (RMI, SOAP, etc) employed for
 * client/server communication.
 */
public class ApiProtocolManager {
    /**
     * Map of available API apiProtocols.
     */
    private final Map<String, ApiProtocol> mApiProtocols = new HashMap<String, ApiProtocol>();
    /**
     * Default protocol name.
     */
    private String mDefaultProtocolName;
    /**
     * Logger for this class.
     */
    private static final Logger smLogger = Logger.getLogger(ApiProtocolManager.class);

    /**
     * Singleton <code>ApiProtocolManager</code> class instance.
     */
    private static ApiProtocolManager smProtocolManager = null;

    /**
     * Initialize the protocol manager through a descriptor.
     * 
     * @param protocolsUrl
     *            the URL to our sf-api-protocols.xml file
     */
    public static void init(final URL protocolsUrl) {
        smProtocolManager = new ApiProtocolManager(protocolsUrl);
    }

    /**
     * Initialize the protocol manager directly.
     * 
     * @param apiProtocol
     *            Protocol to use.
     */
    public static void init(final ApiProtocol apiProtocol) {
        smProtocolManager = new ApiProtocolManager();
        smProtocolManager.defineProtocol(apiProtocol, true);
    }

    /**
     * Checks if protocol manager is initialized.
     * 
     * @return true if protocol manager is initialized, false otherwise.
     */
    public static boolean isInitialized() {
        return smProtocolManager != null;
    }

    /**
     * Default private constructor.
     */
    public ApiProtocolManager() {
        // empty
    }

    /**
     * The constructor loads API protocol definitions from $CLIENT_CONFIG/sf-api-apiProtocols.xml) file. All errors are
     * logged - no exceptions are thrown by the constructor.
     * 
     * @param protocolsUrl
     *            the URL to our sf-api-protocols.xml file
     */
    private ApiProtocolManager(final URL protocolsUrl) {
        if (protocolsUrl == null) {
            smLogger.error("Missing protocol configuration file");
            return;
        }

        // parse API protocols configuration file
        Document protocolsDoc = null;

        try {
            protocolsDoc = XmlHelper.parseXml(protocolsUrl);
        } catch (final XmlException e) {
            smLogger.error("Error parsing API protocol configuration file: " + protocolsUrl);
            throw new SfSystemException(e);
        }

        // walk through each protocol
        try {
            final Element elProtocols = protocolsDoc.getDocumentElement();
            if (elProtocols != null) {
                for (Element elProtocol = XmlHelper.getFirstChildOfType(elProtocols, Tag.PROTOCOL); elProtocol != null; elProtocol = XmlHelper
                                                                                                                                              .getNextChildOfType(
                                                                                                                                                                  elProtocol,
                                                                                                                                                                  Tag.PROTOCOL)) {
                    final String protocolName = getProtocolName(elProtocol);
                    final Class<?> protocolClass = getProtocolClass(elProtocol);
                    final Properties params = getProtocolParams(elProtocol);
                    final ApiProtocol apiProtocol = (ApiProtocol) protocolClass.newInstance();

                    apiProtocol.initialize(protocolName, params);
                    defineProtocol(apiProtocol, false);
                }
            }

            mDefaultProtocolName = XmlHelper.getChildElementText(elProtocols, Tag.DEFAULT_PROTOCOL);
            if (StringUtil.isEmpty(mDefaultProtocolName)) {
                throw new ProtocolException("Default protocol must be specified.");
            }
            if (mApiProtocols.get(mDefaultProtocolName) == null) {
                throw new ProtocolException("Invalid default protocol name: " + mDefaultProtocolName);
            }
        } catch (final Exception e) {
            e.printStackTrace();
            smLogger.error("Error processing API protocol configuration file.", e);
            throw new SfSystemException(e);
        }
    }

    /**
     * Defines a new protocol.
     * 
     * @param apiProtocol
     *            Protocol instance.
     * @param defaultProtocol
     *            Specified if specified protocol is the default protocol.
     */
    private void defineProtocol(final ApiProtocol apiProtocol, final boolean defaultProtocol) {
        mApiProtocols.put(apiProtocol.getProtocolName(), apiProtocol);
        if (defaultProtocol) {
            mDefaultProtocolName = apiProtocol.getProtocolName();
        }
    }

    /**
     * Looks up the apiProtocols hash table for the specified protocol.
     * 
     * @param protocolName
     *            Name of the protocol.
     * @return Protocol specified or null if none is defined.
     */
    public static ApiProtocol getApiProtocolByName(final String protocolName) {
        return smProtocolManager.mApiProtocols.get(protocolName);
    }

    /**
     * Returns default protocol name.
     * 
     * @return Name of the default protocol, if any.
     */
    public static String getDefaultProtocolName() {
        return smProtocolManager.mDefaultProtocolName;
    }

    /**
     * Returns the default protocol, if any.
     * 
     * @return Default protocol, if any, null otherwise.
     */
    public static ApiProtocol getDefaultProtocol() {
        final String defaultProtocolName = getDefaultProtocolName();
        if (defaultProtocolName == null) {
            return null;
        }

        return getApiProtocolByName(defaultProtocolName);
    }

    /**
     * Helper method to extract protocol name from the protocol element.
     * 
     * @param elProtocol
     *            Protocol element.
     * @return name of the protocol.
     */
    private String getProtocolName(final Element elProtocol) {
        final String protocolName = XmlHelper.getChildElementText(elProtocol, Tag.PROTOCOL_NAME);

        if (!SfNameHelper.validate(protocolName)) {
            throw new ProtocolException("Specified protocol name is invalid: " + protocolName);
        }

        return protocolName;
    }

    /**
     * Helper method to extract protocol class from the protocol element.
     * 
     * @param elProtocol
     *            Protocol element.
     * @return name of the protocol.
     */
    private Class<?> getProtocolClass(final Element elProtocol) {
        final String protocolClassName = XmlHelper.getChildElementText(elProtocol, Tag.PROTOCOL_CLASS);

        if (StringUtil.isEmpty(protocolClassName)) {
            throw new ProtocolException("Protocol class name must be specified.");
        }

        Class<?> protocolClass;

        try {
            protocolClass = Class.forName(protocolClassName);
        } catch (final ClassNotFoundException e) {
            throw new ProtocolException("Specified protocol class not found: " + protocolClassName);
        }

        if (!ApiProtocol.class.isAssignableFrom(protocolClass)) {
            throw new ProtocolException("Protocol class must implement " + ApiProtocol.class.getName() + " interface.");
        }

        return protocolClass;
    }

    /**
     * Helper method to extract protocol parameters from the protocol element.
     * 
     * @param elProtocol
     *            Protocol element.
     * @return Protocol parameters.
     */
    private Properties getProtocolParams(final Element elProtocol) {
        final Properties params = new Properties();
        final Element elConfigParams = XmlHelper.getFirstChildOfType(elProtocol, Tag.CONFIG_PARAMS);
        if (elConfigParams != null) {
            for (Element elParam = XmlHelper.getFirstChildOfType(elConfigParams, Tag.PARAM); elParam != null; elParam = XmlHelper
                                                                                                                                 .getNextChildOfType(
                                                                                                                                                     elParam,
                                                                                                                                                     Tag.PARAM)) {
                final String paramName = XmlHelper.getChildElementText(elParam, Tag.PARAM_NAME);
                if (StringUtil.isEmpty(paramName)) {
                    throw new ProtocolException("Protocol parameter name not specified.");
                }
                final String paramValue = XmlHelper.getChildElementText(elParam, Tag.PARAM_VALUE);
                if (StringUtil.isEmpty(paramValue)) {
                    smLogger.warn("Protocol parameter value for " + paramName + " not specified.");
                } else {
                    params.put(paramName, paramValue);
                }
            }
        }

        return params;
    }

    /**
     * The <code>Tag</code> inner class defines the XML tags used within the protocol configuration file.
     */
    public class Tag {
        /** XML tag for a describing protocol */
        public static final String PROTOCOL = "protocol";
        /** XML tag for defining the default protocol */
        public static final String DEFAULT_PROTOCOL = "default-protocol";
        /** XML tag for the protocol name */
        public static final String PROTOCOL_NAME = "protocol-name";
        /** XML tag for the protocol class */
        public static final String PROTOCOL_CLASS = "protocol-class";
        /** XML tag for configuration paramters */
        public static final String CONFIG_PARAMS = "config-params";
        /** XML tag for a parameter */
        public static final String PARAM = "param";
        /** XML tag for the parameter name. */
        public static final String PARAM_NAME = "param-name";
        /** XML tag for the parameter value. */
        public static final String PARAM_VALUE = "param-value";
    }
}
