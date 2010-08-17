/*
 * CollabNet TeamForge
 * Copyright 2010 CollabNet, Inc.  All rights reserved.
 * http://www.collab.net
 */
package com.vasoftware.sf.externalintegration;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Locale;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;

import com.vasoftware.sf.api.ApiProtocolManager;
import com.vasoftware.sf.common.logger.Logger;

/**
 * This servlet is initialized when the application is launched. Its primary purpose at the moment is to initialize the
 * API Protocol Manager.
 */
@SuppressWarnings("serial")
public class BootstrapServlet extends HttpServlet {
    /** Logger for this class */
    private static Logger smLogger = Logger.getLogger(BootstrapServlet.class);

    /**
     * Bootstrap our application
     * 
     * Configure the API Protocol Manager according to the configuration file that comes as part of this application.
     * 
     * @param config
     *            the configuration parameters from web.xml
     * @throws ServletException
     *             if we fail to configure the API Protocol Manager
     */
    @Override
    public void init(final ServletConfig config) throws ServletException {
        super.init(config);

        if (smLogger.isInfoEnabled()) {
            smLogger.info("*** Begin Bootstrap Process ***");
        }

        if (smLogger.isInfoEnabled()) {
            smLogger.info("*** Loading Sourceforge Global Options ***");
        }

        if (smLogger.isInfoEnabled()) {
            smLogger.info("*** Setting system default locale ***");
        }

        // The default locale for the whole system should be "en".
        Locale.setDefault(new Locale("en"));

        final String urlString = config.getInitParameter("apiProtocolUrl");
        if (urlString == null) {
            throw new ServletException("Bootstrap missing apiProtocolUrl parameter");
        }

        URL apiProtocol = null;
        try {
            apiProtocol = config.getServletContext().getResource(urlString);
        } catch (final MalformedURLException e) {
            throw new ServletException(e);
        }
        if (apiProtocol == null) {
            throw new ServletException("Invalid apiProtocolUrl parameter: " + apiProtocol);
        }

        ApiProtocolManager.init(apiProtocol);

        if (smLogger.isInfoEnabled()) {
            smLogger.info("*** End Bootstrap Process ***");
        }
    }
}
