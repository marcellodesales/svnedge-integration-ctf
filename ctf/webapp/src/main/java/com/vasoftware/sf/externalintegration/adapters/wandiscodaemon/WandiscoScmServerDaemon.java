/*
 * $RCSfile: WandiscoScmServerDaemon.java,v $
 *
 * SourceForge(r) Enterprise Edition
 * Copyright 2007 CollabNet, Inc.  All rights reserved.
 * http://www.collab.net
 *
 * The source code included in this listing is the confidential
 * and proprietary information of CollabNet, Inc.
 */

package com.vasoftware.sf.externalintegration.adapters.wandiscodaemon;

import java.io.File;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;

import com.vasoftware.sf.common.logger.Logger;
import com.vasoftware.sf.externalintegration.IntegrationFault;
import com.vasoftware.sf.externalintegration.ObjectAlreadyExistsFault;
import com.vasoftware.sf.externalintegration.RBACConstants;
import com.vasoftware.sf.externalintegration.ScmConstants;
import com.vasoftware.sf.externalintegration.adapters.SynchronizedScmServerDaemon;
import com.vasoftware.sf.externalintegration.execution.ObjectNotFoundFault;
import com.vasoftware.sf.externalintegration.execution.ScmLimitationFault;
import com.vasoftware.sf.externalintegration.execution.UserAlreadyExistsFault;
import com.wandisco.webservices.scmapi_1_0.ScmType;

/**
 * The <code>WandiscoScmServerDaemon</code> provides a simple implementation of the SCM adapter for WANdisco Integration
 * 
 * @version $Revision: 1.19 $ $Date: 2007/05/24 00:37:28 $
 * 
 * @sf.integration-soap-server
 */
@SuppressWarnings("unchecked")
public abstract class WandiscoScmServerDaemon extends SynchronizedScmServerDaemon {

    private static final Logger smLogger = Logger.getLogger(WandiscoScmServerDaemon.class);

    protected WandiscoWrapper mWandisco = null;

    /**
     * Constructor. Instantiates the OS command executor.
     * 
     * @throws IntegrationFault
     *             Thrown if getting the com mand executor fails.
     */
    public WandiscoScmServerDaemon() throws IntegrationFault {
        super();
    }

    /**
     * Return SCM type
     * 
     * @return SCM type
     */
    protected ScmType getScmType() {
        return null;
    }

    /**
     * Add all the necessary info into the specified repository
     * 
     * @param systemId
     *            The Guid of the system
     * @param repositoryId
     *            The id of the newly created repository
     * @param repositoryDir
     *            The name of the repository, the directory under the root directory
     * @throws IntegrationFault
     *             AN error while executing the com mands.
     */
    @Override
    public void setupRepository(final String systemId, final String repositoryId, final String repositoryDir)
                                                                                                             throws IntegrationFault {

        // create wandisco repository-write group
        try {
            mWandisco.createGroup(repositoryId + ScmConstants.WD_WRITE_GROUP_EXTENSION, "write group for "
                    + repositoryId);
        } catch (final ObjectAlreadyExistsFault e) {
            // TODO: what should be done if group already exists.. remove its
            // users?
        }
        // create wandisco repository-read group
        try {
            mWandisco
                     .createGroup(repositoryId + ScmConstants.WD_READ_GROUP_EXTENSION, "Read group for " + repositoryId);

        } catch (final ObjectAlreadyExistsFault e) {
            // TODO: what should be done if group already exists.. remove its
            // users?
        }
    }

    /**
     * Add the users in usernames array to the group
     * 
     * @param usernames
     *            Usernames to be added to the group
     * @param groupId
     *            The group name to add user to
     * @return the list of newly created users on the system
     * @throws IntegrationFault
     *             An error occurred while executing the command.
     * @throws com.vasoftware.sf.externalintegration.execution.ScmLimitationFault
     *             An error occured when we run out of Scm Licensing
     */
    @Override
    public String[] addUsersToAccessGroup(final String[] usernames, final String groupId) throws IntegrationFault,
                                                                                         ScmLimitationFault {
        if (smLogger.isDebugEnabled()) {
            smLogger.debug("addUsersToAccessGroup " + groupId);
        }
        mWandisco.addUsersToGroup(usernames, groupId);
        return usernames; // To change body of implemented methods use File |
        // Settings | File Templates.
    }

    /**
     * Removes the users in the usernames array from the group
     * 
     * @param usernames
     *            Usernames to be removed from the group
     * @param groupName
     *            The group name to remove users from
     * @throws IntegrationFault
     *             An error occurred while executing the command.
     */
    @Override
    public void removeUsersFromAccessGroup(final String[] usernames, final String groupName) throws IntegrationFault {
        removeUsersFromAccessGroups(usernames, new String[] { groupName });
    }

    /**
     * Removes the username in the usernames array from the group
     * 
     * @param usernames
     *            Usernames to be removed from the group
     * @param groupNames
     *            The groups to remove users from
     * @throws IntegrationFault
     *             An error occurred while executing the command.
     */
    public void removeUsersFromAccessGroups(final String[] usernames, final String[] groupNames)
                                                                                                throws IntegrationFault {

        for (int i = 0; i < groupNames.length; i++) {
            final String groupname = groupNames[i];
            if (groupname != null) {
                mWandisco.removeUsersFromGroup(usernames, groupname);
            }
        }

    }

    /**
     * Removes the username in the usernames array from the group
     * 
     * @throws IntegrationFault
     *             An error occurred while executing the command.
     */
    public void deleteAllUsers() throws IntegrationFault {

        mWandisco.deleteAllUsers();

    }

    /**
     * List users that belong to the given group
     * 
     * @param groupName
     *            The name of the group to check
     * @return An array of usernames that are part of that group
     * @throws IntegrationFault
     *             If there was an error executing the command
     */
    @Override
    public String[] listGroupMembers(final String groupName) throws IntegrationFault {
        try {
            return mWandisco.listGroupMembers(groupName);
        } catch (final ObjectNotFoundFault e) {
            throw new IntegrationFault(e);
        }
    }

    /**
     * Makes the given group contain exactly the usernames include in usernames
     * 
     * @param usernames
     *            User names to be synced to the SCM server.
     * @param groupId
     *            The group name to add users to
     * @return An array of users created on this adapter.
     * @throws IntegrationFault
     *             An error occurred while executing the command.
     * @throws com.vasoftware.sf.externalintegration.execution.ScmLimitationFault
     *             An error occured when we run out of Scm Licensing
     */
    @Override
    public String[] setAccessList(final String[] usernames, final String groupId) throws IntegrationFault,
                                                                                 ScmLimitationFault {
        mWandisco.modifyUsersGroupAssociation(usernames, groupId);

        // TODO: should only return the new users
        return usernames;
    }

    /**
     * Implementation of create repository method.
     * 
     * @param repositoryId
     *            the id of the repository to create
     * @param repositoryDir
     *            The path to the repository
     * @param systemId
     *            The Guid of the system this adapter is running on
     * @param repositoryBaseUrl
     *            the repository base url (wandisco subversion uses this)
     * @return the repository filesystem path of the new repository
     * @throws IntegrationFault
     *             An error occurred while executing the command.
     */
    public abstract String createRepository(String repositoryId, String repositoryDir, String systemId,
                                            String repositoryBaseUrl) throws IntegrationFault;

    /**
     * Create the ACLs for the specified operation.
     * 
     * @param repositoryDir
     *            The full directory of the mounted CC vob
     * @param groupId
     *            The repository's ID to use as the group for member and private access
     * @param level
     *            the access level
     * @param operationName
     *            the operation name for which we want set the ACL
     * @throws ObjectAlreadyExistsFault
     *             if one of the acls we try to create already exst
     * @throws IntegrationFault
     *             An error occurred while executing the command.
     * @throws IntegrationFault
     *             any errors
     */
    public void setACLs(final String repositoryDir, final String groupId, final String level, final String operationName)
                                                                                                                         throws ObjectAlreadyExistsFault,
                                                                                                                         IntegrationFault {
    }

    /**
     * Create the initial groups on the Wandisco system.
     * 
     * @param scmRoot
     *            the repository root for the new system
     * @throws IntegrationFault
     *             An error occurred while executing the command.
     */
    public void createInitialGroupsAndAcls(final String scmRoot) throws IntegrationFault {
        // first create the unrestricted group
        try {
            mWandisco.createGroup(ScmConstants.UNRESTRICTED_USERS_GROUP, "Group for SFEE unrestricted users");
        } catch (final ObjectAlreadyExistsFault e) {
            // TODO: what should be done if group already exists.. remove its
            // user?
        }
        // create the sfall group
        try {
            mWandisco.createGroup(ScmConstants.ALL_USERS_GROUP, "Group for all users");
        } catch (final ObjectAlreadyExistsFault e) {
            // TODO: what should be done if group already exists.. remove its
            // user?
        }

    }

    /**
     * Create the ACLs for the specified operation.
     * 
     * @param repositoryDir
     *            The full directory of the mounted CC vob
     * @param groupId
     *            The repository's ID to use as the group for member and private access
     * @param level
     *            the access level
     * @param operationName
     *            the operation name for which we want set the ACL
     * @param regex
     *            the regular expresion for the root directory
     * @throws ObjectAlreadyExistsFault
     *             if one of the acls we try to create already exst the command.
     * @throws IntegrationFault
     *             An error occurred while executing the command.
     */
    protected void setACLs(final String repositoryDir, final String groupId, final String level,
                           final String operationName, final String regex) throws ObjectAlreadyExistsFault,
                                                                          IntegrationFault {

        if (smLogger.isInfoEnabled()) {
            smLogger.info("Setting repository '" + repositoryDir + "' to access level '" + level + "'");
        }

        final Integer accessLevel = new Integer(level);
        // create the ACLs

        if (ScmConstants.REPOSITORY_EDIT_OPERATION_NAME.equals(operationName)) {

            mWandisco.createACLIfNotExist(true, ScmConstants.WD_ADMIN_PERMISSION, groupId, repositoryDir + "/.*", true,
                                          getAclExternalId(groupId));

        } else if (ScmConstants.REPOSITORY_USE_OPERATION_NAME.equals(operationName)) {
            // if the project has not unrestrited level for this operation, try
            // to remove the unrestricted rule
            removeUnrestrictedRuleForGroup(accessLevel, groupId);

            if (accessLevel.equals(RBACConstants.DEFAULT_USER_CLASS_AUTHENTICATED)) {

                mWandisco.createACLIfNotExist(true, ScmConstants.WD_ADMIN_PERMISSION, ScmConstants.ALL_USERS_GROUP,
                                              repositoryDir + regex, true, getAclExternalId(groupId));

            } else if (accessLevel.equals(RBACConstants.DEFAULT_USER_CLASS_UNRESTRICTED)) {

                mWandisco.createACLIfNotExist(true, ScmConstants.WD_ADMIN_PERMISSION,
                                              ScmConstants.UNRESTRICTED_USERS_GROUP, repositoryDir + regex, true,
                                              getAclExternalId(groupId, true));

                mWandisco.createACLIfNotExist(true, ScmConstants.WD_ADMIN_PERMISSION, groupId, repositoryDir + regex,
                                              true, getAclExternalId(groupId));

            } else if (accessLevel.equals(RBACConstants.DEFAULT_USER_CLASS_MEMBER)
                    || accessLevel.equals(RBACConstants.DEFAULT_USER_CLASS_NONE)) {

                mWandisco.createACLIfNotExist(true, ScmConstants.WD_ADMIN_PERMISSION, groupId, repositoryDir + regex,
                                              true, getAclExternalId(groupId));

            }
        } else if (ScmConstants.REPOSITORY_VIEW_OPERATION_NAME.equals(operationName)) {
            // if the project has not unrestrited level for this operation, try
            // to remove the unrestricted rule
            removeUnrestrictedRuleForGroup(accessLevel, groupId);

            if (accessLevel.equals(RBACConstants.DEFAULT_USER_CLASS_ALL)
                    || accessLevel.equals(RBACConstants.DEFAULT_USER_CLASS_AUTHENTICATED)) {
                mWandisco.createACLIfNotExist(true, ScmConstants.WD_READ_PERMISSION, ScmConstants.ALL_USERS_GROUP,
                                              repositoryDir + regex, true, getAclExternalId(groupId));
            } else if (accessLevel.equals(RBACConstants.DEFAULT_USER_CLASS_UNRESTRICTED)) {
                mWandisco.createACLIfNotExist(true, ScmConstants.WD_READ_PERMISSION,
                                              ScmConstants.UNRESTRICTED_USERS_GROUP, repositoryDir + regex, true,
                                              getAclExternalId(groupId, true));
                mWandisco.createACLIfNotExist(true, ScmConstants.WD_READ_PERMISSION, groupId, repositoryDir + regex,
                                              true, getAclExternalId(groupId));
            } else if (accessLevel.equals(RBACConstants.DEFAULT_USER_CLASS_MEMBER)
                    || accessLevel.equals(RBACConstants.DEFAULT_USER_CLASS_NONE)) {
                mWandisco.createACLIfNotExist(true, ScmConstants.WD_READ_PERMISSION, groupId, repositoryDir + regex,
                                              true, getAclExternalId(groupId));
            }
        }
    }

    /**
     * Remove the unrestricted rule for a repository if the access level is not unrestricted
     * 
     * @param accessLevel
     *            the acces level
     * @param groupId
     *            the group id (ie reps101-read)
     * @throws IntegrationFault
     *             if an error occured
     */
    private void removeUnrestrictedRuleForGroup(final Integer accessLevel, final String groupId)
                                                                                                throws IntegrationFault {
        // if the access level is not unrestrited, make sure the unrestricted
        // rule is not there
        if (!accessLevel.equals(RBACConstants.DEFAULT_USER_CLASS_UNRESTRICTED)) {
            try {
                mWandisco.deleteACL(getAclExternalId(groupId, true));
            } catch (final ObjectNotFoundFault e) {
                // do nothing
            }
        }
    }

    /**
     * Set the repository access level (public, gated, member, private) by changing the group ownership
     * 
     * @param repositoryDir
     *            The full directory of the mounted CC vob
     * @param groupId
     *            The repository's ID to use as the group for member and private access
     * @param level
     *            the access level to change to (from RBACConstants)
     * @throws IntegrationFault
     *             An error occurred while executing the command.
     */
    public void setRepositoryAccessLevel(final String repositoryDir, final String groupId, final String level)
                                                                                                              throws IntegrationFault {
        // this operations should not be used for WD
        // setACLs(repositoryDir, groupId, level,
        // RepositoryType.CATEGORY_VIEW.VIEW);
    }

    /**
     * @see com.vasoftware.sf.externalintegration.adapters.ScmDaemon#checkoutRepository(String,String,java.io.File)
     */
    public void checkoutRepository(final String repositoryPath, final String repositoryPathFromRoot,
                                   final File destinationDirectory) throws IntegrationFault {
        // To change body of implemented methods use File | Settings | File
        // Templates.
    }

    /**
     * @see com.vasoftware.sf.externalintegration.adapters.ScmDaemon#verifyPath(String,String,String,String)
     */
    public void verifyPath(final String externalBlackduckProjectId, final String repositoryId,
                           final String repositoryPath, final String repositoryPathFromRoot) throws IntegrationFault {
        // To change body of implemented methods use File | Settings | File
        // Templates.
    }

    /**
     * Add the users to Wandisco system.
     * 
     * @param usernames
     *            Usernames to be added
     * @param md5Passwords
     *            the apache md5 passwords for each user
     * @return the users that got created
     * @throws IntegrationFault
     *             An error occurred while executing the command.
     */
    public String[] addUsers(final String[] usernames, final String[] md5Passwords) throws IntegrationFault {
        final List usersCreated = new ArrayList();

        for (int i = 0; i < usernames.length; i++) {
            try {
                mWandisco.createUser(usernames[i], true);
                usersCreated.add(usernames[i]);
            } catch (final UserAlreadyExistsFault e) {
                // do nothing
            }
        }
        return (String[]) usersCreated.toArray(new String[] {});
    }

    /**
     * Add the users to Wandisco system and add him to the sfall group.
     * 
     * @param userName
     *            the user to add
     * @throws IntegrationFault
     *             An error occurred while executing the command.
     * @throws com.vasoftware.sf.externalintegration.execution.UserAlreadyExistsFault
     *             if the user already existed
     */
    public void createUser(final String userName) throws IntegrationFault, UserAlreadyExistsFault {
        mWandisco.createUser(userName, true);
        mWandisco.addUsersToGroup(new String[] { userName }, ScmConstants.ALL_USERS_GROUP);
    }

    /**
     * deletes a user from Wandisco system.
     * 
     * @param userName
     *            the user to add
     * @throws ObjectNotFoundFault
     *             if the user we want to delete is not found on WD system
     * @throws IntegrationFault
     *             An error occurred while executing the command.
     */
    public void deleteUser(final String userName) throws IntegrationFault, ObjectNotFoundFault {
        mWandisco.deleteUser(userName);
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
        mWandisco.createAuthorizedKeysFile(usernameLowered, authorizedKeys);
    }

    /**
     * get the name for aclExternalId based on the groupid
     * 
     * @param groupId
     *            the group id for which we want to create an acl
     * @param aclForUnrestrictedGroup
     *            if it is acl for the unrestricted group
     * @return the aclExternalId
     */
    protected String getAclExternalId(final String groupId, final boolean aclForUnrestrictedGroup) {

        String aclId = "acl" + groupId;
        if (aclForUnrestrictedGroup) {
            aclId = aclId + "unrest";
        }
        return aclId;
    }

    /**
     * get the name for aclExternalId based on the groupid
     * 
     * @param groupId
     *            the group id
     * @return the aclExternalId
     */
    protected String getAclExternalId(final String groupId) {
        return getAclExternalId(groupId, false);
    }

    /**
     * Delete the system wide groups and ACLS
     * 
     * @param systemId
     *            The id of the system
     * @throws IntegrationFault
     *             if an error is encountered
     */
    @Override
    public void deleteExternalSystem(final String systemId) throws IntegrationFault {

        // delete the unrestricted group
        try {
            mWandisco.deleteGroup(ScmConstants.UNRESTRICTED_USERS_GROUP);
        } catch (final ObjectNotFoundFault e) {
            // do nothing
        }

        try {
            // delete the sfall group
            mWandisco.deleteGroup(ScmConstants.ALL_USERS_GROUP);
        } catch (final ObjectNotFoundFault e) {
            // do nothing
        }
        // delete the users from the ADMIN group
        try {
            final String[] adminUsers = mWandisco.listGroupMembers(ScmConstants.WD_ADMIN_USERS_GROUP);
            if (adminUsers != null) {
                mWandisco.removeUsersFromGroup(adminUsers, ScmConstants.WD_ADMIN_USERS_GROUP);
            }
        } catch (final ObjectNotFoundFault e) {
            throw new IntegrationFault(e);
        }

        // delete all the users on WD database and system database
        mWandisco.deleteAllUsers();

    }

    /**
     * Delete the groups and ACLS specifc to repository
     * 
     * @param repositoryId
     *            the repository root
     * @throws IntegrationFault
     *             An error occurred while executing the command.
     */
    public void deleteRepositoryGroupsAndAcls(final String repositoryId) throws IntegrationFault {
        final String readGroupId = repositoryId + ScmConstants.WD_READ_GROUP_EXTENSION;
        final String writeGroupId = repositoryId + ScmConstants.WD_WRITE_GROUP_EXTENSION;
        // delete the read, write groups
        try {
            mWandisco.deleteGroup(readGroupId);
        } catch (final ObjectNotFoundFault e) {
            // do nothing
        }
        try {
            mWandisco.deleteGroup(writeGroupId);
        } catch (final ObjectNotFoundFault e) {
            // do nothing
        }
        // delete the ACLs for each of this group
        try {
            mWandisco.deleteACL(getAclExternalId(readGroupId));
        } catch (final ObjectNotFoundFault e) {
            // do nothing
        }
        try {
            mWandisco.deleteACL(getAclExternalId(writeGroupId));
        } catch (final ObjectNotFoundFault e) {
            // do nothing
        }
        try {
            mWandisco.deleteACL(getAclExternalId(writeGroupId, true));
        } catch (final ObjectNotFoundFault e) {
            // do nothing
        }
        try {
            mWandisco.deleteACL(getAclExternalId(readGroupId, true));
        } catch (final ObjectNotFoundFault e) {
            // do nothing
        }
    }

    /**
     * Verify that the WANdisco server is up and running.
     * 
     * @param adapterType
     *            The type of adapter we are expected to be.
     * @throws com.vasoftware.sf.externalintegration.IntegrationFault
     *             if the command could not be executed
     */
    public void verifyExternalSystem(final String adapterType) throws IntegrationFault {
        if (adapterType.indexOf(getSupportedAdapterType()) == -1) {
            throw new IntegrationFault("Unsupported adapter type: " + adapterType + ". Supported Types are:"
                    + getSupportedAdapterType());
        }

        try {
            mWandisco.verifyConnection();
        } catch (final RemoteException e) {
            throw new IntegrationFault("Failed to verify connection with WANdisco server: " + e.toString());
        }
    }

    /**
     * Get the access level of the repository/operation. Cuuld be one of the following
     * 
     * @see RBACConstants#DEFAULT_USER_CLASS_ALL
     * @see RBACConstants#DEFAULT_USER_CLASS_NONE
     * @see RBACConstants#DEFAULT_USER_CLASS_UNRESTRICTED
     * @param groupId
     *            The id of the group (will say which operation we are querying)
     * 
     * @return The access level of the repository
     * @throws IntegrationFault
     *             If there was an error executing the command
     */
    public Integer getAccessLevel(final String groupId) throws IntegrationFault {
        // query to see if rule for unrestricted group exists
        try {
            mWandisco.queryACL(getAclExternalId(groupId, true));
            return RBACConstants.DEFAULT_USER_CLASS_UNRESTRICTED;
        } catch (final ObjectNotFoundFault e) {
            // do nothing
        }
        // query to get the acl for the group
        try {
            final AclDO aclDO = mWandisco.queryACL(getAclExternalId(groupId, false));
            final String group = aclDO.getUserGroupPattern();
            if (groupId.equals(group)) {
                return RBACConstants.DEFAULT_USER_CLASS_NONE;
            } else if (ScmConstants.ALL_USERS_GROUP.equals(group)) {
                return RBACConstants.DEFAULT_USER_CLASS_ALL;
            } else {
                return null;

            }
        } catch (final ObjectNotFoundFault e) {
            return null;
        }

    }

    /**
     * @see com.vasoftware.sf.externalintegration.adapters.ScmDaemon#archiveRepository(String)
     */
    @Override
    public Boolean archiveRepository(final String repositoryPath) throws IntegrationFault {

        mWandisco.archiveRepository(repositoryPath, getArchiveRepositoryRootPath());

        return Boolean.TRUE;

    }

    /**
     * verifies wether a group exists or not on Wandisco system.
     * 
     * @param groupId
     *            the group id
     * @return true if the group exists on WD system
     * @throws IntegrationFault
     *             An error occurred while executing the command.
     */
    public Boolean groupExists(final String groupId) throws IntegrationFault {
        return Boolean.valueOf(mWandisco.groupExists(groupId));
    }

    /**
     * verifies wether a user exists or not on Wandisco system.
     * 
     * @param userId
     *            the user id
     * @return true if the user exists on WD system
     * @throws IntegrationFault
     *             An error occurred while executing the command.
     */
    public Boolean userExists(final String userId) throws IntegrationFault {
        return Boolean.valueOf(mWandisco.userExists(userId));
    }

    /**
     * verifies wether a user has permissions or not on a list of files
     * 
     * @param userId
     *            the user id
     * @param privilege
     *            the privilege ie READ, WRITE
     * @param files
     *            the list of files for which we are checking
     * @param tag
     *            the list of tags for which we are checking
     * @return true if the user has permissions
     * @throws IntegrationFault
     *             An error occurred while executing the command.
     */
    public Boolean hasPermissions(final String userId, final String privilege, final String[] files, final String[] tag)
                                                                                                                        throws IntegrationFault {
        return Boolean.valueOf(mWandisco.hasPermissions(userId, privilege, files, tag));
    }

    /**
     * get the supported adapter type for this daemon
     * 
     * @return the supported adapter type
     */
    protected abstract String getSupportedAdapterType();
}
