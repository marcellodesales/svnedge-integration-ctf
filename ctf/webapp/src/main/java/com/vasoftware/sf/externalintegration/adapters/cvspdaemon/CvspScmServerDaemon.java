/*
 * CollabNet TeamForge
 * Copyright 2010 CollabNet, Inc.  All rights reserved.
 * http://www.collab.net
 */
package com.vasoftware.sf.externalintegration.adapters.cvspdaemon;

import java.io.File;
import java.io.IOException;

import com.vasoftware.sf.common.logger.Logger;
import com.vasoftware.sf.common.util.TunnelKeysUtil;
import com.vasoftware.sf.externalintegration.IntegrationFault;
import com.vasoftware.sf.externalintegration.ObjectAlreadyExistsFault;
import com.vasoftware.sf.externalintegration.ScmConstants;
import com.vasoftware.sf.externalintegration.adapters.ScmScmServerDaemon;
import com.vasoftware.sf.externalintegration.adapters.cvsdaemon.CvsWrapper;
import com.vasoftware.sf.externalintegration.execution.CommandExecutor;
import com.vasoftware.sf.externalintegration.execution.CommandWrapperFault;

/**
 * The <code>CvspScmServerDaemon</code> provides a simple implementation for the SCM adapter for Cvsp integration.
 */
public class CvspScmServerDaemon extends ScmScmServerDaemon {
    private static final Logger smLogger = Logger.getLogger(CvspScmServerDaemon.class);

    private final CvsWrapper mCvs = new CvsWrapper(getCommandExecutor(), CvsWrapper.CvsType.PSERVER);

    /**
     * Constructor. Instantiates the OS command executor.
     * 
     * @throws com.vasoftware.sf.externalintegration.IntegrationFault
     *             Thrown if getting the command executor fails.
     */
    public CvspScmServerDaemon() throws IntegrationFault {
        super();
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
     * @see com.vasoftware.sf.externalintegration.adapters.ScmDaemon#createRepository(java.lang.String, java.lang.String, java.lang.String, java.lang.String)
     */
    public String createRepository(final String repositoryGroup, final String repositoryDir, final String systemId,
                                   final String repositoryBaseUrl) throws IntegrationFault, ObjectAlreadyExistsFault {

        final File repositoryDirFile = new File(repositoryDir);

        // Verify the path is available
        final CommandExecutor commandExecutor = getCommandExecutor();
        if (commandExecutor.pathExists(repositoryDirFile)) {
            smLogger.info("Directory '" + repositoryDirFile.getAbsolutePath() + "' already exists - failing.");
            throw new ObjectAlreadyExistsFault(repositoryDirFile.getAbsolutePath());
        }

        // Create the repository and set it up
        try {
            commandExecutor.createPath(repositoryDirFile);
            mCvs.doInit(repositoryDirFile);
        } catch (final CommandWrapperFault e) {
            throw new IntegrationFault(e);
        }
        setupRepository(systemId, null, repositoryDir);

        return repositoryDirFile.getAbsolutePath();
    }

    /**
     * Add all the necessary info into the specified repository
     * 
     * @param systemId
     *            The Guid of the system
     * @param repositoryGroup
     *            The group of the newly created repository (ignored for Cvsp)
     * @param repositoryDir
     *            The name of the repository, the directory under the cvsroot
     * @throws com.vasoftware.sf.externalintegration.IntegrationFault
     *             AN error while executing the commands.
     */
    public void setupRepository(final String systemId, final String repositoryGroup, final String repositoryDir)
                                                                                                                throws IntegrationFault {
        final File repositoryDirFile = new File(repositoryDir);

        try {
            final CommandExecutor executor = getCommandExecutor();
            mCvs.setupTriggers(systemId, repositoryDirFile.getAbsoluteFile());
            executor.removeGroupOtherPermissions(repositoryDirFile, true);
        } catch (final CommandWrapperFault e) {
            throw new IntegrationFault(e);
        }
    }

    /**
     * @see com.vasoftware.sf.externalintegration.adapters.ScmDaemon#verifyExternalSystem(java.lang.String)
     */
    public void verifyExternalSystem(final String adapterType) throws IntegrationFault {
        if (adapterType.indexOf("CVS") == -1) {
            throw new IntegrationFault("Unsupported adapter type: " + adapterType + ". Supported Types are: CVS");
        }

        int[] cvsVersion;
        try {
            cvsVersion = mCvs.getCvsVersion();
        } catch (final CommandWrapperFault commandWrapperFault) {
            throw new IntegrationFault(commandWrapperFault);
        }

        if (cvsVersion == null) {
            throw new IntegrationFault("No cvs version found");
        }

        final int[] expected = { 1, 11, 0 };
        for (int i = 0; i < expected.length; i++) {
            final int expectedVersion = expected[i];
            int actualVersion;
            if (i >= cvsVersion.length) {
                actualVersion = 0;
            } else {
                actualVersion = cvsVersion[i];
            }

            if (actualVersion < expectedVersion) {
                String message = "VERSION (requires 1.11.0 or better):";
                for (final int version : cvsVersion) {
                    message = message + version + ".";
                }
                throw new IntegrationFault(message);
            } else if (expectedVersion == actualVersion) {
                // this sub version matches, so let's check the next subversion
            } else {
                // we passed
                break;
            }
        }
        if (smLogger.isDebugEnabled()) {
            smLogger.debug("External System valid: " + adapterType);
        }
    }

    /**
     * @see ScmScmServerDaemon#setRepositoryAccessLevel(String,String,String)
     */
    public void setRepositoryAccessLevel(final String repositoryDir, final String repositoryId, final String level)
                                                                                                                   throws IntegrationFault {
        // do nothing here
    }

    /**
     * @see com.vasoftware.sf.externalintegration.adapters.ScmDaemon#verifyPath(String,String,String,String)
     */
    public void verifyPath(final String externalBlackduckProjectId, final String repositoryId,
                           final String repositoryPath, final String repositoryPathFromRoot) throws IntegrationFault {

        final File repositoryRoot = new File(repositoryPath);
        final File actualFile = new File(repositoryRoot, repositoryPathFromRoot);

        // CVS cannot check out from the root, a module is required, so fail if
        // the path equals the root
        if (repositoryPathFromRoot.equals(File.separator)) {
            throw new IntegrationFault(INVALID_PATH);
        }

        if (!actualFile.exists()) {
            throw new IntegrationFault(INVALID_PATH);
        }
    }

    /**
     * @see com.vasoftware.sf.externalintegration.adapters.ScmDaemon#checkoutRepository(String,String,java.io.File)
     */
    public void checkoutRepository(final String repositoryRoot, final String repositoryPathFromRoot,
                                   final File destinationDirectory) throws IntegrationFault {
        try {
            mCvs.doCheckout(repositoryRoot, repositoryPathFromRoot, destinationDirectory);
        } catch (final CommandWrapperFault f) {
            throw new IntegrationFault(f);
        }
    }

    /**
     * Create an authorized_keys file for the tunnel user.
     * 
     * @param authorizedKeys
     *            the authorized keys file contents
     * @throws IntegrationFault
     *             thrown if the operation fails.
     */
    public void createTunnelAuthorizedKeysFile(final String authorizedKeys) throws IntegrationFault {
        if (!ScmConstants.TUNNEL_ENABLED) {
            return;
        }

        try {
            final String homePath = mExecutor.getUserHomeDirectoryFromOS(ScmConstants.TUNNEL_USER);
            final File homeDir = new File(homePath);
            final File sshDir = new File(homeDir, ScmConstants.SSH_RESOURCE_DIRECTORY);
            final File authorizedKeysFile = new File(sshDir, ScmConstants.SSH_AUTHORIZED_KEYS_FILE);

            mExecutor.setUserOnPath(ScmConstants.APP_USER, sshDir); // recursive
            TunnelKeysUtil.writeAuthorizedKeysFile(authorizedKeysFile, authorizedKeys);
            mExecutor.setUserOnPath(ScmConstants.TUNNEL_USER, sshDir); // recursive

            mExecutor.setToOnlyReadWriteUserPermissions(sshDir, true);
            mExecutor.setToOnlyReadWriteExecuteUserPermissions(sshDir, false); // add
            // back
            // u+x
            // to
            // directory
        } catch (final CommandWrapperFault f) {
            throw new IntegrationFault(f);
        } catch (final IOException e) {
            throw new IntegrationFault(e);
        }
    }

    /**
     * Replace a user's keys in the tunnel user's authorized_keys file.
     * 
     * @param username
     *            user's login
     * @param keys
     *            the user's keys
     * @throws IntegrationFault
     *             thrown if the operation fails.
     */
    public void replaceEntryTunnelAuthorizedKeysFile(final String username, final String keys) throws IntegrationFault {
        if (!ScmConstants.TUNNEL_ENABLED) {
            return;
        }

        try {
            final String homePath = mExecutor.getUserHomeDirectoryFromOS(ScmConstants.TUNNEL_USER);
            final File homeDir = new File(homePath);
            final File sshDir = new File(homeDir, ScmConstants.SSH_RESOURCE_DIRECTORY);
            final File authorizedKeysFile = new File(sshDir, ScmConstants.SSH_AUTHORIZED_KEYS_FILE);

            mExecutor.setUserOnPath(ScmConstants.APP_USER, sshDir); // recursive
            TunnelKeysUtil.replaceUserEntry(authorizedKeysFile, username, keys);
            mExecutor.setUserOnPath(ScmConstants.TUNNEL_USER, sshDir); // recursive

            mExecutor.setToOnlyReadWriteUserPermissions(sshDir, true);
            mExecutor.setToOnlyReadWriteExecuteUserPermissions(sshDir, false); // add
            // back
            // u+x
            // to
            // directory
        } catch (final CommandWrapperFault f) {
            throw new IntegrationFault(f);
        } catch (final IOException e) {
            throw new IntegrationFault(e);
        }
    }

    /**
     * @see com.vasoftware.sf.externalintegration.adapters.ScmScmServerDaemon#getDaemonType()
     */
    @Override
    public String getDaemonType() {
        return "cvs";
    }
}
