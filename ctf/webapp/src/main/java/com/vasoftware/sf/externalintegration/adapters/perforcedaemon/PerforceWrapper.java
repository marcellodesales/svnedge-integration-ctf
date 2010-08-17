package com.vasoftware.sf.externalintegration.adapters.perforcedaemon;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.Random;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.perforce.api.Client;
import com.perforce.api.Env;
import com.vasoftware.sf.common.configuration.GlobalOptionKeys;
import com.vasoftware.sf.common.configuration.SfGlobalOptions;
import com.vasoftware.sf.common.configuration.SfGlobalOptionsManager;
import com.vasoftware.sf.common.configuration.SfPaths;
import com.vasoftware.sf.common.logger.Logger;
import com.vasoftware.sf.common.util.StringUtil;
import com.vasoftware.sf.externalintegration.ObjectAlreadyExistsFault;
import com.vasoftware.sf.externalintegration.execution.CommandExecutor;
import com.vasoftware.sf.externalintegration.execution.CommandResult;
import com.vasoftware.sf.externalintegration.execution.CommandWrapperFault;
import com.vasoftware.sf.externalintegration.execution.ScmLimitationFault;
import com.vasoftware.sf.externalintegration.execution.UserAlreadyExistsFault;

/**
 * Class for abstracting the p4 client executable
 */
@SuppressWarnings("unchecked")
public class PerforceWrapper {
    private static final Logger smLogger = Logger.getLogger(PerforceWrapper.class);

    /** Location of the perforce binary */
    private final String mPerforceBinary;

    /** The OS command executor */
    private final CommandExecutor mExecutor;

    /** the port used to communicate to the perforce host */
    private final String mPerforcePort;

    /** Perforce admin user */
    private String mPerforceAdminUser = null;

    /** admin password */
    private String mPerforceAdminPassword = null;

    /* Prefix of line returned by "p4 info" that specifies Server Version */
    private static final String VERSION_LINE_PREFIX = "Server version:";

    /** The command used to dump a file to stdout */
    private static final String CAT_COMMAND = "cat";

    /** The shell to use when executing a p4 command with an input form. */
    private static final String SHELL_COMMAND = "sh";
    public static final String ADMIN_USER_KEY = "admin_user";
    public static final String ADMIN_PASSWORD_KEY = "admin_password";
    private static final String LICENSE_QUOTA_EXCEEDED = "over license quota";

    /** Prefix for Perforce Triggers (Changed from version 2005.2 onwards) */
    private static final String TRIGGER_TYPE_PREFIX = "change-";
    private static final String PERFORCE_ADMIN_TXT = "perforce_admin.txt";

    /**
     * Default constructor.
     * 
     * @param executor
     *            the executor class for executing external commands
     */
    public PerforceWrapper(final CommandExecutor executor) {
        final SfGlobalOptions options = SfGlobalOptionsManager.getOptions();
        mPerforceBinary = options.getOption(GlobalOptionKeys.SFMAIN_PERFORCE_CLIENT);
        mPerforcePort = options.getOption(GlobalOptionKeys.PERFORCE_PORT);
        mExecutor = executor;
    }

    /**
     * Update values for the Perforce admin username and password and the process owner. This sets up a persistent login
     * for the admin user, available to the daemon process owner.
     * 
     * @param adminUsername
     *            the admin user
     * @param adminPassword
     *            the password
     * @param processOwner
     *            the process owner
     * @param triggerPrefixes
     *            the array of trigger prefixes to update the admin name param against
     * @throws CommandWrapperFault
     *             if a command fails
     * @throws ScmLimitationFault
     *             if this action fails to to exceeding license count
     */
    public void updateAdminInfo(final String adminUsername, final String adminPassword, final String processOwner,
                                final String[] triggerPrefixes) throws CommandWrapperFault, ScmLimitationFault {

        final String adminPasswordPath = SfPaths.dataEtcPath(PERFORCE_ADMIN_TXT);
        final File adminPasswordFile = new File(adminPasswordPath);
        final String[] args = new String[] {
            "/bin/ls",
            SfGlobalOptionsManager.getSourceForgePropertiesPath()
        };

        // verify that this user exists, can see sourceforge.properties (for triggers)
        CommandResult result = mExecutor.runCommandAs(processOwner, args, null, true);

        if (result.getReturnValue() != CommandResult.RETURN_SUCCESS) {
            final String message = "process owner doesn't exist or can't read properties file: "
                + result.getCommandOutput();

            smLogger.error(message);

            throw new CommandWrapperFault(new StringBuilder()
                                          .append(args[0])
                                          .append(" ")
                                          .append(args[1]).toString(), sanitizeString(message));
        }
        // TODO: ideally, validate that they are running the perforce process

        // Now, we have to handle a variety of scenarios involving potential previous values.
        String previousAdminUser = null;
        String previousAdminPassword = null;
        if (adminPasswordFile.exists()) {
            // we've had this info before - this may be a change
            previousAdminUser = getAdminUser();
            previousAdminPassword = getAdminPassword();

            if (adminUsername.equals(previousAdminUser)) {
                if (!adminPassword.equals(previousAdminPassword)) {

                    // do this before assigning mPerforceAdminUser{Password}, so we use the old values.
                    // we don't need to call create, since the setPassword will do it.
                    try {
                        setPassword(adminUsername, adminPassword);
                        mPerforceAdminPassword = adminPassword;
                    } catch (final CommandWrapperFault e) {
                        // maybe that failed because the password has already been set in perforce - try new one
                        mPerforceAdminPassword = adminPassword;
                        setPassword(adminUsername, adminPassword);
                    }
                } else {
                    // username and password match our previous values.
                    // For safety, should still set password (which would also create the user
                    // if they don't exist in perforce), in case, e.g., perforce has been wiped clean
                    setPassword(adminUsername, adminPassword);
                }
            } else {
                // this is a change in admin user - create, setpassword, give superuser. update trigger
                try {
                    // first try setting up new user using the old values
                    createAdminUser(adminUsername);
                    setPassword(adminUsername, adminPassword);
                    mPerforceAdminUser = adminUsername;
                    mPerforceAdminPassword = adminPassword;
                } catch (final CommandWrapperFault e) {
                    // old admin info may no longer be valid - e.g. has been wiped from perforce - try
                    // it again with the new info.
                    // (TODO: examine the exception more carefully to make sure this is the problem)
                    mPerforceAdminUser = adminUsername;
                    mPerforceAdminPassword = adminPassword;
                    createAdminUser(adminUsername);
                    setPassword(adminUsername, adminPassword);
                }
            }
        } else {
            // this is the first time - create, setpassword, give superuser
            mPerforceAdminUser = adminUsername;
            mPerforceAdminPassword = adminPassword;
            createAdminUser(adminUsername);
            setPassword(adminUsername, adminPassword);
        }

        // verify that this admin user is really a super user.
        try {
            getCommandOutput(new String[] { "protect", "-o" }, null);
        } catch (final CommandWrapperFault e) {
            if (previousAdminUser != null) {
                // revert to the old values
                mPerforceAdminUser = previousAdminUser;
                mPerforceAdminPassword = previousAdminPassword;
            }
            throw new CommandWrapperFault("updateAdminInfo", "new admin cannot perform super-user commands", e);
        }

        // now update the triggers
        updateAdminParamForTriggers(adminUsername, triggerPrefixes);

        // Write the admin properties into a separate file.
        // Note that the python triggers also need to know location of this file.
        final String contents = PerforceWrapper.ADMIN_USER_KEY + "=" + adminUsername + "\n"
                + PerforceWrapper.ADMIN_PASSWORD_KEY + "=" + adminPassword + "\n";
        mExecutor.createFile(adminPasswordFile, contents);
        mExecutor.setOwnerToOnlyReadWritePermissions("root", adminPasswordFile, false);

        // make this login we created persistent forever, by putting admin user in a group and giving
        // an infinite timeout. This needs to be done BEFORE the first login.
        final PerforceForm form = getGroupForm("sourceforge-admin");
        final List members = new ArrayList(1);
        members.add(adminUsername);
        form.setListValue("Users", members);
        form.setStringValue("Timeout", "0"); // set to 0 for ticket that does not expire
        executeWithInputForm("group", form.toString(), null, null);

        // Do a "p4 login" as the admin user, running as the unix owner of the process,
        // so that triggers do not need password
        File tempfile = null;
        try {
            tempfile = File.createTempFile("sfee", "p4");
            final FileWriter fileWriter = new FileWriter(tempfile);
            fileWriter.write(adminPassword);
            fileWriter.close();

            final String echoCommand = CAT_COMMAND + " \"" + tempfile.getAbsolutePath() + "\" | " + mPerforceBinary
                    + " " + getHostPortParam() + " " + getAdminUserParam() + " login";

            result = mExecutor
                              .runCommandAs(processOwner, new String[] { SHELL_COMMAND, "-c", echoCommand }, null, true);
            if (result.getReturnValue() != CommandResult.RETURN_SUCCESS) {
                smLogger.error("Could not execute command: " + result.getCommandOutput());
                throw new CommandWrapperFault(echoCommand, "Error: " + sanitizeString(result.getCommandOutput()));
            }

        } catch (final IOException e) {
            throw new CommandWrapperFault("updateAdminInfo", "Could not create temporary file for perforce password", e);
        } finally {
            if (tempfile != null) {
                tempfile.delete();
            }
        }

        // verify that the user has a ticket
        final String[] command = new String[] { mPerforceBinary, getHostPortParam(), "tickets" };
        result = mExecutor.runCommandAs(processOwner, command, null, true);
        if (result.getReturnValue() != CommandResult.RETURN_SUCCESS) {
            throw new CommandWrapperFault(sanitizeString(result.getCommand()),
                                          "Could not verify ticket for perforce admin user  (" + adminUsername
                                                  + ") for process owner (" + processOwner + "): " + result.getStderr());
        } else {
            if (result.getStdout().indexOf("(" + adminUsername + ")") < 0) {
                final String commandOutput = "stdout: " + result.getStdout() + ", stderr: " + result.getStderr();
                throw new CommandWrapperFault("updateAdminInfo", "Could not verify ticket for perforce admin user  ("
                        + adminUsername + ") for process owner (" + processOwner
                        + "): user not found in 'tickets' output: " + sanitizeString(commandOutput));
            }
        }
    }

    /**
     * Get the version of Perforce.
     * 
     * @return an array of integers representing the version, e.g. 2004, 04
     * @throws CommandWrapperFault
     *             thrown if we couldn't determine the version of Cvs.
     */
    public int[] getPerforceVersion() throws CommandWrapperFault {
        int[] result = null;

        // note: admin username and password are omitted here as a special case for the "info" command
        final CommandResult commandResult = mExecutor.runCommand(new String[] { mPerforceBinary, getHostPortParam(),
                                                                               "info" }, null, null);
        if (commandResult.getReturnValue() != CommandResult.RETURN_SUCCESS) {
            smLogger.error("Could not get Perforce version: " + commandResult.getCommandOutput());
            throw new CommandWrapperFault(sanitizeString(commandResult.getCommand()),
                                          "Could not get Perforce version: "
                                                  + sanitizeString(commandResult.getCommandOutput()));
        }

        final String output = commandResult.getStdout();

        String versionLine = null;
        final String[] lines = output.split("\n");
        for (int i = 0; i < lines.length; i++) {

            if (lines[i].startsWith(VERSION_LINE_PREFIX)) {
                versionLine = lines[i];
                break;
            }
        }

        if (versionLine == null) {
            throw new CommandWrapperFault(mPerforceBinary + " -V", "Could not determine Perforce version: "
                    + commandResult.getStdout());
        }

        final String pattern = VERSION_LINE_PREFIX + " [^/]*/[^/]*/([^/\\.]*)\\.([^/]*)/.*";
        final Pattern p = Pattern.compile(pattern);
        final Matcher m = p.matcher(versionLine);

        final boolean versionFound = m.matches();

        if (!versionFound || m.groupCount() < 2) {
            throw new CommandWrapperFault(mPerforceBinary + " -V", "Could not determine Perforce version: "
                    + commandResult.getStdout());
        }

        result = new int[2];
        result[0] = Integer.parseInt(m.group(1));
        result[1] = Integer.parseInt(m.group(2));

        return result;
    }

    /**
     * get the string specifying server host and port
     * 
     * @return host/port param
     */
    private String getHostPortParam() {
        return "-p" + mPerforcePort;
    }

    /**
     * get the string specifying admin user
     * 
     * @return admin user param
     * @throws CommandWrapperFault
     *             on failure
     */
    private String getAdminUserParam() throws CommandWrapperFault {
        if (mPerforceAdminUser == null) {
            loadAdminParams();
        }
        return "-u" + mPerforceAdminUser;
    }

    /**
     * get the string specifying admin user
     * 
     * @return admin username
     * @throws CommandWrapperFault
     *             on failure
     */
    public String getAdminUser() throws CommandWrapperFault {
        if (mPerforceAdminUser == null) {
            loadAdminParams();
        }
        return mPerforceAdminUser;
    }

    /**
     * Get the admin password
     * 
     * @return admin password
     * @throws CommandWrapperFault
     *             if we have a problem getting the current password
     */
    public String getAdminPassword() throws CommandWrapperFault {
        if (mPerforceAdminPassword == null) {
            loadAdminParams();
        }
        return mPerforceAdminPassword;
    }

    /**
     * Get the admin password parameter
     * 
     * @return a suitable password parameter for supplying to perforce commands
     * @throws CommandWrapperFault
     *             if we have a problem getting the current password
     */
    private String getAdminPasswordParam() throws CommandWrapperFault {
        if (mPerforceAdminPassword == null) {
            loadAdminParams();
        }
        if (mPerforceAdminPassword.length() == 0) {
            return "-P\"\""; // specifying a -P with no password causes problems...
        } else {
            return "-P" + escapeString(mPerforceAdminPassword);
        }
    }

    /**
     * Escapes the string
     * 
     * @param origString
     *            original string
     * @return escaped string
     */
    private String escapeString(final String origString) {
        /*
         * boolean addQuotes = false; StringBuffer escapedString = new StringBuffer(); for (int i = 0; i <
         * origString.length(); i++) { char thisChar = origString.charAt(i); if (thisChar == '\'') { addQuotes = true; }
         * 
         * escapedString.append(thisChar); }
         * 
         * if (addQuotes) { return "\"" + escapedString + "\""; } else { return escapedString.toString(); }
         */
        return origString;
    }

    /**
     * Load the administrator params from a special file
     * 
     * @throws CommandWrapperFault
     *             if a problem is encountered
     */
    private void loadAdminParams() throws CommandWrapperFault {
        final String adminPasswordPath = SfPaths.dataEtcPath(PERFORCE_ADMIN_TXT);
        final File adminPasswordFile = new File(adminPasswordPath);

        final Properties adminProperties = new Properties();
        try {
            adminProperties.load(new FileInputStream(adminPasswordFile));
        } catch (final IOException e) {
            final String message = "could not find admin properties file, " + adminPasswordFile.getAbsolutePath();
            smLogger.error(message);
            throw new CommandWrapperFault("loadAminParams", message, e);
        }

        mPerforceAdminUser = adminProperties.getProperty(ADMIN_USER_KEY);
        if (mPerforceAdminUser == null) {
            final String message = "could not find admin username in file " + adminPasswordFile.getAbsolutePath()
                    + "(looking for key: " + ADMIN_USER_KEY + ")";
            smLogger.error(message);
            throw new CommandWrapperFault("loadAdminParams", message);
        }

        mPerforceAdminPassword = adminProperties.getProperty(ADMIN_PASSWORD_KEY);
        if (mPerforceAdminPassword == null) {
            final String message = "could not find admin password in file " + adminPasswordFile.getAbsolutePath()
                    + "(looking for key: " + ADMIN_PASSWORD_KEY + ")";
            smLogger.error(message);
            throw new CommandWrapperFault("loadAdminParams", message);
        }
    }

    /**
     * Execute the perforce command with a form as input. The "-i" param is automatically added.
     * 
     * @param command
     *            the command to execute
     * @param form
     *            the form to provide as input
     * @param username
     *            the user to execute as
     * @param extraParam
     *            an optional extra parameter.
     * @return the command result for the execution
     * @throws CommandWrapperFault
     *             thrown if there were problems setting up the command execution
     */
    public CommandResult executeWithInputForm(final String command, final String form, final String username,
                                              final String extraParam) throws CommandWrapperFault {
        File tempfile = null;
        try {
            tempfile = File.createTempFile("sfee", "p4");
            final FileWriter fileWriter = new FileWriter(tempfile);
            fileWriter.write(form);
            fileWriter.close();

            String echoCommand = CAT_COMMAND + " \"" + tempfile.getAbsolutePath() + "\" | " + mPerforceBinary + " "
                    + getHostPortParam() + " " + getUserParam(username) + " '" + getUserPasswordParam(username) + "' "
                    + command + " -i";
            if (extraParam != null) {
                echoCommand += " " + extraParam;
            }

            final CommandResult result = mExecutor.runCommand(new String[] { SHELL_COMMAND, "-c", echoCommand }, null,
                                                              null);
            if (result.getReturnValue() != CommandResult.RETURN_SUCCESS) {
                smLogger.error("Could not execute command: " + result.getCommandOutput());
                throw new CommandWrapperFault(sanitizeString(echoCommand), "Error: "
                        + sanitizeString(result.getCommandOutput()));
            }

            return result;

        } catch (final IOException e) {
            throw new CommandWrapperFault("executeWithInputForm", "Could not create temporary file for perforce form ["
                    + form + "]", e);
        } finally {
            if (tempfile != null) {
                tempfile.delete();
            }
        }

    }

    /**
     * syntactic sugar for getCommandOutput(String[]) with a single supplied command name. This should be used for
     * commands that do NOT need a "-o" param to force output to standard out.
     * 
     * @see #getCommandOutput(String[],String)
     */
    public String getCommandOutput(final String command) throws CommandWrapperFault, ScmLimitationFault {
        return getCommandOutput(new String[] { command }, null);
    }

    /**
     * Return the output of executing the specified command. The "-o" parameter is NOT implicit.
     * 
     * @param commandComponents
     *            array of words for the command
     * @param username
     *            the user to run this command as
     * @return standard out from the command
     * @throws CommandWrapperFault
     *             if it fails
     * @throws ScmLimitationFault
     *             If a limitation was exceeded on the scm system
     */
    public String getCommandOutput(final String[] commandComponents, final String username) throws CommandWrapperFault,
                                                                                           ScmLimitationFault {
        final String[] initialComponents = new String[] { mPerforceBinary, getHostPortParam(), getUserParam(username),
                                                         getUserPasswordParam(username) };

        // build a single string array of initial components and new ones
        final String[] fullCommand = new String[initialComponents.length + commandComponents.length];
        for (int i = 0; i < initialComponents.length; i++) {
            fullCommand[i] = initialComponents[i];
        }
        for (int i = 0; i < commandComponents.length; i++) {
            fullCommand[initialComponents.length + i] = commandComponents[i];
        }

        final CommandResult result = mExecutor.runCommand(fullCommand, null, null);
        if (result.getReturnValue() != CommandResult.RETURN_SUCCESS) {
            final String stderr = result.getStderr();
            if (stderr.indexOf(LICENSE_QUOTA_EXCEEDED) > -1) {
                throw new ScmLimitationFault("Perforce: " + stderr);
            }
            throw new CommandWrapperFault(sanitizeString(commandComponents[0]), "Could not execute command for user ("
                    + username + "). stderr: [" + result.getStderr() + "]  stdout: [" + result.getStdout() + "]");
        }
        final String output = result.getStdout();
        return output;
    }

    /**
     * Add a trigger with a specific name, type (e.g., "submit" or "commit"), path and command. If a submit trigger
     * already exists on the name and path, replace it with the command being passed in.
     * 
     * @param triggerName
     *            the name of the trigger
     * @param triggerType
     *            the perforce trigger stage to attach the trigger to
     * @param triggerPath
     *            the path the trigger is active on
     * @param command
     *            the command that the trigger executes
     * @throws CommandWrapperFault
     *             thrown on error
     */
    public void addOrReplaceTrigger(final String triggerName, final String triggerType, final String triggerPath,
                                    final String command) throws CommandWrapperFault {

        try {
            final String[] triggersCommand = new String[] { mPerforceBinary, getHostPortParam(), getAdminUserParam(),
                                                           getAdminPasswordParam(), "triggers", "-o" };
            CommandResult result = mExecutor.runCommand(triggersCommand, null, null);
            if (result.getReturnValue() != CommandResult.RETURN_SUCCESS) {
                throw new CommandWrapperFault(sanitizeString(result.getCommand()),
                                              "Could not query triggers on repository");
            }
            final String[] triggers = result.getStdout().split("\n");
            final String triggerPrefix = "\t" + triggerName + " " + TRIGGER_TYPE_PREFIX + triggerType + " "
                    + triggerPath;
            String triggerForm = "Triggers:\n" + triggerPrefix + " " + command + "\n";
            for (int i = 0; i < triggers.length; i++) {
                // Skip over any comments or the "Triggers:" heading
                if (!triggers[i].startsWith("\t")) {
                    continue;
                }
                // Skip over any trigger with the same name, type, and trigger path... we're clobbering it.
                if (triggers[i].startsWith(triggerPrefix)) {
                    continue;
                }
                triggerForm += triggers[i] + "\n";
            }

            result = executeWithInputForm("triggers", triggerForm, null, null);
            if (result.getReturnValue() != CommandResult.RETURN_SUCCESS) {
                throw new CommandWrapperFault(sanitizeString(result.getCommand()),
                                              "Could not create triggers on repository");
            }
        } catch (final IOException e) {
            throw new CommandWrapperFault("setupRepository", sanitizeString(e.getMessage()), e);
        }
    }

    /**
     * Covers up the password in the string.
     * 
     * @param command
     *            the original string
     * @return sanitized version
     */
    public static String sanitizeString(final String command) {
        return command.replaceAll(" -P[ ]*[^ ]*", " -P********");
    }

    /**
     * Updates admin param on all triggers to the new admin name
     * 
     * @param newAdminName
     *            new admin name for the triggers
     * @param prefixes
     *            the array of prefixes to update the new admin name
     * @throws CommandWrapperFault
     *             if this operation fails
     */
    public void updateAdminParamForTriggers(final String newAdminName, final String[] prefixes)
                                                                                               throws CommandWrapperFault {
        for (int i = 0; i < prefixes.length; i++) {
            updateAdminParamForTriggers(prefixes[i], newAdminName);
        }
    }

    /**
     * Sets the admin name parameter for all triggers with the triggerPrefix
     * 
     * @param triggerPrefix
     *            trigger prefix to match
     * @param newAdminName
     *            new admin name
     * @throws CommandWrapperFault
     *             if this operation fails
     */
    private void updateAdminParamForTriggers(final String triggerPrefix, final String newAdminName)
                                                                                                   throws CommandWrapperFault {

        if (StringUtil.isEmpty(newAdminName)) {
            throw new CommandWrapperFault("triggers", "calling updateAdminParamForTriggers with newAdminName ("
                    + newAdminName + ")");
        }

        try {
            final String[] triggersCommand = new String[] { mPerforceBinary, getHostPortParam(), getAdminUserParam(),
                                                           getAdminPasswordParam(), "triggers", "-o" };
            CommandResult result = mExecutor.runCommand(triggersCommand, null, null);
            if (result.getReturnValue() != CommandResult.RETURN_SUCCESS) {
                throw new CommandWrapperFault(sanitizeString(result.getCommand()),
                                              "Could not query triggers on repository");
            }
            final String[] triggers = result.getStdout().split("\n");
            final StringBuffer triggerForm = new StringBuffer();
            triggerForm.append("Triggers:\n");
            for (int i = 0; i < triggers.length; i++) {
                // Skip over any comments or the "Triggers:" heading
                if (!triggers[i].startsWith("\t")) {
                    continue;
                }

                final String triggerLine = triggers[i].trim();

                if (triggerLine.startsWith(triggerPrefix)) {
                    // gonna assume the structure of the trigger line is of the following format:
                    // <trigger name> <trigger type> <repo name>/... "<trigger location> <extsystem id> <admin>
                    // %user% %changelist% <repo name>"

                    final StringBuffer sb = new StringBuffer();
                    sb.append("\t");
                    final String[] triggerLineParts = triggerLine.split(" ");
                    for (int j = 0; j < triggerLineParts.length; j++) {
                        final String curPart = triggerLineParts[j];
                        if (j != 0) {
                            if ("%user%".equals(curPart)) {
                                triggerLineParts[j - 1] = newAdminName;
                            }
                            sb.append(triggerLineParts[j - 1]);
                            sb.append(" ");
                        }
                    }
                    // append the last part
                    sb.append(triggerLineParts[triggerLineParts.length - 1]);
                    sb.append("\n");
                    triggerForm.append(sb);
                } else {
                    // not a match - just retain it unchanged
                    triggerForm.append(triggers[i]);
                    triggerForm.append("\n");
                }
            }

            result = executeWithInputForm("triggers", triggerForm.toString(), null, null);
            if (result.getReturnValue() != CommandResult.RETURN_SUCCESS) {
                throw new CommandWrapperFault(sanitizeString(result.getCommand()),
                                              "Could not modify admin parameter for triggers");
            }
        } catch (final IOException e) {
            throw new CommandWrapperFault("updateAdminParamForTriggers", e.getMessage(), e);
        }
    }

    /**
     * Create a perforce depot with a given name.
     * 
     * @param depotName
     *            Create a depot with a given name
     * @throws CommandWrapperFault
     *             thrown if there was a failure executing perforce commands
     * @throws ObjectAlreadyExistsFault
     *             thrown if a depot with that name already exists.
     */
    public void createDepot(final String depotName) throws CommandWrapperFault, ObjectAlreadyExistsFault {

        if (depotExists(depotName)) {
            throw new ObjectAlreadyExistsFault("depot:" + depotName);
        }

        final String depotForm = "Depot: " + depotName + "\nDescription:\n\t" + depotName
                + " Created by SFEE\nType:\n\tlocal\n" + "Address: subdir\n" + "Map:" + depotName + "/...";
        final CommandResult result = executeWithInputForm("depot", depotForm, null, null);

        if (result.getReturnValue() != CommandResult.RETURN_SUCCESS) {
            throw new CommandWrapperFault(sanitizeString(result.getCommand()), "Could not create depot: "
                    + result.getStderr() + result.getStdout());
        }
    }

    /**
     * Return true iff the named depot exists
     * 
     * @param depotName
     *            name of the depot to look for
     * @return true iff the depot exists
     * @throws CommandWrapperFault
     *             if set of existing depots cannot be read
     */
    public boolean depotExists(final String depotName) throws CommandWrapperFault {
        final String[] depotsCommand = new String[] { mPerforceBinary, getHostPortParam(), getAdminUserParam(),
                                                     getAdminPasswordParam(), "depots" };
        final CommandResult result = mExecutor.runCommand(depotsCommand, null, null);
        if (result.getReturnValue() != CommandResult.RETURN_SUCCESS) {
            throw new CommandWrapperFault(sanitizeString(result.getCommand()), "Could not get a list of depots: "
                    + sanitizeString(result.getStderr() + result.getStdout()));
        }
        final String[] depotList = result.getStdout().split("\n");
        for (int i = 0; i < depotList.length; i++) {
            if (depotList[i].startsWith("Depot " + depotName + " ")) {
                return true;
            }
        }
        return false;
    }

    /**
     * Return true iff the named directory path exist
     * 
     * @param clientName
     *            the name of the Perforce client
     * @param dirPath
     *            the path of the directory, including depot
     * @return true iff the directory exists
     * @throws CommandWrapperFault
     *             if set of existing depots cannot be read
     */
    public boolean directoryPathExists(final String clientName, final String dirPath) throws CommandWrapperFault {
        final String[] command = new String[] { mPerforceBinary, getHostPortParam(), getAdminUserParam(),
                                               getAdminPasswordParam(), "-c", clientName, "dirs", dirPath };
        final CommandResult result = mExecutor.runCommand(command, null, null);
        if (result.getReturnValue() != CommandResult.RETURN_SUCCESS) {
            throw new CommandWrapperFault(sanitizeString(result.getCommand()), "Could not verify path: " + dirPath
                    + ": " + result.getStderr());
        }
        final String dirsResult = result.getStdout().trim();
        // if the directory is not found, the name will be followed by an error string, e.g. "- no such file(s)".
        // alas, this can be internationalized - so we'll just look for the input, which means no error
        // message.
        final boolean matches = dirPath.equals(dirsResult);
        return matches;
    }

    /**
     * Create a user on the Perforce server and sets password to a random string. This should be used for creating new
     * accounts for sourceforge users.
     * 
     * @param userName
     *            the user
     * @throws CommandWrapperFault
     *             if it fails
     * @throws UserAlreadyExistsFault
     *             if the user already exists in Perforce
     * @throws ScmLimitationFault
     *             If a limitation was exceeded on the scm system
     */
    public void createUser(final String userName) throws UserAlreadyExistsFault, CommandWrapperFault,
                                                 ScmLimitationFault {

        // First see if the user already exists
        final String currentUserFormText = getCommandOutput(new String[] { "user", "-fo", userName }, null);
        final PerforceForm form = PerforceForm.parse(currentUserFormText);
        if (form.containsKey("Updated") && !"".equals(form.getStringValue("Updated"))) {
            throw new UserAlreadyExistsFault(userName);
        }

        form.setStringValue("Email", "none"); // TODO: can/should we get real email?
        final CommandResult result = executeWithInputForm("user", form.toString(), null, "-f");

        if (result.getReturnValue() != CommandResult.RETURN_SUCCESS) {
            throw new CommandWrapperFault(sanitizeString(result.getCommand()), "Could not create user: " + userName
                    + ": " + result.getStderr());
        }

        // random password so that user must go set one to use perforce
        // TODO (D): improve randomness algorithm? Better seed than current time?
        final String randomPassword = "pw" + Integer.toString((new Random()).nextInt());
        setPassword(userName, randomPassword);
    }

    /**
     * Create an admin user on the Perforce server. This differs from regular CreateAdmin user, in that: 1) It doesn't
     * throw UserAlreadyExists exception, since we don't care, and 2) it doesn't set the password to garbage, since
     * we'll be setting the password immediately.
     * 
     * @param userName
     *            the user
     * @throws CommandWrapperFault
     *             if it fails
     * @throws ScmLimitationFault
     *             If a limitation was exceeded on the scm system
     */
    public void createAdminUser(final String userName) throws CommandWrapperFault, ScmLimitationFault {

        // First see if the user already exists
        final String userFormOutput = getCommandOutput(new String[] { "user", "-fo", userName }, null);
        final PerforceForm userForm = PerforceForm.parse(userFormOutput);
        validateForm(userForm, "user", "User");
        final CommandResult result = executeWithInputForm("user", userForm.toString(), null, "-f");

        if (result.getReturnValue() != CommandResult.RETURN_SUCCESS) {
            throw new CommandWrapperFault(sanitizeString(result.getCommand()), "Could not create user: " + userName
                    + ": " + result.getStderr());
        }
    }

    /**
     * Delete a user from the Perforce server
     * 
     * @param username
     *            the user
     * @param deleteFromGroups
     *            true if the user should be deleted from groups as well as deleted as a user. This would be false if
     *            this method is being called after the user was already removed from her last group.
     * @throws CommandWrapperFault
     *             if it fails
     */
    public void deleteUser(final String username, final boolean deleteFromGroups) throws CommandWrapperFault {

        if (deleteFromGroups) {
            final String[] groups = getUserGroups(username);
            for (int i = 0; i < groups.length; i++) {
                final String group = groups[i];

                // yes, this looks a lot like PerforceScmServerDaemon.removeUsersFromAccessGroup(),
                // but that method would call deleteUser after the last one, starting a loop back to here...
                String currentGroupForm = null;
                try {
                    currentGroupForm = getCommandOutput(new String[] { "group", "-o", group }, null);
                } catch (final ScmLimitationFault scmLimitationFault) {
                    throw new CommandWrapperFault("group", "Unexpected error", scmLimitationFault);
                }
                final PerforceForm form = PerforceForm.parse(currentGroupForm);
                validateForm(form, "group", "Group");
                final List members = form.getListValue("Users");
                members.remove(username);
                form.setListValue("Users", members);
                executeWithInputForm("group", form.toString(), null, null);
            }

        }

        // Before deleting the user, we need to figure out if he has any files open on any clients
        final String[] openedCommand = new String[] { mPerforceBinary, getHostPortParam(), getAdminUserParam(),
                                                     getAdminPasswordParam(), "opened", "-a" };
        final CommandResult openedCommandResult = mExecutor.runCommand(openedCommand, null, null);
        if (openedCommandResult.getReturnValue() == CommandResult.RETURN_SUCCESS) {
            final String[] openedFiles = openedCommandResult.getStdout().split("\n");
            for (int i = 0; i < openedFiles.length; i++) {
                final String line = openedFiles[i];
                final int usernameIndex = line.indexOf(username + "@");
                // only care about open entries that belong to this user that we want to delete
                if (usernameIndex >= 0) {
                    final String clientName = line.substring(usernameIndex + username.length() + 1);
                    // delete the client to delete the open files
                    deleteClient(clientName);
                }
            }
        } else {
            throw new CommandWrapperFault(sanitizeString(openedCommandResult.getCommand()),
                                          "Could not display all the open files in preparation "
                                                  + "for user deletion of " + username + ": "
                                                  + sanitizeString(openedCommandResult.getStderr()));
        }

        final String[] deleteUserCommand = new String[] { mPerforceBinary, getHostPortParam(), getAdminUserParam(),
                                                         getAdminPasswordParam(), "user", "-df", username };
        final CommandResult result = mExecutor.runCommand(deleteUserCommand, null, null);
        if (result.getReturnValue() != CommandResult.RETURN_SUCCESS) {
            if (result.getStderr().indexOf("User " + username + " doesn't exist") != -1) {
                if (smLogger.isDebugEnabled()) {
                    smLogger.debug("Attempted to delete user " + username + ", who doesn't exist - ignoring.");
                }
            } else {
                throw new CommandWrapperFault(sanitizeString(result.getCommand()), "Could not delete user " + username
                        + ": " + sanitizeString(result.getStderr()));
            }
        }
    }

    /**
     * Validates that a particular field exists in the form
     * 
     * @param form
     *            the form
     * @param command
     *            the command to show in the exception
     * @param fieldName
     *            the fieldname to look
     * @throws CommandWrapperFault
     *             if the field isn't found
     */
    private void validateForm(final PerforceForm form, final String command, final String fieldName)
                                                                                                    throws CommandWrapperFault {
        if (!form.containsKey(fieldName)) {
            throw new CommandWrapperFault(sanitizeString(command), "Could not find required field '" + fieldName
                    + "' in form: " + form.toString());
        }
    }

    /**
     * Get the current list of perforce users. This does not include users already added to groups but not yet quite
     * "created" as users on their own.
     * 
     * @return a Set containing all created users.
     * @throws CommandWrapperFault
     *             on failure
     */
    public Set getCurrentUserSet() throws CommandWrapperFault {
        String userOutput = null;
        try {
            userOutput = getCommandOutput("users");
        } catch (final ScmLimitationFault scmLimitationFault) {
            throw new CommandWrapperFault("users", "Unexpected exception", scmLimitationFault);
        }
        final String[] lines = userOutput.split("\n");
        final Set usernames = new HashSet();

        for (int i = 0; i < lines.length; i++) {
            final String line = lines[i];
            if (line.length() == 0) {
                continue;
            }
            final String username = line.substring(0, line.indexOf(" "));
            usernames.add(username);
        }

        return usernames;
    }

    /**
     * Delete a Perforce client from the server.
     * 
     * @param clientName
     *            the name of the client to delete
     * @throws CommandWrapperFault
     *             if it fails
     */
    public void deleteClient(final String clientName) throws CommandWrapperFault {

        final String[] command = new String[] { mPerforceBinary, getHostPortParam(), getAdminUserParam(),
                                               getAdminPasswordParam(), "client", "-df", clientName };
        final CommandResult result = mExecutor.runCommand(command, null, null);
        if (result.getReturnValue() != CommandResult.RETURN_SUCCESS) {
            throw new CommandWrapperFault(sanitizeString(result.getCommand()), "Could not delete client " + clientName
                    + ": " + sanitizeString(result.getStderr()));
        }
    }

    /**
     * Synchronizes the path in the client workspace
     * 
     * @param clientName
     *            name of the client workspace
     * @param path
     *            path to synchronize (instead of entire client workspace)
     * @param user
     *            the perforce user to do this as. If user is null, the SF admin user is used.
     * @throws CommandWrapperFault
     *             if the synchronization fails
     */
    public void sync(final String clientName, final String path, final String user) throws CommandWrapperFault {
        String[] command;

        if (path != null) {
            command = new String[] { mPerforceBinary, getHostPortParam(), getUserParam(user),
                                    getUserPasswordParam(user), "-c", clientName, "sync", path };
        } else {
            command = new String[] { mPerforceBinary, getHostPortParam(), getUserParam(user),
                                    getUserPasswordParam(user), "-c", clientName, "sync" };
        }
        final CommandResult result = mExecutor.runCommand(command, null, null);
        if (result.getReturnValue() != CommandResult.RETURN_SUCCESS) {
            throw new CommandWrapperFault(sanitizeString(result.getCommand()), "Could not sync with client "
                    + clientName + ": " + sanitizeString(result.getStderr()));
        }

        final String commandOutput = result.getCommandOutput();
        if (commandOutput != null && commandOutput.endsWith("- no such file(s).")) {
            throw new CommandWrapperFault(sanitizeString(result.getCommand()), "Could not sync with client "
                    + clientName + ": " + sanitizeString(commandOutput));
        }
    }

    /**
     * Marks a file for deletion
     * 
     * @param clientName
     *            name of the client workspace
     * @param file
     *            file to be delted
     * @param username
     *            the user as whom to execute the command
     * @throws CommandWrapperFault
     *             if the deletion fails
     */
    public void deleteFile(final String clientName, final File file, final String username) throws CommandWrapperFault {
        executeSimpleFileCommand("delete", clientName, file, username);
    }

    /**
     * get the Perforce "Env" object for the current server and user.
     * 
     * @param userName
     *            Perforce userName
     * @param password
     *            password to the Perforce user
     * @param clientName
     *            Perforce clientName
     * @return the Env perforce environment
     */
    public Env createP4Env(final String userName, final String password, final String clientName) {
        final Env p4Env = new Env();
        p4Env.setPort(mPerforcePort);
        p4Env.setUser(userName);
        if (password != null) {
            p4Env.setPassword(password);
        }
        p4Env.setClient(clientName);
        p4Env.setExecutable(mPerforceBinary);

        return p4Env;
    }

    /**
     * Construct (if necessary) a Perforce client for working with the specified repository
     * 
     * @param p4Env
     *            Perforce environment
     * @return Perforce client
     */
    public Client createPerforceClient(final Env p4Env) {
        final Client p4Client = new Client(p4Env, p4Env.getClient());
        return p4Client;
    }

    /**
     * Set a user's password
     * 
     * @param username
     *            the user
     * @param password
     *            the new password
     * @throws CommandWrapperFault
     *             on failure
     */
    public void setPassword(final String username, final String password) throws CommandWrapperFault {
        String adminPasswdParam = getAdminPassword();
        if (adminPasswdParam.length() == 0) {
            adminPasswdParam = "-O\"\"";
        } else {
            adminPasswdParam = "-O" + escapeString(adminPasswdParam);
        }

        String[] command;
        if ("".equals(password)) {
            command = new String[] { mPerforceBinary, getHostPortParam(), getAdminUserParam(), getAdminPasswordParam(),
                                    "passwd", adminPasswdParam, "-P''", username }; // this syntax works for setting
            // empty password
        } else {
            command = new String[] { mPerforceBinary, getHostPortParam(), getAdminUserParam(), getAdminPasswordParam(),
                                    "passwd", adminPasswdParam, "-P", escapeString(password), username };
        }
        final CommandResult result = mExecutor.runCommand(command, null, null);
        if (result.getReturnValue() != CommandResult.RETURN_SUCCESS) {
            if (result.getStderr().indexOf("User " + username + " doesn't exist") != -1) {
                if (smLogger.isDebugEnabled()) {
                    smLogger
                            .debug("Attempted to set password for user " + username + ", who doesn't exist - ignoring.");
                }
            } else {
                throw new CommandWrapperFault(sanitizeString(result.getCommand()), "Could not set password for "
                        + username + ": " + sanitizeString(result.getStderr()));
            }
        }
    }

    /**
     * get the groups of which the user is a member
     * 
     * @param username
     *            person to ask about
     * @return array of group names of which the user is a member
     * @throws CommandWrapperFault
     *             if command cannot be executed.
     */
    public String[] getUserGroups(final String username) throws CommandWrapperFault {
        String output = null;
        try {
            output = getCommandOutput(new String[] { "groups", username }, null);
        } catch (final ScmLimitationFault scmLimitationFault) {
            throw new CommandWrapperFault("groups", "Unexpected error", scmLimitationFault);
        }
        if ("".equals(output)) {
            return new String[0];
        } else {
            return output.split("\n");
        }
    }

    /**
     * Calls "p4 integrate" to copy a file (while keeping the history in tact)
     * 
     * @param clientName
     *            name of client to use
     * @param srcFile
     *            src file to copy from
     * @param dstFile
     *            dst file
     * @param username
     *            the user to run the command as.
     * @throws CommandWrapperFault
     *             if the command fails
     */
    public void integrateFile(final String clientName, final File srcFile, final File dstFile, final String username)
                                                                                                                     throws CommandWrapperFault {
        final String[] command = new String[] { mPerforceBinary, getHostPortParam(), getUserParam(username),
                                               getUserPasswordParam(username), "-c", clientName, "integrate",
                                               srcFile.getAbsolutePath(), dstFile.getAbsolutePath() };
        final CommandResult result = mExecutor.runCommand(command, null, null);
        if (result.getReturnValue() != CommandResult.RETURN_SUCCESS) {
            throw new CommandWrapperFault(sanitizeString(result.getCommand()), "Could not integrate "
                    + srcFile.getAbsolutePath() + "and " + dstFile.getAbsolutePath() + " in client " + clientName
                    + ": " + sanitizeString(result.getStderr()));
        }
    }

    /**
     * Add a file to perforce
     * 
     * @param clientName
     *            the client
     * @param file
     *            the file
     * @param username
     *            the user
     * @throws CommandWrapperFault
     *             if it fails
     */
    public void addFile(final String clientName, final File file, final String username) throws CommandWrapperFault {
        executeSimpleFileCommand("add", clientName, file, username);
    }

    /**
     * Mark a file for edit in perforce
     * 
     * @param clientName
     *            the client
     * @param file
     *            the file
     * @param username
     *            the user
     * @throws CommandWrapperFault
     *             if it fails
     */
    public void editFile(final String clientName, final File file, final String username) throws CommandWrapperFault {
        executeSimpleFileCommand("edit", clientName, file, username);
    }

    /**
     * Mark a file for edit in perforce
     * 
     * @param clientName
     *            the client
     * @param file
     *            the file
     * @param username
     *            the user
     * @throws CommandWrapperFault
     *             if it fails
     */
    public void removePendingChanges(final String clientName, final File file, final String username)
                                                                                                     throws CommandWrapperFault {
        executeSimpleFileCommand("revert", clientName, file, username);
    }

    /**
     * Execute a simple perforce command
     * 
     * @param command
     *            the command to execute
     * @param clientName
     *            name of the client workspace
     * @param file
     *            file to be delted
     * @param username
     *            the username to execute as; if this is null, use the admin username
     * @throws CommandWrapperFault
     *             if the deletion fails
     */
    private void executeSimpleFileCommand(final String command, final String clientName, final File file,
                                          final String username) throws CommandWrapperFault {
        final String filePath = file.getAbsolutePath();
        final String[] commandArray = new String[] { mPerforceBinary, getHostPortParam(), getUserParam(username),
                                                    getUserPasswordParam(username), "-c", clientName, command, filePath };
        final CommandResult result = mExecutor.runCommand(commandArray, null, null);
        if (result.getReturnValue() != CommandResult.RETURN_SUCCESS || !StringUtil.isEmpty(result.getStderr())) {
            String errmsg = result.getStderr();
            if (errmsg == null || "".equals(errmsg)) {
                errmsg = result.getStdout(); // sometimes Perforce reports to stdout instead of stderr
            }
            throw new CommandWrapperFault(sanitizeString(result.getCommand()), "Could not execute " + command
                    + " on file " + filePath + "in client " + clientName + ": " + sanitizeString(errmsg));
        }
    }

    /**
     * Utility method to return the username parameter for the supplied username - or the admin user parameter if the
     * supplied username is null.
     * 
     * @param username
     *            the username to put into a param. If this is null, return the admin user param
     * @return the user parameter suitable for passing into a perforce command.
     * @throws CommandWrapperFault
     *             on failure
     */
    private String getUserParam(final String username) throws CommandWrapperFault {
        if (username == null) {
            return getAdminUserParam();
        } else {
            return "-u" + username;
        }
    }

    /**
     * Utility method to return the password parameter for the supplied username - or the admin pasword parameter if the
     * supplied username is null.
     * 
     * @param username
     *            the username to put into a param. If this is null, return the admin password; otherwise, return
     *            username (since all our test users have the same password as username)
     * @return the user parameter suitable for passing into a perforce command.
     * @throws CommandWrapperFault
     *             if the password cannot be fetched
     */
    private String getUserPasswordParam(final String username) throws CommandWrapperFault {
        if (username == null) {
            return getAdminPasswordParam();
        } else {
            return "-P" + username;
        }
    }

    /**
     * commit a change to Perforce (actually a submit in P4 terminology)
     * 
     * @param clientName
     *            the client
     * @param comment
     *            description
     * @param username
     *            who to do it as
     * @throws CommandWrapperFault
     *             if it fails
     */
    public void commit(final String clientName, final String comment, final String username) throws CommandWrapperFault {

        // oddly, the "submit" command will bring up a form to edit by default, but does not take "-o" and "-i"
        // flags. Instead, we edit a "change" to set the description, then submit that change number.

        // first we edit the change to make the description
        String formText = null;
        try {
            formText = getCommandOutput(new String[] { "-c", clientName, "change", "-o" }, username);
        } catch (final ScmLimitationFault scmLimitationFault) {
            throw new CommandWrapperFault("change", "Unexpected error", scmLimitationFault);
        }
        PerforceForm form = PerforceForm.parse(formText);

        final List descriptionList = new ArrayList();
        descriptionList.add(comment);
        form.setListValue("Description", descriptionList);

        CommandResult result;
        File tempfile = null;
        try {
            tempfile = File.createTempFile("sfee", "p4");
            final FileWriter fileWriter = new FileWriter(tempfile);
            fileWriter.write(form.toString());
            fileWriter.close();

            final String echoCommand = CAT_COMMAND + " \"" + tempfile.getAbsolutePath() + "\" | " + mPerforceBinary
                    + " " + getHostPortParam() + " " + getUserParam(username) + " '" + getUserPasswordParam(username)
                    + "' " + " -c" + clientName + " change -i";

            result = mExecutor.runCommand(new String[] { SHELL_COMMAND, "-c", echoCommand }, null, null);

        } catch (final IOException e) {
            throw new CommandWrapperFault("executeWithInputForm", "Could not create temporary file for perforce form",
                                          e);
        }

        if (result.getReturnValue() != CommandResult.RETURN_SUCCESS) {
            throw new CommandWrapperFault(sanitizeString(result.getCommand()), "Could not commit client: " + clientName
                    + ": " + sanitizeString(result.getStderr()));
        }

        // extract the change number, use it to submit
        final String stdout = result.getStdout().trim();
        final String pattern = "^Change ([0-9]+) created.*$";
        final Pattern p = Pattern.compile(pattern);
        final Matcher m = p.matcher(stdout);
        final boolean changeNumFound = m.matches();
        if (!changeNumFound || m.groupCount() < 1) {
            throw new CommandWrapperFault("commit", "Could not find change number in: " + stdout);
        }
        final String changeNum = m.group(1);

        final String[] commandArray = new String[] { mPerforceBinary, getHostPortParam(), getUserParam(username),
                                                    getUserPasswordParam(username), "-c", clientName, "submit", "-c",
                                                    changeNum };
        result = mExecutor.runCommand(commandArray, null, null);
        if (result.getReturnValue() != CommandResult.RETURN_SUCCESS) {
            /* revert the changelist */
            try {
                formText = getCommandOutput(new String[] { "-c", clientName, "change", "-o", changeNum }, username);
            } catch (final ScmLimitationFault scmLimitationFault) {
                throw new CommandWrapperFault("change", "Unexpected error", scmLimitationFault);
            }
            form = PerforceForm.parse(formText);
            form.setListValue("Description", descriptionList);
            form.setListValue("Files", new LinkedList());
            try {
                tempfile = File.createTempFile("sfee", "p4");
                final FileWriter fileWriter = new FileWriter(tempfile);
                fileWriter.write(form.toString());
                fileWriter.close();
            } catch (final IOException e) {
                throw new CommandWrapperFault("executeWithInputForm",
                                              "Could not create temporary file for perforce form", e);
            }

            throw new CommandWrapperFault(sanitizeString(result.getCommand()), "Could not execute submit: " + ": "
                    + sanitizeString(result.getStderr()));
        }
    }

    /**
     * Get a perforce group form
     * 
     * @param groupName
     *            The group form to get
     * @return The group form
     * @throws CommandWrapperFault
     *             If the command had a problem
     */
    public PerforceForm getGroupForm(final String groupName) throws CommandWrapperFault {
        String currentGroupForm = null;
        try {
            currentGroupForm = getCommandOutput(new String[] { "group", "-o", groupName }, null).trim();
        } catch (final ScmLimitationFault scmLimitationFault) {
            throw new CommandWrapperFault("group", "Unexpected error", scmLimitationFault);
        }
        final PerforceForm form = PerforceForm.parse(currentGroupForm);
        validateForm(form, "group", "Group");
        return form;
    }

    /**
     * Get the perforce protect form
     * 
     * @return The protect form
     * @throws CommandWrapperFault
     *             If the command had a problem
     */
    public PerforceForm getProtectForm() throws CommandWrapperFault {
        // add perforce write access for the group (and add some protection)
        String currentProtect = null;
        try {
            currentProtect = getCommandOutput(new String[] { "protect", "-o" }, null).trim();
        } catch (final ScmLimitationFault scmLimitationFault) {
            throw new CommandWrapperFault("protect", "Unexpected error", scmLimitationFault);
        }
        final PerforceForm form = PerforceForm.parse(currentProtect);
        validateForm(form, "protect", "Protections");
        return form;
    }

    /**
     * Returns a list containing all existing perforce username
     * 
     * @return list of usernames
     * @throws CommandWrapperFault
     *             if the operation fails
     */
    public String[] listUsers() throws CommandWrapperFault {
        final String[] commandArray = new String[] { mPerforceBinary, getHostPortParam(), getAdminUserParam(),
                                                    getAdminPasswordParam(), "users" };
        final CommandResult result = mExecutor.runCommand(commandArray, null, null);
        if (result.getReturnValue() != CommandResult.RETURN_SUCCESS) {
            String errmsg = result.getStderr();
            if (errmsg == null || "".equals(errmsg)) {
                errmsg = result.getStdout(); // sometimes Perforce reports to stdout instead of stderr
            }
            throw new CommandWrapperFault(sanitizeString(result.getCommand()),
                                          "Could not get list of all perforce users: " + sanitizeString(errmsg));
        } else {
            // now strip out all other junks (eg. email, fullname, access time, etc)
            final String[] users = result.getStdout().split("\n");
            for (int i = 0; i < users.length; i++) {
                final String user = users[i];
                users[i] = user.substring(0, user.indexOf(" "));
            }
            return users;
        }
    }

    /**
     * Create a super user, for testing purposes.
     * 
     * @param username
     *            the user to create as super
     * @param password
     *            the password to set for the user.
     * @throws CommandWrapperFault
     *             thrown if one or more of the underlying Operating system commands fail.
     * @throws ScmLimitationFault
     *             if this exceeds the license limitations
     */
    public void createSuperUser(final String username, final String password) throws CommandWrapperFault,
                                                                             ScmLimitationFault {
        createAdminUser(username);
        setPassword(username, password);

        // make this user super
        final PerforceForm protectForm = getProtectForm();
        final List protectionList = protectForm.getListValue("Protections");
        protectionList.add("super user " + username + " * //...");
        protectForm.setListValue("Protections", protectionList);
        executeWithInputForm("protect", protectForm.toString(), null, null);
    }
}
