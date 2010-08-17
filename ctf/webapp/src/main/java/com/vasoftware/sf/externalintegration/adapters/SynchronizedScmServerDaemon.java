/*
 * $RCSfile $
 * SourceForge(r) Enterprise Edition
 * Copyright 2007 CollabNet, Inc.  All rights reserved.
 * http://www.collab.net
 */

package com.vasoftware.sf.externalintegration.adapters;

import com.vasoftware.sf.externalintegration.IntegrationFault;
import com.vasoftware.sf.externalintegration.execution.ScmLimitationFault;


/**
 * Abstract parent of SCM servers which synchronize permissions between
 * SF and the SCM system's own permission system
 *
 * @author Priyanka Dhanda <pdhanda@vasoftware.com>
 * @author Jamison Gray <jgray@vasoftware.com>
 * @version $Revision: 1.8 $ $Date: 2007/05/24 00:37:28 $
 *
 * @sf.integration-soap-server
 */
public abstract class SynchronizedScmServerDaemon extends ScmScmServerDaemon {

    /**
     * We have to declare this trivial constructor here because the parent's throws
     * an exception.
     * @throws com.vasoftware.sf.externalintegration.IntegrationFault If the command did not execute correctly
     */
    public SynchronizedScmServerDaemon() throws IntegrationFault {
	super();
    }

    /**
     * Add all the necessary info into the specified repository
     * @param systemId The Guid of the system
     * @param repositoryId The guid of the repository
     * @param repositoryDir The name of the repository, the directory under the cvsroot
     * @throws IntegrationFault AN error while executing the commands.
     */
    public abstract void setupRepository(String systemId, String repositoryId, String repositoryDir)
	    throws IntegrationFault;

    /**
     * Add the users in usernames array to the group
     *
     * @param usernames Usernames to be added to the group
     * @param groupName The group name to add user to
     * @return the list of newly created users on the system
     * @throws IntegrationFault An error occured while executing the command.
     * @throws ScmLimitationFault If a limitation was exceeded on the scm system
     */
    public abstract String[] addUsersToAccessGroup(String[] usernames, String groupName)
	    throws IntegrationFault, ScmLimitationFault;

    /**
     * Removes the usernames in the usernames array from the group
     *
     * @param usernames Usernames to be removed from the group
     * @param groupName The group name to remove users from
     * @throws IntegrationFault An error occurred while executing the command.
     */
    public abstract void removeUsersFromAccessGroup(String[] usernames, String groupName) throws IntegrationFault;

    /**
     * List users that belong to the given group
     * @param groupName The name of the group to check
     * @return An array of usernames that are part of that group
     * @throws IntegrationFault If there was an error executing the command
     */
    public abstract String[] listGroupMembers(String groupName) throws IntegrationFault;

    /**
     * sets a user's password to a value. If the password is null, then this method
     * "locks" the account, making it impossible to log in.
     *
     * @param username name of user to set password for
     * @param password value of password
     * @param cryptedPassword value of the  crypted password
     * @throws IntegrationFault thrown if one or more of the underlying Operating system commands fail.
     */
    public abstract void setPassword(String username, String password, String cryptedPassword)
	    throws IntegrationFault;

    /**
     * Makes the given group contain exactly the usernames include in usernames
     *
     * @param usernames User names to be synced to the CVS server.
     * @param groupName The group name to add users to
     * @return An array of users created on this adapter.
     * @throws IntegrationFault An error occured while executing the command.
     * @throws ScmLimitationFault If a limitation was exceeded on the scm system     
     */
    public abstract String[] setAccessList(String[] usernames, String groupName)
	    throws IntegrationFault, ScmLimitationFault;
}
