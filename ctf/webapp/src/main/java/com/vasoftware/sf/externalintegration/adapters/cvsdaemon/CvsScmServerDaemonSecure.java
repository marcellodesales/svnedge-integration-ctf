/*
 * CollabNet TeamForge
 * Copyright 2010 CollabNet, Inc.  All rights reserved.
 * http://www.collab.net
 */
package com.vasoftware.sf.externalintegration.adapters.cvsdaemon;

import com.vasoftware.sf.externalintegration.IntegrationFault;

/**
 * The <code>CvsScmServerDaemonSecure</code> class is the actual SOAP service that gets called and delegates to
 * <code>CvsScmServerDaemon</code> once the transaction key has been verified.
 */
public class CvsScmServerDaemonSecure extends CvsScmServerDaemon {
    /**
     * Constructor.
     * 
     * @throws IntegrationFault
     *             If anything were to go wrong.
     */
    public CvsScmServerDaemonSecure() throws IntegrationFault {
        super();
    }

    /**
     * @see com.vasoftware.sf.externalintegration.adapters.cvsdaemon.CvsScmServerDaemon#createUser(String, String)
     */
    public void createUser(final String transactionKey, final java.lang.String username,
                           final java.lang.String passwordCrypt)
                                                                throws com.vasoftware.sf.externalintegration.execution.UserAlreadyExistsFault,
                                                                com.vasoftware.sf.externalintegration.IntegrationFault,
                                                                IntegrationFault {
        verifyKey(transactionKey);
        createUser(username, passwordCrypt);
    }

    /**
     * @see com.vasoftware.sf.externalintegration.adapters.cvsdaemon.CvsScmServerDaemon#addUsersToAccessGroup(String[],
     *      String)
     */
    public java.lang.String[] addUsersToAccessGroup(final String transactionKey, final java.lang.String[] usernames,
                                                    final java.lang.String groupName)
                                                                                     throws com.vasoftware.sf.externalintegration.IntegrationFault,
                                                                                     IntegrationFault {
        verifyKey(transactionKey);
        return addUsersToAccessGroup(usernames, groupName);
    }

    /**
     * @see com.vasoftware.sf.externalintegration.adapters.cvsdaemon.CvsScmServerDaemon#removeUsersFromAccessGroup(String[],
     *      String)
     */
    public void removeUsersFromAccessGroup(final String transactionKey, final java.lang.String[] usernames,
                                           final java.lang.String groupName)
                                                                            throws com.vasoftware.sf.externalintegration.IntegrationFault,
                                                                            IntegrationFault {
        verifyKey(transactionKey);
        removeUsersFromAccessGroup(usernames, groupName);
    }

    /**
     * @see com.vasoftware.sf.externalintegration.adapters.cvsdaemon.CvsScmServerDaemon#setUsersStatus(String[],
     *      String[])
     */
    public void setUsersStatus(final String transactionKey, final java.lang.String[] usernames,
                               final java.lang.String[] stati)
                                                              throws com.vasoftware.sf.externalintegration.IntegrationFault,
                                                              IntegrationFault {
        verifyKey(transactionKey);
        setUsersStatus(usernames, stati);
    }

    /**
     * @see com.vasoftware.sf.externalintegration.adapters.cvsdaemon.CvsScmServerDaemon#setAccessList(String[], String)
     */
    public java.lang.String[] setAccessList(final String transactionKey, final java.lang.String[] usernames,
                                            final java.lang.String groupName)
                                                                             throws com.vasoftware.sf.externalintegration.IntegrationFault,
                                                                             IntegrationFault {
        verifyKey(transactionKey);
        return setAccessList(usernames, groupName);
    }

    /**
     * @see com.vasoftware.sf.externalintegration.adapters.cvsdaemon.CvsScmServerDaemon#createRepository(String, String,
     *      String, String)
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
     * @see com.vasoftware.sf.externalintegration.adapters.cvsdaemon.CvsScmServerDaemon#verifyExistingRepositoryDirectory(String)
     */
    public java.lang.String verifyExistingRepositoryDirectory(final String transactionKey,
                                                              final java.lang.String repositoryDir)
                                                                                                   throws com.vasoftware.sf.externalintegration.IntegrationFault,
                                                                                                   IntegrationFault {
        verifyKey(transactionKey);
        return verifyExistingRepositoryDirectory(repositoryDir);
    }

    /**
     * @see com.vasoftware.sf.externalintegration.adapters.cvsdaemon.CvsScmServerDaemon#setRepositoryAccessLevel(String,
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
     * @see com.vasoftware.sf.externalintegration.adapters.cvsdaemon.CvsScmServerDaemon#setupRepository(String, String,
     *      String)
     */
    public void setupRepository(final String transactionKey, final java.lang.String systemId,
                                final java.lang.String repositoryGroup, final java.lang.String repositoryDir)
                                                                                                             throws com.vasoftware.sf.externalintegration.IntegrationFault,
                                                                                                             IntegrationFault {
        verifyKey(transactionKey);
        setupRepository(systemId, repositoryGroup, repositoryDir);
    }

    /**
     * @see com.vasoftware.sf.externalintegration.adapters.cvsdaemon.CvsScmServerDaemon#verifyExternalSystem(String)
     */
    public void verifyExternalSystem(final String transactionKey, final java.lang.String adapterType)
                                                                                                     throws com.vasoftware.sf.externalintegration.IntegrationFault,
                                                                                                     IntegrationFault {
        verifyKey(transactionKey);
        verifyExternalSystem(adapterType);
    }

    /**
     * @see com.vasoftware.sf.externalintegration.adapters.cvsdaemon.CvsScmServerDaemon#verifyPath(String, String,
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
     * @see com.vasoftware.sf.externalintegration.adapters.cvsdaemon.CvsScmServerDaemon#checkoutRepository(String,
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
     * @see com.vasoftware.sf.externalintegration.adapters.cvsdaemon.CvsScmServerDaemon#listGroupMembers(String)
     */
    public java.lang.String[] listGroupMembers(final String transactionKey, final java.lang.String groupName)
                                                                                                             throws com.vasoftware.sf.externalintegration.IntegrationFault,
                                                                                                             IntegrationFault {
        verifyKey(transactionKey);
        return listGroupMembers(groupName);
    }

    /**
     * @see com.vasoftware.sf.externalintegration.adapters.cvsdaemon.CvsScmServerDaemon#getAccessLevel(String, String)
     */
    public java.lang.Integer getAccessLevel(final String transactionKey, final java.lang.String repositoryId,
                                            final java.lang.String repositoryDir)
                                                                                 throws com.vasoftware.sf.externalintegration.IntegrationFault,
                                                                                 IntegrationFault {
        verifyKey(transactionKey);
        return getAccessLevel(repositoryId, repositoryDir);
    }

    /**
     * @see com.vasoftware.sf.externalintegration.adapters.cvsdaemon.CvsScmServerDaemon#setPassword(String, String,
     *      String)
     */
    public void setPassword(final String transactionKey, final java.lang.String username,
                            final java.lang.String password, final java.lang.String cryptedPassword)
                                                                                                    throws com.vasoftware.sf.externalintegration.IntegrationFault,
                                                                                                    IntegrationFault {
        verifyKey(transactionKey);
        setPassword(username, password, cryptedPassword);
    }

    /**
     * @see com.vasoftware.sf.externalintegration.adapters.cvsdaemon.CvsScmServerDaemon#createAuthorizedKeysFile(String,
     *      String)
     */
    public void createAuthorizedKeysFile(final String transactionKey, final java.lang.String username,
                                         final java.lang.String authorizedKeys)
                                                                               throws com.vasoftware.sf.externalintegration.IntegrationFault,
                                                                               IntegrationFault {
        verifyKey(transactionKey);
        createAuthorizedKeysFile(username, authorizedKeys);
    }

    /**
     * @see com.vasoftware.sf.externalintegration.adapters.cvsdaemon.CvsScmServerDaemon#initializeExternalSystem(String)
     */
    public void initializeExternalSystem(final String transactionKey, final java.lang.String systemId)
                                                                                                      throws com.vasoftware.sf.externalintegration.IntegrationFault,
                                                                                                      IntegrationFault {
        verifyKey(transactionKey);
        initializeExternalSystem(systemId);
    }

    /**
     * @see com.vasoftware.sf.externalintegration.adapters.cvsdaemon.CvsScmServerDaemon#deleteExternalSystem(String)
     */
    public void deleteExternalSystem(final String transactionKey, final java.lang.String systemId)
                                                                                                  throws com.vasoftware.sf.externalintegration.IntegrationFault,
                                                                                                  IntegrationFault {
        verifyKey(transactionKey);
        deleteExternalSystem(systemId);
    }

    /**
     * @see com.vasoftware.sf.externalintegration.adapters.cvsdaemon.CvsScmServerDaemon#archiveRepository(String)
     */
    public java.lang.Boolean archiveRepository(final String transactionKey, final java.lang.String repositoryPath)
                                                                                                                  throws com.vasoftware.sf.externalintegration.IntegrationFault,
                                                                                                                  IntegrationFault {
        verifyKey(transactionKey);
        return archiveRepository(repositoryPath);
    }

    /**
     * @see com.vasoftware.sf.externalintegration.adapters.cvsdaemon.CvsScmServerDaemon#getArchiveRepositoryRootPath()
     */
    public java.lang.String getArchiveRepositoryRootPath(final String transactionKey) throws IntegrationFault {
        verifyKey(transactionKey);
        return getArchiveRepositoryRootPath();
    }

    /**
     * @see com.vasoftware.sf.externalintegration.adapters.cvsdaemon.CvsScmServerDaemon#setScmToProcess(com.vasoftware.sf.externalintegration.adapters.ScmDaemon)
     */
    public void setScmToProcess(final String transactionKey,
                                final com.vasoftware.sf.externalintegration.adapters.ScmDaemon scmDaemon)
                                                                                                         throws IntegrationFault {
        verifyKey(transactionKey);
        setScmToProcess(scmDaemon);
    }

    /**
     * @see com.vasoftware.sf.externalintegration.adapters.cvsdaemon.CvsScmServerDaemon#beginBlackduckAnalysis(String,
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
     * @see com.vasoftware.sf.externalintegration.adapters.cvsdaemon.CvsScmServerDaemon#cancelBlackduckAnalysis(String)
     */
    public void cancelBlackduckAnalysis(final String transactionKey, final java.lang.String externalBlackduckProjectId)
                                                                                                                       throws IntegrationFault {
        verifyKey(transactionKey);
        cancelBlackduckAnalysis(externalBlackduckProjectId);
    }

    /**
     * @see com.vasoftware.sf.externalintegration.adapters.cvsdaemon.CvsScmServerDaemon#getBlackduckAnalysisStatus(String)
     */
    public java.lang.String getBlackduckAnalysisStatus(final String transactionKey,
                                                       final java.lang.String externalBlackduckProjectId)
                                                                                                         throws IntegrationFault {
        verifyKey(transactionKey);
        return getBlackduckAnalysisStatus(externalBlackduckProjectId);
    }

    /**
     * @see com.vasoftware.sf.externalintegration.adapters.cvsdaemon.CvsScmServerDaemon#cleanupBlackduckRepository(String)
     */
    public void cleanupBlackduckRepository(final String transactionKey,
                                           final java.lang.String externalBlackduckProjectId)
                                                                                             throws com.vasoftware.sf.externalintegration.IntegrationFault,
                                                                                             IntegrationFault {
        verifyKey(transactionKey);
        cleanupBlackduckRepository(externalBlackduckProjectId);
    }

    /**
     * @see com.vasoftware.sf.externalintegration.adapters.cvsdaemon.CvsScmServerDaemon#isBlackduckEnabled(String, int,
     *      String, String)
     */
    public void isBlackduckEnabled(final String transactionKey, final java.lang.String hostName, final int port,
                                   final java.lang.String username, final java.lang.String password)
                                                                                                    throws com.vasoftware.sf.externalintegration.IntegrationFault,
                                                                                                    IntegrationFault {
        verifyKey(transactionKey);
        isBlackduckEnabled(hostName, port, username, password);
    }
}
