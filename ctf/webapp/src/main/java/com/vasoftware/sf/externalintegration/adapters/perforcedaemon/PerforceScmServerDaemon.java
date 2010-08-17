/*
 * CollabNet TeamForge
 * Copyright 2010 CollabNet, Inc.  All rights reserved.
 * http://www.collab.net
 */

package com.vasoftware.sf.externalintegration.adapters.perforcedaemon;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.perforce.api.Client;
import com.perforce.api.CommitException;
import com.perforce.api.Env;
import com.vasoftware.sf.common.configuration.GlobalOptionKeys;
import com.vasoftware.sf.common.configuration.SfGlobalOptionsManager;
import com.vasoftware.sf.common.configuration.SfPaths;
import com.vasoftware.sf.common.logger.Logger;
import com.vasoftware.sf.common.util.StringUtil;
import com.vasoftware.sf.externalintegration.IntegrationFault;
import com.vasoftware.sf.externalintegration.ObjectAlreadyExistsFault;
import com.vasoftware.sf.externalintegration.RBACConstants;
import com.vasoftware.sf.externalintegration.UserConstants;
import com.vasoftware.sf.externalintegration.adapters.SynchronizedScmServerDaemon;
import com.vasoftware.sf.externalintegration.execution.CommandWrapperFault;
import com.vasoftware.sf.externalintegration.execution.ScmLimitationFault;
import com.vasoftware.sf.externalintegration.execution.UserAlreadyExistsFault;

/**
 * Provides the methods executed from the PerforceAdapter
 */
@SuppressWarnings("unchecked")
public class PerforceScmServerDaemon extends SynchronizedScmServerDaemon {

    /**
     * Name of client to create view for checkout of Blackduck repositories
     */
    public static final String SF_CLIENT_NAME = "SF_CLIENT";

    private final PerforceWrapper mP4;
    private static Logger smLogger = Logger.getLogger(PerforceScmServerDaemon.class);
    private static String smClientSyncObject = "just an object to synchronize on";
    private static Client smP4Client;
    private static final String TRIGGER_PREFIX_SUBMIT = "submit-";
    private static final String TRIGGER_PREFIX_COMMIT = "commit-";

    /**
     * default constructor for this class
     * 
     * @throws IntegrationFault
     *             thrown if no command executer could be found
     */
    public PerforceScmServerDaemon() throws IntegrationFault {
        super();
        mP4 = new PerforceWrapper(getCommandExecutor());
    }

    /**
     * Initialize this system. Sets the admin user to be only usable from localhost.
     * 
     * @param adminUser
     *            perforce admin username
     * @param adminPass
     *            perforce admin password
     * @param processOwner
     *            perforce server process owner
     * @throws IntegrationFault
     *             if it fails
     * @throws ScmLimitationFault
     *             if adding this admin user exceeds the license count
     */
    public void initializeSystem(final String adminUser, final String adminPass, final String processOwner)
    throws IntegrationFault,
    ScmLimitationFault {
        try {
            mP4.updateAdminInfo(adminUser, adminPass, processOwner, new String[] { TRIGGER_PREFIX_SUBMIT,
                                                                                   TRIGGER_PREFIX_COMMIT });
        } catch (final CommandWrapperFault f) {
            throw new IntegrationFault(f);
        }
    }

    /**
     * @see com.vasoftware.sf.externalintegration.adapters.ScmDaemon#createRepository(java.lang.String, java.lang.String, java.lang.String, java.lang.String)
     */
    public String createRepository(final String repositoryGroup, final String repositoryName, final String systemId,
                                   final String repositoryBaseUrl) throws IntegrationFault, ObjectAlreadyExistsFault {
        final String depotPath = "//" + repositoryName;
        try {
            mP4.createDepot(repositoryName);
        } catch (final CommandWrapperFault f) {
            throw new IntegrationFault(f);
        }

        // note: creating the group itself would be pointless, as they only
        // exist if they have members.

        setupRepository(systemId, repositoryGroup, depotPath);

        return depotPath;
    }

    /**
     * Verify that a repository exists at the following path.
     * 
     * @param depotName
     *            the name of he depot
     * @return returns the verified depot mapping path
     * @throws IntegrationFault
     *             thrown if the repository could not be verified.
     */
    public String verifyExistingDepot(final String depotName) throws IntegrationFault {
        smLogger.info("Verifying depot: '" + depotName + "'");

        try {
            final String depotPath = "//" + depotName;
            if (mP4.depotExists(depotName)) {
                return depotPath;
            } else {
                smLogger.error("Depot '" + depotPath + "' does not exist");
                throw new IntegrationFault("Could not find path: " + depotPath);
            }
        } catch (final CommandWrapperFault f) {
            throw new IntegrationFault(f);
        }
    }

    /**
     * @see com.vasoftware.sf.externalintegration.adapters.SynchronizedScmServerDaemon#setupRepository(java.lang.String, java.lang.String, java.lang.String)
     */
    @Override
    public void setupRepository(final String systemId, final String repositoryId, final String repositoryDir)
    throws IntegrationFault {

        try {
            final PerforceForm form = mP4.getProtectForm();
            final List protections = form.getListValue("Protections");
            boolean modified = false;

            // TODO: look for more general patterns instead of these exact ones
            final String defaultAccessForAllLine = "write user * * //...";
            /* default line that gives access to the world */
            if (protections.contains(defaultAccessForAllLine)) {
                protections.remove(defaultAccessForAllLine);
                modified = true;
            }

            final String repositoryLine = "write group " + repositoryId + " * " + repositoryDir + "/...";
            if (!protections.contains(repositoryLine)) {
                // Groups go at the start, so that disabled user restrictions at
                // the bottom override them
                protections.add(0, repositoryLine);
                modified = true;
            }

            if (modified) {
                form.setListValue("Protections", protections);
                mP4.executeWithInputForm("protect", form.toString(), null, null);
            }

            // add the triggers
            final File triggerDir = new File(SfPaths.getIntegrationScriptsRootPath());
            final File submitTriggerFile = new File(triggerDir, "PerforceSubmit.py");
            final String submitTriggerName = TRIGGER_PREFIX_SUBMIT + systemId + "-" + repositoryId;
            final File commitTriggerFile = new File(triggerDir, "PerforceCommit.py");
            final String commitTriggerName = TRIGGER_PREFIX_COMMIT + systemId + "-" + repositoryId;
            final String triggerPath = repositoryDir + "/...";
            final String sfPropertiesPath = SfGlobalOptionsManager.getSourceForgePropertiesPath();
            final String pythonPath = SfGlobalOptionsManager.getOptions()
                .getOption(GlobalOptionKeys.SCM_PYTHON_PATH);

            // Setup triggers
            try {
                final StringBuilder submitTriggerCommand = new StringBuilder()
                .append("export SOURCEFORGE_PROPERTIES_PATH='")
                .append(sfPropertiesPath)
                .append("'\n");
                if (null != pythonPath) {
                    submitTriggerCommand.append("export PYTHONPATH='")
                    .append(pythonPath)
                    .append("'\n");
                }
                submitTriggerCommand.append("\"")
                .append(submitTriggerFile.getAbsolutePath())
                .append(" ")
                .append(systemId)
                .append(" ")
                .append(mP4.getAdminUser())
                .append(" %user% ")
                .append(" %changelist% ")
                .append(repositoryDir)
                .append("\"");

                mP4.addOrReplaceTrigger(submitTriggerName, "submit", triggerPath, submitTriggerCommand.toString());

                final StringBuilder commitTriggerCommand = new StringBuilder()
                .append("export SOURCEFORGE_PROPERTIES_PATH='")
                .append(sfPropertiesPath)
                .append("'\n");
                if (null != pythonPath) {
                    commitTriggerCommand.append("export PYTHONPATH='")
                    .append(pythonPath)
                    .append("'\n");
                }
                commitTriggerCommand.append("\"")
                .append(commitTriggerFile.getAbsolutePath())
                .append(" ")
                .append(systemId)
                .append(" ")
                .append(mP4.getAdminUser())
                .append(" %user% ")
                .append(" %changelist% ")
                .append(repositoryDir)
                .append("\"")
                ;
                mP4.addOrReplaceTrigger(commitTriggerName, "commit", triggerPath, commitTriggerCommand.toString());
            } catch (final IOException e) {
                throw new IntegrationFault("setupRepository(" + repositoryId + ") failed", e);
            }
        } catch (final CommandWrapperFault f) {
            throw new IntegrationFault(f);
        }
    }

    /**
     * @see com.vasoftware.sf.externalintegration.adapters.ScmDaemon#verifyExternalSystem(java.lang.String)
     */
    public void verifyExternalSystem(final String adapterType) throws IntegrationFault {
        int[] version = null;

        try {
            version = mP4.getPerforceVersion();
        } catch (final CommandWrapperFault commandWrapperFault) {
            throw new IntegrationFault("Could not get perforce version", commandWrapperFault);
        }

        if (version == null) {
            throw new IntegrationFault("No p4 version found");
        }

        if (!((version[0] == 2004 && version[1] >= 2) || version[0] > 2004)) {
            throw new IntegrationFault("VERSION (requires 2004.2 or better): " + version[0] + "." + version[1]);
        }
        if (smLogger.isDebugEnabled()) {
            smLogger.debug("External System valid: " + adapterType + "(found version: " + version[0] + "." + version[1]
                                                                                                                     + ")");
        }
    }

    /**
     * @see com.vasoftware.sf.externalintegration.adapters.ScmDaemon#setRepositoryAccessLevel(java.lang.String, java.lang.String, java.lang.String)
     */
    public void setRepositoryAccessLevel(final String repositoryDir, final String repositoryId, final String level)
    throws IntegrationFault {

        if (smLogger.isInfoEnabled()) {
            smLogger.info("Not yet implemented: Setting repository '" + repositoryDir + "' to access level '" + level
                          + "'");
        }
    }

    /**
     * @see com.vasoftware.sf.externalintegration.adapters.SynchronizedScmServerDaemon#addUsersToAccessGroup(java.lang.String[], java.lang.String)
     */
    @Override
    public String[] addUsersToAccessGroup(final String[] usernames, final String groupName) throws IntegrationFault,
        ScmLimitationFault {

        if (smLogger.isInfoEnabled()) {
            smLogger.info("Adding users '" + StringUtil.join(usernames, "', '") + "' to group '" + groupName + "'");
        }

        if (usernames.length == 0) {
            return new String[0];
        }

        return setAccessList(usernames, groupName, true);
    }

    /**
     * Set the access list to the specified list, possibly including the current members.
     * 
     * @param usernames
     *            Usernames to be added to the group
     * @param groupName
     *            The group name to add user to
     * @param includeCurrent
     *            true if the current members of the group should be maintained in addition to the specified usernames
     * @return the list of newly created users on the system
     * @throws IntegrationFault
     *             An error occured while executing the command.
     * @throws ScmLimitationFault
     *             If a limitation was exceeded on the scm system
     */
    private String[] setAccessList(final String[] usernames, final String groupName, final boolean includeCurrent)
        throws IntegrationFault, ScmLimitationFault {

        try {
            final Set allCurrentUsers = mP4.getCurrentUserSet();

            final PerforceForm form = mP4.getGroupForm(groupName);

            Set updatedMembers;
            Set removedMembers;
            if (includeCurrent) {
                final List currentMembers = form.getListValue("Users");
                updatedMembers = new HashSet(currentMembers);
                removedMembers = new HashSet(currentMembers); // start with
                // current set -
                // will winnow
                // down
            } else {
                updatedMembers = new HashSet();
                removedMembers = new HashSet();

            }

            final List newUsers = new ArrayList();
            for (int i = 0; i < usernames.length; i++) {
                final String username = usernames[i];
                if (!updatedMembers.contains(username)) {
                    updatedMembers.add(username);
                    if (!allCurrentUsers.contains(username)) {
                        try {
                            createUser(username);
                        } catch (final UserAlreadyExistsFault userAlreadyExistsFault) {
                            ; // ignore
                        }
                        newUsers.add(username);
                    }
                }
                if (!includeCurrent) {
                    removedMembers.remove(username); // we won't be removing
                    // this person
                }
            }

            form.setListValue("Users", new ArrayList(updatedMembers));

            mP4.executeWithInputForm("group", form.toString(), null, null);

            if (!includeCurrent) {
                deleteUsersIfNoAccess(removedMembers);
            }

            return (String[]) newUsers.toArray(new String[newUsers.size()]);
        } catch (final CommandWrapperFault f) {
            throw new IntegrationFault(f);
        }
    }

    /**
     * Potentially delete the users if they no longer have any access to the system
     * 
     * @param removedUsers
     *            users who may need deletion from the system
     * @throws IntegrationFault
     *             if command fails
     */
    private void deleteUsersIfNoAccess(final Collection removedUsers) throws IntegrationFault {
        try {
            for (final Iterator iterator = removedUsers.iterator(); iterator.hasNext();) {
                final String username = (String) iterator.next();
                final String[] userGroups = mP4.getUserGroups(username);
                if (userGroups.length == 0) {
                    mP4.deleteUser(username, false);
                }
            }
        } catch (final CommandWrapperFault f) {
            throw new IntegrationFault(f);
        }
    }

    /**
     * @see com.vasoftware.sf.externalintegration.adapters.SynchronizedScmServerDaemon#setAccessList(java.lang.String[], java.lang.String)
     */
    @Override
    public String[] setAccessList(final String[] usernames, final String groupName) throws IntegrationFault,
    ScmLimitationFault {

        if (smLogger.isInfoEnabled()) {
            smLogger.info("Setting users '" + StringUtil.join(usernames, "', '") + "' to group '" + groupName + "'");
        }

        return setAccessList(usernames, groupName, false);
    }

    /**
     * Create a new user on the SCM system
     * 
     * @param username
     *            The username to create
     * @throws UserAlreadyExistsFault
     *             if the user already exists on the system
     * @throws IntegrationFault
     *             If the user creation failed
     * @throws ScmLimitationFault
     *             If a limitation was exceeded on the scm system
     */
    public void createUser(final String username) throws UserAlreadyExistsFault, IntegrationFault, ScmLimitationFault {
        if (smLogger.isDebugEnabled()) {
            smLogger.debug("Creating  user '" + username + "'");
        }

        // TODO: we may not want to actually do this - does it create users
        // before they really get access?
        try {
            mP4.createUser(username);
        } catch (final CommandWrapperFault f) {
            throw new IntegrationFault(f);
        }
    }

    /**
     * Delete user on the SCM system
     * 
     * @param username
     *            The username to delete
     * @throws IntegrationFault
     *             If the user creation failed
     */
    public void deleteUser(final String username) throws IntegrationFault {
        if (smLogger.isDebugEnabled()) {
            smLogger.debug("Deleting user '" + username + "'");
        }

        try {
            mP4.deleteUser(username, true);
        } catch (final CommandWrapperFault f) {
            throw new IntegrationFault(f);
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
            final PerforceForm form = mP4.getGroupForm(groupName);
            final List currentMembers = form.getListValue("Users");

            final Set updatedMembers = new HashSet(currentMembers);
            final List removedUsers = new ArrayList(usernames.length);
            for (int i = 0; i < usernames.length; i++) {
                final String username = usernames[i];
                updatedMembers.remove(username);
                removedUsers.add(username);
            }
            form.setListValue("Users", updatedMembers);

            mP4.executeWithInputForm("group", form.toString(), null, null);

            deleteUsersIfNoAccess(removedUsers);
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

        try {
            mP4.setPassword(username, password);
        } catch (final CommandWrapperFault f) {
            throw new IntegrationFault(f);
        }
    }

    /**
     * changes a user's status to reflect the status value
     * 
     * @param username
     *            name of user to set password for
     * @param status
     *            value of status
     * @param protectionList
     *            The list to generate
     */
    private void setUserStatus(final String username, final String status, final List protectionList) {
        final String disableLine = "super user " + username + " * " + "-/...";

        if (status.equals(UserConstants.DISABLED_USER_STATUS_NAME)) {
            if (!protectionList.contains(disableLine)) {
                // Disable lines go at the bottom, so that they override group
                // permissions at the top
                protectionList.add(disableLine);
            }
        } else if (status.equals(UserConstants.ACTIVE_USER_STATUS_NAME)) {
            if (protectionList.contains(disableLine)) {
                protectionList.remove(disableLine);
            }
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
        try {
            final PerforceForm protectForm = mP4.getProtectForm();
            final List protectionList = protectForm.getListValue("Protections");

            for (int i = 0; i < usernames.length; i++) {
                final String username = usernames[i];
                final String status = stati[i];
                setUserStatus(username, status, protectionList);
            }

            protectForm.setListValue("Protections", protectionList);
            mP4.executeWithInputForm("protect", protectForm.toString(), null, null);
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
            final PerforceForm form = mP4.getGroupForm(groupName);
            final List currentMembers = form.getListValue("Users");
            return (String[]) currentMembers.toArray(new String[currentMembers.size()]);
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
        return RBACConstants.DEFAULT_USER_CLASS_NONE;
        // TODO: WTF to do here?
    }

    /**
     * @see com.vasoftware.sf.externalintegration.adapters.ScmDaemon#verifyPath(String,String,String,String)
     */
    public void verifyPath(final String externalBlackduckProjectId, final String repositoryId, final String depot,
                           final String repositoryPathFromRoot) throws IntegrationFault {
        try {
            addView(externalBlackduckProjectId, depot);

            final String viewPath = getViewPath(depot, repositoryPathFromRoot);

            if (!mP4.directoryPathExists(SF_CLIENT_NAME, "//" + viewPath)) {
                throw new IntegrationFault("path does not exist:" + viewPath);
            }
        } catch (final CommandWrapperFault commandWrapperFault) {
            throw new IntegrationFault("unable to verify path: " + depot, commandWrapperFault);
        }
    }

    /**
     *@see com.vasoftware.sf.externalintegration.adapters.ScmDaemon#checkoutRepository(String,String,java.io.File)
     */
    public void checkoutRepository(final String depot, final String repositoryPathFromRoot,
                                   final File destinationDirectory) throws IntegrationFault {

        final String prefix = destinationDirectory.getName(); // essentially,
        // externalBlackduckProjectId

        addView(prefix, depot);

        final String viewPath = getViewPath(depot, repositoryPathFromRoot);

        try {
            // Sync client workspace
            mP4.sync(SF_CLIENT_NAME, "//" + viewPath + "/...", null);
        } catch (final CommandWrapperFault f) {
            throw new IntegrationFault(f);
        }
    }

    /**
     * Add view to client. This can be called multiple times as addView is idempotent.
     * 
     * @param externalBlackduckProjectId
     *            External blackduck project id, used as part of path
     * @param depot
     *            the perforce depot
     * @throws IntegrationFault
     *             if depot doesn't start with "//"
     */
    private void addView(final String externalBlackduckProjectId, final String depot) throws IntegrationFault {
        try {
            synchronized (smClientSyncObject) {
                if (smP4Client == null) {
                    final String userName = mP4.getAdminUser();
                    final String password = mP4.getAdminPassword();

                    final Env p4env = mP4.createP4Env(userName, password, SF_CLIENT_NAME);
                    smP4Client = mP4.createPerforceClient(p4env);
                }

                smP4Client.sync(); // note: not same as "p4 sync"

                smP4Client.setRoot(getBlackduckSourceRoot().getAbsolutePath());

                final String viewLocalMapping = getViewLocalMapping(externalBlackduckProjectId);
                smP4Client.addView(depot + "/...", viewLocalMapping + "/...");
                try {
                    smP4Client.commit();
                } catch (final CommitException e) {
                    throw new IntegrationFault("commit of addView failed", e);
                }
            }
        } catch (final CommandWrapperFault f) {
            throw new IntegrationFault(f);
        }
    }

    /**
     * Construct local mapping for view
     * 
     * @param prefix
     *            External blackduck project id, used as part of path
     * @return view mapping
     */
    private static String getViewLocalMapping(final String prefix) {
        return "//" + SF_CLIENT_NAME + "/" + prefix;
    }

    /**
     * Return Perforce path as defined by the client view
     * 
     * @param depot
     *            the perforce depot
     * @param repositoryPathFromRoot
     *            the path to the repository from the root path
     * @return perforce path
     */
    private static String getViewPath(final String depot, final String repositoryPathFromRoot) {
        final StringBuffer viewPath = new StringBuffer();

        if (depot.startsWith("//")) {
            viewPath.append(depot.substring(2));
        } else if (depot.startsWith("/")) {
            viewPath.append(depot.substring(1));
        } else {
            viewPath.append(depot);
        }

        if (repositoryPathFromRoot != null) {
            if (repositoryPathFromRoot.equals("/")) {
                ; // don't add ending slash
            } else if (repositoryPathFromRoot.startsWith("/")) {
                viewPath.append(repositoryPathFromRoot);
            } else {
                viewPath.append("/" + repositoryPathFromRoot);
            }
        }

        return viewPath.toString();
    }

    /**
     * @see com.vasoftware.sf.externalintegration.adapters.ScmScmServerDaemon#archiveRepository(java.lang.String)
     */
    @Override
    public Boolean archiveRepository(final String repositoryPath) throws IntegrationFault {
        // for now, this is a no-op.
        return Boolean.TRUE;

        // if we want to move the data aside, this would maintain a copy of the
        // current state,
        // but lose all history:

        // first, see if depot is already gone - if so, exit

        // create archive depot if necessary
        // add it to sf-admin's client
        // p4 integrate (to "copy" it)
        // p4 delete
        // p4 submit
    }

    /**
     * @see com.vasoftware.sf.externalintegration.adapters.ScmScmServerDaemon#getDaemonType()
     */
    @Override
    public String getDaemonType() {
        return "perforce";
    }
}
