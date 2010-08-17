/*
 * CollabNet TeamForge
 * Copyright 2010 CollabNet, Inc.  All rights reserved.
 * http://www.collab.net
 */

package com.vasoftware.sf.common.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.vasoftware.sf.common.logger.Logger;
import com.vasoftware.sf.externalintegration.RBACConstants;

/**
 * The <code>ScmPermissionsUtil</code> class provides utility methods for dealing with Scms. (This class is a
 * thread-safe, lazily-loaded singleton.
 * 
 * @since 5.2
 */
public class ScmPermissionsUtil {
    private final long mCacheDuration = (1000 * 60 * 5); // 5 minute cache duration
    private final long mJanitorSleepDuration = (1000 * 60); // CacheJanitor runs every minute
    private boolean mCacheEnabled = true;

    private Map<String, Long> mCacheTimestamps;
    private Map<String, String[]> mPermissionCache;

    /** Logger for this class. */
    private static final Logger smLogger = Logger.getLogger(ScmPermissionsUtil.class);

    /**
     * Protected constructor.
     */
    protected ScmPermissionsUtil() {
        initCache();

        if (EnvironmentUtil.getInstance().isSandboxEnvironment()) {
            mCacheEnabled = false;
        } else {
            (new CacheJanitorThread()).start();
        }
    }

    /**
     * <code>SingletonHolder</code> is loaded on the first execution of
     * 
     * @see ScmPermissionsUtil#getInstance() or the first access to
     * @see SingletonHolder#INSTANCE, not before.
     */
    private static class SingletonHolder {
        private static final ScmPermissionsUtil INSTANCE = new ScmPermissionsUtil();
    }

    /**
     * Returns a thread-safe <code>ScmPermissionsUtil</code>.
     * 
     * @return the thread-safe, ScmPermissionsUtil singleton.
     */
    public static ScmPermissionsUtil getInstance() {
        return SingletonHolder.INSTANCE;
    }

    /**
     * @see #hasPermission(String, String, String[])
     * 
     *      Just like <code>#hasPermission(String, String, String[])</code> but this method will use the callback to
     *      retrieve the permissions before checking permission.
     * 
     *      This method will use an internal cache to help performance so that the callback is only used when necessary.
     * 
     * @param username
     *            the username being authorized (Used in the cache key)
     * @param systemId
     *            the external system id the repository belongs to (Used in the cache key)
     * @param repoPath
     *            the path attempting to be accessed (Example: REPO_NAME/REPO/PATH)
     * @param accessType
     *            the type of access requested (none, view, commit)
     * @param callback
     *            the implementation of <code>ScmPermissionsCallback</code> to retrieve the permissions
     * 
     * @return a boolean of true if the requested permission is allowed or false otherwise
     */
    public boolean hasPermission(final String username, final String systemId, final String repoPath,
                                 final String accessType, final ScmPermissionsCallback callback) {
        final String repoName = parseSubversionUrl(repoPath)[0];
        final String cacheKey = username + ":" + systemId + ":" + repoName;
        String[] perms = null;

        if (callback != null) {
            Long cacheKeyExpiration = null;
            if (mCacheEnabled) {
                cacheKeyExpiration = mCacheTimestamps.get(cacheKey);
            }

            // Check cache before using the callback
            if (mCacheEnabled && cacheKeyExpiration != null && cacheKeyExpiration > System.currentTimeMillis()) {
                perms = mPermissionCache.get(cacheKey);
            } else {
                perms = callback.retrieveScmPermissions(username, systemId, repoName);

                // Cache the permission if necessary
                if (mCacheEnabled) {
                    mPermissionCache.put(cacheKey, perms);
                    mCacheTimestamps.put(cacheKey, System.currentTimeMillis() + mCacheDuration);
                }
            }
        }

        return hasPermission(repoPath, accessType, perms);
    }

    /**
     * Returns whether or not the requested access for the given path is permissible.
     * 
     * @param repoPath
     *            the path attempting to be accessed (Example: REPO_NAME/REPO/PATH)
     * @param accessType
     *            the type of access requested (none, view, commit)
     * @param perms
     *            the permissions model
     * 
     *            (Note: accessType can have a modifier appended to the end to signify that the permissions check should
     *            handle special cases, like checking to see if the requested access is available for any location in
     *            the repository for example.)
     * 
     * @return a boolean of true if the requested permission is allowed or false otherwise
     */
    public boolean hasPermission(final String repoPath, final String accessType, final String[] perms) {
        boolean hasAccess = false;
        final boolean hasAccessTypeModifier = (getAccessTypeModifier(accessType) == null ? false : true);
        final Map<String, String> permMap = new HashMap<String, String>();
        final String[] repoPathParts = parseSubversionUrl(repoPath);

        if (perms == null) {
            return hasAccess; // Shouldn't happen but could so might as well check
        }

        /* Create a map of paths for easier traversal */
        for (int i = 0; i < perms.length; i++) {
            final String[] parts = perms[i].split(":");
            final String type = parts[0];
            final String path = parts[1];

            permMap.put(path, type);
        }

        if (hasAccessTypeModifier) {
            hasAccess = hasPermissionBasedOnAccessTypeModifier(repoPathParts[1], accessType, permMap);
        } else if (!accessType.equals(RBACConstants.ROLE_PATH_TYPE_NONE)) {
            hasAccess = hasRequestedAccessAtPath(repoPathParts[1], accessType, permMap);
        }

        return hasAccess;
    }

    /**
     * Clears the permissions cache.
     */
    public void clearCache() {
        initCache();
    }

    /**
     * Initializes the cache.
     */
    private void initCache() {
        mCacheTimestamps = new ConcurrentHashMap<String, Long>();
        mPermissionCache = new ConcurrentHashMap<String, String[]>();
    }

    /**
     * Takes an access type and returns its value as an int.
     * 
     * @param accessType
     *            the access type to convert to an int
     * 
     * @return int the access type's integer value
     */
    private int accessTypeToInt(final String accessType) {
        int perm = 0;

        if (accessType.equals(RBACConstants.ROLE_PATH_TYPE_VIEW)) {
            perm = 1;
        } else if (accessType.equals(RBACConstants.ROLE_PATH_TYPE_COMMIT)) {
            perm = 2;
        }

        return perm;
    }

    /**
     * Return the access type modifier or <code>null</code> if there is none or the access type modifier is unknown.
     * 
     * @param accessType
     *            the type of access requested (none, view, commit)
     * 
     * @return String either the access type modifier or null
     */
    public String getAccessTypeModifier(final String accessType) {
        /* Check to see if there is an access type modifier in place, that we know about */
        String accessTypeModifier = null;

        if (accessType.indexOf("-") > -1) {
            final String[] accessTypeParts = accessType.split("-");

            if (accessTypeParts.length == 2
                    && RBACConstants.ROLE_PATH_SUPPORTED_TYPE_MODIFIERS.contains(accessTypeParts[1])) {
                accessTypeModifier = accessTypeParts[1];
            }
        }

        return accessTypeModifier;
    }

    /**
     * Checks a user's permission based on an access type with a modifier.
     * 
     * @param repoPath
     *            the path attempting to be accessed (Example: REPO/PATH)
     * @param accessType
     *            the type of access requested with a known modifier at the end
     * @param permMap
     *            the map of permissions
     * 
     * @return boolean whether or not access is granted
     */
    private boolean hasPermissionBasedOnAccessTypeModifier(final String repoPath, final String accessType,
                                                           final Map<String, String> permMap) {
        final String accessTypeModifier = getAccessTypeModifier(accessType);
        final String actualAccessType = accessType.split("-")[0];
        boolean hasAccess = false;

        if (accessTypeModifier.equals(RBACConstants.ROLE_PATH_TYPE_MODIFIER_ALL)) {
            hasAccess = hasRequestedAccessEverywhereAtAndBelowPath(repoPath, actualAccessType, permMap);
        } else if (accessTypeModifier.equals(RBACConstants.ROLE_PATH_TYPE_MODIFIER_ANY)) {
            hasAccess = hasRequestedAccessAnywhereAtOrBelowPath(repoPath, actualAccessType, permMap);
        } else {
            throw new RuntimeException(accessTypeModifier + " is a known access type modifier"
                    + "but does not have an implemented handler.");
        }

        return hasAccess;
    }

    /**
     * Checks whether the requested access is available at a path.
     * 
     * @param repoPath
     *            the path attempting to be accessed without the repository name (Example: REPO/PATH)
     * @param accessType
     *            the type of access requested with a known modifier at the end
     * @param permMap
     *            the map of permissions
     * 
     * @return boolean whether or not access is granted
     */
    private boolean hasRequestedAccessAtPath(final String repoPath, final String accessType,
                                             final Map<String, String> permMap) {
        final String[] pathParts = repoPath.split("/");
        boolean hasAccess = false;
        final int requested = accessTypeToInt(accessType);

        /* Perform access check */
        if (pathParts.length == 0) {
            final String perm = permMap.get("/");
            final int given = (perm == null ? -1 : accessTypeToInt(perm));

            /* commit > view > none */
            if (given >= requested) {
                hasAccess = true;
            }
        } else {
            for (int i = pathParts.length - 1; i >= 0; i--) {
                final StringBuilder sb = new StringBuilder("/");

                for (int j = 1; j <= i; j++) {
                    sb.append(pathParts[j]);

                    /* Append a trailing slash if it is not the last url path segment */
                    if (j != i) {
                        sb.append("/");
                    }
                }

                if (permMap.containsKey(sb.toString())) {
                    final String perm = permMap.get(sb.toString());
                    final int given = accessTypeToInt(perm);

                    /* commit > view > none */
                    if (given >= requested) {
                        hasAccess = true;
                    }

                    break; // No longer need to check, path found.
                }
            }
        }

        return hasAccess;
    }

    /**
     * Checks whether the requested access is available at a path or anywhere below it.
     * 
     * @param repoPath
     *            the path attempting to be accessed without the repository name (Example: REPO/PATH)
     * @param accessType
     *            the type of access requested with a known modifier at the end
     * @param permMap
     *            the map of permissions
     * 
     * @return boolean whether or not access is granted
     */
    private boolean hasRequestedAccessAnywhereAtOrBelowPath(final String repoPath, final String accessType,
                                                            final Map<String, String> permMap) {
        final int requested = accessTypeToInt(accessType);
        boolean hasAccess = hasRequestedAccessAtPath(repoPath, accessType, permMap);

        /* Only check for child access if the requested access isn't already available */
        if (!hasAccess) {
            for (final String path : permMap.keySet()) {
                if (path.startsWith(repoPath)) {
                    final int given = accessTypeToInt(permMap.get(path));

                    /* commit > view > none */
                    if (given >= requested) {
                        hasAccess = true;

                        /* Access has been given, stop checking */
                        break;
                    }
                }
            }
        }

        return hasAccess;
    }

    /**
     * Checks whether the requested access is available at a path and everywhere below it.
     * 
     * @param repoPath
     *            the path attempting to be accessed without the repository name (Example: REPO/PATH)
     * @param accessType
     *            the type of access requested with a known modifier at the end
     * @param permMap
     *            the map of permissions
     * 
     * @return boolean whether or not access is granted
     */
    private boolean hasRequestedAccessEverywhereAtAndBelowPath(final String repoPath, final String accessType,
                                                               final Map<String, String> permMap) {
        final int requested = accessTypeToInt(accessType);
        boolean hasAccess = hasRequestedAccessAtPath(repoPath, accessType, permMap);

        /* There was no access to start with. No need to validate that all lower paths have access */
        if (hasAccess) {
            for (final String path : permMap.keySet()) {
                if (path.startsWith(repoPath)) {
                    final int given = accessTypeToInt(permMap.get(path));

                    /* commit > view > none */
                    if (given < requested) {
                        hasAccess = false;

                        /* Access has been taken away, stop checking */
                        break;
                    }
                }
            }
        }

        return hasAccess;
    }

    /**
     * Parses a Subversion url into the repository name and repository path.
     * 
     * @param url
     *            the Subversion in the format of "REPO_NAME/REPO/PATH"
     * 
     * @return String[] array containing the repository name at index 0 and the repository path at index 1
     */
    private String[] parseSubversionUrl(final String url) {
        final String[] urlParts = url.split("/");
        final String repoName = urlParts[0];
        String repoPath = "";

        if (urlParts.length > 1) {
            for (int i = 1; i < urlParts.length; i++) {
                repoPath += ("/" + urlParts[i]);
            }
        } else {
            repoPath = "/";
        }

        return new String[] { repoName, repoPath };
    }

    /**
     * Janitor thread that handles cleaning up the permission cached items in order to offload each request from having
     * to play this role. Since each request will attempt to clean up for itself this doesn't have to run too often so
     * it's going to run every minute.
     */
    private class CacheJanitorThread extends Thread {

        /**
         * core for the Janitor
         */
        @Override
        public void run() {
            while (true) {
                if (mCacheEnabled) {
                    if (smLogger.isDebugEnabled()) {
                        smLogger.debug("[ScmPermissionsUtil] Cache Janitor STARTING mCacheTimestamps.size() = "
                                + mCacheTimestamps.size());
                    }

                    // Remove stale cache entries
                    final List<String> staleKeys = new ArrayList<String>();
                    final long oldestValidTimestamp = System.currentTimeMillis();

                    // Get a list of stale cache keys
                    for (final String key : mCacheTimestamps.keySet()) {
                        if (mCacheTimestamps.get(key) < oldestValidTimestamp) {
                            staleKeys.add(key);
                        }
                    }

                    // Remove the bad cache entries
                    for (final String key : staleKeys) {
                        mPermissionCache.remove(key);
                        mCacheTimestamps.remove(key);
                    }

                    if (smLogger.isDebugEnabled()) {
                        smLogger.debug("[ScmPermissionsUtil] Cache Janitor is DONE. " + "mCacheTimestamps.size() = "
                                + mCacheTimestamps.size() + "Cleaning took: "
                                + (System.currentTimeMillis() - oldestValidTimestamp) + " milliseconds.");
                    }
                }

                try {
                    Thread.sleep(mJanitorSleepDuration);
                } catch (final InterruptedException e) {
                    if (smLogger.isDebugEnabled()) {
                        smLogger
                                .debug("[ScmPermissionsUtil] Janitor thread's sleep was " + "interrupted unexpectedly.");
                    }
                }
            }
        }
    }
}
