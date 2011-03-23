/*
 * CollabNet TeamForge
 * Copyright 2010 CollabNet, Inc.  All rights reserved.
 * http://www.collab.net
 */

package com.vasoftware.sf.common.configuration;

import java.io.File;

/**
 * The <code>GlobalOptionKeys</code> class provides all keys for global configuration, sourceforge.properties.
 * 
 * TODO: Document each key
 */
public class GlobalOptionKeys {
    private static final boolean IS_SUSE = new File("/etc/SuSE-release").exists();

    /* Apache related keys */
    public static final String DEFAULT_HTTPD_GROUP = IS_SUSE ? "www" : "apache";
    public static final String DEFAULT_HTTPD_USER = IS_SUSE ? "wwwrun" : "apache";
    public static final String HTTPD_GROUP_KEY = "httpd.group";
    public static final String HTTPD_USER_KEY = "httpd.user";

    /* Application server related keys */
    public static final String APP_USER_KEY = "app.user";
    public static final String DEFAULT_APP_USER = "sf-admin";
    public static final String WEBSERVER_ROOT_URL = "webserver.root-url";

    /* Branding related keys */
    public static final String SCM_BRANDING_REPO = "scm.branding.repo";

    /* CVS LDAP related keys */
    public static final String CVS_LDAP_BIND_DN = "sfmain.ldap.bind.dn";
    public static final String CVS_LDAP_BIND_PASSWORD = "sfmain.ldap.bind.password";
    public static final String CVS_LDAP_GROUPS_DN = "sfmain.ldap.groups.container";
    public static final String CVS_LDAP_GROUPS_STARTID = "sfmain.ldap.groups.startid";
    public static final String CVS_LDAP_HOST = "sfmain.ldap.host";
    public static final String CVS_LDAP_PORT = "sfmain.ldap.port";
    public static final String CVS_LDAP_USERS_DN = "sfmain.ldap.users.container";
    public static final String CVS_LDAP_USERS_STARTID = "sfmain.ldap.users.startid";

    /* Integration executables related keys */
    public static final String SFMAIN_INTEGRATION_EXECUTABLES_BDSTOOL = "sfmain.integration.executables.bdstool";
    public static final String SFMAIN_INTEGRATION_EXECUTABLES_CVS = "sfmain.integration.executables.cvs";
    public static final String SFMAIN_INTEGRATION_EXECUTABLES_CVSRELAY = "sfmain.integration.executables.cvsrelay";
    public static final String SFMAIN_INTEGRATION_EXECUTABLES_SUBVERSION = "sfmain.integration.executables.subversion";
    public static final String SFMAIN_INTEGRATION_EXECUTABLES_SUBVERSION_ADMIN = "sfmain.integration.executables.subversion_admin";
    public static final String SFMAIN_INTEGRATION_EXECUTABLES_SUBVERSION_LOOK = "sfmain.integration.executables.subversion_look";

    /* Integration listener related keys */
    public static final String SFMAIN_INTEGRATION_LISTENER_HOST = "sfmain.integration.listener_host";
    public static final String SFMAIN_INTEGRATION_LISTENER_PORT = "sfmain.integration.listener_port";
    public static final String SFMAIN_INTEGRATION_LISTENER_SSL = "sfmain.integration.listener_ssl";

    /* Integration miscellaenous related keys */
    public static final String SFMAIN_INTEGRATION_OS = "sfmain.integration.os";
    public static final String SFMAIN_INTEGRATION_REPOSITORY_ARCHIVE_ROOT = "sfmain.integration.repository-archive";
    public static final String SFMAIN_INTEGRATION_SCRIPTS_ROOT = "sfmain.integration.scripts-root";
    public static final String SFMAIN_INTEGRATION_SECURITY_CHECK_TIMESTAMP = "sfmain.integration.security.check_timestamp";
    public static final String SFMAIN_INTEGRATION_SECURITY_SHARED_SECRET = "sfmain.integration.security.shared_secret";
    public static final String SFMAIN_INTEGRATION_SUBVERSION_FS_TYPE = "sfmain.integration.subversion.fstype";
    public static final String SFMAIN_INTEGRATION_USER_GROUP = "sfmain.integration.user_group";
    public static final String SFMAIN_INTEGRATION_USER_HOME_DIRECTORY = "sfmain.integration.user_home";
    public static final String SFMAIN_INTEGRATION_USER_SHELL_ACTIVE = "sfmain.integration.user_shell.active";
    public static final String SFMAIN_INTEGRATION_USER_SHELL_DISABLED = "sfmain.integration.user_shell.disabled";
    public static final String SFMAIN_INTEGRATION_VIEWVC_PATH = "sfmain.integration.viewvc.path";
    public static final String SFMAIN_SOAP_API_SERVER_URL = "sfmain.soap.api_server.url";
    public static final String SFMAIN_SOURCEFORGE_HOME = "sfmain.sourceforge_home";
    public static final String SFMAIN_TEMP_DIRECTORY = "sfmain.tempdir";

    /* Perforce related keys */
    public static final String PERFORCE_PORT = "sfmain.integration.perforce.port";
    public static final String SFMAIN_PERFORCE_CLIENT = "sfmain.integration.executables.perforce";

    /* SSH tunnel related keys */
    public static final String TUNNEL_USER_KEY = "tunnel.user";
    public static final String TUNNEL_ENABLED_KEY = "tunnel.enabled";
    public static final Boolean DEFAULT_TUNNEL_ENABLED = Boolean.FALSE;
    public static final String DEFAULT_TUNNEL_USER = "tunnel";

    /* WANDisco related keys */
    public static final String SFMAIN_WANDISCO_CVS_PREFIX = "sfmain.integration.wandisco.cvs";
    public static final String SFMAIN_WANDISCO_SUBVERSION_PREFIX = "sfmain.integration.wandisco.subversion";

    /** Key to specify the Display Timezone */
    public static final String DISPLAY_TIMEZONE = "ctf.displayTimezone";

    /* CSVN (Edge) related keys */
    public static final String SFMAIN_INTEGRATION_CSVN_MODE = "sfmain.integration.subversion.csvn";
}
