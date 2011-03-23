package com.vasoftware.sf.externalintegration.adapters.cvsdaemon;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.StringTokenizer;

import com.vasoftware.sf.common.configuration.GlobalOptionKeys;
import com.vasoftware.sf.common.configuration.SfGlobalOptions;
import com.vasoftware.sf.common.configuration.SfGlobalOptionsManager;
import com.vasoftware.sf.common.configuration.SfPaths;
import com.vasoftware.sf.common.logger.Logger;
import com.vasoftware.sf.externalintegration.ScmConstants;
import com.vasoftware.sf.externalintegration.execution.CommandExecutor;
import com.vasoftware.sf.externalintegration.execution.CommandResult;
import com.vasoftware.sf.externalintegration.execution.CommandWrapperFault;

/**
 * Class which provides a clean looking wrapper around a bunch of dirty system exec calls to CVS.
 * 
 * @author Richard Lee <rlee@vasoftware.com>
 * @version $Revision: 1.28 $ $Date: 2007/01/24 23:08:06 $
 */
public class CvsWrapper {
    private static final Logger smLogger = Logger.getLogger(CvsWrapper.class);

    public enum CvsType {
        WANDISCO, SSH, PSERVER
    }

    public static final String MODULE_CVSROOT = "CVSROOT";

    private static final String CVS_INIT = "init";
    private static final String CVS_CHECKOUT = "checkout";
    private static final String CVS_IMPORT = "import";
    private static final String CVS_UPDATE = "update";
    private static final String CVS_ADD = "add";
    private static final String CVS_DELETE = "rm";
    private static final String CVS_STATUS = "status";
    private static final String CVS_COMMIT = "commit";

    public static final String SFEE_SH = "sfee.sh";

    private final CommandExecutor mExecutor;
    private String mCvsUser;
    private String mCvsBinary;
    private final CvsType mCvsType;

    /**
     * Default constructor for CvsWrapper.
     * 
     * @param executor
     *            the executor class for executing external commands
     * @param cvsType
     *            type of CVS implementation
     */
    public CvsWrapper(final CommandExecutor executor, final CvsType cvsType) {
        final SfGlobalOptions options = SfGlobalOptionsManager.getOptions();
        mExecutor = executor;
        mCvsType = cvsType;

        if (cvsType == CvsType.PSERVER) {
            mCvsUser = null; // run as current user
        } else {
            mCvsUser = ScmConstants.CVS_USER;
        }

        if (cvsType == CvsType.WANDISCO) {
            mCvsBinary = options.getOption(GlobalOptionKeys.SFMAIN_INTEGRATION_EXECUTABLES_CVSRELAY);
        } else {
            mCvsBinary = options.getOption(GlobalOptionKeys.SFMAIN_INTEGRATION_EXECUTABLES_CVS);
        }
    }

    /**
     * Set the cvs user to the specified user.
     * 
     * @param username
     *            The username to run commands as.
     */
    public void setCvsUser(final String username) {
        mCvsUser = username;
    }

    /**
     * Get the user this wrapper is operating under
     * 
     * @return The username
     */
    public String getCvsUser() {
        return mCvsUser;
    }

    /**
     * Gets the command that this wrapper executes.
     * 
     * @return String - the command executed as a String.
     */
    public String getCommand() {
        return mCvsBinary;
    }

    /**
     * Run a CVS init comand
     * 
     * @param path
     *            the path to create the repository on.
     * @throws CommandWrapperFault
     *             An error occurred while executing the command.
     */
    public void doInit(final File path) throws CommandWrapperFault {
        smLogger.info("CVS init for " + path);

        try {
            final String cmdString[] = { mCvsBinary, "-d", path.getPath(), CVS_INIT };
            final CommandResult result = mExecutor.runCommandAs(mCvsUser, cmdString, null, false);

            if (result.getReturnValue() != CommandResult.RETURN_SUCCESS) {
                throw new CommandWrapperFault(result.getCommand(), result.getCommandOutput());
            }
        } catch (final CommandWrapperFault e) {
            smLogger.error("CVS error: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Run a CVS Checkout command
     * 
     * @param cvsroot
     *            the CVSROOT to use
     * @param module
     *            the CVS module to check out
     * @param destination
     *            the location to check the module out into.
     * @throws CommandWrapperFault
     *             An error occurred while executing the command.
     */
    public void doCheckout(final String cvsroot, String module, final File destination) throws CommandWrapperFault {
        smLogger.info("CVS checkout for " + cvsroot + "/" + module + " => " + destination);
        if (module.startsWith("/")) {
            module = module.substring(1);
        }
        try {
            final String[] cmd = { mCvsBinary, "-d", cvsroot, CVS_CHECKOUT, module };
            final CommandResult result = mExecutor.runCommandAs(mCvsUser, cmd, destination, false);
            if (result.getReturnValue() != CommandResult.RETURN_SUCCESS) {
                throw new CommandWrapperFault(result.getCommand(), "Could not checkout from cvsroot " + cvsroot + " "
                                              + module + " to destination " + destination.getAbsolutePath() + ": "
                                              + result.getCommandOutput());
            }
        } catch (final CommandWrapperFault e) {
            smLogger.error("CVS error: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Run a CVS Checkin command.
     * 
     * @param cvsroot
     *            the CVSROOT to use for the checkin
     * @param directory
     *            the directory to perform the checkin on.
     * @param message
     *            the commit message of this commit.
     * @throws CommandWrapperFault
     *             An error occurred while executing the command.
     */
    public void doCheckin(final String cvsroot, final File directory, final String message) throws CommandWrapperFault {
        smLogger.info("CVS checkin for " + cvsroot + " <= " + directory);
        final String[] cmd = { mCvsBinary, "-d", cvsroot, CVS_COMMIT, "-m", message };
        final CommandResult result = mExecutor.runCommandAs(mCvsUser, cmd, directory, false);

        if (result.getReturnValue() != CommandResult.RETURN_SUCCESS) {
            throw new CommandWrapperFault(result.getCommand(), "Could not check in to cvsroot " + cvsroot
                                          + " from directory " + directory.getAbsolutePath() + ": " + result.getCommandOutput());
        }
    }

    /**
     * Run a forced CVS Checkin command - this will bump the revision number, even if the files have not been changed.
     * Useful if we want to force update of administrative files. Note that this works on single named files, rather
     * than a directory (see CVS docs for the "-f" option to commit)
     * 
     * @param cvsroot
     *            the CVSROOT to use for the checkin
     * @param directory
     *            the directory from which to checkin from
     * @param file
     *            an array of files to check in
     * @param message
     *            the commit message of this commit.
     * @throws CommandWrapperFault
     *             An error occurred while executing the command.
     */
    public void doForcedCheckin(final String cvsroot, final File directory, final File[] file, final String message)
    throws CommandWrapperFault {
        if (smLogger.isInfoEnabled()) {
            String files = "";
            for (final File aFile : file) {
                if (files.length() > 0) {
                    files += ", ";
                }
                files += aFile;
            }
            smLogger.info("CVS forced checkin for " + cvsroot + " <= " + directory + "{" + files + "}");
        }

        final String[] cmd = { mCvsBinary, "-d", cvsroot, CVS_COMMIT, "-f", "-m", message };
        final String dirAbsolutePath = directory.getAbsolutePath();

        // build up a complete command by taking the initial command, and appending all the
        // specified files. Convert the resultant list into an array again.
        final List<String> argList = new ArrayList<String>(Arrays.asList(cmd));
        for (final File thisFile : file) {
            if (!thisFile.isFile()) {
                throw new CommandWrapperFault("doForcedCheckin()", "'" + thisFile.getAbsolutePath()
                                              + "' does not appear to be a file");
            }
            final String absolutePath = thisFile.getAbsolutePath();
            if (!absolutePath.startsWith(dirAbsolutePath)) {
                throw new CommandWrapperFault("doForcedCheckin()",
                                              "forced checkin files must be relative to directory: " + absolutePath
                                              + " is not under " + dirAbsolutePath);
            }
            argList.add(absolutePath.substring(dirAbsolutePath.length() + 1));
        }

        final String[] realArgs = argList.toArray(new String[argList.size()]);
        final CommandResult result = mExecutor.runCommandAs(mCvsUser, realArgs, directory, false);

        if (result.getReturnValue() != CommandResult.RETURN_SUCCESS) {
            throw new CommandWrapperFault(result.getCommand(), "Could not check in to cvsroot " + cvsroot
                                          + result.getCommandOutput());
        }
    }

    /**
     * Run a CVS Update command.
     * 
     * @param cvsroot
     *            the CVSROOT to use for the update.
     * @param directory
     *            directory into which the update should be performed
     * @throws CommandWrapperFault
     *             An error occurred while executing the command.
     */
    public void doUpdate(final String cvsroot, final File directory) throws CommandWrapperFault {
        smLogger.info("CVS update for " + cvsroot + " => " + directory);
        final String[] cmdString = { mCvsBinary, "-d", cvsroot, "-q", CVS_UPDATE };
        final CommandResult result = mExecutor.runCommandAs(mCvsUser, cmdString, directory, false);
        if (result.getReturnValue() != CommandResult.RETURN_SUCCESS) {
            throw new CommandWrapperFault(result.getCommand(), "Could not update in to cvsroot " + cvsroot
                                          + " from directory " + directory.getAbsolutePath() + ": " + result.getCommandOutput());
        }
    }

    /**
     * Run a cvs -version command for verification of the cvs binary
     * 
     * @return The result of the cvs version command
     * @throws CommandWrapperFault
     *             if there was a problem executing
     */
    public CommandResult doVersion() throws CommandWrapperFault {
        final CommandResult result = mExecutor.runCommand(new String[] { mCvsBinary, "-version" }, null, null);
        if (result.getReturnValue() != CommandResult.RETURN_SUCCESS) {
            smLogger.error("Could not get CVS version: " + result.getCommandOutput());
            throw new CommandWrapperFault(result.getCommand(), "Could not get CVS version: "
                                          + result.getCommandOutput());
        }
        smLogger.info("CVS version: " + result.getCommandOutput());
        return result;
    }

    /**
     * Get the version of CVS by running the cvs -version and parsing its result
     * 
     * @return an array of integers where the elements are [0]: release, [1]: major, [2]: minor
     * @throws CommandWrapperFault
     *             thrown if we couldn't determine the version of Cvs.
     */
    public int[] getCvsVersion() throws CommandWrapperFault {
        int[] result;

        final CommandResult cmdResult = doVersion();

        final String output = cmdResult.getStdout();
        final StringTokenizer tokenizer = new StringTokenizer(output, " ");

        boolean versionFound = false;
        String versionToken = null;
        while (tokenizer.hasMoreTokens() && !versionFound) {
            final String token = tokenizer.nextToken();
            if (token.equals("(CVS)")) {
                versionToken = tokenizer.nextToken();
                versionFound = true;
            }
        }

        if (versionFound) {
            final int firstDot = versionToken.indexOf(".");
            final int secondDot = versionToken.indexOf(".", firstDot + 1);

            String releaseString;
            String majorString = "0";
            String minorString = "0";
            // must account for cases where the CVS version does not have all three parts, e.g. 1.2
            if (firstDot > 0) {
                releaseString = versionToken.substring(0, firstDot);
                if (secondDot > 0) {
                    // case: release.major.minor
                    majorString = versionToken.substring(firstDot + 1, secondDot);
                    minorString = versionToken.substring(secondDot + 1);
                } else {
                    // case: release.major
                    majorString = versionToken.substring(firstDot + 1);
                }
            } else {
                // case: release
                releaseString = versionToken;
            }

            result = new int[3];

            result[0] = Integer.parseInt(releaseString);
            result[1] = Integer.parseInt(majorString);
            try {
                result[2] = Integer.parseInt(minorString);
            } catch (final NumberFormatException e) {
                // Sometimes the minor number has stuff like 1p1... in this case, let's just set
                // it to 0 for now, since we don't really use minor releases
                result[2] = 0;
            }

        } else {
            throw new CommandWrapperFault(mCvsBinary + " -version", "Could not determine cvs version: "
                                          + cmdResult.getStdout());
        }
        return result;
    }

    /**
     * Import a directory into a cvs repository.
     * 
     * @param cvsroot
     *            The cvs root directory
     * @param directory
     *            The working directory
     * @param moduleName
     *            The name of the module to import to.
     * @throws CommandWrapperFault
     *             If there was a problem importing the directory.
     */
    public void doImport(final String cvsroot, final File directory, final String moduleName)
    throws CommandWrapperFault {
        smLogger.info("CVS import for " + directory.getPath() + " - Module: " + moduleName);
        final String[] cmdString = { mCvsBinary, "-d", cvsroot, CVS_IMPORT, "-m", "Initial import", moduleName,
                                     "vendor", "start" };
        final CommandResult result = mExecutor.runCommandAs(mCvsUser, cmdString, directory, false);
        if (result.getReturnValue() != CommandResult.RETURN_SUCCESS) {
            throw new CommandWrapperFault(result.getCommand(), "Could not import in to cvsroot " + cvsroot
                                          + " from directory " + directory.getAbsolutePath() + ": " + result.getCommandOutput());
        }
    }

    /**
     * Add a file to a cvs repository.
     * 
     * @param fileToAdd
     *            The file to add.
     * @throws CommandWrapperFault
     *             If there was a problem adding the file.
     */
    public void doAddFile(final File fileToAdd) throws CommandWrapperFault {
        smLogger.info("CVS add for " + fileToAdd.getAbsolutePath());
        final String[] cmdString = { mCvsBinary, CVS_ADD, fileToAdd.getName() };
        final CommandResult result = mExecutor.runCommandAs(mCvsUser, cmdString, fileToAdd.getParentFile(), false);
        if (result.getReturnValue() != CommandResult.RETURN_SUCCESS) {
            throw new CommandWrapperFault(result.getCommand(), "Could not add file " + fileToAdd.getAbsolutePath()
                                          + ": " + result.getCommandOutput());
        }
    }

    /**
     * Add a file to a cvs repository; do not report if add fails.
     * 
     * @param fileToAdd
     *            The file to add.
     * @throws CommandWrapperFault
     *             If there was a problem adding the file.
     */
    public void doAddFileSilent(final File fileToAdd) throws CommandWrapperFault {
        smLogger.info("CVS add for " + fileToAdd.getAbsolutePath());
        final String[] cmdString = { mCvsBinary, CVS_ADD, fileToAdd.getName() };
        mExecutor.runCommandAs(mCvsUser, cmdString, fileToAdd.getParentFile(), false);
    }

    /**
     * Delete a file from a cvs repository.
     * 
     * @param fileToDelete
     *            The file to delete.
     * @throws CommandWrapperFault
     *             If there was a problem deleting the file.
     */
    public void doDeleteFile(final File fileToDelete) throws CommandWrapperFault {
        smLogger.info("CVS delete for " + fileToDelete.getAbsolutePath());
        final String[] cmdString = { mCvsBinary, CVS_DELETE, "-f", fileToDelete.getName() };
        final CommandResult result = mExecutor.runCommandAs(mCvsUser, cmdString, fileToDelete.getParentFile(), false);
        if (result.getReturnValue() != CommandResult.RETURN_SUCCESS) {
            throw new CommandWrapperFault(result.getCommand(), "Could not delete file "
                                          + fileToDelete.getAbsolutePath() + ": " + result.getCommandOutput());
        }
    }

    /**
     * Do a cvs status check on a specific file.
     * 
     * @param file
     *            The file that is being checked for status.
     * @return The output of the cvs status call.
     * @throws CommandWrapperFault
     *             If there was a problem getting the file status.
     */
    public String doStatusCheck(final File file) throws CommandWrapperFault {
        smLogger.info("CVS status for " + file.getAbsolutePath());
        final String[] cmdString = { mCvsBinary, CVS_STATUS, file.getName() };
        final CommandResult result = mExecutor.runCommandAs(mCvsUser, cmdString, file.getParentFile(), false);
        if (result.getReturnValue() != CommandResult.RETURN_SUCCESS) {
            throw new CommandWrapperFault(result.getCommand(), "Could not get status for file "
                                          + file.getAbsolutePath() + ": " + result.getCommandOutput());
        }

        return result.getCommandOutput();
    }

    /**
     * Set up the CVS triggers on a repository. Add a verifymsg and loginfo trigger.
     * 
     * @param systemId
     *            The Guid of the system
     * @param repositoryPath
     *            The absolute path of the repository
     * @throws CommandWrapperFault
     *             AN error while executing the commands.
     */
    public void setupTriggers(final String systemId, final File repositoryPath) throws CommandWrapperFault {
        // These are the actual files in /cvsrootFile/<repository>/CVSROOT. Use them as source to merge in our triggers.
        // Although technically we should use the checkout version as the source for adding our triggers,
        // the reality is that many people directly modify these files instead of checking them in.
        // This works even if the user adds the trigger the proper way because cvs immediately checks them out here.

        final File cvsrootFile = new File(repositoryPath, MODULE_CVSROOT);

        String cmdPrefix = "";

        if (mCvsType == CvsType.WANDISCO) {
            cmdPrefix = cvsrootFile + File.separator + SFEE_SH + " ";
        } else if (mCvsType == CvsType.SSH) {
            // Prevent checkin during this update by taking away group permissions but
            // give 'nobody' permission to checkout
            mExecutor.setToOnlyReadWriteExecuteUserPermissions(repositoryPath, false);

            // Fix all permissions on all files within CVSROOT

            // It turns out that everything in CVSROOT can have read only access except for
            // val-tags, history, and Emptydir. Emptydir is a directory, so we can special
            // case it as a directory. The other two, we recognize as names.
            final File[] contents = cvsrootFile.listFiles();
            for (final File content : contents) {
                if (content.isDirectory()) {
                    mExecutor.setToOnlyReadWriteExecuteGroupUserPermissions(content, false);
                } else {
                    final String fileName = content.getName();
                    if (fileName.equals("val-tags") || fileName.equals("history")) {
                        mExecutor.setToOnlyReadWriteGroupUserPermissions(content, false);
                    } else {
                        mExecutor.setToOnlyReadGroupUserPermissions(content, false);
                    }
                }
            }
        }

        final File commitinfoInRepository = new File(cvsrootFile, "commitinfo");
        final File loginfoInRepository = new File(cvsrootFile, "loginfo");
        final File verifymsgInRepository = new File(cvsrootFile, "verifymsg");
        final String temporaryTriggerComment = "#Temporarily removed by SourceForge\n";

        // Add trigger information to verifymsg.
        final File tempdir = mExecutor.createTempDirectory();
        mExecutor.setUserOnPath(mCvsUser, tempdir);
        mExecutor.setToOnlyReadWriteExecuteUserPermissions(tempdir, true);

        doCheckout(repositoryPath.getAbsolutePath(), MODULE_CVSROOT, tempdir);

        final File checkedOutCvsRoot = new File(tempdir, MODULE_CVSROOT);
        final String sfIntegrationsRoot = SfPaths.getIntegrationScriptsRootPath();
        final String sfPrefix = "# " + CommandExecutor.BEGIN_SOURCEFORGE_SECTION + " - Do not remove these lines\n";
        final String sfSuffix = "# " + CommandExecutor.END_SOURCEFORGE_SECTION + "\n";
        final String isWandiscoString = Boolean.toString(mCvsType == CvsType.WANDISCO);
        final String sfPropertiesPath = SfGlobalOptionsManager.getSourceForgePropertiesPath();
        final String pythonPath = SfGlobalOptionsManager.getOptions()
            .getOption(GlobalOptionKeys.SCM_PYTHON_PATH);

        final StringBuilder verifyMsgContent = new StringBuilder();
        final File verifymsgCheckedOut = new File(checkedOutCvsRoot, "verifymsg");

        // Create CVSROOT/verifymsg that calls SOURCEFORGE_HOME/integration/VerifyMsg.py
        verifyMsgContent.append(sfPrefix)
            .append("export SOURCEFORGE_PROPERTIES_PATH='")
            .append(sfPropertiesPath)
            .append("'\n");
        if (null != pythonPath) {
            verifyMsgContent.append("export PYTHONPATH='")
                .append(pythonPath)
                .append("'\n");
        }
        verifyMsgContent.append(".*\t")
	    .append(cmdPrefix)
            .append("python2 ")
            .append(sfIntegrationsRoot)
            .append("/VerifyMsg.py ")
            .append(isWandiscoString)
            .append(" ")
            .append(systemId)
            .append(" %l\n")
            .append(sfSuffix);

        mExecutor.addTriggerToFile(verifymsgInRepository, verifymsgCheckedOut, verifyMsgContent.toString());

        final StringBuilder logInfoContent = new StringBuilder();
        final File loginfoCheckedOut = new File(checkedOutCvsRoot, "loginfo");

        // Create CVSROOT/loginfo that calls SOURCEFORGE_HOME/integration/LogInfo.py
        logInfoContent.append(sfPrefix)
            .append("export SOURCEFORGE_PROPERTIES_PATH='")
            .append(sfPropertiesPath)
            .append("'\n");
        if (null != pythonPath) {
            logInfoContent.append("export PYTHONPATH='")
                .append(pythonPath)
                .append("'\n");
        }
        logInfoContent.append("ALL\t")
            .append(cmdPrefix)
            .append("python2 ")
            .append(sfIntegrationsRoot)
            .append("/LogInfo.py ")
            .append(isWandiscoString)
            .append(" ")
            .append(systemId)
            .append(" %{sVv}\n")
            .append(sfSuffix);

        mExecutor.addTriggerToFile(loginfoInRepository, loginfoCheckedOut, logInfoContent.toString());

        final StringBuilder commitInfoContent = new StringBuilder();
        final File commitInfoCheckedOut = new File(checkedOutCvsRoot, "commitinfo");

        // Create CVSROOT/commitinfo that calls SOURCEFORGE_HOME/integration/CommitInfo.py
        commitInfoContent.append(sfPrefix)
            .append("export SOURCEFORGE_PROPERTIES_PATH='")
            .append(sfPropertiesPath)
            .append("'\n");
        if (null != pythonPath) {
            commitInfoContent.append("export PYTHONPATH='")
                .append(pythonPath)
                .append("'\n");
        }
        commitInfoContent.append("ALL\t")
            .append(cmdPrefix)
            .append("python2 ")
            .append(sfIntegrationsRoot)
            .append("/CommitInfo.py ")
            .append(isWandiscoString)
            .append(" ")
            .append(systemId)
            .append(" %r/%p\n")
            .append(sfSuffix);

        mExecutor.addTriggerToFile(commitinfoInRepository, commitInfoCheckedOut, commitInfoContent.toString());

        // Install sfee.sh script that ensures that trigger scripts only executed on primary node
        if (mCvsType == CvsType.WANDISCO) {
            final StringBuilder sfeeShContent = new StringBuilder();
            final File sfeeSh = new File(checkedOutCvsRoot, SFEE_SH);

            sfeeShContent.append("#!/bin/bash\nif [ -f \"")
                .append(sfPropertiesPath)
                .append("\" ]; then\n  $*\nfi\ntrue");

            mExecutor.createFile(sfeeSh, sfeeShContent.toString());
            mExecutor.setUserOnPath(mCvsUser, sfeeSh);
            mExecutor.setToOnlyReadWriteExecuteGroupUserPermissions(sfeeSh, false);
            doAddFileSilent(sfeeSh);

            final File checkoutList = new File(checkedOutCvsRoot, "checkoutlist");
            final String checkoutListContent = sfPrefix + SFEE_SH + "\n" + sfSuffix;

            mExecutor.addTriggerToFile(commitinfoInRepository, checkoutList, checkoutListContent);
        }

        // Clear the existing files so that triggers don't fire
        mExecutor.setToOnlyReadWriteUserPermissions(commitinfoInRepository, false);
        mExecutor.setToOnlyReadWriteUserPermissions(loginfoInRepository, false);
        mExecutor.setToOnlyReadWriteUserPermissions(verifymsgInRepository, false);

        mExecutor.createFile(commitinfoInRepository, temporaryTriggerComment);
        mExecutor.createFile(loginfoInRepository, temporaryTriggerComment);
        mExecutor.createFile(verifymsgInRepository, temporaryTriggerComment);

        // Checkin our new merged trigger files
        final String commitMessage = "SF: Adding verifyMessage and logInfo triggers " + repositoryPath;
        final File[] triggerFiles = new File[] { commitInfoCheckedOut, loginfoCheckedOut, verifymsgCheckedOut };
        doForcedCheckin(repositoryPath.getAbsolutePath(), tempdir, triggerFiles, commitMessage);

        if (mCvsType == CvsType.SSH) {
            // Unlock the cvs server for more commits
            mExecutor.setToOnlyReadWriteExecuteGroupUserPermissions(repositoryPath, false);
        }
    }
}
