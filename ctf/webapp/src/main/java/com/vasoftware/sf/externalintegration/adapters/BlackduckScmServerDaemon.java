/*
 * $RCSfile: BlackduckScmServerDaemon.java,v $
 *
 * SourceForge(r) Enterprise Edition
 * Copyright 2007 CollabNet, Inc.  All rights reserved.
 * http://www.collab.net
 */

package com.vasoftware.sf.externalintegration.adapters;

import java.io.File;

import com.vasoftware.sf.common.configuration.GlobalOptionKeys;
import com.vasoftware.sf.common.configuration.SfGlobalOptions;
import com.vasoftware.sf.common.configuration.SfGlobalOptionsManager;
import com.vasoftware.sf.common.logger.Logger;
import com.vasoftware.sf.externalintegration.IntegrationFault;
import com.vasoftware.sf.externalintegration.execution.CommandExecutor;
import com.vasoftware.sf.externalintegration.execution.CommandExecutorException;
import com.vasoftware.sf.externalintegration.execution.CommandWrapperFault;
import com.vasoftware.sf.externalintegration.execution.ExecutionUtil;

/**
 * Implementation of Blackduck analysis support for any SCM daemon.
 * 
 * Note that to keep code shims to minimum, SCM daemons don't forward their BlackduckScmSupport method calls to instance
 * of this class, but rather inherit from it, so *this* class implements BlackduckScmSupport interface of them. They
 * still maintain complete separation from this class - so, all member variables here must be private, and the only
 * method which is allowed to be called (and must be called) is setScmToProcess() (plus constructor, obviously).
 * Consider that once that's done, this class automagically intercepts calls to BlackduckScmSupport methods of that
 * object.
 * 
 * Don't call this hack, call this pattern. And believe me, it is better than what was here before.
 * 
 * @author Paul Sokolovsky <psokolovsky@vasoftware.com>
 * @version $Revision: 1.3 $ $Date: 2007/05/24 00:37:28 $
 */
public class BlackduckScmServerDaemon implements BlackduckScmSupport {
    private static final Logger smLogger = Logger.getLogger(BlackduckScmServerDaemon.class);

    protected CommandExecutor mExecutor;

    private ScmDaemon mCagedDaemon;

    /**
     * Constructor
     * 
     * @throws IntegrationFault
     *             thrown if there was a problem getting the command executor.
     */
    public BlackduckScmServerDaemon() throws IntegrationFault {
        try {
            mExecutor = ExecutionUtil.getCommandExecutor();
        } catch (final CommandExecutorException e) {
            throw new IntegrationFault(e);
        }
    }

    /**
     * Get the command executor, to execute system commands with
     * 
     * @return The command executor
     */
    protected CommandExecutor getCommandExecutor() {
        return mExecutor;
    }

    /**
     * Set SCM daemon for the repository for Blackduck processing to act on.
     * 
     * @param scmDaemon
     *            SCM Daemon
     */
    public void setScmToProcess(final ScmDaemon scmDaemon) {
        mCagedDaemon = scmDaemon;
    }

    /**
     * Begin a blackduck analysis on the specified repository and report the results back for processing
     * 
     * @param hostName
     *            hostname of the scm server
     * @param port
     *            port for the scm server
     * @param username
     *            username to log in as
     * @param password
     *            password for the username
     * @param blackduckRepositoryId
     *            The id of the blackduck repository
     * @param externalBlackduckProjectId
     *            the id blackduck project
     * @param repositoryPath
     *            The base path of the repository
     * @param repositoryPathFromRoot
     *            The relative path of the code to be analyzed
     * @throws com.vasoftware.sf.externalintegration.IntegrationFault
     *             if the operation causes an error
     */
    public void beginBlackduckAnalysis(final String hostName, final int port, final String username,
                                       final String password, final String blackduckRepositoryId,
                                       final String externalBlackduckProjectId, final String repositoryPath,
                                       final String repositoryPathFromRoot) throws IntegrationFault {
        // start a new thread so that control can be returned immediately
        final BlackduckAnalysisManager manager = BlackduckAnalysisManager.getManager();
        try {
            manager.beginBlackduckAnalysis(mCagedDaemon, hostName, port, username, password, blackduckRepositoryId,
                                           externalBlackduckProjectId, repositoryPath, repositoryPathFromRoot);
        } catch (final CommandExecutorException e) {
            throw new IntegrationFault(e);
        }
    }

    /**
     * Cancel a blackduck analysis
     * 
     * @param externalBlackduckProjectId
     *            The analysis to cancel
     */
    public void cancelBlackduckAnalysis(final String externalBlackduckProjectId) {
        final BlackduckAnalysisManager manager = BlackduckAnalysisManager.getManager();
        manager.cancel(externalBlackduckProjectId);
    }

    /**
     * Get the blackduck analysis status
     * 
     * @param externalBlackduckProjectId
     *            The analysis to get the status of
     * @return the status
     */
    public String getBlackduckAnalysisStatus(final String externalBlackduckProjectId) {
        final BlackduckAnalysisManager manager = BlackduckAnalysisManager.getManager();
        return manager.getStatus(externalBlackduckProjectId);
    }

    /**
     * Cleanup the checked out repositories and blackduck files
     * 
     * @param externalBlackduckProjectId
     *            The blackduck project id
     * @throws com.vasoftware.sf.externalintegration.IntegrationFault
     *             If something goes wrong
     */
    public void cleanupBlackduckRepository(final String externalBlackduckProjectId) throws IntegrationFault {
        smLogger.debug("Cleaning up directory " + externalBlackduckProjectId);

        final File blackduckSourceRoot = getBlackduckSourceRoot();
        final File workingDirectory = new File(blackduckSourceRoot, externalBlackduckProjectId);
        try {
            mExecutor.deletePath(workingDirectory);
        } catch (final CommandWrapperFault commandWrapperFault) {
            throw new IntegrationFault(commandWrapperFault);
        }
    }

    /**
     * Check if blackduck is enabled on this scm server
     * 
     * @param hostName
     *            The hostname of the blackduck server
     * @param port
     *            The port of the blackduck server
     * @param username
     *            The username of the admin on blackduck
     * @param password
     *            The password of the admin on blackduck
     * @throws com.vasoftware.sf.externalintegration.IntegrationFault
     *             If blackduck is not enabled on this server
     */
    public void isBlackduckEnabled(final String hostName, final int port, final String username, final String password)
                                                                                                                       throws IntegrationFault {
        try {
            final BlackduckWrapper blackduck = new BlackduckWrapper(ExecutionUtil.getCommandExecutor());
            final String clientVersion = blackduck.getVersion("client.bdstool", hostName, port, username, password);

            final int firstDot = clientVersion.indexOf(".");
            int secondDot = clientVersion.indexOf(".", firstDot + 1);

            if (secondDot == -1) {
                secondDot = clientVersion.length();
            }

            final int major = Integer.parseInt(clientVersion.substring(0, firstDot));
            final int minor = Integer.parseInt(clientVersion.substring(firstDot + 1, secondDot));

            if (major < 2 || (major == 2 && minor < 2)) {
                throw new IntegrationFault("Black Duck support is provided for Black Duck version 2.2 and above");
            }

        } catch (final CommandWrapperFault commandWrapperFault) {
            throw new IntegrationFault("Error getting the blackduck client version", commandWrapperFault);
        } catch (final CommandExecutorException e) {
            throw new IntegrationFault("Error getting command executor", e);
        }
    }

    /**
     * Get Blackduck source root
     * 
     * @return File representing blackduck source root directory
     */
    protected static File getBlackduckSourceRoot() {
        final SfGlobalOptions options = SfGlobalOptionsManager.getOptions();
        final String sfhome = options.getOption(GlobalOptionKeys.SFMAIN_SOURCEFORGE_HOME);

        final File blackduckSourceRoot = new File(sfhome, BlackduckAnalysisManager.BLACKDUCK_SOURCE_ROOT);
        return blackduckSourceRoot;
    }
}
