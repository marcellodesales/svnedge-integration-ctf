/*
 * CollabNet TeamForge
 * Copyright 2010 CollabNet, Inc.  All rights reserved.
 * http://www.collab.net
 */

package com.vasoftware.sf.externalintegration;

import com.vasoftware.sf.common.configuration.GlobalOptionKeys;
import com.vasoftware.sf.common.configuration.SfGlobalOptions;
import com.vasoftware.sf.common.configuration.SfGlobalOptionsManager;

/**
 * The <code>ScmConstants</code> class provides constants for SCM.
 */
public class ScmConstants {
    /* Represents the RepositoryType.CATEGORY_EDIT.EDIT name */
    public static final String REPOSITORY_EDIT_OPERATION_NAME = "edit";

    /* Represents the RepositoryType.CATEGORY_USER.USE name */
    public static final String REPOSITORY_USE_OPERATION_NAME = "use";

    /* Represents the RepositoryType.CATEGORY_VIEW.VIEW name */
    public static final String REPOSITORY_VIEW_OPERATION_NAME = "view";

    /* The group on the SCM server that contains all CollabNet TeamForge users. Must be 8 letters or less on Solaris. */
    public static final String ALL_USERS_GROUP = "sfall";

    /* The group created on the SCM server that contains all unrestricted users. Must be 8 letters or less on Solaris */
    public static final String UNRESTRICTED_USERS_GROUP = "sfunrest";

    /* Apache constants */
    public static final String HTTPD_GROUP = getHttpdGroup();
    public static final String HTTPD_USER = getHttpdUser();

    /* Application constants */
    public static final String APP_USER = getAppUser();

    /* CVS constants */
    public static final String CVS_USER = "nobody";

    /* SSH constants */
    public static final String SSH_AUTHORIZED_KEYS_FILE = "authorized_keys";
    public static final String SSH_RESOURCE_DIRECTORY = ".ssh";
    public static final Boolean TUNNEL_ENABLED = getTunnelEnabled();
    public static final String TUNNEL_USER = getTunnelUser();

    /* WANDisco constants */
    public static final String WD_ADMIN_GROUP_EXTENSION = "-admin";
    public static final String WD_ADMIN_PERMISSION = "ADMIN";
    public static final String WD_ADMIN_USERS_GROUP = "Admin";
    public static final String WD_READ_GROUP_EXTENSION = "-read";
    public static final String WD_READ_PERMISSION = "READ";
    public static final String WD_WRITE_GROUP_EXTENSION = "-write";

    /* Windows constants */
    public static final String FAKE_WINDOWS_SCMROOT = "/windows-scmroot";

    /**
     * Get the user that the app server runs as (and integration server in SaaS mode).
     * 
     * @return The proper user.
     */
    public static String getAppUser() {
        final SfGlobalOptions options = SfGlobalOptionsManager.getOptions();
        final String user = options.getOption(GlobalOptionKeys.APP_USER_KEY);

        if (user == null || "".equals(user)) {
            return GlobalOptionKeys.DEFAULT_APP_USER;
        } else {
            return user;
        }
    }

    /**
     * Get the user that the daemon should use for subversion directory ownership.
     * 
     * @return The proper user.
     */
    public static String getHttpdUser() {
        final SfGlobalOptions options = SfGlobalOptionsManager.getOptions();
        final String user = options.getOption(GlobalOptionKeys.HTTPD_USER_KEY);

        if (user == null || "".equals(user)) {
            return GlobalOptionKeys.DEFAULT_HTTPD_USER;
        } else {
            return user;
        }
    }

    /**
     * Get the user that the daemon should use for subversion directory ownership.
     * 
     * @return The proper user.
     */
    public static String getHttpdGroup() {
        final SfGlobalOptions options = SfGlobalOptionsManager.getOptions();
        final String user = options.getOption(GlobalOptionKeys.HTTPD_GROUP_KEY);

        if (user == null || "".equals(user)) {
            return GlobalOptionKeys.DEFAULT_HTTPD_GROUP;
        } else {
            return user;
        }
    }

    /**
     * Get the user for ssh tunneling
     * 
     * @return The proper user.
     */
    public static Boolean getTunnelEnabled() {
        final SfGlobalOptions options = SfGlobalOptionsManager.getOptions();
        final String value = options.getOption(GlobalOptionKeys.TUNNEL_ENABLED_KEY);

        if (value == null) {
            return GlobalOptionKeys.DEFAULT_TUNNEL_ENABLED;
        } else {
            return Boolean.valueOf(value);
        }
    }

    /**
     * Get the user for ssh tunneling
     * 
     * @return The proper user.
     */
    public static String getTunnelUser() {
        final SfGlobalOptions options = SfGlobalOptionsManager.getOptions();
        final String user = options.getOption(GlobalOptionKeys.TUNNEL_USER_KEY);

        if (user == null || "".equals(user)) {
            return GlobalOptionKeys.DEFAULT_TUNNEL_USER;
        } else {
            return user;
        }
    }
}
