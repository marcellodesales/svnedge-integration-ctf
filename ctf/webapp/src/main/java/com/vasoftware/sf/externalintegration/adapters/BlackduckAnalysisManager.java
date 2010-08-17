/*
 * $RCSfile $
 * SourceForge(r) Enterprise Edition
 * Copyright 2007 CollabNet, Inc.  All rights reserved.
 * http://www.collab.net
 */

package com.vasoftware.sf.externalintegration.adapters;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.vasoftware.sf.common.logger.Logger;
import com.vasoftware.sf.externalintegration.execution.CommandExecutorException;

/**
 * A singleton manager that collects threads that perform a blackduck analysis and can handle cancelling and getting the
 * status from them.
 * 
 * @author Dominick Bellizzi <dbellizzi@vasoftware.com>
 * @version $Revision: 1.5 $ $Date: 2007/05/24 00:37:28 $
 */
public class BlackduckAnalysisManager {
    /** The root directory to store the blackduck checked out sources in **/
    public static final String BLACKDUCK_SOURCE_ROOT = "blackduckSources";

    /** Started analysis **/
    public static final String STATUS_STARTED = "started";

    /** Checking out source code **/
    public static final String STATUS_CHECKOUT = "checkout";

    /** Logging in to blackduck **/
    public static final String STATUS_LOGIN = "login";

    /** Attaching sources to blackduck **/
    public static final String STATUS_ATTACH = "attach";

    /** Performing analysis **/
    public static final String STATUS_ANALYZE = "analyze";

    /** Uploading analysis **/
    public static final String STATUS_UPLOAD = "upload";

    /** Exception in processing **/
    public static final String STATUS_EXCEPTION = "exception";

    /** Not running an analysis **/
    public static final String STATUS_NOT_RUNNING = "not_running";

    private static Logger smLogger = Logger.getLogger(BlackduckAnalysisManager.class);
    private static BlackduckAnalysisManager smManager;

    private final Map<String, BlackduckAnalysis> mAnalysisMap;

    /**
     * A singleton blackduck analysis manager
     */
    public BlackduckAnalysisManager() {
        mAnalysisMap = Collections.synchronizedMap(new HashMap<String, BlackduckAnalysis>());
    }

    /**
     * Get the singleton blackduck analysis manager
     * 
     * @return The manager
     */
    public static BlackduckAnalysisManager getManager() {
        synchronized (BlackduckAnalysisManager.class) {
            if (smManager != null) {
                return smManager;
            } else {
                smManager = new BlackduckAnalysisManager();
            }
            return smManager;
        }
    }

    /**
     * Begin a blackduck analysis
     * 
     * @param daemon
     *            The Scm Daemon calling, to be used to checkout the repository
     * @param hostName
     *            The host name of the blackduck server
     * @param port
     *            The port of the blackduck server
     * @param username
     *            The username to log in to blackduck with
     * @param password
     *            The password to log in to blackduck with
     * @param blackduckRepositoryId
     *            The blackduck repository id that the analysis is linked to.
     * @param externalBlackduckProjectId
     *            The external blackduck project id to report against
     * @param repositoryPath
     *            The repository root path on disk
     * @param repositoryPathFromRoot
     *            The relative path to analyze, from the repositoryPath
     * @throws CommandExecutorException
     *             thrown if there was a problem getting the command executor.
     */
    public void beginBlackduckAnalysis(final ScmDaemon daemon, final String hostName, final int port,
                                       final String username, final String password,
                                       final String blackduckRepositoryId, final String externalBlackduckProjectId,
                                       final String repositoryPath, final String repositoryPathFromRoot)
                                                                                                        throws CommandExecutorException {
        BlackduckAnalysis analysis;

        synchronized (mAnalysisMap) {
            // Only start analysis again if one is already running
            if (mAnalysisMap.containsKey(externalBlackduckProjectId)) {
                smLogger.debug("analysis thread already running for " + externalBlackduckProjectId);
                return;
            }

            analysis = new BlackduckAnalysis(daemon, hostName, port, username, password, blackduckRepositoryId,
                                             externalBlackduckProjectId, repositoryPath, repositoryPathFromRoot);
            mAnalysisMap.put(externalBlackduckProjectId, analysis);
        }

        final Thread analysisThread = new Thread(analysis);
        analysisThread.start();
    }

    /**
     * Cancel an existing blackduck analysis
     * 
     * @param externalBlackduckProjectId
     *            The analysis to cancel
     */
    public void cancel(final String externalBlackduckProjectId) {
        smLogger.debug("Cancel called for " + externalBlackduckProjectId);

        final BlackduckAnalysis analysis = mAnalysisMap.get(externalBlackduckProjectId);

        if (analysis != null) {
            analysis.cancel();
            cleanupThread(externalBlackduckProjectId);
        } else {
            smLogger.debug("Cancel called on non running analysis for " + externalBlackduckProjectId);
        }
    }

    /**
     * Clean up the map that stores the analysis for a finished analysis
     * 
     * @param externalBlackduckProjectId
     *            The analysis to cleanup
     */
    public void cleanupThread(final String externalBlackduckProjectId) {
        smLogger.debug("Cleanup called for " + externalBlackduckProjectId);
        mAnalysisMap.remove(externalBlackduckProjectId);
    }

    /**
     * Get the status of an analysis
     * 
     * @param externalBlackduckProjectId
     *            The analysis to get the status for
     * @return The status
     */
    public String getStatus(final String externalBlackduckProjectId) {
        smLogger.debug("Status called for " + externalBlackduckProjectId);

        final BlackduckAnalysis analysis = mAnalysisMap.get(externalBlackduckProjectId);

        if (analysis != null) {
            return analysis.getStatus();
        } else {
            return STATUS_NOT_RUNNING;
        }
    }
}
