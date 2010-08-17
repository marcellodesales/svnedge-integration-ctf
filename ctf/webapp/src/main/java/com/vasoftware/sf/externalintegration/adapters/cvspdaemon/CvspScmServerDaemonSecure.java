/*
 * CollabNet TeamForge
 * Copyright 2010 CollabNet, Inc.  All rights reserved.
 * http://www.collab.net
 */
package com.vasoftware.sf.externalintegration.adapters.cvspdaemon;

import com.vasoftware.sf.externalintegration.IntegrationFault;

/**
 * The <code>CvspScmServerDaemonSecure</code> class is the actual SOAP service that gets called and delegates to
 * <code>CvspScmServerDaemon</code> once the transaction key has been verified.
 */
public class CvspScmServerDaemonSecure extends CvspScmServerDaemon {
    /**
     * Constructor.
     * 
     * @throws IntegrationFault
     *             If anything goes wrong.
     */
    public CvspScmServerDaemonSecure() throws IntegrationFault {
        super();
    }

    /**
     * @see com.vasoftware.sf.externalintegration.adapters.cvspdaemon.CvspScmServerDaemon#initializeExternalSystem(String)
     */
    public void initializeExternalSystem(final String transactionKey, final java.lang.String systemId)
                                                                                                      throws com.vasoftware.sf.externalintegration.IntegrationFault,
                                                                                                      IntegrationFault {
        verifyKey(transactionKey);
        initializeExternalSystem(systemId);
    }

    /**
     * @see com.vasoftware.sf.externalintegration.adapters.cvspdaemon.CvspScmServerDaemon#deleteExternalSystem(String)
     */
    public void deleteExternalSystem(final String transactionKey, final java.lang.String systemId)
                                                                                                  throws com.vasoftware.sf.externalintegration.IntegrationFault,
                                                                                                  IntegrationFault {
        verifyKey(transactionKey);
        deleteExternalSystem(systemId);
    }

    /**
     * @see com.vasoftware.sf.externalintegration.adapters.cvspdaemon.CvspScmServerDaemon#createRepository(String,
     *      String, String, String)
     */
    public java.lang.String createRepository(final String transactionKey, final java.lang.String repositoryGroup,
                                             final java.lang.String repositoryDir, final java.lang.String systemId,
                                             final java.lang.String repositoryBaseUrl)
                                                                                      throws com.vasoftware.sf.externalintegration.IntegrationFault,
                                                                                      com.vasoftware.sf.externalintegration.ObjectAlreadyExistsFault,
                                                                                      IntegrationFault {
        verifyKey(transactionKey);
        return createRepository(repositoryGroup, repositoryDir, systemId, repositoryBaseUrl);
    }

    /**
     * @see com.vasoftware.sf.externalintegration.adapters.cvspdaemon.CvspScmServerDaemon#setupRepository(String,
     *      String, String)
     */
    public void setupRepository(final String transactionKey, final java.lang.String systemId,
                                final java.lang.String repositoryGroup, final java.lang.String repositoryDir)
                                                                                                             throws com.vasoftware.sf.externalintegration.IntegrationFault,
                                                                                                             IntegrationFault {
        verifyKey(transactionKey);
        setupRepository(systemId, repositoryGroup, repositoryDir);
    }

    /**
     * @see com.vasoftware.sf.externalintegration.adapters.cvspdaemon.CvspScmServerDaemon#verifyExternalSystem(String)
     */
    public void verifyExternalSystem(final String transactionKey, final java.lang.String adapterType)
                                                                                                     throws com.vasoftware.sf.externalintegration.IntegrationFault,
                                                                                                     IntegrationFault {
        verifyKey(transactionKey);
        verifyExternalSystem(adapterType);
    }

    /**
     * @see com.vasoftware.sf.externalintegration.adapters.cvspdaemon.CvspScmServerDaemon#setRepositoryAccessLevel(String,
     *      String, String)
     */
    public void setRepositoryAccessLevel(final String transactionKey, final java.lang.String repositoryDir,
                                         final java.lang.String repositoryId, final java.lang.String level)
                                                                                                           throws com.vasoftware.sf.externalintegration.IntegrationFault,
                                                                                                           IntegrationFault {
        verifyKey(transactionKey);
        setRepositoryAccessLevel(repositoryDir, repositoryId, level);
    }

    /**
     * @see com.vasoftware.sf.externalintegration.adapters.cvspdaemon.CvspScmServerDaemon#verifyPath(String, String,
     *      String, String)
     */
    public void verifyPath(final String transactionKey, final java.lang.String externalBlackduckProjectId,
                           final java.lang.String repositoryId, final java.lang.String repositoryPath,
                           final java.lang.String repositoryPathFromRoot)
                                                                         throws com.vasoftware.sf.externalintegration.IntegrationFault,
                                                                         IntegrationFault {
        verifyKey(transactionKey);
        verifyPath(externalBlackduckProjectId, repositoryId, repositoryPath, repositoryPathFromRoot);
    }

    /**
     * @see com.vasoftware.sf.externalintegration.adapters.cvspdaemon.CvspScmServerDaemon#checkoutRepository(String,
     *      String, java.io.File)
     */
    public void checkoutRepository(final String transactionKey, final java.lang.String repositoryRoot,
                                   final java.lang.String repositoryPathFromRoot,
                                   final java.io.File destinationDirectory)
                                                                           throws com.vasoftware.sf.externalintegration.IntegrationFault,
                                                                           IntegrationFault {
        verifyKey(transactionKey);
        checkoutRepository(repositoryRoot, repositoryPathFromRoot, destinationDirectory);
    }

    /**
     * @see com.vasoftware.sf.externalintegration.adapters.cvspdaemon.CvspScmServerDaemon#createTunnelAuthorizedKeysFile(String)
     */
    public void createTunnelAuthorizedKeysFile(final String transactionKey, final java.lang.String authorizedKeys)
                                                                                                                  throws com.vasoftware.sf.externalintegration.IntegrationFault,
                                                                                                                  IntegrationFault {
        verifyKey(transactionKey);
        createTunnelAuthorizedKeysFile(authorizedKeys);
    }

    /**
     * @see com.vasoftware.sf.externalintegration.adapters.cvspdaemon.CvspScmServerDaemon#replaceEntryTunnelAuthorizedKeysFile(String,
     *      String)
     */
    public void replaceEntryTunnelAuthorizedKeysFile(final String transactionKey, final java.lang.String username,
                                                     final java.lang.String keys)
                                                                                 throws com.vasoftware.sf.externalintegration.IntegrationFault,
                                                                                 IntegrationFault {
        verifyKey(transactionKey);
        replaceEntryTunnelAuthorizedKeysFile(username, keys);
    }

    /**
     * @see com.vasoftware.sf.externalintegration.adapters.cvspdaemon.CvspScmServerDaemon#archiveRepository(String)
     */
    public java.lang.Boolean archiveRepository(final String transactionKey, final java.lang.String repositoryPath)
                                                                                                                  throws com.vasoftware.sf.externalintegration.IntegrationFault,
                                                                                                                  IntegrationFault {
        verifyKey(transactionKey);
        return archiveRepository(repositoryPath);
    }

    /**
     * @see com.vasoftware.sf.externalintegration.adapters.cvspdaemon.CvspScmServerDaemon#getArchiveRepositoryRootPath()
     */
    public java.lang.String getArchiveRepositoryRootPath(final String transactionKey) throws IntegrationFault {
        verifyKey(transactionKey);
        return getArchiveRepositoryRootPath();
    }

    /**
     * @see com.vasoftware.sf.externalintegration.adapters.cvspdaemon.CvspScmServerDaemon#setScmToProcess(com.vasoftware.sf.externalintegration.adapters.ScmDaemon)
     */
    public void setScmToProcess(final String transactionKey,
                                final com.vasoftware.sf.externalintegration.adapters.ScmDaemon scmDaemon)
                                                                                                         throws IntegrationFault {
        verifyKey(transactionKey);
        setScmToProcess(scmDaemon);
    }

    /**
     * @see com.vasoftware.sf.externalintegration.adapters.cvspdaemon.CvspScmServerDaemon#beginBlackduckAnalysis(String,
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
     * @see com.vasoftware.sf.externalintegration.adapters.cvspdaemon.CvspScmServerDaemon#cancelBlackduckAnalysis(String)
     */
    public void cancelBlackduckAnalysis(final String transactionKey, final java.lang.String externalBlackduckProjectId)
                                                                                                                       throws IntegrationFault {
        verifyKey(transactionKey);
        cancelBlackduckAnalysis(externalBlackduckProjectId);
    }

    /**
     * @see com.vasoftware.sf.externalintegration.adapters.cvspdaemon.CvspScmServerDaemon#getBlackduckAnalysisStatus(String)
     */
    public java.lang.String getBlackduckAnalysisStatus(final String transactionKey,
                                                       final java.lang.String externalBlackduckProjectId)
                                                                                                         throws IntegrationFault {
        verifyKey(transactionKey);
        return getBlackduckAnalysisStatus(externalBlackduckProjectId);
    }

    /**
     * @see com.vasoftware.sf.externalintegration.adapters.cvspdaemon.CvspScmServerDaemon#cleanupBlackduckRepository(String)
     */
    public void cleanupBlackduckRepository(final String transactionKey,
                                           final java.lang.String externalBlackduckProjectId)
                                                                                             throws com.vasoftware.sf.externalintegration.IntegrationFault,
                                                                                             IntegrationFault {
        verifyKey(transactionKey);
        cleanupBlackduckRepository(externalBlackduckProjectId);
    }

    /**
     * @see com.vasoftware.sf.externalintegration.adapters.cvspdaemon.CvspScmServerDaemon#isBlackduckEnabled(String,
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
