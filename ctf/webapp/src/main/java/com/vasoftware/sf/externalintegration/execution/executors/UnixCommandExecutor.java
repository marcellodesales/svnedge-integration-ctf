/*
 * CollabNet TeamForge
 * Copyright 2010 CollabNet, Inc.  All rights reserved.
 * http://www.collab.net
 *
 * The source code included in this listing is the confidential
 * and proprietary information of CollabNet, Inc.
 */
package com.vasoftware.sf.externalintegration.execution.executors;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.vasoftware.sf.common.configuration.GlobalOptionKeys;
import com.vasoftware.sf.common.configuration.SfGlobalOptions;
import com.vasoftware.sf.common.configuration.SfGlobalOptionsManager;
import com.vasoftware.sf.common.logger.Logger;
import com.vasoftware.sf.common.util.FileUtil;
import com.vasoftware.sf.common.util.StringUtil;
import com.vasoftware.sf.externalintegration.ScmConstants;
import com.vasoftware.sf.externalintegration.execution.CommandExecutor;
import com.vasoftware.sf.externalintegration.execution.CommandResult;
import com.vasoftware.sf.externalintegration.execution.CommandWrapperFault;
import com.vasoftware.sf.externalintegration.execution.ExecutionUtil;
import com.vasoftware.sf.externalintegration.execution.GroupAlreadyExistsFault;
import com.vasoftware.sf.externalintegration.execution.UserAlreadyExistsFault;

/**
 * @author Richard Lee <rlee@vasoftware.com>
 * @version $Revision: 1.46 $
 */
public class UnixCommandExecutor extends AbstractCommandExecutor implements CommandExecutor {
    protected static final Logger smLogger = Logger.getLogger(ExecutionUtil.class);

    protected static final String RUN_COMMAND_AS_SHELL = "/bin/sh";
    protected static final String GROUP_FILE = "/etc/group";

    protected static final String PERM_GROUP_USER_READ_WRITE_EXECUTE = "2770"; // gu+rwxs
    protected static final String PERM_GROUP_USER_READ = "0440"; // gu+r
    protected static final String PERM_GROUP_USER_READWRITE = "0660"; // gu+rw

    protected static final String PERM_USER_READ_WRITE = "600"; // u+rw
    protected static final String PERM_USER_READ_WRITE_EXECUTE = "700"; // u+rwx

    protected static final String PERM_USER_RWX_GROUPOTHER_RX = "755";
    protected static final String PERM_OWNER_ONLY = "go-rwx"; // u+rwx

    protected static final int ADDGROUP_EXIT_DUPLICATEGROUP = 9;
    protected static final String PASSWORD_FILE = "/etc/passwd";

    protected static final String GROUPS_COMMAND = "/usr/bin/groups";
    protected static final String USERMOD_COMMAND = "/usr/sbin/usermod";
    protected static final String CHOWN_COMMAND = "/bin/chown";
    protected static final String CHGRP_COMMAND = "/bin/chgrp";
    protected static final String CHMOD_COMMAND = "/bin/chmod";
    protected static final String ADDGROUP_COMMAND = "/usr/sbin/groupadd";
    protected static final String MKDIR_COMMAND = "/bin/mkdir";
    protected static final String DELGROUP_COMMAND = "/usr/sbin/groupdel";
    protected static final String RM_COMMAND = "/bin/rm";
    protected static final String USERADD_COMMAND = "/usr/sbin/useradd";
    protected static final String USERDEL_COMMAND = "/usr/sbin/userdel";

    private static final String SU_COMMAND = "su";

    protected static final int USERADD_EXIT_DUPLICATEUSER = 9;

    /**
     * List of usernames that we are NOT allowed to perform operations on
     */
    private static final List<String> RESTRICTED_USERNAMES = Arrays.asList("abuse", "adm", "apache", "backup", "bin",
                                                                           "cvs", "daemon", "dbus", "debian",
                                                                           "download", "dummy", "email", "exim", "ftp",
                                                                           "games", "gnats", "gopher", "haldaemon",
                                                                           "halt", "hostmaster", "httpd", "irc",
                                                                           "jboss", "list", "lp", "mail",
                                                                           "mailer-daemon", "mailnull", "majordom",
                                                                           "man", "mqm", "msql", "mysql", "named",
                                                                           "netdump", "netsaint", "news", "nobody",
                                                                           "ns", "nscd", "ntp", "operator", "oracle",
                                                                           "pcap", "postgres", "postmaster", "proxy",
                                                                           "root", "rpc", "rpm", "shell", "shutdown",
                                                                           "smmsp", "ssh", "sshd", "staff", "support",
                                                                           "sync", "sys", "uucp", "vcsa", "web", "www",
                                                                           "www-data", "xfs");

    /** The group created on the SCM server that contains all sourceforge users. Must be 8 letters or less on Solaris */
    public static final String ALL_USERS_GROUP = "sfall";

    /**
     * The group created on the SCM server that contains all unrestricted users. Must be 8 letters or less on Solaris
     */
    public static final String UNRESTRICTED_USERS_GROUP = "sfunrest";

    protected String mUserGroup;
    protected String mUserHomeDir;
    protected String mUserActiveShell;
    protected String mUserDisabledShell;

    /**
     * basic constructor. Initializes member variables from global options.
     */
    public UnixCommandExecutor() {
        super();
        final SfGlobalOptions options = SfGlobalOptionsManager.getOptions();
        mUserGroup = options.getOption(GlobalOptionKeys.SFMAIN_INTEGRATION_USER_GROUP);
        mUserHomeDir = options.getOption(GlobalOptionKeys.SFMAIN_INTEGRATION_USER_HOME_DIRECTORY);
        mUserActiveShell = options.getOption(GlobalOptionKeys.SFMAIN_INTEGRATION_USER_SHELL_ACTIVE);
        mUserDisabledShell = options.getOption(GlobalOptionKeys.SFMAIN_INTEGRATION_USER_SHELL_DISABLED);
    }

    /**
     * Get the default user group for new users
     * 
     * @return the user group as a string
     */
    protected String getUserGroup() {
        return mUserGroup;
    }

    /**
     * Get the home directory root under which new home dirs are created.
     * 
     * @return the home directory root as a string
     */
    protected String getUserHomeBaseDir() {
        return mUserHomeDir;
    }

    /**
     * Get the active shell for a user.
     * 
     * @return the path to the active shell.
     */
    protected String getUserActiveShell() {
        return mUserActiveShell;
    }

    /**
     * Get the disabled shell for a user
     * 
     * @return the path to a disabled shell.
     */
    protected String getUserDisabledShell() {
        return mUserDisabledShell;
    }

    /**
     * Create a group with the specified name
     * 
     * @param groupname
     *            The group we want to create
     * @throws CommandWrapperFault
     *             An error occurred while executing the command
     * @throws GroupAlreadyExistsFault
     *             if group exists
     */
    public void createGroup(final String groupname) throws CommandWrapperFault, GroupAlreadyExistsFault {
        final CommandResult result = runCommand(new String[] { ADDGROUP_COMMAND, groupname }, null, null);

        final int exitcode = result.getReturnValue();
        if (exitcode == ADDGROUP_EXIT_DUPLICATEGROUP) {
            throw new GroupAlreadyExistsFault(groupname);
        }

        if (exitcode != CommandResult.RETURN_SUCCESS) {
            throw new CommandWrapperFault(result.getCommand(), "Could not add group: " + groupname + " "
                    + result.getCommandOutput());
        }

        if (isRollbackEnabled()) {
            addGroupForRollback(groupname);
        }

    }

    /**
     * Calls the groupdel command to remove a group.
     * 
     * @param groupname
     *            the group name to remove
     * @throws CommandWrapperFault
     *             thrown if the group could not be deleted
     */
    public void deleteGroup(final String groupname) throws CommandWrapperFault {
        final CommandResult result = runCommand(new String[] { DELGROUP_COMMAND, groupname }, null, null);

        final int exitcode = result.getReturnValue();
        if (exitcode != CommandResult.RETURN_SUCCESS) {
            throw new CommandWrapperFault(result.getCommand(), "Could not delete group: " + groupname + " "
                    + result.getCommandOutput());
        }

    }

    /**
     * Add the user to the specified group
     * 
     * @param groupname
     *            The group we want to add the user to.
     * @param username
     *            The user we want to add to the group.
     * @throws CommandWrapperFault
     *             An error occurred while executing the command
     */
    public void addUserToGroup(final String groupname, final String username) throws CommandWrapperFault {
        final String usernameToLower = username.toLowerCase();

        // if group is sfall, set sfall as users default group
        if (ALL_USERS_GROUP.equals(groupname)) {
            final CommandResult result = runCommand(new String[] { USERMOD_COMMAND, "-g", ALL_USERS_GROUP,
                                                                  usernameToLower }, null, null);
            if (result.getReturnValue() != CommandResult.RETURN_SUCCESS) {
                throw new CommandWrapperFault(result.getCommand(), "Could not set default group for " + username
                        + " to " + ALL_USERS_GROUP + " : " + result.getCommandOutput());
            }
            return;
        }

        final CommandResult result = runCommand(new String[] { GROUPS_COMMAND, usernameToLower }, null, null);

        if (result.getReturnValue() != CommandResult.RETURN_SUCCESS) {
            throw new CommandWrapperFault(result.getCommand(), "Could not get groups for user " + username + " "
                    + result.getCommandOutput());
        }

        final String groupsout = result.getStdout();
        final String grouplist = groupsout.substring(groupsout.indexOf(':') + 1).trim();

        String newGroupList = "";
        final String[] groups = grouplist.split("[ ]+");

        // Iterate through the array of existing groups. return if user is already in group
        for (final String group : groups) {
            if (group.equals(mUserGroup) || group.equals(ALL_USERS_GROUP)) {
                continue;
            }
            // If the user is already a member of this group, simply return.
            if (group.equals(groupname)) {
                return;
            }
            newGroupList += " " + group;
        }

        // add the new group to the newGroupList, then turn the string into a comma delimited list
        newGroupList += " " + groupname;
        newGroupList = newGroupList.trim().replace(' ', ',');

        // execute usermod
        final CommandResult addresult = runCommand(
                                                   new String[] { USERMOD_COMMAND, "-G", newGroupList, usernameToLower },
                                                   null, null);

        if (addresult.getReturnValue() != CommandResult.RETURN_SUCCESS || addresult.getStderr().length() > 0) {
            throw new CommandWrapperFault(addresult.getCommand(), "Could not add user " + username + " to group "
                    + groupname + " " + addresult.getCommandOutput());
        }

    }

    /**
     * Remove a user from the specified group
     * 
     * @param groupname
     *            The group we want to remove the user from.
     * @param username
     *            The user we want to remove from the group.
     * @throws CommandWrapperFault
     *             An error occurred while executing the command
     */
    public void removeUserFromGroup(final String groupname, final String username) throws CommandWrapperFault {
        final String usernameLowerCase = username.toLowerCase();

        // if group is sfall, reset users default group
        if (ALL_USERS_GROUP.equals(groupname)) {
            final CommandResult result = runCommand(new String[] { USERMOD_COMMAND, "-g", getUserGroup(),
                                                                  usernameLowerCase }, null, null);
            if (result.getReturnValue() != CommandResult.RETURN_SUCCESS) {
                throw new CommandWrapperFault(result.getCommand(), "Could not set default group for " + username
                        + " to " + getUserGroup() + " : " + result.getCommandOutput());
            }
        }

        final CommandResult result = runCommand(new String[] { GROUPS_COMMAND, usernameLowerCase }, null, null);

        if (result.getReturnValue() != CommandResult.RETURN_SUCCESS) {
            throw new CommandWrapperFault(result.getCommand(), "Could not get groups for user " + username + " "
                    + result.getCommandOutput());
        }

        final String groupsout = result.getStdout();
        final String grouplist = groupsout.substring(groupsout.indexOf(':') + 1).trim();

        String newGroupList = "";
        final String[] groups = grouplist.split("[ ]+");

        // Iterate through the array of existing groups. don't add if its the group or the default group.
        for (final String group : groups) {
            if (group.equals(mUserGroup) || group.equals(ALL_USERS_GROUP) || group.equals(groupname)) {
                continue;
            }
            newGroupList += " " + group;
        }

        // turn the string into a comma delimited list
        newGroupList = newGroupList.trim().replace(' ', ',');

        // execute usermod, explicitly setting the user's default group to our "expected" value
        final CommandResult removeresult = runCommand(new String[] { USERMOD_COMMAND, "-G", newGroupList,
                                                                    usernameLowerCase }, null, null);

        if (removeresult.getReturnValue() != CommandResult.RETURN_SUCCESS) {
            throw new CommandWrapperFault(removeresult.getCommand(), "Could not remove user " + username
                    + " from group " + groupname + " " + removeresult.getCommandOutput());
        }

    }

    /**
     * @see CommandExecutor#addUsersToGroup(String, String[])
     */
    public void addUsersToGroup(final String groupname, final String[] usernames) throws CommandWrapperFault {
        for (final String username : usernames) {
            addUserToGroup(groupname, username);
        }
    }

    /**
     * @see CommandExecutor#removeUsersFromGroup(String, String[])
     */
    public void removeUsersFromGroup(final String groupname, final String[] usernames) throws CommandWrapperFault {
        for (final String username : usernames) {
            removeUserFromGroup(groupname, username);
        }
    }

    /**
     * Set the appropriate group on the repositoy path.
     * 
     * @param groupname
     *            The group name to change ownership to.
     * @param directory
     *            The path to chgrp
     * @throws CommandWrapperFault
     *             An error occurred while executing the command
     */
    public void setGroupOnPath(final String groupname, final File directory) throws CommandWrapperFault {
        final CommandResult chgrpResult = runCommand(new String[] { CHGRP_COMMAND, "-R", groupname,
                                                                   directory.getPath() }, null, null);

        if (chgrpResult.getReturnValue() != CommandResult.RETURN_SUCCESS) {
            throw new CommandWrapperFault(chgrpResult.getCommand(), "Could not chgrp the directory "
                    + directory.getPath() + " to " + groupname + " " + chgrpResult.getCommandOutput());
        }
    }

    /**
     * Set to only have group read, write, and execute permission on the path
     * 
     * @param directory
     *            The directory
     * @param recursive
     *            whether to recurse subdirectories or not.
     * @throws CommandWrapperFault
     *             An error occurred while executing the command
     */
    public void setToOnlyReadWriteExecuteGroupUserPermissions(final File directory, final boolean recursive)
                                                                                                            throws CommandWrapperFault {
        chmod(directory, PERM_GROUP_USER_READ_WRITE_EXECUTE, recursive);
    }

    /**
     * Set to only have user/group read permission on the path
     * 
     * @param directory
     *            The directory
     * @param recursive
     *            whether to recurse subdirectories or not.
     * @throws CommandWrapperFault
     *             An error occurred while executing the command
     */
    public void setToOnlyReadGroupUserPermissions(final File directory, final boolean recursive)
                                                                                                throws CommandWrapperFault {
        chmod(directory, PERM_GROUP_USER_READ, recursive);
    }

    /**
     * Set to only have user/group read/write permission on the path
     * 
     * @param directory
     *            The directory
     * @param recursive
     *            whether to recurse subdirectories or not.
     * @throws CommandWrapperFault
     *             An error occurred while executing the command
     */
    public void setToOnlyReadWriteGroupUserPermissions(final File directory, final boolean recursive)
                                                                                                     throws CommandWrapperFault {
        chmod(directory, PERM_GROUP_USER_READWRITE, recursive);
    }

    /**
     * Set to only have user read write permissions on a file or directory. Recursive if called on a directory
     * 
     * @param file
     *            The file or directory to change permissions on
     * @param recursive
     *            whether to recurse subdirectories or not.
     * @throws CommandWrapperFault
     *             If any problems occur
     */
    public void setToOnlyReadWriteUserPermissions(final File file, final boolean recursive) throws CommandWrapperFault {
        chmod(file, PERM_USER_READ_WRITE, recursive);
    }

    /**
     * Set the user read write and execute permissions on a file or directory. Recursive if called on a directory
     * 
     * @param file
     *            The file or directory to change permissions on
     * @param recursive
     *            whether to recurse subdirectories or not.
     * @throws CommandWrapperFault
     *             If any problems occur
     */
    public void setToOnlyReadWriteExecuteUserPermissions(final File file, final boolean recursive)
                                                                                                  throws CommandWrapperFault {
        chmod(file, PERM_USER_READ_WRITE_EXECUTE, recursive);
    }

    /**
     * @see CommandExecutor#setOwnerToOnlyReadWritePermissions(String,java.io.File,boolean)
     */
    public void setOwnerToOnlyReadWritePermissions(final String owner, final File file, final boolean recursive)
                                                                                                                throws CommandWrapperFault {
        setUserOnPath(owner, file);
        setToOnlyReadWriteUserPermissions(file, recursive);
    }

    /**
     * @see CommandExecutor#removeGroupOtherPermissions(java.io.File,boolean)
     */
    public void removeGroupOtherPermissions(final File file, final boolean recursive) throws CommandWrapperFault {
        chmod(file, PERM_OWNER_ONLY, recursive);
    }

    /**
     * Set the owner and permissions on a file so that only the specified owner can read it.
     * 
     * @param owner
     *            set file owner to this user
     * @param file
     *            The file being altered.
     * @param recursive
     *            whether to recurse subdirectories or not.
     * @throws CommandWrapperFault
     *             If any problems occur
     */
    public void setOwnerToRwxOthersRxPermissions(final String owner, final File file, final boolean recursive)
                                                                                                              throws CommandWrapperFault {
        setUserOnPath(owner, file);
        chmod(file, PERM_USER_RWX_GROUPOTHER_RX, recursive);
    }

    /**
     * Call chmod with the permission string on the path. Use the recursive option
     * 
     * @param directory
     *            The directory to chmod
     * @param permissionString
     *            The permission string to use
     * @param recursive
     *            whether to recurse subdirectories or not.
     * @throws CommandWrapperFault
     *             If there was any problem with the command
     */
    private void chmod(final File directory, final String permissionString, final boolean recursive)
                                                                                                    throws CommandWrapperFault {
        String[] commandParts;
        if (recursive) {
            commandParts = new String[] { CHMOD_COMMAND, "-R", permissionString, directory.getPath() };
        } else {
            commandParts = new String[] { CHMOD_COMMAND, "-NR", permissionString, directory.getPath() };
        }
        final CommandResult chmodResult = runCommand(commandParts, null, null);

        if (chmodResult.getReturnValue() != CommandResult.RETURN_SUCCESS) {
            throw new CommandWrapperFault(chmodResult.getCommand(), "Could not chmod the directory "
                    + directory.getPath() + " to " + permissionString + " " + chmodResult.getCommandOutput());
        }
    }

    /**
     * Get the line that matches the specified column in a delimited file. This is used for getting a list of users from
     * a group in the groups file or a user's home directory from the password file.
     * 
     * @param fileName
     *            The name of the file.
     * @param delimiter
     *            The delimiter to split on.
     * @param matchString
     *            The string being matched.
     * @return The split string for the matching line.
     * @throws CommandWrapperFault
     *             If a match was not found or the file could not be read.
     */
    private String[] getMatchingLineFromFile(final String fileName, final String delimiter, final String matchString)
                                                                                                                     throws CommandWrapperFault {
        FileReader fileReader = null;
        BufferedReader reader = null;
        try {
            fileReader = new FileReader(fileName);
            reader = new BufferedReader(fileReader);

            String line;
            while ((line = reader.readLine()) != null) {
                final String[] elements = line.split(delimiter);
                if (elements[0].equals(matchString)) {
                    return elements;
                }
            }
        } catch (final FileNotFoundException e) {
            throw new CommandWrapperFault("File open", "Could not open " + fileName + " file", e);
        } catch (final IOException e) {
            throw new CommandWrapperFault("File read", "Could not read " + fileName + " file", e);
        } finally {
            // Close the readers
            if (fileReader != null) {
                try {
                    fileReader.close();
                } catch (final IOException e) {
                    // don't do anything
                }
            }

            if (reader != null) {
                try {
                    reader.close();
                } catch (final IOException e) {
                    // don't do anything
                }
            }
        }

        throw new CommandWrapperFault("Read file", "No match found for: " + matchString + " in file: " + fileName);
    }

    /**
     * Get all users with group id as the default group.
     * 
     * @param groupId
     *            group id
     * @return Set of users with sfall as default group
     * @throws CommandWrapperFault
     *             If a match was not found or the file could not be read.
     */
    private Set<String> getUsersWithDefaultGroup(final String groupId) throws CommandWrapperFault {
        final Set<String> users = new HashSet<String>();

        FileReader fileReader = null;
        BufferedReader reader = null;
        try {
            fileReader = new FileReader(PASSWORD_FILE);
            reader = new BufferedReader(fileReader);

            String line;
            while ((line = reader.readLine()) != null) {
                final String[] elements = line.split(":");
                if ((elements.length >= 4) && (groupId.equals(elements[3]))) {
                    users.add(elements[0]);
                }
            }
            return users;
        } catch (final FileNotFoundException e) {
            throw new CommandWrapperFault("File open", "Could not open " + PASSWORD_FILE + " file", e);
        } catch (final IOException e) {
            throw new CommandWrapperFault("File read", "Could not read " + PASSWORD_FILE + " file", e);
        } finally {
            FileUtil.close(reader);
            FileUtil.close(fileReader);
        }
    }

    /**
     * This method returns an array of users who belong in a specified group as an array of username objects.
     * 
     * @param groupName
     *            the group to return the membership for
     * @return String[] an array of usernames
     * @throws CommandWrapperFault
     *             Thrown if errors occur during execution.
     */
    public String[] listUsersInGroup(final String groupName) throws CommandWrapperFault {
        final String[] elements = getMatchingLineFromFile(GROUP_FILE, ":", groupName);
        String[] users;
        if (elements.length == 4) {
            users = elements[3].split(",");
        } else {
            users = new String[0];
        }

        if (ALL_USERS_GROUP.equals(groupName)) {
            if (elements.length < 3) {
                throw new CommandWrapperFault("listUsersInGroup", "Unable to find " + ALL_USERS_GROUP + " in "
                        + GROUP_FILE);
            }
            // if sfall, include all users with sfall as default group
            final Set<String> userSet = getUsersWithDefaultGroup(elements[2]);
            Collections.addAll(userSet, users);
            return userSet.toArray(new String[userSet.size()]);
        } else {
            return users;
        }
    }

    /**
     * Sets a user's shell
     * 
     * @param username
     *            the username of the user whose shell is to be set
     * @param shell
     *            the value to set the user's shell to.
     * @throws CommandWrapperFault
     *             An error occurred while executing the command
     */
    protected void setShell(final String username, final String shell) throws CommandWrapperFault {
        try {
            // Run usermod with the -s option to change shell.
            final CommandResult shellresult = runCommand(new String[] { USERMOD_COMMAND, "-s", shell, username }, null,
                                                         null);

            if (shellresult.getReturnValue() != CommandResult.RETURN_SUCCESS) {
                smLogger.error("Could not set shell for user '" + username + "': " + shellresult.getCommandOutput());
            }

        } catch (final CommandWrapperFault e) {
            smLogger.error("Could not set shell for user '" + username + "'", e);
        }
    }

    /**
     * @see CommandExecutor#activateUser(String)
     */
    public void activateUser(final String username) throws CommandWrapperFault {
        if (isRestrictedUsername(username)) {
            throw new CommandWrapperFault("activateUser()", "Username is restricted: " + username);
        }

        setShell(username, getUserActiveShell());
        final String homeDirectory = getUserHomeDirectory(username);
        final File homeDirectoryFile = new File(homeDirectory);
        if (!homeDirectoryFile.exists()) {
            homeDirectoryFile.mkdirs();
            setUserOnPath(username, homeDirectoryFile);
        }
    }

    /**
     * @see CommandExecutor#deactivateUser(String)
     */
    public void deactivateUser(final String username) throws CommandWrapperFault {
        if (isRestrictedUsername(username)) {
            throw new CommandWrapperFault("deactivateUser()", "Username is restricted: " + username);
        }

        setShell(username, getUserDisabledShell());
    }

    /**
     * @see CommandExecutor#deleteUser(String)
     */
    public void deleteUser(final String username) throws CommandWrapperFault {
        if (isRestrictedUsername(username)) {
            throw new CommandWrapperFault("deleteUser()", "Username is restricted: " + username);
        }

        final CommandResult result = runCommand(new String[] { USERDEL_COMMAND, "-r", username }, null, null);
        if (result.getReturnValue() != CommandResult.RETURN_SUCCESS) {
            throw new CommandWrapperFault(result.getCommand(), "Could not delete user " + username + " : "
                    + result.getCommandOutput());
        }
    }

    /**
     * @see CommandExecutor#getUserHomeDirectoryFromOS
     */
    public String getUserHomeDirectoryFromOS(final String username) throws CommandWrapperFault {
        final String[] elements = getMatchingLineFromFile(PASSWORD_FILE, ":", username);
        if (elements.length > 5) {
            return elements[5];
        }

        throw new CommandWrapperFault("getUserHomeDirectory", "Unable to get home directory for: " + username);
    }

    /**
     * @see com.vasoftware.sf.externalintegration.execution.CommandExecutor#setUserOnPath
     */
    public void setUserOnPath(final String username, final File file) throws CommandWrapperFault {
        if (StringUtil.isEmpty(username)) {
            return;
        }

        final CommandResult chownResult = runCommand(new String[] { CHOWN_COMMAND, "-R", username,
                                                                   file.getPath() }, null, null);

        if (chownResult.getReturnValue() != CommandResult.RETURN_SUCCESS) {
            throw new CommandWrapperFault(chownResult.getCommand(), "Could not chown the file " + file.getPath()
                    + " to " + username + " " + chownResult.getCommandOutput());
        }
    }

    /**
     * This method escapes out single quotes from commands.
     * 
     * @param cmdString
     *            the command to escape out.
     * @return String - the command with quotes escaped.
     */
    public static String escapeCommand(final String cmdString) {
        String slashescaped = "";
        int lastIndex = 0;
        int index = cmdString.indexOf('\\');
        while (index != -1) {
            slashescaped += (cmdString.substring(lastIndex, index) + "\\\\");
            lastIndex = index + 1;
            index = cmdString.indexOf('\\', lastIndex);
        }
        slashescaped += cmdString.substring(lastIndex);
        String result = "";

        lastIndex = 0;
        index = slashescaped.indexOf('\"');
        while (index != -1) {
            result += (slashescaped.substring(lastIndex, index) + "\\\"");
            lastIndex = index + 1;
            index = slashescaped.indexOf('\"', lastIndex);
        }
        result += slashescaped.substring(lastIndex);

        return result;

    }

    /**
     * note that this implementation can return the entire output of 'ls -ld' on the File, not just the owning group. an
     * indexOf on this could do the wrong thing if the checked string is part of the filename or owning user.
     * 
     * @see CommandExecutor#getFileGroup
     */
    public String getFileGroup(final File repositoryDir) throws CommandWrapperFault {
        CommandResult result;
        result = runCommand(new String[] { "ls", "-ld", repositoryDir.getAbsolutePath() }, null, null);
        final String line = result.getStdout();
        final String[] cols = line.replaceAll(" +", " ").split(" ");
        if (cols.length >= 4) {
            return cols[3];
        }
        return line;
    }

    /**
     * Generates a user home directory from the username. Typically this string is going to be of the form
     * &lt;homebasedir&gt;/r/rl/rlee for the username rlee. if the username has less than 3 characters, it puts the
     * username in the directory of the same name. Say you had a user named ab, it would create it as /home/a/ab/ab so
     * that if user abc came along, you would not get a conflict between the home directory /a/ab and the container
     * directory /a/ab for /home/a/ab/abc.
     * 
     * @param username
     *            the username to get the homedirectory for
     * @return the the home directory including the base home directory.
     * @throws CommandWrapperFault
     *             thrown if the username is invalid
     */
    protected String getUserHomeDirectory(final String username) throws CommandWrapperFault {
        if (username == null || username.length() < 1) {
            throw new CommandWrapperFault("getUserHomeDirectory()", "Username cannot be empty or null.");
        }

        String dirPrefix = username.substring(0, 1);

        if (username.length() > 1) {
            dirPrefix = dirPrefix + File.separator + username.substring(0, 2);
        }

        return getUserHomeBaseDir() + File.separator + dirPrefix + File.separator + username;
    }

    /**
     * Executes the useradd command in specified groupname.
     * 
     * @param username
     *            The new user we want to create
     * @param passwordCrypted
     *            The crypted password of the created user
     * @throws com.vasoftware.sf.externalintegration.execution.CommandWrapperFault
     *             An error occurred while executing the command
     * @throws UserAlreadyExistsFault
     *             if user exists
     */
    public void createUser(final String username, final String passwordCrypted) throws CommandWrapperFault,
                                                                               UserAlreadyExistsFault {
        CommandResult addUserResult;

        final String homeDirectory = getUserHomeDirectory(username);
        final File homeDirectoryParentFile = new File(homeDirectory).getParentFile();
        if (!homeDirectoryParentFile.exists()) {
            homeDirectoryParentFile.mkdirs();
        }

        if (isRestrictedUsername(username)) {
            throw new CommandWrapperFault("createUser()", "Username is restricted: " + username);
        }

        if (passwordCrypted == null) {
            // Try to run the adduser command
            addUserResult = runCommand(new String[] { USERADD_COMMAND, "-g", getUserGroup(), "-s",
                                                     getUserActiveShell(), "-d", homeDirectory, "-m", username }, null,
                                       null);
        } else {
            // Try to run the adduser command
            addUserResult = runCommand(new String[] { USERADD_COMMAND, "-g", getUserGroup(), "-s",
                                                     getUserActiveShell(), "-d", homeDirectory, "-m", "-p",
                                                     passwordCrypted, username }, null, null);
        }
        final int exitvalue = addUserResult.getReturnValue();

        // If we have a duplicate user, then throw a duplicate user exception
        if (exitvalue == USERADD_EXIT_DUPLICATEUSER) {
            throw new UserAlreadyExistsFault(username);
        }

        // If we had any other exit code, then throw a generic exception.
        if (exitvalue != CommandResult.RETURN_SUCCESS) {
            throw new CommandWrapperFault(addUserResult.getCommand(), "Could not add user:"
                    + addUserResult.getCommandOutput());
        }

        if (isRollbackEnabled()) {
            addUserForRollback(username);
        }
    }

    /**
     * Return true if the username is null or in our list of explicitly restricted names.
     * 
     * @param username
     *            the username to check
     * @return returns true if username is restricted, otherwise returns false.
     */
    protected static boolean isRestrictedUsername(final String username) {
        return username == null || RESTRICTED_USERNAMES.contains(username);
    }

    /**
     * Sets a user's password to a value. If the password is null, then this method
     * "locks" the account, making it impossible to log in.
     *
     * @param username name of user to set password for
     * @param passwordCrypted the crypted password
     * @throws CommandWrapperFault Error executing commands which perform the operation
     */
    public void setPassword(String username, String passwordCrypted) throws CommandWrapperFault {
        CommandResult passwdresult;
        try {
            // If the password is null, we disable the account by running "usermod -L"
            if (passwordCrypted == null) {
                passwdresult = runLoggedCommand(new String[]{USERMOD_COMMAND, "-L", username}, null, null, true);

                int returnValue = passwdresult.getReturnValue();
                // Check for success; ignore SUSE return code when user already locked
                if (returnValue != CommandResult.RETURN_SUCCESS) {
                    smLogger.error("Could not disable user '" + username + "': " + passwdresult.getCommandOutput());
                    throw new CommandWrapperFault("passwd", "Could not disable user '" + username + "': " +
                            passwdresult.getCommandOutput());
                }
            } else {
                passwdresult = runLoggedCommand(new String[]{USERMOD_COMMAND, "-p", passwordCrypted, username},
                        null, null, true);
                if (passwdresult.getReturnValue() != CommandResult.RETURN_SUCCESS) {
                    smLogger.error("Could not set password for user '" + username + "': " +
                            passwdresult.getCommandOutput());
                    throw new CommandWrapperFault("passwd", "Could not set password for user '" +
                            username + "': " + passwdresult.getCommandOutput());
                }
            }
        } catch (CommandWrapperFault e) {
            smLogger.error("Could not set password for user '" + username + "': " + e);
            throw e;
        }
    }

    /**
     * Run a command in a particular directory with environment and return a CommandResult structure.
     *
     * @param username the user to run the command as.
     * @param cmd      the command and its parameters
     * @param dir      the directory to execute the command in
     * @param login    if true, make shell a login shell
     * @return CommandResult - the result of the command.
     * @throws CommandWrapperFault An error occurred while executing the command.
     */
    public CommandResult runCommandAs(String username, String cmd[], File dir, boolean login)
            throws CommandWrapperFault {

        if (cmd == null) {
            throw new CommandWrapperFault("", "Could not execute empty command");
        }

        String cmdString = cmd[0];
        for (int param = 1; param < cmd.length; param++) {
            String s = escapeCommand(cmd[param]);
            cmdString = cmdString + " \"" + s + "\"";
        }

        if (username == null) {
            return runCommand(cmd, null, dir);
        } else {
            String[] suCmd;
            if (login) {
                suCmd = new String[]{SU_COMMAND, "-s", RUN_COMMAND_AS_SHELL, "-c", cmdString, "-", username};
            } else {
                suCmd = new String[]{SU_COMMAND, "-s", RUN_COMMAND_AS_SHELL, "-c", cmdString, username};
            }

            return runCommand(suCmd, null, dir);
        }
    }

    /**
     * Creates or updates Unix/Linux hook scripts in the repository
     * @see CommandExecutor#createHookScript(String, com.vasoftware.sf.externalintegration.execution.CommandExecutor.HookEvent, String) 
     * @param repositoryDir the repo whose hooks script to modify
     * @param hook HookEvent for which to create script
     * @param scriptContent the command(s) to run when the event is fired
     * @throws CommandWrapperFault
     */
    public void createHookScript(String repositoryDir, HookEvent hook, String scriptContent) throws CommandWrapperFault {
        
        super.createHookScript(repositoryDir, hook, scriptContent);

        final File hookScriptFile = getHookScriptFile(repositoryDir, hook);
        setUserOnPath(ScmConstants.HTTPD_USER, hookScriptFile);
        setToOnlyReadWriteExecuteUserPermissions(hookScriptFile, true);
        
    }

    /**
     * @see com.vasoftware.sf.externalintegration.execution.executors.AbstractCommandExecutor#getEnvironmentSetCommand()
     */
    @Override
    protected String getEnvironmentSetCommand() {
        return "export";
    }

    /**
     * @see com.vasoftware.sf.externalintegration.execution.executors.AbstractCommandExecutor#getPathSeparator()
     */
    @Override
    protected String getPathSeparator() {
        return ":";
    }

    /**
     * @see com.vasoftware.sf.externalintegration.execution.executors.AbstractCommandExecutor#getFileSeparator()
     */
    @Override
    protected String getFileSeparator() {
        return "/";
    }

    /**
     * @see com.vasoftware.sf.externalintegration.execution.executors.AbstractCommandExecutor#getArgumentPrefix()
     */
    @Override
    protected String getArgumentPrefix() {
        return "\\$";
    }

    /**
     * @see com.vasoftware.sf.externalintegration.execution.executors.AbstractCommandExecutor#getHookScriptFile(java.lang.String, com.vasoftware.sf.externalintegration.execution.CommandExecutor.HookEvent)
     */
    @Override
    protected File getHookScriptFile(String repositoryDir, HookEvent hook) {
        return new File(new File(repositoryDir, "hooks"), hook.toScriptName());
    }

    /**
     * @see com.vasoftware.sf.externalintegration.execution.executors.AbstractCommandExecutor#doScriptModifications(java.lang.StringBuilder)
     */
    @Override
    protected StringBuilder doScriptModifications(StringBuilder sb) {
        // TODO: Make the shell configurable from sourceforge.properties
        final String shebangLine = "#!/bin/sh\n\n";
        sb.insert(0, shebangLine);
        return sb;
    }

    /**
     * @see com.vasoftware.sf.externalintegration.execution.executors.AbstractCommandExecutor#getCommentString()
     */
    @Override
    protected String getCommentString() {
        return "#";
    }
}
