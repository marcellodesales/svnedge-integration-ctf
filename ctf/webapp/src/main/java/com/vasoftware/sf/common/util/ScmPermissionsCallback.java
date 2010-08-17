/*
 * CollabNet TeamForge
 * Copyright 2010 CollabNet, Inc.  All rights reserved.
 * http://www.collab.net
 */

package com.vasoftware.sf.common.util;

/**
 * Callback interface for consumers of PBP needing to retrieve, and cache, a user's scm permissions.
 * 
 * @since 5.2
 */
public interface ScmPermissionsCallback {
    /**
     * Retrieves the user's scm permissions.
     * 
     * @param username
     *            The username.
     * @param systemId
     *            The system id.
     * @param repositoryName
     *            The repository name.
     * 
     * @return permissions A string array of permissions. (Each entry is in the form of perm:path.)
     */
    public String[] retrieveScmPermissions(String username, String systemId, String repositoryName);
}