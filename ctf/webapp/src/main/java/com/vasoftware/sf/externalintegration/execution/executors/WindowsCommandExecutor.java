/*
 * CollabNet TeamForge
 * Copyright 2010 CollabNet, Inc.  All rights reserved.
 * http://www.collab.net
 *
 * The source code included in this listing is the confidential
 * and proprietary information of CollabNet, Inc.
 */
package com.vasoftware.sf.externalintegration.execution.executors;

import java.io.File;

import com.vasoftware.sf.externalintegration.execution.CommandExecutor;
import com.vasoftware.sf.externalintegration.execution.CommandResult;
import com.vasoftware.sf.externalintegration.execution.CommandWrapperFault;
import com.vasoftware.sf.externalintegration.execution.GroupAlreadyExistsFault;
import com.vasoftware.sf.externalintegration.execution.UserAlreadyExistsFault;

/**
 * The WindowsExecutor provides the necessary methods for performing
 * command execution for the TeamForge integration servers.
 */
public class WindowsCommandExecutor extends AbstractCommandExecutor implements CommandExecutor {
    /**
     * @see com.vasoftware.sf.externalintegration.execution.CommandExecutor#activateUser(java.lang.String)
     */
    @Override
    public void activateUser(String username) throws CommandWrapperFault {
        ; // Nothing to do
    }

    /**
     * @see com.vasoftware.sf.externalintegration.execution.CommandExecutor#addUserToGroup(java.lang.String,
     *                                                                                     java.lang.String)
     */
    @Override
    public void addUserToGroup(String groupname, String username) throws CommandWrapperFault {
        ; // Nothing to do
    }

    /**
     * @see com.vasoftware.sf.externalintegration.execution.CommandExecutor#addUsersToGroup(java.lang.String,
     *                                                                                      java.lang.String[])
     */
    @Override
    public void addUsersToGroup(String groupname, String[] usernames) throws CommandWrapperFault {
        ; // Nothing to do
    }

    /**
     * @see com.vasoftware.sf.externalintegration.execution.CommandExecutor#createGroup(java.lang.String)
     */
    @Override
    public void createGroup(String groupname) throws CommandWrapperFault, GroupAlreadyExistsFault {
        ; // Nothing to do
    }

    /**
     * @see com.vasoftware.sf.externalintegration.execution.CommandExecutor#createUser(java.lang.String,
     *                                                                                 java.lang.String)
     */
    @Override
    public void createUser(String username, String passwordCrypt) throws CommandWrapperFault, UserAlreadyExistsFault {
        ; // Nothing to do
    }

    /**
     * @see com.vasoftware.sf.externalintegration.execution.CommandExecutor#deactivateUser(java.lang.String)
     */
    @Override
    public void deactivateUser(String username) throws CommandWrapperFault {
        ; // Nothing to do
    }

    /**
     * @see com.vasoftware.sf.externalintegration.execution.CommandExecutor#deleteGroup(java.lang.String)
     */
    @Override
    public void deleteGroup(String groupname) throws CommandWrapperFault {
        ; // Nothing to do
    }

    /**
     * @see com.vasoftware.sf.externalintegration.execution.CommandExecutor#deleteUser(java.lang.String)
     */
    @Override
    public void deleteUser(String username) throws CommandWrapperFault {
        ; // Nothing to do
    }

    /**
     * @see com.vasoftware.sf.externalintegration.execution.CommandExecutor#getFileGroup(java.io.File)
     */
    @Override
    public String getFileGroup(File repositoryDir) throws CommandWrapperFault {
        return null;
    }

    /**
     * @see com.vasoftware.sf.externalintegration.execution.CommandExecutor#getUserHomeDirectoryFromOS(java.lang.String)
     */
    @Override
    public String getUserHomeDirectoryFromOS(String username) throws CommandWrapperFault {
        return null;
    }

    /**
     * @see com.vasoftware.sf.externalintegration.execution.CommandExecutor#listUsersInGroup(java.lang.String)
     */
    @Override
    public String[] listUsersInGroup(String groupName) throws CommandWrapperFault {
        return null;
    }

    /**
     * @see com.vasoftware.sf.externalintegration.execution.CommandExecutor#removeGroupOtherPermissions(java.io.File,
     *                                                                                                  boolean)
     */
    @Override
    public void removeGroupOtherPermissions(File file, boolean recursive) throws CommandWrapperFault {
        ; // Nothing to do
    }

    /**
     * @see com.vasoftware.sf.externalintegration.execution.CommandExecutor#removeUserFromGroup(java.lang.String,
     *                                                                                          java.lang.String)
     */
    @Override
    public void removeUserFromGroup(String groupname, String username) throws CommandWrapperFault {
        ; // Nothing to do
    }

    /**
     * @see com.vasoftware.sf.externalintegration.execution.CommandExecutor#removeUsersFromGroup(java.lang.String,
     *                                                                                           java.lang.String[])
     */
    @Override
    public void removeUsersFromGroup(String groupname, String[] usernames) throws CommandWrapperFault {
        ; // Nothing to do
    }

    /**
     * @see com.vasoftware.sf.externalintegration.execution.CommandExecutor#setGroupOnPath(java.lang.String,
     *                                                                                     java.io.File)
     */
    @Override
    public void setGroupOnPath(String groupName, File directory) throws CommandWrapperFault {
        ; // Nothing to do
    }

    /**
     * @see com.vasoftware.sf.externalintegration.execution.CommandExecutor
     *     #setOwnerToOnlyReadWritePermissions(java.lang.String, java.io.File, boolean)
     */
    @Override
    public void setOwnerToOnlyReadWritePermissions(String owner, File file, boolean recursive)
        throws CommandWrapperFault {
        ; // Nothing to do
    }

    /**
     * @see com.vasoftware.sf.externalintegration.execution.CommandExecutor
     *     #setOwnerToRwxOthersRxPermissions(java.lang.String, java.io.File, boolean)
     */
    @Override
    public void setOwnerToRwxOthersRxPermissions(String owner, File file, boolean recursive) throws CommandWrapperFault {
        ; // Nothing to do
    }

    /**
     * @see com.vasoftware.sf.externalintegration.execution.CommandExecutor#setPassword(java.lang.String,
     *                                                                                  java.lang.String)
     */
    @Override
    public void setPassword(String username, String passwordCrypted) throws CommandWrapperFault {
        ; // Nothing to do
    }

    /**
     * @see com.vasoftware.sf.externalintegration.execution.CommandExecutor
     *     #setToOnlyReadGroupUserPermissions(java.io.File, boolean)
     */
    @Override
    public void setToOnlyReadGroupUserPermissions(File directory, boolean recursive) throws CommandWrapperFault {
        ; // Nothing to do
    }

    /**
     * @see com.vasoftware.sf.externalintegration.execution.CommandExecutor
     *     #setToOnlyReadWriteExecuteGroupUserPermissions(java.io.File, boolean)
     */
    @Override
    public void setToOnlyReadWriteExecuteGroupUserPermissions(File directory, boolean recursive)
        throws CommandWrapperFault {
        ; // Nothing to do
    }

    /**
     * @see com.vasoftware.sf.externalintegration.execution.CommandExecutor
     *     #setToOnlyReadWriteExecuteUserPermissions(java.io.File, boolean)
     */
    @Override
    public void setToOnlyReadWriteExecuteUserPermissions(File file, boolean recursive) throws CommandWrapperFault {
        ; // Nothing to do
    }

    /**
     * @see com.vasoftware.sf.externalintegration.execution.CommandExecutor
     *     #setToOnlyReadWriteGroupUserPermissions(java.io.File, boolean)
     */
    @Override
    public void setToOnlyReadWriteGroupUserPermissions(File directory, boolean recursive) throws CommandWrapperFault {
        ; // Nothing to do
    }

    /**
     * @see com.vasoftware.sf.externalintegration.execution.CommandExecutor
     *     #setToOnlyReadWriteUserPermissions(java.io.File, boolean)
     */
    @Override
    public void setToOnlyReadWriteUserPermissions(File file, boolean recursive) throws CommandWrapperFault {
        ; // Nothing to do
    }

    /**
     * @see com.vasoftware.sf.externalintegration.execution.CommandExecutor#setUserOnPath(java.lang.String,
     *                                                                                    java.io.File)
     */
    @Override
    public void setUserOnPath(String username, File file) throws CommandWrapperFault {
        ; // Nothing to do
    }

    /**
     * @see com.vasoftware.sf.externalintegration.execution.CommandExecutor#runCommandAs(java.lang.String,
     *                                                                                   java.lang.String[],
     *                                                                                   java.io.File, boolean)
     */
    @Override
    public CommandResult runCommandAs(String username, String[] cmd, File dir, boolean login)
        throws CommandWrapperFault {
        return runCommand(cmd, null, dir);
    }

    /**
     * @see com.vasoftware.sf.externalintegration.execution.executors.AbstractCommandExecutor#getEnvironmentVariableString
     */
    @Override
    protected String getEnvironmentVariableString(String name, String value) {
        StringBuilder sb = new StringBuilder();
        sb.append("SET ")
                .append(name)
                .append("=")
                .append(value);
        return sb.toString();
    }

    /**
     * @see com.vasoftware.sf.externalintegration.execution.executors.AbstractCommandExecutor#getPathSeparator()
     */
    @Override
    protected String getPathSeparator() {
        return ";";
    }

    /**
     * @see com.vasoftware.sf.externalintegration.execution.executors.AbstractCommandExecutor#getFileSeparator()
     */
    @Override
    protected String getFileSeparator() {
        return "\\\\";
    }

    /**
     * @see com.vasoftware.sf.externalintegration.execution.executors.AbstractCommandExecutor#replaceArguments
     */
    @Override
    protected String replaceArguments(String scriptContent) {
        return scriptContent.replaceAll("\"\\$(\\d+)\"", "%$1");
    }

    /**
     * @see com.vasoftware.sf.externalintegration.execution.executors.AbstractCommandExecutor#getHookScriptFile(java.lang.String, com.vasoftware.sf.externalintegration.execution.CommandExecutor.HookEvent)
     */
    @Override
    protected File getHookScriptFile(String repository, HookEvent hook) {
        return new File(new File(repository, "hooks"),
                new StringBuilder(hook.toScriptName()).append(".bat").toString());
    }

    /**
     * @see com.vasoftware.sf.externalintegration.execution.executors.AbstractCommandExecutor#doScriptModifications(java.lang.StringBuilder)
     */
    @Override
    protected StringBuilder doScriptModifications(StringBuilder sb) {
        final String turnOffEcho = "@ECHO OFF\n\n";
        sb.insert(0, turnOffEcho);
        return sb;
    }

    /**
     * @see com.vasoftware.sf.externalintegration.execution.executors.AbstractCommandExecutor#getCommentString()
     */
    @Override
    protected String getCommentString() {
        return "::";
    }
}
