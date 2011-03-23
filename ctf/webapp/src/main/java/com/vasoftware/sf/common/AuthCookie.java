/*
 * CollabNet TeamForge
 * Copyright 2010 CollabNet, Inc.  All rights reserved.
 * http://www.collab.net
 */

package com.vasoftware.sf.common;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * The <code>AuthCookie</code> class provides static methods for working with CollabNet TeamForge authentication
 * cookies.
 */
@SuppressWarnings("unchecked")
public class AuthCookie {
    /* The name of the cookie */
    public static final String SF_AUTH_COOKIE_NAME = "sf_auth";

    /**
     * Wrapper for information stored in cookie
     */
    public static class AuthParams {
        private String mUserSessionId;
        private String mJavaSessionId;

        /**
         * Get guid of UserSessionKey for logged in user.
         * 
         * @return Guid of UserSessionKey
         */
        public String getUserSessionId() {
            return mUserSessionId;
        }

        /**
         * Set guid of UserSessionKey for logged in user.
         * 
         * @param id
         */
        public void setUserSessionId(final String id) {
            mUserSessionId = id;
        }

        /**
         * Get value of 'jsessionid' cookie/URL param.
         * 
         * @return jsessionid
         */
        public String getJavaSessionId() {
            return mJavaSessionId;
        }

        /**
         * Set value of 'jsessionid' cookie/URL param.
         * 
         * @param id
         */
        public void setJavaSessionId(final String id) {
            mJavaSessionId = id;
        }
    }

    /**
     * Filter query string out of Auth params
     * 
     * @param request
     *            HttpServletRequest
     * @return Filtered string if there were Auth params, or null otherwise
     */
    public static String filterQueryString(final HttpServletRequest request) {
        final String userSession = request.getParameter("us");
        final String jSession = request.getParameter("js");

        if (userSession != null && jSession != null) {
            final StringBuffer result = new StringBuffer();
            final Map params = new HashMap(request.getParameterMap());

            params.remove("us");
            params.remove("js");

            for (final Iterator i = params.entrySet().iterator(); i.hasNext();) {
                final Map.Entry entry = (Map.Entry) i.next();

                if (result.length() != 0) {
                    result.append("&");
                }

                result.append(entry.getKey() + "=");

                try {
                    result.append(URLEncoder.encode(((String[]) entry.getValue())[0], "UTF-8"));
                } catch (final UnsupportedEncodingException e) {
                    ; // not possible
                }
            }

            return result.toString();
        }

        return null;
    }

    /**
     * Get decoded authentication parameters from cookie.
     * 
     * @param request
     *            HttpServletRequest
     * @return AuthParams
     */
    public static AuthParams getAuthParams(final HttpServletRequest request) {
        final String userSession = request.getParameter("us");
        final String jSession = request.getParameter("js");

        if (userSession != null && jSession != null) {
            final AuthParams authParams = new AuthParams();

            authParams.setUserSessionId(userSession);
            authParams.setJavaSessionId(jSession);

            return authParams;
        }

        final Cookie[] cookies = request.getCookies();

        if (cookies != null) {
            for (int i = 0; i < cookies.length; i++) {
                if (SF_AUTH_COOKIE_NAME.equals(cookies[i].getName())) {
                    final String cookie = cookies[i].getValue();
                    final String[] sa = cookie.split("&", 2);
                    final AuthParams authParams = new AuthParams();

                    authParams.setUserSessionId(sa[0]);
                    authParams.setJavaSessionId(sa[1]);

                    return authParams;
                }
            }
        }

        return null;
    }

    /**
     * Store authentication parameters from the active session onto HTTP response.
     * 
     * @param guid
     *            User's session GUID
     * @param id
     *            http servlet Session Id
     * @param response
     *            HttpServletResponse
     */
    public static void setAuthParams(final String guid, final String id, final HttpServletResponse response) {
        final Cookie cookie = new Cookie(SF_AUTH_COOKIE_NAME, guid + "&" + id);

        cookie.setMaxAge(-1);
        cookie.setPath("/");
        response.addCookie(cookie);
    }

    /**
     * Re-Store authentication parameters onto the current HTTP response.
     * 
     * @param authParams
     *            AuthParams object
     * @param response
     *            HttpServletResponse
     */
    public static void setAuthParams(final AuthParams authParams, final HttpServletResponse response) {
        setAuthParams(authParams.getUserSessionId(), authParams.getJavaSessionId(), response);
    }
}
