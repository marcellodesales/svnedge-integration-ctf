/*
 * CollabNet TeamForge
 * Copyright 2010 CollabNet, Inc.  All rights reserved.
 * http://www.collab.net
 */

package com.vasoftware.sf.externalintegration;

import java.util.HashSet;
import java.util.Set;

/**
 * The <code>RBACConstants</code> class provides constants related to RBAC.
 */
public class RBACConstants {
    /* Constant representing no default user class (RBAC controlled access) */
    public static final Integer DEFAULT_USER_CLASS_NONE = new Integer(0);

    /* Constant representing All users, logged in or otherwise. */
    public static final Integer DEFAULT_USER_CLASS_ALL = new Integer(1);

    /* Constant representing all Authenticated (logged in) users. */
    public static final Integer DEFAULT_USER_CLASS_AUTHENTICATED = new Integer(2);

    /* Constant representing all Unrestricted (logged in with an additionalflag) users. */
    public static final Integer DEFAULT_USER_CLASS_UNRESTRICTED = new Integer(3);

    /* Constant representing all Members of the current or nearest Project. */
    public static final Integer DEFAULT_USER_CLASS_MEMBER = new Integer(4);

    /* Define the supported role path types */
    public static final String ROLE_PATH_TYPE_NONE = "none";
    public static final String ROLE_PATH_TYPE_VIEW = "view";
    public static final String ROLE_PATH_TYPE_COMMIT = "commit";
    public static final Set<String> ROLE_PATH_SUPPORTED_TYPES = new HashSet<String>();

    static {
        ROLE_PATH_SUPPORTED_TYPES.add(ROLE_PATH_TYPE_NONE);
        ROLE_PATH_SUPPORTED_TYPES.add(ROLE_PATH_TYPE_VIEW);
        ROLE_PATH_SUPPORTED_TYPES.add(ROLE_PATH_TYPE_COMMIT);
    }

    /* Define the supported role path type modifiers */
    public static final String ROLE_PATH_TYPE_MODIFIER_ALL = "all";
    public static final String ROLE_PATH_TYPE_MODIFIER_ANY = "any";
    public static final Set<String> ROLE_PATH_SUPPORTED_TYPE_MODIFIERS = new HashSet<String>();

    static {
        ROLE_PATH_SUPPORTED_TYPE_MODIFIERS.add(ROLE_PATH_TYPE_MODIFIER_ALL);
        ROLE_PATH_SUPPORTED_TYPE_MODIFIERS.add(ROLE_PATH_TYPE_MODIFIER_ANY);
    }
}
