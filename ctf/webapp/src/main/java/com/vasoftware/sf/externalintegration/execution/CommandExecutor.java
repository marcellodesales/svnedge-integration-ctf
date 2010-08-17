/*
 * $RCSfile: CommandExecutor.java,v $
 *
 * SourceForge(r) Enterprise Edition
 * Copyright 2007 CollabNet, Inc.  All rights reserved.
 * http://www.collab.net
 *
 * The source code included in this listing is the confidential
 * and proprietary information of CollabNet, Inc.
 */

package com.vasoftware.sf.externalintegration.execution;

import java.io.File;

/**
 * @author Richard Lee <rlee@vasoftware.com>
 * @version $Revision: 1.30 $
 */
public interface CommandExecutor {

    /**
     * Enum representing the available SCM hooks
     */
    public enum HookEvent {
        START_COMMIT,
        PRE_COMMIT,
        POST_COMMIT, 
        PRE_REVPROP_CHANGE,
        POST_REVPROP_CHANGE;
        
        // convenience for converting enum element to 
        // corresponding script file name
        public String toScriptName() {
            return this.name().toLowerCase().replaceAll("_", "-");
        }
    }
    
    String BEGIN_SOURCEFORGE_SECTION = "BEGIN SOURCEFORGE SECTION - Do not remove these lines";
    String END_SOURCEFORGE_SECTION = "END SOURCEFORGE SECTION";



    /**
     * Create a number of users who are unable to login.
     * @param usernames the usernames to create
     * @return An array of usernames that were added to the system
     * @throws CommandWrapperFault thrown if creating users fails
     */
    String[] createUsersIfMissing(String[] usernames) throws CommandWrapperFault;

    /**
     * Creates a user with the specified group.
     *
     * @param username The new user we want to create
     * @param passwordCrypt   The crypted password of the created user
     * @throws CommandWrapperFault An error occurred while executing the command
     * @throws UserAlreadyExistsFault if user exists
     */
    void createUser(String username, String passwordCrypt) throws CommandWrapperFault, UserAlreadyExistsFault;

    /**
     * Deletes a user.
     *
     * @param username The new user we want to create
     * @throws CommandWrapperFault An error occurred while executing the command
     */
    void deleteUser(String username) throws CommandWrapperFault;

    /**
     * Makes a user active. If the user is already active, pretend that it was activated anyway
     * and complete silently.
     * @param username username to activate
     * @throws CommandWrapperFault Thrown if user could not be activated.
     */
    void activateUser(String username) throws CommandWrapperFault;

    /**
     * Deactivate a user. If the user is already deactivated, silently complete anyway.
     * @param username the username to deactivate
     * @throws CommandWrapperFault thrown if user could not be deactivated.
     */
    void deactivateUser(String username) throws CommandWrapperFault;

    /**
     * Bulk operation for setting user statuses
     * @param username an array of usernames
     * @param statuses a corresponding array of users' statuses
     * @throws CommandWrapperFault thrown if there was an error in setting statuses
     */
    void setUserStatuses(String[] username, String[] statuses) throws CommandWrapperFault;
    
    /**
     * Create a group with the specified name
     *
     * @param groupname The group we want to create
     * @throws CommandWrapperFault An error occurred while executing the command
     * @throws GroupAlreadyExistsFault if group exists
     */
    void createGroup(String groupname) throws CommandWrapperFault, GroupAlreadyExistsFault;

    /**
     * delete a group with the specified name
     *
     * @param groupname The group we want to create
     * @throws CommandWrapperFault An error occurred while executing the command
     */
    void deleteGroup(String groupname) throws CommandWrapperFault;

    /**
     * Add the user to the specified group
     *
     * @param groupname The group we want to add the user to.
     * @param username  The user we want to add to the group.
     * @throws CommandWrapperFault An error occurred while executing the command
     */
    void addUserToGroup(String groupname, String username) throws CommandWrapperFault;

    /**
     * Add a list of users to a group
     * @param groupname the group to add users to
     * @param usernames the users who are to be members of the group.
     * @throws CommandWrapperFault An error occurred while executing the command
     */
    void addUsersToGroup(String groupname, String[] usernames) throws CommandWrapperFault;
    
    /**
     * Removes a user from the specified group
     *
     * @param groupname The group we want to remove the user from.
     * @param username  The user we want to remove from the group.
     * @throws CommandWrapperFault An error occurred while executing the command
     */
    void removeUserFromGroup(String groupname, String username) throws CommandWrapperFault;

    /**
     * Remove a list of users from a group
     * @param groupname the group to remove users from
     * @param usernames the users who are to be removed from the group.
     * @throws CommandWrapperFault An error occurred while executing the command
     */
    void removeUsersFromGroup(String groupname, String[] usernames) throws CommandWrapperFault;

    /**
     * Set the appropriate permissions on the repositoy path.
     *
     * @param groupName The name of the repository which we use as the group that owns the repository
     * @param directory The repository path
     * @throws CommandWrapperFault An error occurred while executing the command
     */
    void setGroupOnPath(String groupName, File directory) throws CommandWrapperFault;

    /**
     * Set to only have group read, write, and execute permission on the path
     *
     * @param directory The directory
     * @param recursive whether to recurse subdirectories or not.
     * @throws CommandWrapperFault An error occurred while executing the command
     */
    void setToOnlyReadWriteExecuteGroupUserPermissions(File directory, boolean recursive) throws CommandWrapperFault;

    /**
     * Set to only have group read permission on the path
     *
     * @param directory The directory
     * @param recursive whether to recurse subdirectories or not.
     * @throws CommandWrapperFault An error occurred while executing the command
     */
    void setToOnlyReadGroupUserPermissions(File directory, boolean recursive) throws CommandWrapperFault;

    /**
     * Set to only have group read and write permission on the path
     *
     * @param directory The directory
     * @param recursive whether to recurse subdirectories or not.
     * @throws CommandWrapperFault An error occurred while executing the command
     */
    void setToOnlyReadWriteGroupUserPermissions(File directory, boolean recursive) throws CommandWrapperFault;
    
    /**
     * Set the user read write permissions on a file or directory.  Recursive if called on a directory
     *
     * @param file The file or directory to change permissions on
     * @param recursive whether to recurse subdirectories or not.
     * @throws CommandWrapperFault If any problems occur
     */
    public void setToOnlyReadWriteUserPermissions(File file, boolean recursive) throws CommandWrapperFault;

    /**
     * Set the user read write and execute permissions on a file or directory.  Recursive if called on a directory
     *
     * @param file The file or directory to change permissions on
     * @param recursive whether to recurse subdirectories or not.
     * @throws CommandWrapperFault If any problems occur
     */
    public void setToOnlyReadWriteExecuteUserPermissions(File file, boolean recursive) throws CommandWrapperFault;

    /**
     * Set the owner and permissions on a file so that only the specified owner can read it.
     * @param owner set file owner to this user
     * @param file The file being altered.
     * @param recursive whether to recurse subdirectories or not.
     * @throws CommandWrapperFault If any problems occur
     */
    void setOwnerToOnlyReadWritePermissions(String owner, File file, boolean recursive) throws CommandWrapperFault;

    /**
     * Remove group and other permissions--leave only owner permissions.
     * @param file The file being altered.
     * @param recursive whether to recurse subdirectories or not.
     * @throws CommandWrapperFault If any problems occur
     */
    void removeGroupOtherPermissions(File file, boolean recursive) throws CommandWrapperFault;

    /**
     * Set the owner and permissions on a file so that only the owner can write but everyone can read execute.
     * @param owner set file owner to this user
     * @param file The file being altered.
     * @param recursive whether to recurse subdirectories or not.
     * @throws CommandWrapperFault If any problems occur
     */
    void setOwnerToRwxOthersRxPermissions(String owner, File file, boolean recursive) throws CommandWrapperFault;

    /**
     * This method returns an array of users who belong in a specified group as an array of
     * username objects.
     *
     * @param groupName the group to return the membership for
     * @return String[] an array of usernames
     * @throws CommandWrapperFault Thrown if errors occur during execution.
     */
    public String[] listUsersInGroup(String groupName) throws CommandWrapperFault;

    /**
     * Sets a user's password
     *
     * @param username the username whose password is to be set
     * @param passwordCrypted the Linux md5 crypt hash of the password to.
     * @throws CommandWrapperFault thrown if there are any problems setting the password.
     */
    public void setPassword(String username, String passwordCrypted) throws CommandWrapperFault;

    /**
     * Gets a user's home directory
     *
     * @param username the username whose home directory to look up.
     * @return The user's home directory as a string
     * @throws CommandWrapperFault thrown if the user does not exist.
     */
    public String getUserHomeDirectoryFromOS(String username) throws CommandWrapperFault;

    /**
     * Verifies that a particular path exists and is a directory.
     *
     * @param s a string representing the path to verify.
     * @return true if the path exists and is a directory.
     */
    boolean pathExists(File s);

    /**
     * Recursively removes the directory pointed to by path.
     *
     * @param path the string representing the absolute path
     * @throws CommandWrapperFault thrown if removal of path fails.
     */
    public void deletePath(File path) throws CommandWrapperFault;

    /**
     * Creates a directory described by path.
     *
     * @param path A string of the absolute path to create.
     * @throws CommandWrapperFault thrown if creation of path fails.
     */
    public void createPath(File path) throws CommandWrapperFault;

    /**
     * Sets the given file to be owned and ONLY readable and writable by the given user.
     *
     * @param username the user to give permissions to
     * @param file     the file whose permission is to be changed.
     * @throws CommandWrapperFault thrown if there were errors setting permission on the file.
     */
    void setUserOnPath(String username, File file) throws CommandWrapperFault;

    /**
     * Run a command as a given user.
     *
     * @param username the user to run the command as.
     * @param cmd      the command to execute
     * @param dir      the directory to run the command in.
     * @param login    if true, make shell a login shell
     * @return the result of the command.
     * @throws CommandWrapperFault thrown if there was an error while executing command.
     */
    CommandResult runCommandAs(String username, String[] cmd, File dir, boolean login)
	    throws CommandWrapperFault;

    /**
     * Run a command in a particular directory with environment and return a CommandResult structure.
     *
     * @param cmd  the command and its parameters
     * @param envp the command environment as a set of var=val strings
     * @param dir  the directory to execute the command in
     * @return CommandResult - the result of the command.
     * @throws CommandWrapperFault An error occurred while executing the command.
     */
    CommandResult runCommand(String[] cmd, String[] envp, File dir) throws CommandWrapperFault;

    /**
     * Create a file on the filesystem
     *
     * @param fileToCreate the path including filename
     * @param contents     the contents of the file.
     * @throws CommandWrapperFault thrown if the file could not be created.
     */
    void createFile(File fileToCreate, String contents) throws CommandWrapperFault;

    /**
     * Add sourceforge trigger to file.
     * <p>Read srcFile. If sourceforge trigger exists, replace with new sourceforge trigger,
     * else prepend sourceforge trigger to start. Write results to dstFile.
     * Typically, srcFile and dstFile is the same. </p> 
     *  
     * @param srcFile Source file for triggers
     * @param dstFile Results written to this file
     * @param sourceforgeTrigger trigger block to insert (needs to start with BEGIN_SOURCEFORGE_SECTION
     *                           and end with END_SOURCEFORGE_SECTION)
     * @throws CommandWrapperFault thrown if problem modifying file.
     */
    public void addTriggerToFile(File srcFile, File dstFile, String sourceforgeTrigger) throws CommandWrapperFault;
    
    /**
     * Create a temporary directory
     *
     * @return A File object for the newly created directory.
     * @throws CommandWrapperFault thrown if the directory could not be created.
     */
    File createTempDirectory() throws CommandWrapperFault;

    /**
     * Gets the name of the group owning a directory or file.  this method may truncate the group name at 8 characters.
     *
     * @param repositoryDir the file or directory on which to find ownership
     * @return the name of the owning group
     * @throws CommandWrapperFault An error occurred while executing the command.
     */
    String getFileGroup(File repositoryDir) throws CommandWrapperFault;

    /**
     * Run a command in a particular directory with environment and return a Process handle.  This does not wait
     * until the process has finished to return.
     *
     * @param cmd  the command and its parameters
     * @param envp the environment.
     * @param dir  the directory to execute the command in
     * @return The process handle to the running process
     * @throws CommandWrapperFault An error occurred while executing the command.
     */    
    Process runCommandAsync(String[] cmd, String[] envp, File dir) throws CommandWrapperFault;

    /**
     * enables a user for testing (should only be called from test classes)
     * @param username the user to be enabled for testing
     * @throws CommandWrapperFault on error
     */
    void enableUserForTesting(String username) throws CommandWrapperFault;

    /**
     * Create or update the hook script for the given event with the command
     * content here. Concrete implementations will take care of OS-specific
     * boilerplate and writing the file. 
     * @param repositoryDir the repo whose hooks script to modify
     * @param hook HookEvent for which to create script
     * @param scriptContent the command(s) to run when the event is fired
     * @throws CommandWrapperFault An error occurred while executing the command. 
     */
    void createHookScript(String repositoryDir, HookEvent hook, String scriptContent) throws CommandWrapperFault ;
}
