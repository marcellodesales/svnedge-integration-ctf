/*
 * CollabNet TeamForge
 * Copyright 2010 CollabNet, Inc.  All rights reserved.
 * http://www.collab.net
 */

package com.vasoftware.sf.externalintegration.adapters.ccdaemon;

import java.io.File;

import com.vasoftware.sf.common.SfSystemException;
import com.vasoftware.sf.common.logger.Logger;
import com.vasoftware.sf.externalintegration.IntegrationFault;
import com.vasoftware.sf.externalintegration.ObjectAlreadyExistsFault;
import com.vasoftware.sf.externalintegration.adapters.SynchronizedScmServerDaemon;
import com.vasoftware.sf.externalintegration.execution.CommandWrapperFault;

/**
 * The <code>ClearcaseScmServerDaemon</code> provides a simple implementation for the SCM adapter for Clearcase
 * integration.
 */

public class ClearcaseScmServerDaemon extends SynchronizedScmServerDaemon {
    private static final Logger smLogger = Logger.getLogger(ClearcaseScmServerDaemon.class);

    private final ClearcaseWrapper mClearcase;
    public static final String SUBVERSION_USER = getClearcaseUser();
    public static final String SUBVERSION_GROUP = getClearcaseGroup();

    /**
     * Constructor. Instantiates the OS command executor.
     * 
     * @throws IntegrationFault
     *             Thrown if getting the command executor fails.
     */

    public ClearcaseScmServerDaemon() throws IntegrationFault {
        super();
        mClearcase = new ClearcaseWrapper(getCommandExecutor());
    }

    /**
     * Get the user that the daemon should use for Clearcase directory ownership. This function is not used for
     * clearcase.
     * 
     * @return The proper user.
     */
    private static String getClearcaseUser() {
        if (smLogger.isDebugEnabled()) {
            smLogger.debug("getting the cc user - this is a  noop for clearcase");
        }

        return "nobody";
    }

    /**
     * @see com.vasoftware.sf.externalintegration.adapters.SynchronizedScmServerDaemon#setAccessList(java.lang.String[], java.lang.String)
     */
    @Override
    public String[] setAccessList(final String[] usernames, final String groupName) throws IntegrationFault {
        if (smLogger.isDebugEnabled()) {
            smLogger.debug("setting users - this is a  noop for clearcase");
        }

        final String[] users = new String[0];

        return (users);
    }

    /**
     * @see com.vasoftware.sf.externalintegration.adapters.SynchronizedScmServerDaemon#setPassword(java.lang.String, java.lang.String, java.lang.String)
     */
    @Override
    public void setPassword(final String username, final String password, final String cryptedPassword)
                                                                                                       throws IntegrationFault {
        if (smLogger.isDebugEnabled()) {
            smLogger.debug("setting password - noop for clearcase");
        }
    }

    /**
     * @see com.vasoftware.sf.externalintegration.adapters.SynchronizedScmServerDaemon#listGroupMembers(java.lang.String)
     */
    @Override
    public String[] listGroupMembers(final String groupName) throws IntegrationFault {

        if (smLogger.isDebugEnabled()) {
            smLogger.debug("retrieving group members - noop for clearcase");
        }

        final String[] users = new String[0];

        return (users);
    }

    /**
     * @see com.vasoftware.sf.externalintegration.adapters.SynchronizedScmServerDaemon#removeUsersFromAccessGroup(java.lang.String[], java.lang.String)
     */
    @Override
    public void removeUsersFromAccessGroup(final String[] usernames, final String groupName) throws IntegrationFault {
        if (smLogger.isDebugEnabled()) {
            smLogger.debug("removing users - noop for clearcase");
        }
    }

    /**
     * @see com.vasoftware.sf.externalintegration.adapters.SynchronizedScmServerDaemon#addUsersToAccessGroup(java.lang.String[], java.lang.String)
     */
    @Override
    public String[] addUsersToAccessGroup(final String[] usernames, final String groupName) throws IntegrationFault {
        if (smLogger.isDebugEnabled()) {
            smLogger.debug("adding users - noop for clearcase");
        }
        final String[] users = new String[0];

        return (users);
    }

    /**
     * Get the user that the daemon should use for Clearcase directory ownership.
     * 
     * @return The proper user.
     */
    private static String getClearcaseGroup() {
        if (smLogger.isDebugEnabled()) {
            smLogger.debug("getting group - noop for clearcase");
        }
        return "nobody";
    }

    /**
     * @see com.vasoftware.sf.externalintegration.adapters.ScmDaemon#createRepository(java.lang.String, java.lang.String, java.lang.String, java.lang.String)
     */
    public String createRepository(final String repositoryGroup, final String repositoryDir, final String systemId,
                                   final String repositoryBaseUrl) throws IntegrationFault, ObjectAlreadyExistsFault {

        final File qualifiedDirFile = new File(repositoryDir);

        // Verify the path is available
        if (!getCommandExecutor().pathExists(qualifiedDirFile)) {
            smLogger.info("view " + qualifiedDirFile.getAbsolutePath() + "' does not exist - failing.");
            throw new SfSystemException(qualifiedDirFile.getAbsolutePath() + " does not exist");
        }

        // TODO: Some type of more intelligent directory checking to verify that
        // what is supplied is actually a view

        return qualifiedDirFile.getAbsolutePath();
    }

    /**
     * @see com.vasoftware.sf.externalintegration.adapters.SynchronizedScmServerDaemon#setupRepository(java.lang.String, java.lang.String, java.lang.String)
     */
    @Override
    public void setupRepository(final String systemId, final String repositoryId, final String repositoryDir)
                                                                                                             throws IntegrationFault {

        if (smLogger.isDebugEnabled()) {
            smLogger.debug("setup repository - noop for clearcase");
        }
    }

    /**
     * @see com.vasoftware.sf.externalintegration.adapters.ScmScmServerDaemon#initializeExternalSystem(java.lang.String)
     */
    @Override
    public void initializeExternalSystem(final String systemId) throws IntegrationFault {
        super.initializeExternalSystem(systemId);
        initializeExternalSystemId(systemId);
    }

    /**
     * @see com.vasoftware.sf.externalintegration.adapters.ScmScmServerDaemon#deleteExternalSystem(java.lang.String)
     */
    @Override
    public void deleteExternalSystem(final String systemId) throws IntegrationFault {
        super.deleteExternalSystem(systemId);
        deleteExternalSystemId();
    }

    /**
     * @see com.vasoftware.sf.externalintegration.adapters.ScmDaemon#verifyExternalSystem(java.lang.String)
     */
    public void verifyExternalSystem(final String adapterType) throws IntegrationFault {
        if (!"Clearcase".equals(adapterType)) {
            throw new IntegrationFault("Unsupported adapter type: " + adapterType + ". Supported Types are: Clearcase");
        }

        // TODO: figure out some way to validate that clearcase is installed
        // int[] version;
        // try {
        // version = mClearcase.getVersion();
        // } catch (IntegrationFault commandWrapperFault) {
        // throw new IntegrationFault(commandWrapperFault);
        // }

        if (smLogger.isDebugEnabled()) {
            smLogger.debug("External System valid: " + adapterType);
        }
    }

    /**
     * @see super#setRepositoryAccessLevel(String, String, String)
     */
    public void setRepositoryAccessLevel(final String repositoryDir, final String repositoryId, final String level)
                                                                                                                   throws IntegrationFault {
        // do nothing here
    }

    /**
     * @see super#verifyPath(String,String,String,String)
     */
    public void verifyPath(final String externalBlackduckProjectId, final String repositoryId,
                           final String repositoryPath, final String repositoryPathFromRoot) throws IntegrationFault {
        try {
            final boolean valid = mClearcase.verifyPath(repositoryPath, repositoryPathFromRoot);
            if (!valid) {
                if (smLogger.isDebugEnabled()) {
                    smLogger.debug("Clearcase path not present " + repositoryPath + " with " + repositoryPathFromRoot);
                }
                throw new IntegrationFault(INVALID_PATH);
            } else {
                if (smLogger.isDebugEnabled()) {
                    smLogger.debug("Clearcase path present " + repositoryPath + " with " + repositoryPathFromRoot);
                }
            }
        } catch (final CommandWrapperFault commandWrapperFault) {
            smLogger.error("Error verifying Clearcase path at " + repositoryPath + " with " + repositoryPathFromRoot,
                           commandWrapperFault);
            throw new IntegrationFault(commandWrapperFault);
        }
    }

    /**
     * @see super#checkoutRepository(String, String, File)
     */
    public void checkoutRepository(final String repositoryPath, final String repositoryPathFromRoot,
                                   final File destinationDirectory) throws IntegrationFault {
    }

    /**
     * @see com.vasoftware.sf.externalintegration.adapters.ScmScmServerDaemon#getDaemonType()
     */
    @Override
    public String getDaemonType() {
        return "clearcase";
    }
}
