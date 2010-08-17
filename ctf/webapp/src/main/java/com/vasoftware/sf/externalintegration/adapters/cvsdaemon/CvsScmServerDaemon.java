/*
 * CollabNet TeamForge
 * Copyright 2010 CollabNet, Inc.  All rights reserved.
 * http://www.collab.net
 */
package com.vasoftware.sf.externalintegration.adapters.cvsdaemon;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.vasoftware.sf.common.logger.Logger;
import com.vasoftware.sf.common.util.StringUtil;
import com.vasoftware.sf.externalintegration.IntegrationFault;
import com.vasoftware.sf.externalintegration.ObjectAlreadyExistsFault;
import com.vasoftware.sf.externalintegration.RBACConstants;
import com.vasoftware.sf.externalintegration.ScmConstants;
import com.vasoftware.sf.externalintegration.adapters.SynchronizedScmServerDaemon;
import com.vasoftware.sf.externalintegration.execution.CommandExecutor;
import com.vasoftware.sf.externalintegration.execution.CommandWrapperFault;
import com.vasoftware.sf.externalintegration.execution.GroupAlreadyExistsFault;
import com.vasoftware.sf.externalintegration.execution.UserAlreadyExistsFault;

/**
 * The <code>CvsScmServerDaemon</code> provides a simple implementation for the SCM adapter for CVS integration.
 */
public class CvsScmServerDaemon extends SynchronizedScmServerDaemon {
    private static final Logger smLogger = Logger.getLogger(CvsScmServerDaemon.class);

    private final CvsWrapper mCvs = new CvsWrapper(getCommandExecutor(), CvsWrapper.CvsType.SSH);

    /**
     * Constructor. Instantiates the OS command executor.
     * 
     * @throws IntegrationFault
     *             Thrown if getting the command executor fails.
     */
    public CvsScmServerDaemon() throws IntegrationFault {
        super();
    }

    /**
     * Create a new user on the SCM system
     * 
     * @param username
     *            The username to create
     * @param passwordCrypt
     *            user's password crypted. If null, account is disabled.
     * @throws UserAlreadyExistsFault
     *             if the user already exists on the system
     * @throws IntegrationFault
     *             If the user creation failed
     */
    public void createUser(final String username, final String passwordCrypt) throws UserAlreadyExistsFault,
                                                                             IntegrationFault {
        final String usernameLowered = username.toLowerCase();

        if (smLogger.isInfoEnabled()) {
            smLogger.info("Creating '" + username + "' as '" + usernameLowered + "' on the system");
        }

        try {
            mExecutor.createUser(usernameLowered, passwordCrypt);
        } catch (final CommandWrapperFault e) {
            throw new IntegrationFault(e);
        }
    }

    /**
     * @see com.vasoftware.sf.externalintegration.adapters.SynchronizedScmServerDaemon#addUsersToAccessGroup(java.lang.String[], java.lang.String)
     */
    @Override
    public String[] addUsersToAccessGroup(final String[] usernames, final String groupName) throws IntegrationFault {
        if (smLogger.isInfoEnabled()) {
            smLogger.info("Adding users '" + StringUtil.join(usernames, "', '") + "' to group '" + groupName + "'");
        }

        try {
            try {
                mExecutor.createGroup(groupName);
            } catch (final GroupAlreadyExistsFault e) {
                // Discard, as long as the group is there
            }
            final String[] newUsers = mExecutor.createUsersIfMissing(usernames);
            mExecutor.addUsersToGroup(groupName, usernames);
            return newUsers;
        } catch (final CommandWrapperFault e) {
            throw new IntegrationFault(e);
        }
    }

    /**
     * @see com.vasoftware.sf.externalintegration.adapters.SynchronizedScmServerDaemon#removeUsersFromAccessGroup(java.lang.String[], java.lang.String)
     */
    @Override
    public void removeUsersFromAccessGroup(final String[] usernames, final String groupName) throws IntegrationFault {
        if (smLogger.isInfoEnabled()) {
            smLogger.info("Removing users '" + StringUtil.join(usernames, "', '") + "' from group '" + groupName + "'");
        }

        try {
            mExecutor.removeUsersFromGroup(groupName, usernames);
        } catch (final CommandWrapperFault e) {
            throw new IntegrationFault(e);
        }
    }

    /**
     * changes a user's status to reflect the status value
     * 
     * @param usernames
     *            names of users to set stati for
     * @param stati
     *            array of status values
     * @throws IntegrationFault
     *             thrown if one or more of the underlying Operating system commands fail.
     */
    public void setUsersStatus(final String[] usernames, final String[] stati) throws IntegrationFault {
        if (smLogger.isInfoEnabled()) {
            smLogger.info("Setting users '" + StringUtil.join(usernames, "', '") + "' to statuses '"
                    + StringUtil.join(stati, "', '") + "'");
        }

        try {
            mExecutor.setUserStatuses(usernames, stati);
        } catch (final CommandWrapperFault e) {
            throw new IntegrationFault(e);
        }
    }

    /**
     * @see com.vasoftware.sf.externalintegration.adapters.SynchronizedScmServerDaemon#setAccessList(java.lang.String[], java.lang.String)
     */
    @Override
    public String[] setAccessList(final String[] usernames, final String groupName) throws IntegrationFault {

        if (smLogger.isInfoEnabled()) {
            smLogger.info("Setting users '" + StringUtil.join(usernames, "', '") + "' to group '" + groupName + "'");
        }

        // Dropped in to fix the "sync" button. That way, if the group doesn't
        // exist... we don't blow up.
        String[] existingUsers;

        try {
            existingUsers = mExecutor.listUsersInGroup(groupName);
        } catch (final CommandWrapperFault e) {
            // TODO: diagnose that this is really the issue
            try {
                mExecutor.createGroup(groupName);
            } catch (final GroupAlreadyExistsFault f) {
                throw new IntegrationFault("setAccessList(" + groupName + ") failed", f);
            } catch (final CommandWrapperFault f) {
                throw new IntegrationFault("setAccessList(" + groupName + ") failed", f);
            }
            existingUsers = new String[] {};
        }

        final List<String> usersToAdd = new ArrayList<String>();
        final List<String> usersToDelete = new ArrayList<String>();

        // We start out with all "existing" users in the "remove" map. Ensure
        // they are all lowercase
        // to deal with different operating systems (some maintain case, others
        // lower it)
        for (final String existingUser : existingUsers) {
            usersToDelete.add(existingUser.toLowerCase());
        }

        // We then iterate through and divide the usernames into two categories,
        // new ones to be added
        // and existing ones to be deleted
        for (final String username : usernames) {
            if (usersToDelete.contains(username.toLowerCase())) {
                usersToDelete.remove(username.toLowerCase());
            } else {
                usersToAdd.add(username);
            }
        }

        // Delete removed users
        removeUsersFromAccessGroup(usersToDelete.toArray(new String[usersToDelete.size()]), groupName);

        // Add users not in group
        return addUsersToAccessGroup(usersToAdd.toArray(new String[usersToAdd.size()]), groupName);
    }

    /**
     * @see com.vasoftware.sf.externalintegration.adapters.ScmDaemon#createRepository(java.lang.String, java.lang.String, java.lang.String, java.lang.String)
     */
    public String createRepository(final String repositoryGroup, final String repositoryDir, final String systemId,
                                   final String repositoryBaseUrl) throws IntegrationFault, ObjectAlreadyExistsFault {

        if (smLogger.isInfoEnabled()) {
            smLogger.info("Creating repository: '" + repositoryDir + "', with group '" + repositoryGroup
                    + "' on system '" + systemId + "'");
        }

        /*
         * In case of error, the system group created will have to be deleted if it didn't already exist. Keep track of
         * this status.
         * 
         * An alternate approach would be to have a cleanup method where the various things that may need cleaning up on
         * error (such as group and path) could be set. Then the clean up method could be called to do the clean up.
         * Initially, use this boolean. Later, consider the method.
         */
        final File repositoryDirFile = new File(repositoryDir);

        final CommandExecutor executor = getCommandExecutor();
        if (executor.pathExists(repositoryDirFile)) {
            smLogger.error("Directory '" + repositoryDir + "' already exists - failing.");
            throw new ObjectAlreadyExistsFault(repositoryDirFile.getAbsolutePath());
        }

        try {
            boolean deleteGroup = true;
            try {
                executor.createGroup(repositoryGroup);
            } catch (final GroupAlreadyExistsFault e) {
                // We take over the group. See titan-pd:artf2240 for details.
                smLogger.warn("Group '" + repositoryGroup + "' already exists - will be used as is.");
                deleteGroup = false;
            }

            try {
                executor.createPath(repositoryDirFile);
                executor.setUserOnPath(ScmConstants.CVS_USER, repositoryDirFile);
                mCvs.doInit(repositoryDirFile);

                // Set up all permissions for nobody.repositoryGroup
                executor.setGroupOnPath(repositoryGroup, repositoryDirFile);
                executor.setUserOnPath("nobody", repositoryDirFile);
                executor.setToOnlyReadWriteExecuteGroupUserPermissions(repositoryDirFile, false);

                setupRepository(systemId, repositoryGroup, repositoryDirFile.getAbsolutePath());

                return repositoryDirFile.getAbsolutePath();
            } catch (final CommandWrapperFault e) {
                if (deleteGroup) {
                    executor.deleteGroup(repositoryGroup);
                }
                executor.deletePath(repositoryDirFile);
                throw e;
            }
        } catch (final CommandWrapperFault f) {
            throw new IntegrationFault(f);
        }
    }

    /**
     * Verify that a repository exists at the following path.
     * 
     * @param repositoryDir
     *            the path to the new repository
     * @return returns the verified path to repository
     * @throws IntegrationFault
     *             thrown if the repository could not be verified.
     */
    public String verifyExistingRepositoryDirectory(final String repositoryDir) throws IntegrationFault {
        smLogger.info("Creating (rather, checking existence of) repository: '" + repositoryDir + "'");

        final File cvsrootDir = new File(repositoryDir, CvsWrapper.MODULE_CVSROOT);
        if (getCommandExecutor().pathExists(cvsrootDir)) {
            return repositoryDir;
        } else {
            smLogger.error("Directory '" + repositoryDir + "' does not exist");
            throw new IntegrationFault("Could not find path: " + cvsrootDir.getAbsolutePath());
        }
    }

    /**
     * @see com.vasoftware.sf.externalintegration.adapters.ScmDaemon#setRepositoryAccessLevel(java.lang.String, java.lang.String, java.lang.String)
     */
    public void setRepositoryAccessLevel(final String repositoryDir, final String repositoryId, final String level)
                                                                                                                   throws IntegrationFault {
        if (smLogger.isInfoEnabled()) {
            smLogger.info("Setting repository '" + repositoryDir + "' to access level '" + level + "'");
        }

        try {
            final File repositoryDirectory = new File(repositoryDir);
            final Integer accessLevel = new Integer(level);

            if (accessLevel.equals(RBACConstants.DEFAULT_USER_CLASS_ALL)
                    || accessLevel.equals(RBACConstants.DEFAULT_USER_CLASS_AUTHENTICATED)) {
                getCommandExecutor().setGroupOnPath(ScmConstants.ALL_USERS_GROUP, repositoryDirectory);
            } else if (accessLevel.equals(RBACConstants.DEFAULT_USER_CLASS_UNRESTRICTED)) {
                getCommandExecutor().setGroupOnPath(ScmConstants.UNRESTRICTED_USERS_GROUP, repositoryDirectory);
            } else if (accessLevel.equals(RBACConstants.DEFAULT_USER_CLASS_MEMBER)
                    || accessLevel.equals(RBACConstants.DEFAULT_USER_CLASS_NONE)) {
                getCommandExecutor().setGroupOnPath(repositoryId, repositoryDirectory);
            }
        } catch (final CommandWrapperFault f) {
            throw new IntegrationFault(f);
        }
    }

    /**
     * @see com.vasoftware.sf.externalintegration.adapters.SynchronizedScmServerDaemon#setupRepository(java.lang.String, java.lang.String, java.lang.String)
     */
    @Override
    public void setupRepository(final String systemId, final String repositoryGroup, final String repositoryDir)
                                                                                                                throws IntegrationFault {

        if (smLogger.isInfoEnabled()) {
            smLogger.info("Setting up repository: '" + repositoryDir + "', with group '" + repositoryGroup
                    + "' on system '" + systemId + "'");
        }

        try {
            final File repositoryDirectory = new File(repositoryDir);

            // ensure that the group already exists
            final CommandExecutor executor = getCommandExecutor();
            try {
                executor.createGroup(repositoryGroup);
            } catch (final GroupAlreadyExistsFault e) {
                // do nothing
            }
            // Set up all permissions for nobody.repositoryId
            executor.setGroupOnPath(repositoryGroup, repositoryDirectory);
            executor.setUserOnPath("nobody", repositoryDirectory);
            executor.setToOnlyReadWriteExecuteGroupUserPermissions(repositoryDirectory, false);

            mCvs.setupTriggers(systemId, repositoryDirectory);
        } catch (final CommandWrapperFault f) {
            throw new IntegrationFault(f);
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
                for (final int aCvsVersion : cvsVersion) {
                    message = message + aCvsVersion + ".";
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
        // Checkout CVS as root
        final CvsWrapper cvs = new CvsWrapper(getCommandExecutor(), CvsWrapper.CvsType.SSH);
        cvs.setCvsUser("root");
        try {
            cvs.doCheckout(repositoryRoot, repositoryPathFromRoot, destinationDirectory);
        } catch (final CommandWrapperFault f) {
            throw new IntegrationFault(f);
        }
    }

    /**
     * @see com.vasoftware.sf.externalintegration.adapters.SynchronizedScmServerDaemon#listGroupMembers(java.lang.String)
     */
    @Override
    public String[] listGroupMembers(final String groupName) throws IntegrationFault {
        try {
            return mExecutor.listUsersInGroup(groupName);
        } catch (final CommandWrapperFault f) {
            throw new IntegrationFault(f);
        }
    }

    /**
     * Get the access level of the repository
     * 
     * @see RBACConstants#DEFAULT_USER_CLASS_ALL
     * @see RBACConstants#DEFAULT_USER_CLASS_AUTHENTICATED
     * @see RBACConstants#DEFAULT_USER_CLASS_MEMBER
     * @see RBACConstants#DEFAULT_USER_CLASS_NONE
     * @see RBACConstants#DEFAULT_USER_CLASS_UNRESTRICTED
     * @param repositoryId
     *            The id of the repository
     * @param repositoryDir
     *            The on disk location of the repository directory
     * @return The access level of the repository
     * @throws IntegrationFault
     *             If there was an error executing the command
     */
    public Integer getAccessLevel(final String repositoryId, final String repositoryDir) throws IntegrationFault {
        try {
            final String details = mExecutor.getFileGroup(new File(repositoryDir));
            if (details.indexOf(ScmConstants.ALL_USERS_GROUP) > -1) {
                return RBACConstants.DEFAULT_USER_CLASS_ALL;
            } else if (details.indexOf(ScmConstants.UNRESTRICTED_USERS_GROUP) > -1) {
                return RBACConstants.DEFAULT_USER_CLASS_UNRESTRICTED;
            } else {
                return RBACConstants.DEFAULT_USER_CLASS_NONE;
            }
        } catch (final CommandWrapperFault f) {
            throw new IntegrationFault(f);
        }
    }

    /**
     * @see com.vasoftware.sf.externalintegration.adapters.SynchronizedScmServerDaemon#setPassword(java.lang.String, java.lang.String, java.lang.String)
     */
    @Override
    public void setPassword(final String username, final String password, final String cryptedPassword)
                                                                                                       throws IntegrationFault {
        if (smLogger.isInfoEnabled()) {
            smLogger.info("Setting password for user '" + username + "'");
        }

        final String usernameLowerCase = username.toLowerCase();
        try {
            mExecutor.setPassword(usernameLowerCase, cryptedPassword);
        } catch (final CommandWrapperFault fault) {
            // if we get a fault, try to create user first and then set password
            // again
            try {
                mExecutor.createUser(usernameLowerCase, cryptedPassword);
                mExecutor.setPassword(usernameLowerCase, cryptedPassword);
            } catch (final Throwable t) {
                throw new IntegrationFault("setPassword failed", fault);
            }
        }
    }

    /**
     * Create an authorized_keys file in the user's home directory.
     * 
     * @param username
     *            the username to create the file in
     * @param authorizedKeys
     *            the authorized keys file contents
     * @throws IntegrationFault
     *             thrown if the operation fails.
     */
    public void createAuthorizedKeysFile(final String username, final String authorizedKeys) throws IntegrationFault {
        final String usernameLowered = username.toLowerCase();

        if (smLogger.isInfoEnabled()) {
            smLogger.info("Creating '.ssh/authorized_keys' file for '" + usernameLowered + "'");
        }

        try {
            final String homedir = mExecutor.getUserHomeDirectoryFromOS(usernameLowered);
            final String sshDirectoryDir = homedir + File.separator + ScmConstants.SSH_RESOURCE_DIRECTORY;
            final String authorizedKeysLocation = sshDirectoryDir + File.separator
                    + ScmConstants.SSH_AUTHORIZED_KEYS_FILE;

            final File authorizedKeysFile = new File(authorizedKeysLocation);
            final File sshDirectory = new File(sshDirectoryDir);

            if (!mExecutor.pathExists(sshDirectory)) {
                mExecutor.createPath(sshDirectory);
                mExecutor.setUserOnPath(usernameLowered, sshDirectory);
                mExecutor.setToOnlyReadWriteExecuteUserPermissions(sshDirectory, true);
            }

            mExecutor.createFile(authorizedKeysFile, authorizedKeys);
            mExecutor.setUserOnPath(usernameLowered, authorizedKeysFile);
            mExecutor.setToOnlyReadWriteUserPermissions(authorizedKeysFile, false);
        } catch (final CommandWrapperFault f) {
            throw new IntegrationFault(f);
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
