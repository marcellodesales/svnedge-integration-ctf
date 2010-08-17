/**
 * SourceForge(r) Enterprise Edition
 * Copyright 2007 CollabNet, Inc.  All rights reserved.
 * http://www.collab.net
 */

package com.vasoftware.sf.externalintegration;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.vasoftware.sf.common.configuration.GlobalOptionKeys;
import com.vasoftware.sf.common.configuration.SfGlobalOptions;
import com.vasoftware.sf.common.configuration.SfGlobalOptionsManager;
import com.vasoftware.sf.common.util.NetworkUtil;
import com.vasoftware.sf.common.util.ScmPermissionsCallback;
import com.vasoftware.sf.common.util.ScmPermissionsUtil;
import com.vasoftware.sf.common.util.StringUtil;

/**
 * Simple HTTP servlet used for checking permissions for SCM interactions.
 */
@SuppressWarnings("serial")
public class ScmPermissionsProxyServlet extends HttpServlet {
    /**
     * @see javax.servlet.http.HttpServlet#doGet(javax.servlet.http.HttpServletRequest,
     *      javax.servlet.http.HttpServletResponse)
     */
    @Override
    public void doGet(final HttpServletRequest req, final HttpServletResponse res) throws IOException, ServletException {
        doPost(req, res);
    }

    /**
     * @see javax.servlet.http.HttpServlet#doPost(javax.servlet.http.HttpServletRequest,
     *      javax.servlet.http.HttpServletResponse)
     * 
     *      To call this successfully, you must pass the following query parameters:
     * 
     *      username : This is the username whose permissions we're checking. (Example: jwhitlock) systemId : This is
     *      the external system id that the repository belongs to. (Example: exsy1001) repoPath : This is the repository
     *      path whose permissions are being checked. If checking permissions at the repository level, the path must end
     *      with a "/". (Example: REPO_NAME/REPO/PATH) accessType: This is the type of access being checked. This is
     *      optional if retrieving the global access levels(Example: view)
     * 
     *      Returns an String concatenation in the following format, representing access at differing levels: At path :
     *      At path and all below : At path and/or anywhere below
     */
    @Override
    public void doPost(final HttpServletRequest req, final HttpServletResponse res) throws IOException,
                                                                                   ServletException {
        StringBuffer sb = new StringBuffer();
        final PrintWriter out = res.getWriter();
        int access = IntegrationConstants.ACCESS_UNAUTHORIZED;
        int accessAtPathAndEverywhereBelow = IntegrationConstants.ACCESS_UNAUTHORIZED;
        int accessAtPathAndOrAnywhereBelow = IntegrationConstants.ACCESS_UNAUTHORIZED;
        final ScmPermissionsUtil scmpUtil = ScmPermissionsUtil.getInstance();
        boolean cacheCleared = false;
        final SfGlobalOptions config = SfGlobalOptionsManager.getOptions();
        final String webAppInternalUrl = NetworkUtil.isSecure() ? "https://"
                                                               : "http://"
                                                                       + (config
                                                                                .getOption(GlobalOptionKeys.SFMAIN_INTEGRATION_LISTENER_HOST)
                                                                               + ":" + config
                                                                                             .getOption(GlobalOptionKeys.SFMAIN_INTEGRATION_LISTENER_PORT));

        /* Validate the request and retrieve request parameters */
        String username = req.getParameter("username");
        final String systemId = req.getParameter("systemId");
        final String repoPath = req.getParameter("repoPath");
        String accessType = req.getParameter("accessType");
        final String clearCache = req.getParameter("clearCache");

        /* Default to the anonymous user if no username param is passed */
        if (StringUtil.isEmpty(username)) {
            username = UserConstants.NOBODY_USERNAME;
        }

        if (systemId == null) {
            sb.append("'systemId' is a required request argument.\n");
        }

        if (repoPath == null) {
            sb.append("'repoPath' is a required request argument.\n");
        }

        if (accessType != null) {
            // Make the accessType lowercase
            accessType = accessType.toLowerCase();
        }

        /* Only validate the access type if there are no previous errors */
        if (sb.toString().length() == 0 && accessType != null
                && !RBACConstants.ROLE_PATH_SUPPORTED_TYPES.contains(accessType)
                && scmpUtil.getAccessTypeModifier(accessType) == null) {
            sb.append("'" + accessType + "' is not a valid accessType argument.");
        }

        /* Invalidate the cache */
        if (clearCache != null && clearCache.toLowerCase().equals("true")) {
            scmpUtil.clearCache();
            cacheCleared = true;
            log("[ScmPermissionsProxyServlet]: Permissions cache cleared.");
        }

        /* Only verify permissions if there are no previous errors and we are not retrieving universal information */
        if (sb.toString().length() == 0 && accessType != null) {
            final String actualAccessType = (scmpUtil.getAccessTypeModifier(accessType) == null ? accessType
                                                                                               : accessType.split("-")[0]);

            try {
                boolean hasAccess = scmpUtil.hasPermission(username, systemId, repoPath, actualAccessType,
                                                           new SoapPermissionsCallbackImpl(webAppInternalUrl));

                if (hasAccess) {
                    access = IntegrationConstants.ACCESS_OK;
                }

                hasAccess = scmpUtil
                                    .hasPermission(username, systemId, repoPath, actualAccessType + "-"
                                            + RBACConstants.ROLE_PATH_TYPE_MODIFIER_ALL,
                                                   new SoapPermissionsCallbackImpl(webAppInternalUrl));

                if (hasAccess) {
                    accessAtPathAndEverywhereBelow = IntegrationConstants.ACCESS_OK;
                }

                hasAccess = scmpUtil
                                    .hasPermission(username, systemId, repoPath, actualAccessType + "-"
                                            + RBACConstants.ROLE_PATH_TYPE_MODIFIER_ANY,
                                                   new SoapPermissionsCallbackImpl(webAppInternalUrl));

                if (hasAccess) {
                    accessAtPathAndOrAnywhereBelow = IntegrationConstants.ACCESS_OK;
                }
            } catch (final Exception re) {
                if (re.getMessage().indexOf("repository not found") > -1
                        || re.getMessage().indexOf("invalid username") > -1
                        || re.getMessage().indexOf("Unable to parse guid") > -1) {
                    access = IntegrationConstants.ACCESS_FORBIDDEN;
                    accessAtPathAndEverywhereBelow = IntegrationConstants.ACCESS_FORBIDDEN;
                    accessAtPathAndOrAnywhereBelow = IntegrationConstants.ACCESS_FORBIDDEN;
                } else {
                    // Unexpected exception
                    log("ScmPermissionsProxyServlet error: " + re.getMessage());
                }
            }
        }

        res.setContentType("text/plain");

        // Only return hasAccess if there is no previous error
        if (sb.toString().length() == 0) {
            if (accessType == null) { /* Retrieving the global commit and view access */
                try {
                    final ScmPermissionsCallback callback = new SoapPermissionsCallbackImpl(webAppInternalUrl);
                    final String repoName = repoPath.split("/")[0];
                    final boolean hasUniversalWriteAccess = scmpUtil
                                                                    .hasPermission(
                                                                                   username,
                                                                                   systemId,
                                                                                   repoName + "/",
                                                                                   RBACConstants.ROLE_PATH_TYPE_COMMIT
                                                                                           + "-"
                                                                                           + RBACConstants.ROLE_PATH_TYPE_MODIFIER_ALL,
                                                                                   callback);
                    boolean hasUniversalReadAccess = hasUniversalWriteAccess;

                    if (!hasUniversalReadAccess) {
                        hasUniversalReadAccess = scmpUtil
                                                         .hasPermission(
                                                                        username,
                                                                        systemId,
                                                                        repoName + "/",
                                                                        RBACConstants.ROLE_PATH_TYPE_VIEW
                                                                                + "-"
                                                                                + RBACConstants.ROLE_PATH_TYPE_MODIFIER_ALL,
                                                                        callback);
                    }

                    sb.append((hasUniversalWriteAccess ? IntegrationConstants.ACCESS_OK
                                                      : IntegrationConstants.ACCESS_UNAUTHORIZED)
                            + ":"
                            + (hasUniversalReadAccess ? IntegrationConstants.ACCESS_OK
                                                     : IntegrationConstants.ACCESS_UNAUTHORIZED));
                } catch (final Exception re) {
                    if (re.getMessage().indexOf("repository not found") > -1
                            || re.getMessage().indexOf("invalid username") > -1
                            || re.getMessage().indexOf("Unable to parse guid") > -1) {

                        sb.append(IntegrationConstants.ACCESS_FORBIDDEN + ":" + IntegrationConstants.ACCESS_FORBIDDEN);
                    } else {
                        // Unexpected exception
                        log("ScmPermissionsProxyServlet error: " + re.getMessage());
                    }
                }
            } else {
                sb.append(access + ":" + accessAtPathAndEverywhereBelow + ":" + accessAtPathAndOrAnywhereBelow);
            }
        } else if (cacheCleared) {
            /*
             * Return a message about clearing cache instead of the usual error message about missing parameters, just
             * in case the call to the servlet was purely to clear the cache.
             */
            sb = new StringBuffer("permissions cache cleared");
        }

        out.write(sb.toString());

        out.close();
    }
}
