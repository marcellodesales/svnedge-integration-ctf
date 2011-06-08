/*
 * CollabNet TeamForge
 * Copyright 2010 CollabNet, Inc.  All rights reserved.
 * http://www.collab.net
 */
package com.vasoftware.sf.externalintegration.adapters.subversiondaemon;

import java.io.File;

import com.vasoftware.sf.common.logger.Logger;
import com.vasoftware.sf.externalintegration.IntegrationFault;
import com.vasoftware.sf.externalintegration.ObjectAlreadyExistsFault;
import com.vasoftware.sf.externalintegration.ScmConstants;
import com.vasoftware.sf.externalintegration.adapters.ScmScmServerDaemon;
import com.vasoftware.sf.externalintegration.execution.CommandExecutor;
import com.vasoftware.sf.externalintegration.execution.CommandWrapperFault;

/**
 * The <code>SubversionScmServerDaemon</code> provides a simple implementation for the SCM adapter for Subversion
 * integration.
 */
public class SubversionScmServerDaemon extends ScmScmServerDaemon {
    private static final Logger smLogger = Logger.getLogger(SubversionScmServerDaemon.class);

    private final SubversionWrapper mSubversion = new SubversionWrapper(getCommandExecutor(), false);

    /**
     * Constructor. Instantiates the OS command executor.
     * 
     * @throws IntegrationFault
     *             Thrown if getting the command executor fails.
     */
    public SubversionScmServerDaemon() throws IntegrationFault {
        super();
    }

    /**
     * @see com.vasoftware.sf.externalintegration.adapters.ScmScmServerDaemon#isWindowsSupported()
     */
    @Override
    public boolean isWindowsSupported() {
        return true;
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

        File repositoryDirFile = getRepositoryDirFromCTFRepositoryPath(repositoryDir);
        // Verify the path is available
        if (getCommandExecutor().pathExists(repositoryDirFile)) {
            smLogger.info("Directory '" + repositoryDirFile.getAbsolutePath() + "' already exists - failing.");
            throw new ObjectAlreadyExistsFault(repositoryDirFile.getAbsolutePath());
        }

        // Create the repository and set it up
        try {
            getCommandExecutor().createPath(repositoryDirFile);
            mSubversion.createRepository(repositoryDirFile);
        } catch (final CommandWrapperFault e) {
            throw new IntegrationFault(e);
        }

        setupRepository(systemId, null, repositoryDirFile.getAbsolutePath());

        return getCTFRepositoryPathFromRepositoryDir(repositoryDirFile);
    }

    /**
     * Add all the necessary info into the specified repository
     * 
     * @param systemId
     *            The Guid of the system
     * @param repositoryGroup
     *            The group of the newly created repository (ignored for Subversion)
     * @param repositoryDir
     *            The name of the repository, the directory under the cvsroot
     * @throws IntegrationFault
     *             AN error while executing the commands.
     */
    public void setupRepository(final String systemId, final String repositoryGroup, final String repositoryDir)
        throws IntegrationFault {
        final File repositoryDirFile = getRepositoryDirFromCTFRepositoryPath(repositoryDir);

        try {
            // Setup the triggers first before changing permissions. Once the perms
            // are changed, its difficult to chown in a production build(as it runs as
            // a non-root user
            mSubversion.setupTriggers(systemId, repositoryDirFile.getAbsolutePath());
        } catch (final CommandWrapperFault e) {
            throw new IntegrationFault(e);
        }
    }

    /**
     * @see com.vasoftware.sf.externalintegration.adapters.ScmDaemon#verifyExternalSystem(java.lang.String)
     */
    public void verifyExternalSystem(final String adapterType) throws IntegrationFault {
        if (!"Subversion".equals(adapterType)) {
            throw new IntegrationFault("Unsupported adapter type: " + adapterType + ". Supported Types are: Subversion");
        }

        int[] version = new int[0];
        try {
            version = mSubversion.getVersion();
        } catch (final CommandWrapperFault commandWrapperFault) {
            throw new IntegrationFault(commandWrapperFault);
        }

        if (version == null) {
            throw new IntegrationFault("No subversion version found");
        }

        final int[] expected = { 1, 2, 1 };
        for (int i = 0; i < expected.length; i++) {
            final int expectedVersion = expected[i];
            int actualVersion;
            if (i >= version.length) {
                actualVersion = 0;
            } else {
                actualVersion = version[i];
            }

            if (actualVersion < expectedVersion) {
                String message = "VERSION (requires 1.2.1 or better): ";
                for (int j = 0; j < version.length; j++) {
                    message = message + version[j] + ".";
                }
                throw new IntegrationFault(message);
            } else if (expectedVersion == actualVersion) {
                // this sub version matches, so let's check the next subversion
                continue;
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
     *@see ScmScmServerDaemon#setRepositoryAccessLevel(String,String,String)
     */
    public void setRepositoryAccessLevel(final String repositoryDir, final String repositoryId, final String level)
        throws IntegrationFault {
        ; // do nothing here
    }

    /**
     * @see ScmScmServerDaemon#verifyPath(String,String,String,String)
     */
    public void verifyPath(final String externalBlackduckProjectId, final String repositoryId,
                           String repositoryPath, final String repositoryPathFromRoot) throws IntegrationFault {
        repositoryPath = getRepositoryDirFromCTFRepositoryPath(repositoryPath).getAbsolutePath();

        try {
            final boolean valid = mSubversion.verifyPath(repositoryPath, repositoryPathFromRoot);
            if (!valid) {
                if (smLogger.isDebugEnabled()) {
                    smLogger.debug("Subversion path not present " + repositoryPath + " with " + repositoryPathFromRoot);
                }
                throw new IntegrationFault(INVALID_PATH);
            } else {
                if (smLogger.isDebugEnabled()) {
                    smLogger.debug("Subversion path present " + repositoryPath + " with " + repositoryPathFromRoot);
                }
            }
        } catch (final CommandWrapperFault commandWrapperFault) {
            throw new IntegrationFault(commandWrapperFault);
        }
    }

    /**
     *@see ScmScmServerDaemon#checkoutRepository(String,String,java.io.File)
     */
    public void checkoutRepository(final String repositoryPath, final String repositoryPathFromRoot,
                                   final File destinationDirectory) throws IntegrationFault {
        try {
            final File repositoryRoot = getRepositoryDirFromCTFRepositoryPath(repositoryPath);

            mSubversion.checkoutRepository(repositoryRoot, repositoryPathFromRoot, destinationDirectory);
        } catch (final CommandWrapperFault commandWrapperFault) {
            throw new IntegrationFault(commandWrapperFault);
        }
    }

    /**
     * @see com.vasoftware.sf.externalintegration.adapters.ScmScmServerDaemon#getDaemonType()
     */
    @Override
    public String getDaemonType() {
        return "subversion";
    }
}
