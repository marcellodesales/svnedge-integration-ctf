/*
 * CollabNet TeamForge
 * Copyright 2010 CollabNet, Inc.  All rights reserved.
 * http://www.collab.net
 */
package com.vasoftware.sf.externalintegration.adapters.wandiscodaemon;

import com.vasoftware.sf.externalintegration.IntegrationFault;

/**
 * The <code>WandiscoSubversionScmServerDaemonSecure</code> class is the actual SOAP service that gets called and
 * delegates to <code>WandiscoSubversionScmServerDaemon</code> once the transaction key has been verified.
 */
public class WandiscoSubversionScmServerDaemonSecure extends WandiscoSubversionScmServerDaemon {
    /**
     * Constructor.
     * 
     * @throws IntegrationFault
     *             If anything goes wrong.
     */
    public WandiscoSubversionScmServerDaemonSecure() throws IntegrationFault {
        super();
    }

    /**
     * @see com.vasoftware.sf.externalintegration.adapters.wandiscodaemon.WandiscoSubversionScmServerDaemon#deleteExternalSystem(String)
     */
    public void deleteExternalSystem(final String transactionKey, final java.lang.String systemId)
                                                                                                  throws com.vasoftware.sf.externalintegration.IntegrationFault,
                                                                                                  IntegrationFault {
        verifyKey(transactionKey);
        deleteExternalSystem(systemId);
    }

    /**
     * @see com.vasoftware.sf.externalintegration.adapters.wandiscodaemon.WandiscoSubversionScmServerDaemon#setupRepository(String,
     *      String, String)
     */
    public void setupRepository(final String transactionKey, final java.lang.String systemId,
                                final java.lang.String repositoryId, final java.lang.String repositoryDir)
                                                                                                          throws com.vasoftware.sf.externalintegration.IntegrationFault,
                                                                                                          IntegrationFault {
        verifyKey(transactionKey);
        setupRepository(systemId, repositoryId, repositoryDir);
    }

    /**
     * @see com.vasoftware.sf.externalintegration.adapters.wandiscodaemon.WandiscoSubversionScmServerDaemon#setACLs(String,
     *      String, String, String)
     */
    @Override
    public void setACLs(final String transactionKey, final java.lang.String repositoryDir,
                        final java.lang.String groupId, final java.lang.String level,
                        final java.lang.String operationName)
                                                             throws com.vasoftware.sf.externalintegration.ObjectAlreadyExistsFault,
                                                             com.vasoftware.sf.externalintegration.IntegrationFault,
                                                             IntegrationFault {
        verifyKey(transactionKey);
        setACLs(repositoryDir, groupId, level, operationName);
    }

    /**
     * @see com.vasoftware.sf.externalintegration.adapters.wandiscodaemon.WandiscoSubversionScmServerDaemon#deleteRepositoryGroupsAndAcls(String)
     */
    public void deleteRepositoryGroupsAndAcls(final String transactionKey, final java.lang.String repositoryId)
                                                                                                               throws com.vasoftware.sf.externalintegration.IntegrationFault,
                                                                                                               IntegrationFault {
        verifyKey(transactionKey);
        deleteRepositoryGroupsAndAcls(repositoryId);
    }

    /**
     * @see com.vasoftware.sf.externalintegration.adapters.wandiscodaemon.WandiscoSubversionScmServerDaemon#setPassword(String,
     *      String, String)
     */
    public void setPassword(final String transactionKey, final java.lang.String username,
                            final java.lang.String password, final java.lang.String cryptedPassword)
                                                                                                    throws com.vasoftware.sf.externalintegration.IntegrationFault,
                                                                                                    IntegrationFault {
        verifyKey(transactionKey);
        setPassword(username, password, cryptedPassword);
    }

    /**
     * @see com.vasoftware.sf.externalintegration.adapters.wandiscodaemon.WandiscoSubversionScmServerDaemon#addUsers(String[],
     *      String[])
     */
    public java.lang.String[] addUsers(final String transactionKey, final java.lang.String[] usernames,
                                       final java.lang.String[] md5Passwords)
                                                                             throws com.vasoftware.sf.externalintegration.IntegrationFault,
                                                                             IntegrationFault {
        verifyKey(transactionKey);
        return addUsers(usernames, md5Passwords);
    }

    /**
     * @see com.vasoftware.sf.externalintegration.adapters.wandiscodaemon.WandiscoSubversionScmServerDaemon#createRepository(String,
     *      String, String, String)
     */
    public java.lang.String createRepository(final String transactionKey, final java.lang.String repositoryId,
                                             final java.lang.String repositoryDir, final java.lang.String systemId,
                                             final java.lang.String repositoryBaseUrl)
                                                                                      throws com.vasoftware.sf.externalintegration.IntegrationFault,
                                                                                      IntegrationFault {
        verifyKey(transactionKey);
        return createRepository(repositoryId, repositoryDir, systemId, repositoryBaseUrl);
    }

    /**
     * @see com.vasoftware.sf.externalintegration.adapters.wandiscodaemon.WandiscoSubversionScmServerDaemon#verifyExternalSystem(String,
     *      String, String)
     */
    public void verifyExternalSystem(final String transactionKey, final java.lang.String adapterType,
                                     final java.lang.String repositoryBaseUrl,
                                     final java.lang.String dummyRepositoryPath)
                                                                                throws com.vasoftware.sf.externalintegration.IntegrationFault,
                                                                                IntegrationFault {
        verifyKey(transactionKey);
        verifyExternalSystem(adapterType, repositoryBaseUrl, dummyRepositoryPath);
    }

    /**
     * @see com.vasoftware.sf.externalintegration.adapters.wandiscodaemon.WandiscoSubversionScmServerDaemon#addUsersToAccessGroup(String[],
     *      String)
     */
    public java.lang.String[] addUsersToAccessGroup(final String transactionKey, final java.lang.String[] usernames,
                                                    final java.lang.String groupId)
                                                                                   throws com.vasoftware.sf.externalintegration.IntegrationFault,
                                                                                   com.vasoftware.sf.externalintegration.execution.ScmLimitationFault,
                                                                                   IntegrationFault {
        verifyKey(transactionKey);
        return addUsersToAccessGroup(usernames, groupId);
    }

    /**
     * @see com.vasoftware.sf.externalintegration.adapters.wandiscodaemon.WandiscoSubversionScmServerDaemon#removeUsersFromAccessGroup(String[],
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
     * @see com.vasoftware.sf.externalintegration.adapters.wandiscodaemon.WandiscoSubversionScmServerDaemon#removeUsersFromAccessGroups(String[],
     *      String[])
     */
    public void removeUsersFromAccessGroups(final String transactionKey, final java.lang.String[] usernames,
                                            final java.lang.String[] groupNames)
                                                                                throws com.vasoftware.sf.externalintegration.IntegrationFault,
                                                                                IntegrationFault {
        verifyKey(transactionKey);
        removeUsersFromAccessGroups(usernames, groupNames);
    }

    /**
     * @see com.vasoftware.sf.externalintegration.adapters.wandiscodaemon.WandiscoSubversionScmServerDaemon#deleteAllUsers()
     */
    public void deleteAllUsers(final String transactionKey)
                                                           throws com.vasoftware.sf.externalintegration.IntegrationFault,
                                                           IntegrationFault {
        verifyKey(transactionKey);
        deleteAllUsers();
    }

    /**
     * @see com.vasoftware.sf.externalintegration.adapters.wandiscodaemon.WandiscoSubversionScmServerDaemon#listGroupMembers(String)
     */
    public java.lang.String[] listGroupMembers(final String transactionKey, final java.lang.String groupName)
                                                                                                             throws com.vasoftware.sf.externalintegration.IntegrationFault,
                                                                                                             IntegrationFault {
        verifyKey(transactionKey);
        return listGroupMembers(groupName);
    }

    /**
     * @see com.vasoftware.sf.externalintegration.adapters.wandiscodaemon.WandiscoSubversionScmServerDaemon#setAccessList(String[],
     *      String)
     */
    public java.lang.String[] setAccessList(final String transactionKey, final java.lang.String[] usernames,
                                            final java.lang.String groupId)
                                                                           throws com.vasoftware.sf.externalintegration.IntegrationFault,
                                                                           com.vasoftware.sf.externalintegration.execution.ScmLimitationFault,
                                                                           IntegrationFault {
        verifyKey(transactionKey);
        return setAccessList(usernames, groupId);
    }

    /**
     * @see com.vasoftware.sf.externalintegration.adapters.wandiscodaemon.WandiscoSubversionScmServerDaemon#createInitialGroupsAndAcls(String)
     */
    public void createInitialGroupsAndAcls(final String transactionKey, final java.lang.String scmRoot)
                                                                                                       throws com.vasoftware.sf.externalintegration.IntegrationFault,
                                                                                                       IntegrationFault {
        verifyKey(transactionKey);
        createInitialGroupsAndAcls(scmRoot);
    }

    /**
     * @see com.vasoftware.sf.externalintegration.adapters.wandiscodaemon.WandiscoSubversionScmServerDaemon#setRepositoryAccessLevel(String,
     *      String, String)
     */
    public void setRepositoryAccessLevel(final String transactionKey, final java.lang.String repositoryDir,
                                         final java.lang.String groupId, final java.lang.String level)
                                                                                                      throws com.vasoftware.sf.externalintegration.IntegrationFault,
                                                                                                      IntegrationFault {
        verifyKey(transactionKey);
        setRepositoryAccessLevel(repositoryDir, groupId, level);
    }

    /**
     * @see com.vasoftware.sf.externalintegration.adapters.wandiscodaemon.WandiscoSubversionScmServerDaemon#checkoutRepository(String,
     *      String, java.io.File)
     */
    public void checkoutRepository(final String transactionKey, final java.lang.String repositoryPath,
                                   final java.lang.String repositoryPathFromRoot,
                                   final java.io.File destinationDirectory)
                                                                           throws com.vasoftware.sf.externalintegration.IntegrationFault,
                                                                           IntegrationFault {
        verifyKey(transactionKey);
        checkoutRepository(repositoryPath, repositoryPathFromRoot, destinationDirectory);
    }

    /**
     * @see com.vasoftware.sf.externalintegration.adapters.wandiscodaemon.WandiscoSubversionScmServerDaemon#verifyPath(String,
     *      String, String, String)
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
     * @see com.vasoftware.sf.externalintegration.adapters.wandiscodaemon.WandiscoSubversionScmServerDaemon#createUser(String)
     */
    public void createUser(final String transactionKey, final java.lang.String userName)
                                                                                        throws com.vasoftware.sf.externalintegration.IntegrationFault,
                                                                                        com.vasoftware.sf.externalintegration.execution.UserAlreadyExistsFault,
                                                                                        IntegrationFault {
        verifyKey(transactionKey);
        createUser(userName);
    }

    /**
     * @see com.vasoftware.sf.externalintegration.adapters.wandiscodaemon.WandiscoSubversionScmServerDaemon#deleteUser(String)
     */
    public void deleteUser(final String transactionKey, final java.lang.String userName)
                                                                                        throws com.vasoftware.sf.externalintegration.IntegrationFault,
                                                                                        com.vasoftware.sf.externalintegration.execution.ObjectNotFoundFault,
                                                                                        IntegrationFault {
        verifyKey(transactionKey);
        deleteUser(userName);
    }

    /**
     * @see com.vasoftware.sf.externalintegration.adapters.wandiscodaemon.WandiscoSubversionScmServerDaemon#createAuthorizedKeysFile(String,
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
     * @see com.vasoftware.sf.externalintegration.adapters.wandiscodaemon.WandiscoSubversionScmServerDaemon#verifyExternalSystem(String)
     */
    public void verifyExternalSystem(final String transactionKey, final java.lang.String adapterType)
                                                                                                     throws com.vasoftware.sf.externalintegration.IntegrationFault,
                                                                                                     IntegrationFault {
        verifyKey(transactionKey);
        verifyExternalSystem(adapterType);
    }

    /**
     * @see com.vasoftware.sf.externalintegration.adapters.wandiscodaemon.WandiscoSubversionScmServerDaemon#getAccessLevel(String)
     */
    public java.lang.Integer getAccessLevel(final String transactionKey, final java.lang.String groupId)
                                                                                                        throws com.vasoftware.sf.externalintegration.IntegrationFault,
                                                                                                        IntegrationFault {
        verifyKey(transactionKey);
        return getAccessLevel(groupId);
    }

    /**
     * @see com.vasoftware.sf.externalintegration.adapters.wandiscodaemon.WandiscoSubversionScmServerDaemon#archiveRepository(String)
     */
    public java.lang.Boolean archiveRepository(final String transactionKey, final java.lang.String repositoryPath)
                                                                                                                  throws com.vasoftware.sf.externalintegration.IntegrationFault,
                                                                                                                  IntegrationFault {
        verifyKey(transactionKey);
        return archiveRepository(repositoryPath);
    }

    /**
     * @see com.vasoftware.sf.externalintegration.adapters.wandiscodaemon.WandiscoSubversionScmServerDaemon#groupExists(String)
     */
    public java.lang.Boolean groupExists(final String transactionKey, final java.lang.String groupId)
                                                                                                     throws com.vasoftware.sf.externalintegration.IntegrationFault,
                                                                                                     IntegrationFault {
        verifyKey(transactionKey);
        return groupExists(groupId);
    }

    /**
     * @see com.vasoftware.sf.externalintegration.adapters.wandiscodaemon.WandiscoSubversionScmServerDaemon#userExists(String)
     */
    public java.lang.Boolean userExists(final String transactionKey, final java.lang.String userId)
                                                                                                   throws com.vasoftware.sf.externalintegration.IntegrationFault,
                                                                                                   IntegrationFault {
        verifyKey(transactionKey);
        return userExists(userId);
    }

    /**
     * @see com.vasoftware.sf.externalintegration.adapters.wandiscodaemon.WandiscoSubversionScmServerDaemon#hasPermissions(String,
     *      String, String[], String[])
     */
    public java.lang.Boolean hasPermissions(final String transactionKey, final java.lang.String userId,
                                            final java.lang.String privilege, final java.lang.String[] files,
                                            final java.lang.String[] tag)
                                                                         throws com.vasoftware.sf.externalintegration.IntegrationFault,
                                                                         IntegrationFault {
        verifyKey(transactionKey);
        return hasPermissions(userId, privilege, files, tag);
    }

    /**
     * @see com.vasoftware.sf.externalintegration.adapters.wandiscodaemon.WandiscoSubversionScmServerDaemon#initializeExternalSystem(String)
     */
    public void initializeExternalSystem(final String transactionKey, final java.lang.String systemId)
                                                                                                      throws com.vasoftware.sf.externalintegration.IntegrationFault,
                                                                                                      IntegrationFault {
        verifyKey(transactionKey);
        initializeExternalSystem(systemId);
    }

    /**
     * @see com.vasoftware.sf.externalintegration.adapters.wandiscodaemon.WandiscoSubversionScmServerDaemon#getArchiveRepositoryRootPath()
     */
    public java.lang.String getArchiveRepositoryRootPath(final String transactionKey) throws IntegrationFault {
        verifyKey(transactionKey);
        return getArchiveRepositoryRootPath();
    }

    /**
     * @see com.vasoftware.sf.externalintegration.adapters.wandiscodaemon.WandiscoSubversionScmServerDaemon#setScmToProcess(com.vasoftware.sf.externalintegration.adapters.ScmDaemon)
     */
    public void setScmToProcess(final String transactionKey,
                                final com.vasoftware.sf.externalintegration.adapters.ScmDaemon scmDaemon)
                                                                                                         throws IntegrationFault {
        verifyKey(transactionKey);
        setScmToProcess(scmDaemon);
    }

    /**
     * @see com.vasoftware.sf.externalintegration.adapters.wandiscodaemon.WandiscoSubversionScmServerDaemon#beginBlackduckAnalysis(String,
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
     * @see com.vasoftware.sf.externalintegration.adapters.wandiscodaemon.WandiscoSubversionScmServerDaemon#cancelBlackduckAnalysis(String)
     */
    public void cancelBlackduckAnalysis(final String transactionKey, final java.lang.String externalBlackduckProjectId)
                                                                                                                       throws IntegrationFault {
        verifyKey(transactionKey);
        cancelBlackduckAnalysis(externalBlackduckProjectId);
    }

    /**
     * @see com.vasoftware.sf.externalintegration.adapters.wandiscodaemon.WandiscoSubversionScmServerDaemon#getBlackduckAnalysisStatus(String)
     */
    public java.lang.String getBlackduckAnalysisStatus(final String transactionKey,
                                                       final java.lang.String externalBlackduckProjectId)
                                                                                                         throws IntegrationFault {
        verifyKey(transactionKey);
        return getBlackduckAnalysisStatus(externalBlackduckProjectId);
    }

    /**
     * @see com.vasoftware.sf.externalintegration.adapters.wandiscodaemon.WandiscoSubversionScmServerDaemon#cleanupBlackduckRepository(String)
     */
    public void cleanupBlackduckRepository(final String transactionKey,
                                           final java.lang.String externalBlackduckProjectId)
                                                                                             throws com.vasoftware.sf.externalintegration.IntegrationFault,
                                                                                             IntegrationFault {
        verifyKey(transactionKey);
        cleanupBlackduckRepository(externalBlackduckProjectId);
    }

    /**
     * @see com.vasoftware.sf.externalintegration.adapters.wandiscodaemon.WandiscoSubversionScmServerDaemon#isBlackduckEnabled(String,
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
