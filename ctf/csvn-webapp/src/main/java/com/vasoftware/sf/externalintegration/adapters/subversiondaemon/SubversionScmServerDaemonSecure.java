/*
 * CollabNet TeamForge
 * Copyright 2010 CollabNet, Inc.  All rights reserved.
 * http://www.collab.net
 */
package com.vasoftware.sf.externalintegration.adapters.subversiondaemon;

import java.io.File;

import com.vasoftware.sf.common.configuration.GlobalOptionKeys;
import com.vasoftware.sf.common.configuration.SfGlobalOptions;
import com.vasoftware.sf.common.configuration.SfGlobalOptionsManager;
import com.vasoftware.sf.externalintegration.IntegrationFault;

/**
 * The <code>SubversionScmServerDaemonSecure</code> class is the actual SOAP service that gets called and delegates to
 * <code>SubversionScmServerDaemon</code> once the transaction key has been verified.
 */
public class SubversionScmServerDaemonSecure extends SubversionScmServerDaemon {
    private InactiveScmServerDaemon issd;

    /**
     * Constructor.
     * 
     * @throws IntegrationFault
     *             If anything goes wrong.
     */
    public SubversionScmServerDaemonSecure() throws IntegrationFault {
        super();
        issd = new InactiveScmServerDaemon();
    }

    private boolean isActive() {
        final SfGlobalOptions options = SfGlobalOptionsManager.getOptions();
        String csvnState = options
            .getOption(GlobalOptionKeys.SFMAIN_INTEGRATION_CSVN_MODE);
        return null == csvnState || "csvn-managed".equals(csvnState);
    }

    /**
     * @see com.vasoftware.sf.externalintegration.adapters.subversiondaemon.SubversionScmServerDaemon#initializeExternalSystem(String)
     */
    public void initializeExternalSystem(final String transactionKey, final java.lang.String systemId)
                                                                                                      throws com.vasoftware.sf.externalintegration.IntegrationFault,
                                                                                                      IntegrationFault {
        verifyKey(transactionKey);
        initializeExternalSystem(systemId);            
    }

    /**
     * @see com.vasoftware.sf.externalintegration.adapters.subversiondaemon.SubversionScmServerDaemon#deleteExternalSystem(String)
     */
    public void deleteExternalSystem(final String transactionKey, final java.lang.String systemId)
                                                                                                  throws com.vasoftware.sf.externalintegration.IntegrationFault,
                                                                                                  IntegrationFault {
        if (isActive()) {        
            verifyKey(transactionKey);
            deleteExternalSystem(systemId);
        } else {
            issd.deleteExternalSystem(systemId);
        }
    }

    /**
     * @see com.vasoftware.sf.externalintegration.adapters.subversiondaemon.SubversionScmServerDaemon#createRepository(String,
     *      String, String, String)
     */
    public java.lang.String createRepository(final String transactionKey, final java.lang.String repositoryGroup,
                                             final java.lang.String repositoryDir, final java.lang.String systemId,
                                             final java.lang.String repositoryBaseUrl)
                                                                                      throws com.vasoftware.sf.externalintegration.IntegrationFault,
                                                                                      com.vasoftware.sf.externalintegration.ObjectAlreadyExistsFault,
                                                                                      IntegrationFault {
        verifyKey(transactionKey);
        if (isActive()) {
            return createRepository(repositoryGroup, repositoryDir, 
                                    systemId, repositoryBaseUrl);
        } else {
            issd.createRepository(repositoryGroup, repositoryDir, 
                                  systemId, repositoryBaseUrl);
            File repositoryDirFile = getRepositoryDirFromCTFRepositoryPath(repositoryDir);
            setupRepository(systemId, repositoryGroup, 
                            repositoryDirFile.getAbsolutePath());
            return repositoryDir;
        }
    }

    /**
     * @see com.vasoftware.sf.externalintegration.adapters.subversiondaemon.SubversionScmServerDaemon#setupRepository(String,
     *      String, String)
     */
    public void setupRepository(final String transactionKey, final java.lang.String systemId,
                                final java.lang.String repositoryGroup, final java.lang.String repositoryDir)
                                                                                                             throws com.vasoftware.sf.externalintegration.IntegrationFault,
                                                                                                             IntegrationFault {
        if (isActive()) {
            verifyKey(transactionKey);
            setupRepository(systemId, repositoryGroup, repositoryDir);
        } else {
            issd.setupRepository(systemId, repositoryGroup, repositoryDir);
        }
    }

    /**
     * @see com.vasoftware.sf.externalintegration.adapters.subversiondaemon.SubversionScmServerDaemon#verifyExternalSystem(String)
     */
    public void verifyExternalSystem(final String transactionKey, final java.lang.String adapterType)
                                                                                                     throws com.vasoftware.sf.externalintegration.IntegrationFault,
                                                                                                     IntegrationFault {
        verifyKey(transactionKey);
        verifyExternalSystem(adapterType);
    }

    /**
     * @see com.vasoftware.sf.externalintegration.adapters.subversiondaemon.SubversionScmServerDaemon#setRepositoryAccessLevel(String,
     *      String, String)
     */
    public void setRepositoryAccessLevel(final String transactionKey, final java.lang.String repositoryDir,
                                         final java.lang.String repositoryId, final java.lang.String level)
                                                                                                           throws com.vasoftware.sf.externalintegration.IntegrationFault,
                                                                                                           IntegrationFault {
        if (isActive()) {
            verifyKey(transactionKey);
            setRepositoryAccessLevel(repositoryDir, repositoryId, level);
        } else {
            issd.setRepositoryAccessLevel(repositoryDir, repositoryId, 
                                          level);
        }
    }

    /**
     * @see com.vasoftware.sf.externalintegration.adapters.subversiondaemon.SubversionScmServerDaemon#verifyPath(String,
     *      String, String, String)
     */
    public void verifyPath(final String transactionKey, final java.lang.String externalBlackduckProjectId,
                           final java.lang.String repositoryId, final java.lang.String repositoryPath,
                           final java.lang.String repositoryPathFromRoot)
                                                                         throws com.vasoftware.sf.externalintegration.IntegrationFault,
                                                                         IntegrationFault {
        if (isActive()) {
            verifyKey(transactionKey);
            verifyPath(externalBlackduckProjectId, repositoryId, 
                       repositoryPath, repositoryPathFromRoot);
        } else {
            issd.verifyPath(externalBlackduckProjectId, repositoryId, 
                            repositoryPath, repositoryPathFromRoot);
        }
    }

    /**
     * @see com.vasoftware.sf.externalintegration.adapters.subversiondaemon.SubversionScmServerDaemon#checkoutRepository(String,
     *      String, java.io.File)
     */
    public void checkoutRepository(final String transactionKey, final java.lang.String repositoryPath,
                                   final java.lang.String repositoryPathFromRoot,
                                   final java.io.File destinationDirectory)
                                                                           throws com.vasoftware.sf.externalintegration.IntegrationFault,
                                                                           IntegrationFault {
        if (isActive()) {
            verifyKey(transactionKey);
            checkoutRepository(repositoryPath, repositoryPathFromRoot, 
                               destinationDirectory);
        } else {
            issd.checkoutRepository(repositoryPath, repositoryPathFromRoot,
                                    destinationDirectory);
        }
    }

    /**
     * @see com.vasoftware.sf.externalintegration.adapters.subversiondaemon.SubversionScmServerDaemon#archiveRepository(String)
     */
    public java.lang.Boolean archiveRepository(final String transactionKey, final java.lang.String repositoryPath)
                                                                                                                  throws com.vasoftware.sf.externalintegration.IntegrationFault,
                                                                                                                  IntegrationFault {
        if (isActive()) {
            verifyKey(transactionKey);
            return archiveRepository(repositoryPath);
        } else {
            return issd.archiveRepository(repositoryPath);
        }
    }

    /**
     * @see com.vasoftware.sf.externalintegration.adapters.subversiondaemon.SubversionScmServerDaemon#getArchiveRepositoryRootPath()
     */
    public java.lang.String getArchiveRepositoryRootPath(final String transactionKey) throws IntegrationFault {
        if (isActive()) {
            verifyKey(transactionKey);
            return getArchiveRepositoryRootPath();
        } else {
            return issd.getArchiveRepositoryRootPath();
        }
    }

    /**
     * @see com.vasoftware.sf.externalintegration.adapters.subversiondaemon.SubversionScmServerDaemon#setScmToProcess(com.vasoftware.sf.externalintegration.adapters.ScmDaemon)
     */
    public void setScmToProcess(final String transactionKey,
                                final com.vasoftware.sf.externalintegration.adapters.ScmDaemon scmDaemon)
                                                                                                         throws IntegrationFault {
        verifyKey(transactionKey);
        setScmToProcess(scmDaemon);
    }

    /**
     * @see com.vasoftware.sf.externalintegration.adapters.subversiondaemon.SubversionScmServerDaemon#beginBlackduckAnalysis(String,
     *      int, String, String, String, String, String, String)
     */
    public void beginBlackduckAnalysis(final String transactionKey, final java.lang.String hostName, final int port,
                                       final java.lang.String username, final java.lang.String password,
                                       final java.lang.String blackduckRepositoryId,
                                       final java.lang.String externalBlackduckProjectId,
                                       final java.lang.String repositoryPath,
                                       final java.lang.String repositoryPathFromRoot)
                                                                                     throws com.vasoftware.sf.externalintegration.IntegrationFault,
                                                                                     IntegrationFault {
        verifyKey(transactionKey);
        beginBlackduckAnalysis(hostName, port, username, password, blackduckRepositoryId, externalBlackduckProjectId,
                               repositoryPath, repositoryPathFromRoot);
    }

    /**
     * @see com.vasoftware.sf.externalintegration.adapters.subversiondaemon.SubversionScmServerDaemon#cancelBlackduckAnalysis(String)
     */
    public void cancelBlackduckAnalysis(final String transactionKey, final java.lang.String externalBlackduckProjectId)
                                                                                                                       throws IntegrationFault {
        verifyKey(transactionKey);
        cancelBlackduckAnalysis(externalBlackduckProjectId);
    }

    /**
     * @see com.vasoftware.sf.externalintegration.adapters.subversiondaemon.SubversionScmServerDaemon#getBlackduckAnalysisStatus(String)
     */
    public java.lang.String getBlackduckAnalysisStatus(final String transactionKey,
                                                       final java.lang.String externalBlackduckProjectId)
                                                                                                         throws IntegrationFault {
        verifyKey(transactionKey);
        return getBlackduckAnalysisStatus(externalBlackduckProjectId);
    }

    /**
     * @see com.vasoftware.sf.externalintegration.adapters.subversiondaemon.SubversionScmServerDaemon#cleanupBlackduckRepository(String)
     */
    public void cleanupBlackduckRepository(final String transactionKey,
                                           final java.lang.String externalBlackduckProjectId)
                                                                                             throws com.vasoftware.sf.externalintegration.IntegrationFault,
                                                                                             IntegrationFault {
        verifyKey(transactionKey);
        cleanupBlackduckRepository(externalBlackduckProjectId);
    }

    /**
     * @see com.vasoftware.sf.externalintegration.adapters.subversiondaemon.SubversionScmServerDaemon#isBlackduckEnabled(String,
     *      int, String, String)
     */
    public void isBlackduckEnabled(final String transactionKey, final java.lang.String hostName, final int port,
                                   final java.lang.String username, final java.lang.String password)
                                                                                                    throws com.vasoftware.sf.externalintegration.IntegrationFault,
                                                                                                    IntegrationFault {
        verifyKey(transactionKey);
        isBlackduckEnabled(hostName, port, username, password);
    }

}
