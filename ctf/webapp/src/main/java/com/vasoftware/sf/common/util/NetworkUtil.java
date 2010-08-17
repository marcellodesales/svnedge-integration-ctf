/*
 * CollabNet TeamForge
 * Copyright 2010 CollabNet, Inc.  All rights reserved.
 * http://www.collab.net
 */

package com.vasoftware.sf.common.util;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.Enumeration;

import com.vasoftware.sf.common.SfSystemException;
import com.vasoftware.sf.common.configuration.GlobalOptionKeys;
import com.vasoftware.sf.common.configuration.SfGlobalOptions;
import com.vasoftware.sf.common.configuration.SfGlobalOptionsManager;
import com.vasoftware.sf.common.logger.Logger;

/**
 * The <code>NetworkUtil</code> class provides utility methods for networking.
 */
public class NetworkUtil {
    private static final Logger smLogger = Logger.getLogger(NetworkUtil.class);

    public static final String LOCALHOST = "localhost";

    /**
     * Return a url back to the application server. (Example: http://cu167.cubit.sp.collab.net/sf)
     * 
     * @return the url
     */
    public static String getMainSfUrl() {
        final SfGlobalOptions config = SfGlobalOptionsManager.getOptions();
        String rootUrl = config.getOption(GlobalOptionKeys.WEBSERVER_ROOT_URL);

        if (rootUrl == null || rootUrl.trim().equals("")) {
            throw new SfSystemException("'" + GlobalOptionKeys.WEBSERVER_ROOT_URL
                    + "' must be set in the TeamForge configuration file.");
        }

        // rewrite rootUrl if not really specified
        if (rootUrl.indexOf("localhost") > -1) {
            rootUrl = rootUrl.replaceFirst("localhost", NetworkUtil.hostname());
        } else if (rootUrl.endsWith("/")) {
            rootUrl = rootUrl.substring(0, rootUrl.length() - 1);
        }
        return rootUrl;
    }

    /**
     * Returns a url back to the application server but without any path. (Example: http://cu167.cubit.sp.collab.net)
     * 
     * @return the url
     */
    public static String getAppServerRootUrl() {
        final String sfMainUrl = NetworkUtil.getMainSfUrl();

        return sfMainUrl.substring(0, sfMainUrl.length() - 3);
    }

    /**
     * Takes a url and updates the url schema, hostname and/or port to be accessible by clients. The manipulations only
     * occur if the hostname in the passed url are either localhost, the configured HOST or the configured DOMAIN.
     * 
     * Actions Performed: Make sure the schema is proper based on the site configuration Make sure the hostname is as it
     * should be Make sure the port is stripped when redirecting through Apache
     * 
     * @param url
     *            The url to externalize
     * 
     * @return url The client-consumable url
     */
    public static String externalizeIntegrationsUrl(final String url) {
        String nUrl = url;

        try {
            final String rootUrl = NetworkUtil.getAppServerRootUrl();
            final URL originalURL = new URL(url);
            final String hostname = NetworkUtil.hostname(); /* The actual hostname */
            final String oHostname = originalURL.getHost(); /* The hostname in the original url */
            final String mHostname = new URL(rootUrl).getHost(); /* The hostname in the root url */

            /* Might want to consider checking that it is an integration url at some point */

            /* Fixing the schema */
            if (oHostname.equals("localhost") || oHostname.equals(mHostname) || oHostname.equals(hostname)) {
                if (rootUrl.startsWith("https:")) {
                    if (url.startsWith("http:")) {
                        nUrl = nUrl.replaceFirst("http", "https");
                    }
                } else if (rootUrl.startsWith("http:")) {
                    if (url.startsWith("https:")) {
                        nUrl = nUrl.replaceFirst("https", "http");
                    }
                }

                /* Fix the hostname */
                /*
                 * We always want the hostname in the returned url to have mHostname as long as it's not 'localhost'.
                 */
                if (!mHostname.equals("localhost")) {
                    nUrl = nUrl.replaceFirst(oHostname, mHostname);
                } else {
                    if (oHostname.equals("localhost")) {
                        nUrl = nUrl.replaceFirst(oHostname, hostname);
                    }
                }
            }
            /* Fix the port */
            if (originalURL.getPort() == 7080) {
                nUrl = nUrl.replaceFirst(":7080", "");
            } else if (originalURL.getPort() == 8080) {
                nUrl = nUrl.replaceFirst(":8080", "");
            }
        } catch (final MalformedURLException murle) {
            smLogger.error("Unable to parse url: " + url);
            /* Return the original url */
            return url;
        }

        return nUrl;
    }

    /**
     * Get hostname
     * 
     * @return hostname
     */
    public static String hostname() {
        try {
            final InetAddress localhost = InetAddress.getLocalHost();
            final String hostname = localhost.getCanonicalHostName();

            // If hostname is not specified or is "localhost", query eth0 for its IP address
            if (hostname == null || LOCALHOST.equals(hostname) || "localhost.localdomain".equals(hostname)) {
                // Use IP address of eth0 device instead of getLocalHost().getHostAddress()
                // as it will return 127.0.0.1 if hostname is "localhost"
                final NetworkInterface networkInterface = NetworkInterface.getByName("eth0");

                if (networkInterface != null) {
                    final Enumeration<InetAddress> e = networkInterface.getInetAddresses();

                    while (e.hasMoreElements()) {
                        final InetAddress inetAddress = e.nextElement();

                        if (inetAddress instanceof Inet4Address) {
                            return inetAddress.getHostAddress();
                        }
                    }
                }
            }
            return hostname;
        } catch (final UnknownHostException e) {
            return LOCALHOST;
        } catch (final SocketException e) {
            return LOCALHOST;
        }
    }

    /**
     * Check if it uses SSL to communicate with app server
     * 
     * @return true if it use ssl
     */
    public static boolean isSecure() {
        final SfGlobalOptions config = SfGlobalOptionsManager.getOptions();
        final String protocol = config.getOption(GlobalOptionKeys.SFMAIN_INTEGRATION_LISTENER_SSL);

        return (protocol != null && protocol.equals("true"));
    }
}
