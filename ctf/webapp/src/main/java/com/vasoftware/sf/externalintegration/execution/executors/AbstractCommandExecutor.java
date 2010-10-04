/*
 * $RCSfile: AbstractCommandExecutor.java,v $
 *
 * SourceForge(r) Enterprise Edition
 * Copyright 2007 CollabNet, Inc.  All rights reserved.
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
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import com.vasoftware.sf.common.configuration.GlobalOptionKeys;
import com.vasoftware.sf.common.configuration.SfGlobalOptions;
import com.vasoftware.sf.common.configuration.SfGlobalOptionsManager;
import com.vasoftware.sf.common.configuration.SfPaths;
import com.vasoftware.sf.common.logger.Logger;
import com.vasoftware.sf.common.util.FileUtil;
import com.vasoftware.sf.common.util.GuidGenerator;
import com.vasoftware.sf.common.util.StringUtil;
import com.vasoftware.sf.externalintegration.UserConstants;
import com.vasoftware.sf.externalintegration.execution.CommandExecutor;
import com.vasoftware.sf.externalintegration.execution.CommandResult;
import com.vasoftware.sf.externalintegration.execution.CommandWrapperFault;
import com.vasoftware.sf.externalintegration.execution.ExecutionUtil;
import com.vasoftware.sf.externalintegration.execution.UserAlreadyExistsFault;

/**
 * @author Richard Lee <rlee@vasoftware.com>
 * @author Jamison Gray <jgray@vasoftware.com>
 * @version $Revision: 1.36 $
 */
public abstract class AbstractCommandExecutor implements CommandExecutor {
    /**
     * the logger for this class and its subclasses.
     */
    protected static final Logger smLogger = Logger.getLogger(AbstractCommandExecutor.class);

    protected final String mSourceforgeHome;

    private boolean mRollbackEnabled;

    /**
     * constructor. Initializes member variables from options
     */
    protected AbstractCommandExecutor() {
        mSourceforgeHome = SfPaths.sourceforgeHome();
        mRollbackEnabled = true;
    }

    /**
     * returns the sourceforge home directory.
     * 
     * @return the sourceforge home directory as a string.
     */
    protected String getSourceforgeHome() {
        return mSourceforgeHome;
    }

    /**
     * Attempt to create several users if they don't already exist
     * 
     * @param usernames
     *            the usernames to create
     * @throws CommandWrapperFault
     *             thrown if there was a failure to create users
     * @return returns an array of usernames created on the system
     */
    public String[] createUsersIfMissing(final String[] usernames) throws CommandWrapperFault {
        final ArrayList<String> userList = new ArrayList<String>();
        for (int i = 0; i < usernames.length; i++) {
            final String username = usernames[i];
            try {
                // Make sure that this username is tolowered
                createUser(username.toLowerCase(), null);
                userList.add(username);
            } catch (final UserAlreadyExistsFault userAlreadyExistsFault) {
                smLogger.debug("User " + username + " already exist.");
            }
        }
        return userList.toArray(new String[userList.size()]);
    }

    /**
     * @see CommandExecutor#setUserStatuses(String[], String[])
     */
    public void setUserStatuses(final String[] usernames, final String[] statuses) throws CommandWrapperFault {

        for (int i = 0; i < usernames.length; i++) {
            final String username = usernames[i];
            final String status = statuses[i];
            final String usernameLower = username.toLowerCase();
            if (status.equals(UserConstants.REMOVED_USER_STATUS_NAME)) {
                setPassword(usernameLower, null);
                try {
                    deleteUser(usernameLower);
                } catch (final CommandWrapperFault e) {
                    // do nothing
                }
            } else if (status.equals(UserConstants.ACTIVE_USER_STATUS_NAME)) {
                activateUser(usernameLower);
            } else {
                deactivateUser(usernameLower);
            }
        }
    }

    /**
     * Reads from a stream if its available() returns more than 0.
     * 
     * @param stream
     *            the stream to read from
     * @return String - the agregation of the reads from the stream. if stream has no bytes available, returns null.
     * @throws java.io.IOException
     *             thrown if there was an I/O exception thrown from either the available() or the read().
     */
    private static String readIfAvailable(final InputStream stream) throws IOException {
        int c;
        final StringBuffer resultBuffer = new StringBuffer();
        while ((c = stream.read()) != -1) {
            resultBuffer.append((char) c);
        }
        return resultBuffer.toString();
    }

    /**
     * Takes a process and fills in return value, the stdout and stderr in the result object.
     * 
     * @param command
     *            The command that was executed
     * @param proc
     *            The Process object
     * @return The CommandResult object.
     * @throws com.vasoftware.sf.externalintegration.execution.CommandWrapperFault
     *             An error occurred while executing the command.
     */
    protected CommandResult buildCommandResult(final String command, final Process proc) throws CommandWrapperFault {

        // There are issues with using Process...see:
        // http://www.javaworld.com/javaworld/jw-12-2000/jw-1229-traps.html

        String strStdout = "", strStderr = "";

        final StreamGobbler inputGobbler = new StreamGobbler(proc.getInputStream());
        final StreamGobbler errorGobbler = new StreamGobbler(proc.getErrorStream());

        inputGobbler.start();
        errorGobbler.start();

        int exitValue = -1;
        try {
            inputGobbler.join();
            errorGobbler.join();
            exitValue = proc.waitFor();
            strStdout = inputGobbler.getResult();
            strStderr = errorGobbler.getResult();
        } catch (final InterruptedException e) {
            throw new CommandWrapperFault(command, "Unable to get command exit value", e);
        }

        if (strStdout == null || strStderr == null) {
            throw new CommandWrapperFault(command, "IO error reading from command stream.");
        }

        return new CommandResult(command, strStdout, strStderr, exitValue);
    }

    /**
     * Creates or updates hook scripts for the repository. This implementation is dependent on
     * OS-dependent elements implemented in the concrete subclasses:
     * <code>
     * getEnvironmentSetCommand()
     * getPathSeparator()
     * getFileSepartor()
     * getHookScriptFile()
     * replaceArguments()
     * </code>
     * @see com.vasoftware.sf.externalintegration.execution.CommandExecutor#createHookScript(String, com.vasoftware.sf.externalintegration.execution.CommandExecutor.HookEvent, String)
     * @param repositoryDir the repo whose hooks script to modify
     * @param hook HookEvent for which to create script
     * @param scriptContent the command(s) to run when the event is fired
     * @throws com.vasoftware.sf.externalintegration.execution.CommandWrapperFault
     */
    public void createHookScript(String repositoryDir, HookEvent hook, String scriptContent) throws CommandWrapperFault {

        final SfGlobalOptions config = SfGlobalOptionsManager.getOptions();
        final String sfPropertiesPath = SfGlobalOptionsManager.getSourceForgePropertiesPath();
        final String pythonPath = config.getOption(GlobalOptionKeys.SCM_PYTHON_PATH);
        String quoteStr = "'";

        /* In Windows batch scripts, surrounding environment variables with quotes of any kind break
         * their values in Python because the surrounding quotes become part of the variable value. */
        if (ExecutionUtil.isWindows()) {
            quoteStr = "";
        }

        StringBuilder script = new StringBuilder();
        script.append(getCommentString() + " " + BEGIN_SOURCEFORGE_SECTION);
        script.append("\n\n");

        script.append(getEnvironmentVariableString("SOURCEFORGE_PROPERTIES_PATH",
                quoteStr + FileUtil.normalizePath(sfPropertiesPath) + quoteStr));
        script.append("\n");

        if (null != pythonPath) {
            // update path string for platform, then add to script
            script.append(getEnvironmentVariableString("PYTHONPATH",
                quoteStr + FileUtil.normalizePath(pythonPath) + quoteStr));
            script.append("\n\n");
        } else {
            // For pretty scripts
            script.append("\n");
        }

        // update script command for platform
        // assume "python" is on path
        scriptContent = scriptContent.replaceAll("/", getFileSeparator());
        scriptContent = replaceArguments(scriptContent);

        script.append(scriptContent);
        script.append("\n");
        script.append(getCommentString() + " " + END_SOURCEFORGE_SECTION);
        script.append("\n");

        script = doScriptModifications(script);
        final File hookScriptFile = getHookScriptFile(repositoryDir, hook);

        // if file exists, just insert or replace and insert the triggers
        if (hookScriptFile.exists()) {
            addTriggerToFile(hookScriptFile, hookScriptFile, script.toString());
        } else {
            createFile(hookScriptFile, script.toString());
        }
   }

    protected abstract String getCommentString();

    /**
     * returns the platform-specific set-variable snippet
     * @param variableName name to use
     * @param value value to assign
     * @return the snippet to include in the shell script
     */
    protected abstract String getEnvironmentVariableString(String variableName, String value);

    protected abstract String getPathSeparator();

    protected abstract String getFileSeparator();

    protected abstract String replaceArguments(String scriptContent);

    protected abstract File getHookScriptFile(String repositoryDir, HookEvent hook);

    /**
     * optional final changes to the script before writing to disk; default
     * implementation takes no action
     * @param input StringBuilder
     * @return StringBuilder
     */
    protected StringBuilder doScriptModifications(StringBuilder input) {
        return input;
    }

    /**
     * Utility class a Thread that gobbles up an input stream and returns the result.
     */
    class StreamGobbler extends Thread {
        InputStream mStream;
        String mResult = null;

        /**
         * constructor that specifies the stream to consume.
         * 
         * @param is
         *            the input stream
         */
        StreamGobbler(final InputStream is) {
            mStream = is;
        }

        /** when executed, gobble up data from the input stream */
        @Override
        public void run() {
            try {
                mResult = readIfAvailable(mStream);
            } catch (final IOException e) {
                smLogger.error("StreamGobbler error.  Will return null. ", e);
            }
        }

        /**
         * Get the result of gobbling up the stream.
         * 
         * @return output from the stream, or null if an IO exception was encountere - look in the logs!
         */
        public String getResult() {
            return mResult;
        }
    }

    /**
     * Run a command in a particular directory with environment and return a CommandResult structure. This will log all
     * information about the command including parameters and should not be used with sensitive data.
     * 
     * @param cmd
     *            the command and its parameters
     * @param envp
     *            the command environment as a set of var=val strings
     * @param dir
     *            the directory to execute the command in
     * @return CommandResult - the result of the command.
     * @throws com.vasoftware.sf.externalintegration.execution.CommandWrapperFault
     *             An error occurred while executing the command.
     */
    public CommandResult runCommand(final String[] cmd, final String[] envp, final File dir) throws CommandWrapperFault {
        return runLoggedCommand(cmd, envp, dir, true);
    }

    /**
     * Run a command in a particular directory with environment and return a CommandResult structure.
     * 
     * @param cmd
     *            the command and its parameters
     * @param envp
     *            the command environment as a set of var=val strings
     * @param dir
     *            the directory to execute the command in
     * @param logArguments
     *            true if arguments should be logged.
     * @return CommandResult - the result of the command.
     * @throws com.vasoftware.sf.externalintegration.execution.CommandWrapperFault
     *             An error occurred while executing the command.
     */
    public CommandResult runLoggedCommand(final String[] cmd, final String[] envp, final File dir,
                                          final boolean logArguments) throws CommandWrapperFault {
        Process process = null;
        try {
            process = exec(cmd, envp, dir, logArguments);

            final String commandString = getCommandString(cmd, logArguments);
            return buildCommandResult(commandString, process);
        } finally {
            if (process != null) {
                process.destroy();
            }
        }
    }

    /**
     * Run a command in a particular directory with environment and return a Process handle. This does not wait until
     * the process has finished to return.
     * 
     * @param cmd
     *            the command and its parameters
     * @param envp
     *            the environment.
     * @param dir
     *            the directory to execute the command in
     * @return The process handle to the running process
     * @throws CommandWrapperFault
     *             An error occurred while executing the command.
     */
    public Process runCommandAsync(final String[] cmd, final String[] envp, final File dir) throws CommandWrapperFault {
        return exec(cmd, envp, dir, true);
    }

    /**
     * @see com.vasoftware.sf.externalintegration.execution.CommandExecutor#pathExists
     */
    public boolean pathExists(final File s) {
        return s.exists() && s.isDirectory();
    }

    /**
     * @see CommandExecutor#enableUserForTesting(String)
     */
    public void enableUserForTesting(final String username) throws CommandWrapperFault {
        // nothing
    }

    /**
     * @see CommandExecutor#addTriggerToFile
     */
    public void addTriggerToFile(final File srcFile, final File dstFile, final String sourceforgeTrigger)
                                                                                                         throws CommandWrapperFault {
        final StringBuffer content = new StringBuffer();
        boolean addedSourceforgeTrigger = false;
        try {
            final BufferedReader in = new BufferedReader(new FileReader(srcFile.getAbsolutePath()));
            try {
                String line;
                while ((line = in.readLine()) != null) {
                    if (line.startsWith(getCommentString() + " " + BEGIN_SOURCEFORGE_SECTION)) {
                        // Only add new sourceforge trigger if we haven't added it already
                        if (!addedSourceforgeTrigger) {
                            content.append(sourceforgeTrigger);
                            addedSourceforgeTrigger = true;
                        }

                        // Strip every line until END_SOURCEFORGE_SECTION
                        while ((line = in.readLine()) != null) {
                            // Stop if SOURCEFORGE SECTION terminater
                            if (line.startsWith(getCommentString() + " " + END_SOURCEFORGE_SECTION)) {
                                break;
                            }
                        }
                    } else {
                        content.append(line).append('\n');
                    }
                }
            } catch (final IOException e) {
                smLogger.error("Error reading file", e);
            } finally {
                try {
                    in.close();
                } catch (final Throwable t) {
                    smLogger.error("Failed to close file", t);
                }
            }
        } catch (final FileNotFoundException e) {
            smLogger.error("Failed to read file", e);
        }

        // if there is no pre-existing sourceforge section, we want to add the new sourceforge section first
        if (!addedSourceforgeTrigger) {
            content.insert(0, sourceforgeTrigger);
        }

        createFile(dstFile, content.toString());
    }

    /**
     * Get the executable from a command/argument array.
     * 
     * @param cmd
     *            The command/argument array.
     * @throws CommandWrapperFault
     *             If there was a problem with the array.
     */
    private void validateCommand(final String[] cmd) throws CommandWrapperFault {
        if (cmd == null || cmd.length < 1) {
            throw new CommandWrapperFault("", "Could not execute empty command");
        }

        final String executable = cmd[0];
        if (StringUtil.isEmpty(executable)) {
            throw new CommandWrapperFault("", "Could not execute empty command");
        }
    }

    /**
     * Get the command string that will be used for logging purposes.
     * 
     * @param cmd
     *            The command/argument array that will be executed.
     * @param includeArguments
     *            true if arguments should be included.
     * @return The command string to use for logging.
     */
    private String getCommandString(final String[] cmd, final boolean includeArguments) {
        final StringBuffer buf = new StringBuffer();
        buf.append(cmd[0]);

        // int blankEntries = 0;
        if (includeArguments) {
            for (int i = 1; i < cmd.length; i++) {
                final String param = cmd[i];
                buf.append(" '");
                buf.append(param);
                buf.append("'");
            }
        }

        return buf.toString();
    }

    /**
     * This is the method that actually executes a command.
     * 
     * @param cmd
     *            the command and its parameters
     * @param envp
     *            the environment.
     * @param dir
     *            the directory to execute the command in
     * @param logArguments
     *            true if arguments should be logged.
     * @return The process handle to the running process
     * @throws CommandWrapperFault
     *             An error occurred while executing the command.
     */
    private Process exec(final String[] cmd, final String[] envp, final File dir, final boolean logArguments)
                                                                                                             throws CommandWrapperFault {
        validateCommand(cmd);
        final String commandString = getCommandString(cmd, logArguments); // for logging

        final String cmdGuid = GuidGenerator.newGuid();
        if (smLogger.isDebugEnabled()) {
            smLogger.debug("Executing command[" + cmdGuid + "]: " + commandString);
        }

        try {
            final Runtime runtime = Runtime.getRuntime();
            return runtime.exec(cmd, envp, dir);
        } catch (final IOException e) {
            smLogger.warn("Error during execution of\n\"" + commandString + "\":\n" + e.getMessage());
            throw new CommandWrapperFault(commandString, "Error during exec", e);
        } finally {
            if (smLogger.isDebugEnabled()) {
                smLogger.debug("Finished executing command[" + cmdGuid + "].");
            }
        }
    }

    /**
     * Sets whether rollback is enabled for the executor.
     * 
     * @param rollbackEnabled Whether or not rollback is enabled
     */
    public void setRollbackEnabled(boolean rollbackEnabled) {
        this.mRollbackEnabled = rollbackEnabled;
    }

    /**
     * Returns whether rollback is enabled for the executor.
     * 
     * @return Whether or not rollback is enabled
     */
    public boolean isRollbackEnabled() {
        return mRollbackEnabled;
    }

    /**
     * adds a directory path to the rollback queue
     * 
     * @param dirPath
     *            the path to be rolled back
     */
    protected void addPathForRollback(final String dirPath) {
        if (ExecutionUtil.isWindows()) {
            appendToRollbackScript("rmdir /S /Q \"" + dirPath + "\"\n");
        } else {
            appendToRollbackScript("rm -rf \"" + dirPath + "\"\n");
        }
    }

    /**
     * adds a user to the rollback queue
     * 
     * @param username
     *            the user to be rolled back
     */
    protected void addUserForRollback(final String username) {
        if (!ExecutionUtil.isWindows()) {
            appendToRollbackScript("userdel -r \"" + username + "\"\n");
        }
    }

    /**
     * adds the group to a rollback queue
     * 
     * @param groupname
     *            the name of the group to be rolled back
     */
    protected void addGroupForRollback(final String groupname) {
        if (!ExecutionUtil.isWindows()) {
            appendToRollbackScript("groupdel \"" + groupname + "\"\n");
        }
    }

    /**
     * Appends a string to the generated rollback script
     * 
     * @param string the string to be appended to the script
     */
    protected void appendToRollbackScript(final String string) {
        synchronized (mSourceforgeHome) {
            FileWriter fileWriter = null;
            try {
                final File scriptFile = new File(mSourceforgeHome, "rollbackScmChanges." +
                                                 (ExecutionUtil.isWindows() ? "bat" : "sh"));
                if (!scriptFile.exists()) {
                    scriptFile.createNewFile();
                }
                fileWriter = new FileWriter(scriptFile, true);
                fileWriter.write(string);
            } catch (final IOException e) {
                smLogger.error("Failed to create rollback script for scm actions.", e);
            } finally {
                if (fileWriter != null) {
                    try {
                        fileWriter.close();
                    } catch (final IOException e) {
                        // ignore
                    }
                }
            }
        }
    }

    /**
     * @see com.vasoftware.sf.externalintegration.execution.CommandExecutor#createPath(java.io.File)
     */
    @Override
    public void createPath(final File path) throws CommandWrapperFault {
        if (path.exists()) {
            return;
        }

        try {
            path.mkdirs();
        } catch (SecurityException se) {
            throw new CommandWrapperFault("Could not create the path " + path , se);
        }

        if (isRollbackEnabled()) {
            addPathForRollback(path.getAbsolutePath());
        }
    }

    /**
     * @see com.vasoftware.sf.externalintegration.execution.CommandExecutor#createFile(java.io.File, java.lang.String)
     */
    @Override
    public void createFile(final File fileToCreate, final String contents) throws CommandWrapperFault {
        try {
            FileUtil.createFile(fileToCreate, contents);
        } catch (final IOException e) {
            throw new CommandWrapperFault("Error while creating file " + fileToCreate.getAbsolutePath(), e);
        }

        if (isRollbackEnabled()) {
            addPathForRollback(fileToCreate.getAbsolutePath());
        }
    }

    /**
     * @see com.vasoftware.sf.externalintegration.execution.CommandExecutor#createTempDirectory()
     */
    public File createTempDirectory() throws CommandWrapperFault {
        final SfGlobalOptions options = SfGlobalOptionsManager.getOptions();
        final String sfTempDir = options.getOption(GlobalOptionKeys.SFMAIN_TEMP_DIRECTORY);
        final String dirName = "sf-" + System.currentTimeMillis();

        File dir = null;
        for (int suffix = 0; suffix < 1000; suffix++) {
            dir = new File(sfTempDir + File.separator + dirName + '-' + suffix);
            if (!dir.exists()) {
                if (dir.mkdirs()) {
                    return dir;
                }
            }
        }

        throw new CommandWrapperFault("createTempDirectory()", "Could not create temporary directory" + dir);
    }

    /**
     * @see com.vasoftware.sf.externalintegration.execution.CommandExecutor#deletePath(java.io.File)
     */
    public void deletePath(final File path) throws CommandWrapperFault {
        if (!path.exists()) {
            return;
        }

        boolean success = false;

        if (path.isDirectory()) {
            success = FileUtil.deleteDir(path);
        } else {
            success = path.delete();
        }

        if (!success) {
            throw new CommandWrapperFault("Could not delete the path " + path.getAbsolutePath());
        }
    }
}
