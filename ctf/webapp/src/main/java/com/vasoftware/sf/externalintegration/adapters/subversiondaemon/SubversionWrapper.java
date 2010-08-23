package com.vasoftware.sf.externalintegration.adapters.subversiondaemon;

import java.io.File;
import java.util.StringTokenizer;

import com.vasoftware.sf.common.configuration.GlobalOptionKeys;
import com.vasoftware.sf.common.configuration.SfGlobalOptions;
import com.vasoftware.sf.common.configuration.SfGlobalOptionsManager;
import com.vasoftware.sf.common.configuration.SfPaths;
import com.vasoftware.sf.externalintegration.ScmConstants;
import com.vasoftware.sf.externalintegration.execution.CommandExecutor;
import com.vasoftware.sf.externalintegration.execution.CommandResult;
import com.vasoftware.sf.externalintegration.execution.CommandWrapperFault;

/**
 * Class which provides a clean looking wrapper around a bunch of dirty system exec calls to Subversion.
 * 
 * @author Wei Hsu <whsu@vasoftware.com>
 * @author Jamison Gray <jgray@vasoftware.com>
 * @version $Revision: 1.17 $ $Date: 2007/04/18 21:03:10 $
 */
public class SubversionWrapper {

    // private static final Logger smLogger = Logger.getLogger(SubversionWrapper.class);

    private final String mSubversionBinary;
    private final String mSubversionLookBinary;

    private final CommandExecutor mExecutor;
    private final String mSubversionAdminBinary;
    private final String mSubversionFsType;
    private static final String CHECKOUT = "checkout";
    private final boolean mIsWandisco;
    private static final String ADD = "add";
    private static final String DELETE = "rm";
    private static final String STATUS = "status";
    private static final String COMMIT = "ci";
    private static final String COPY = "copy";
    private static final String MOVE = "move";
    private static final String PROPGET = "propget";
    private static final String PROPSET = "propset";
    private static final String PROPDEL = "propdel";
    private static final String REVERT = "revert";
    private static final String UPDATE = "update";

    /**
     * Default constructor.
     * 
     * @param executor
     *            the executor class for executing external commands
     * @param isWandisco
     *            is this WANdisco subversion
     */
    public SubversionWrapper(final CommandExecutor executor, final boolean isWandisco) {
        final SfGlobalOptions options = SfGlobalOptionsManager.getOptions();
        mSubversionBinary = options.getOption(GlobalOptionKeys.SFMAIN_INTEGRATION_EXECUTABLES_SUBVERSION);
        mSubversionAdminBinary = options.getOption(GlobalOptionKeys.SFMAIN_INTEGRATION_EXECUTABLES_SUBVERSION_ADMIN);
        mSubversionFsType = options.getOption(GlobalOptionKeys.SFMAIN_INTEGRATION_SUBVERSION_FS_TYPE);
        mSubversionLookBinary = options.getOption(GlobalOptionKeys.SFMAIN_INTEGRATION_EXECUTABLES_SUBVERSION_LOOK);
        mExecutor = executor;
        mIsWandisco = isWandisco;
    }

    /**
     * Gets the command that this wrapper executes.
     * 
     * @return String - the command executed as a String.
     */
    public String getCommand() {
        return mSubversionBinary;
    }

    /**
     * Run a subversion -version command for verification of the release version
     * 
     * @return The result of the version command
     * @throws CommandWrapperFault
     *             if there was a problem executing
     */
    public CommandResult doVersion() throws CommandWrapperFault {
        final CommandResult result = mExecutor.runCommand(new String[] { mSubversionBinary, "--version" }, null, null);
        if (result.getReturnValue() != CommandResult.RETURN_SUCCESS) {
            throw new CommandWrapperFault(result.getCommand(), "Could not get version: " + result.getCommandOutput());
        }
        return result;
    }

    /**
     * Get the version of Subversion by running the binary with the -version flag and parsing its result
     * 
     * @return an array of integers where each element is the . part of the version number
     * @throws CommandWrapperFault
     *             thrown if we couldn't determine the version of Subversion.
     */
    public int[] getVersion() throws CommandWrapperFault {
        final CommandResult result = doVersion();
        final String output = result.getStdout();

        StringTokenizer tokenizer = new StringTokenizer(output, " ");

        boolean versionFound = false;
        String versionToken = null;
        while (tokenizer.hasMoreTokens() && !versionFound) {
            final String token = tokenizer.nextToken();
            // to work arround German version output, ignoring case
            if (token.equalsIgnoreCase("version")) {
                versionToken = tokenizer.nextToken();
                versionFound = true;
            }
        }

        if (versionFound) {
            tokenizer = new StringTokenizer(versionToken, ".");
            final int[] version = new int[tokenizer.countTokens()];
            int i = 0;
            while (tokenizer.hasMoreElements()) {
                version[i++] = Integer.parseInt(tokenizer.nextToken());
            }
            return version;
        }

        throw new CommandWrapperFault(mSubversionBinary + " --version", "Could not determine subversion version: "
                                      + output);
    }

    /**
     * Create and initialize a subversion repository
     * 
     * @param repositoryDir
     *            The repository dir to initialize
     * @throws CommandWrapperFault
     *             If the repository could not be created
     */
    public void createRepository(final File repositoryDir) throws CommandWrapperFault {
        final String subversionFsType = (mSubversionFsType != null ? mSubversionFsType : "fsfs");
        final CommandResult result = mExecutor.runCommand(new String[] { mSubversionAdminBinary, "create",
                                                                         "--fs-type=" + subversionFsType,
                                                                         "--pre-1.6-compatible", // This is a workaround
                                                                         // for
                                                                         // a Subversion security
                                                                         // issue and can be removed
                                                                         // once said issue is fixed.
                                                                         // (artf49047)
                                                                         repositoryDir.getAbsolutePath() }, null, null);

        if (result.getReturnValue() != CommandResult.RETURN_SUCCESS) {
            throw new CommandWrapperFault(result.getCommand(), "Could not create repository at "
                                          + repositoryDir.getAbsolutePath() + ": " + result.getCommandOutput());
        }
    }

    /**
     * Verify that a path exists in a subversion repository by calling svnlook /repository /path/in/repository
     * 
     * @param repositoryPath
     *            The path to the repository root on the filesystem
     * @param repositoryPathFromRoot
     *            The relative path from the repository root to the directory that is to be verified
     * @return True or false, if the directory exists in the repository
     * @throws CommandWrapperFault
     *             If the command could not be executed
     */
    public boolean verifyPath(final String repositoryPath, final String repositoryPathFromRoot)
    throws CommandWrapperFault {
        final CommandResult result = mExecutor.runCommandAs(ScmConstants.HTTPD_USER,
                                                            new String[] { mSubversionLookBinary, "tree",
                                                                           repositoryPath, repositoryPathFromRoot },
                                                                           null, false);
        return result.getReturnValue() == CommandResult.RETURN_SUCCESS;
    }

    /**
     * Checkout a subversion repository (or below) on a subversion system. This will update the checked out directory if
     * one exists.
     * 
     * @param repositoryRoot
     *            The base repository directory
     * @param repositoryPathFromRoot
     *            The path to the subdirectory to checkout
     * @param destinationDirectory
     *            The destination directory to check things out into
     * @throws CommandWrapperFault
     *             if something goes wrong
     */
    public void checkoutRepository(final File repositoryRoot, final String repositoryPathFromRoot,
                                   final File destinationDirectory) throws CommandWrapperFault {
        mExecutor.setUserOnPath(ScmConstants.HTTPD_USER, destinationDirectory);

        final File checkoutFile = new File(repositoryRoot, repositoryPathFromRoot);
        final CommandResult result = mExecutor
        .runCommandAs(
                      ScmConstants.HTTPD_USER,
                      new String[] { mSubversionBinary, CHECKOUT,
                                     "file:///" + checkoutFile.getAbsolutePath() },
                                     destinationDirectory, false);
        if (result.getReturnValue() != CommandResult.RETURN_SUCCESS) {
            throw new CommandWrapperFault(result.getCommand(), "Could not get version: " + result.getCommandOutput());
        }
    }

    /**
     * Checkout a subversion repository (or below) on a subversion system. This will update the checked out directory if
     * one exists.
     * 
     * @param repositoryUrl
     *            The base repository url
     * @param username
     *            the username
     * @param password
     *            the password
     * @param destinationDirectory
     *            The destination directory to check things out into
     * @throws CommandWrapperFault
     *             if something goes wrong
     */
    public void checkoutRepository(final String repositoryUrl, final String username, final String password,
                                   final File destinationDirectory) throws CommandWrapperFault {
        mExecutor.setUserOnPath(ScmConstants.HTTPD_USER, destinationDirectory);

        final CommandResult result = mExecutor.runCommandAs(ScmConstants.HTTPD_USER,
                                                            new String[] { mSubversionBinary, CHECKOUT,
                                                                           "--non-interactive", "--username", username,
                                                                           "--password", password, repositoryUrl,
                                                                           destinationDirectory.getPath() },
                                                                           destinationDirectory, false);
        if (result.getReturnValue() != CommandResult.RETURN_SUCCESS) {
            throw new CommandWrapperFault(result.getCommand(), "Could not get version: " + result.getCommandOutput());
        }
    }

    /**
     * Set up the triggers for a Subversion repository
     * 
     * @param systemId
     *            external system id
     * @param repositoryDir
     *            the repository into which to insert triggers
     * @throws CommandWrapperFault
     *             if a command throws an error
     */
    public void setupTriggers(final String systemId, final String repositoryDir) throws CommandWrapperFault {
        final SfGlobalOptions config = SfGlobalOptionsManager.getOptions();
        final String brandingRepo = config.getOption(GlobalOptionKeys.SCM_BRANDING_REPO);
        final String pythonExecutable = config.getOption(GlobalOptionKeys.SFMAIN_INTEGRATION_EXECUTABLES_PYTHON);
        final String sfIntegrationsRoot = SfPaths.getIntegrationScriptsRootPath();
        final String sfPropertiesPath = SfGlobalOptionsManager.getSourceForgePropertiesPath();
        final StringBuilder postCommitContent = new StringBuilder();
        final StringBuilder preCommitContent = new StringBuilder();
        final StringBuilder preRevpropChangeContent = new StringBuilder();

        String sfCommandPrefix = "";
        String sfCommandSuffix = "";

        if (mIsWandisco) {
            // Guard against trigger execution if hooks copied to non-SFEE WANdisco node
            sfCommandPrefix = new StringBuilder()
            .append("if [ -f \"")
            .append(sfPropertiesPath)
            .append("\" ]; then\n").toString();

            sfCommandSuffix = "fi\ntrue\n";
        }

        // Create REPO_HOME/hooks/post-commit that calls SOURCEFORGE_HOME/integration/post-commit.py
        postCommitContent.append(sfCommandPrefix)
        .append(pythonExecutable)
        .append(" \"")        
        .append(sfIntegrationsRoot)
        .append("/post-commit.py\" \"$1\" \"$2\" ")
        .append(systemId)
        .append("\n")
        .append(sfCommandSuffix);

        if (repositoryDir.startsWith(brandingRepo)) {
            postCommitContent.append(sfCommandPrefix)
            .append(pythonExecutable)
            .append(" \"")  
            .append(sfIntegrationsRoot)
            .append("/data-checkout.py\" \"$1\" \"$2\" ")
            .append(systemId)
            .append("\n")
            .append(sfCommandSuffix);
        } 

        mExecutor.createHookScript(repositoryDir, CommandExecutor.HookEvent.POST_COMMIT, postCommitContent.toString());

        if (mIsWandisco) {
            // For WANDisco, remove the pre-commit trigger if it exists
            final File preCommitFile = new File(new File(repositoryDir, "hooks"), "pre-commit");
            if (preCommitFile.exists()) {
                mExecutor.addTriggerToFile(preCommitFile, preCommitFile, "");
            }
        } else {
            
            // Create REPO_HOME/hooks/pre-commit that calls SOURCEFORGE_HOME/integration/pre-commit.py
            preCommitContent.append(pythonExecutable)
            .append(" \"")                      
            .append(sfIntegrationsRoot)
            .append("/pre-commit.py\" \"$1\" \"$2\" ")
            .append(systemId)
            .append("\n");

            mExecutor.createHookScript(repositoryDir, CommandExecutor.HookEvent.PRE_COMMIT, preCommitContent.toString());
        }

   
        // Create REPO_HOME/hooks/pre-revprop-change that calls SOURCEFORGE_HOME/integration/pre-revprop-change.py
        preRevpropChangeContent.append(pythonExecutable)
        .append(" \"")     
        .append(sfIntegrationsRoot)
        .append("/pre-revprop-change.py\" ")
        .append("\"$1\" ")
        .append("\"$2\" ")
        .append("\"$3\" ")
        .append("\"$4\" ")
        .append("\"$5\" ")
        .append(systemId)
        .append("\n");

        mExecutor.createHookScript(repositoryDir, CommandExecutor.HookEvent.PRE_REVPROP_CHANGE, 
                preRevpropChangeContent.toString());

        // TODO: Install the post-revprop-change hook for updating the commit object and sending an email
    }

    /**
     * Add a file to a subversion repository.
     * 
     * @param fileToAdd
     *            The file to add.
     * @throws CommandWrapperFault
     *             If there was a problem adding the file.
     */
    public void doAddFile(final File fileToAdd) throws CommandWrapperFault {

        final String[] cmdString = { mSubversionBinary, ADD, fileToAdd.getName() };
        final File parentDir = fileToAdd.getParentFile();
        final CommandResult result = mExecutor.runCommandAs(ScmConstants.HTTPD_USER, cmdString, parentDir, false);
        if (result.getReturnValue() != CommandResult.RETURN_SUCCESS) {
            throw new CommandWrapperFault(result.getCommand(), "Could not add file " + fileToAdd.getAbsolutePath()
                                          + ": " + result.getCommandOutput());
        }
    }

    /**
     * Delete a file from a subversion repository.
     * 
     * @param fileToDelete
     *            The file to delete.
     * @throws CommandWrapperFault
     *             If there was a problem deleting the file.
     */
    public void doDeleteFile(final File fileToDelete) throws CommandWrapperFault {

        final String[] cmdString = { mSubversionBinary, DELETE, fileToDelete.getName() };
        final File parentDir = fileToDelete.getParentFile();
        final CommandResult result = mExecutor.runCommandAs(ScmConstants.HTTPD_USER, cmdString, parentDir, false);
        if (result.getReturnValue() != CommandResult.RETURN_SUCCESS) {
            throw new CommandWrapperFault(result.getCommand(), "Could not delete file "
                                          + fileToDelete.getAbsolutePath() + ": " + result.getCommandOutput());
        }
    }

    /**
     * Run a subversion Checkin command.
     * 
     * 
     * @param directory
     *            the directory to perform the checkin on.
     * @param message
     *            the commit message of this commit.
     * @param username
     *            the username (for --username)
     * @param password
     *            the password (for --password)
     * @throws CommandWrapperFault
     *             An error occurred while executing the command.
     */
    public void doCheckin(final File directory, final String message, final String username, final String password)
    throws CommandWrapperFault {

        final String[] cmd = { mSubversionBinary, COMMIT, "--non-interactive", "--username", username, "--password",
                               password, "-m", message };
        final CommandResult result = mExecutor.runCommandAs(ScmConstants.HTTPD_USER, cmd, directory, false);

        if (result.getReturnValue() != CommandResult.RETURN_SUCCESS) {
            throw new CommandWrapperFault(result.getCommand(), "Could not check in " + " from directory "
                                          + directory.getAbsolutePath() + ": " + result.getCommandOutput());
        }
    }

    /**
     * Do a Subversion status check on a specific file.
     * 
     * @param file
     *            The file that is being checked for status.
     * @param username
     *            the username (for --username)
     * @param password
     *            the password (for --password)
     * @return The output of the cvs status call.
     * @throws CommandWrapperFault
     *             If there was a problem getting the file status.
     */
    public String doStatusCheck(final File file, final String username, final String password)
    throws CommandWrapperFault {

        final String[] cmdString = { mSubversionBinary, STATUS, "-u", "--username", username, "--password", password,
                                     file.getName() };
        final CommandResult result = mExecutor.runCommandAs(ScmConstants.HTTPD_USER, cmdString, file.getParentFile(),
                                                            false);
        if (result.getReturnValue() != CommandResult.RETURN_SUCCESS) {
            throw new CommandWrapperFault(result.getCommand(), "Could not get status for file "
                                          + file.getAbsolutePath() + ": " + result.getCommandOutput());
        }

        return result.getCommandOutput();
    }

    /**
     * Do a Subversion copy
     * 
     * @param srcFile
     *            The file that is being copied
     * @param destFile
     *            the destination to where it should be copied
     * @param username
     *            the username (for --username)
     * @param password
     *            the password (for --password)
     * @return The output of the cvs status call.
     * @throws CommandWrapperFault
     *             If there was a problem getting the file status.
     */
    public String doCopy(final File srcFile, final File destFile, final String username, final String password)
    throws CommandWrapperFault {

        final String[] cmdString = { mSubversionBinary, COPY, "--username", username, "--password", password,
                                     srcFile.getPath(), destFile.getPath() };
        final CommandResult result = mExecutor.runCommandAs(ScmConstants.HTTPD_USER, cmdString,
                                                            destFile.getParentFile(), false);
        if (result.getReturnValue() != CommandResult.RETURN_SUCCESS) {
            throw new CommandWrapperFault(result.getCommand(), "Could not copy from " + srcFile.getAbsolutePath()
                                          + " to " + destFile.getAbsolutePath() + ":" + result.getCommandOutput());
        }

        return result.getCommandOutput();
    }

    /**
     * Do a Subversion move.
     * 
     * @param srcFile
     *            The file that is being moved
     * @param destFile
     *            the destination to where it should be moved
     * @param username
     *            the username (for --username)
     * @param password
     *            the password (for --password)
     * 
     * @throws CommandWrapperFault
     *             If there was a problem getting the file status.
     * 
     * @return The output of the svn status call.
     */
    public String doMove(final File srcFile, final File destFile, final String username, final String password)
    throws CommandWrapperFault {
        final String[] cmdString = { mSubversionBinary, MOVE, "--username", username, "--password", password,
                                     srcFile.getPath(), destFile.getPath() };
        final CommandResult result = mExecutor.runCommandAs(ScmConstants.HTTPD_USER, cmdString,
                                                            destFile.getParentFile(), false);
        if (result.getReturnValue() != CommandResult.RETURN_SUCCESS) {
            throw new CommandWrapperFault(result.getCommand(), "Could not move from " + srcFile.getAbsolutePath()
                                          + " to " + destFile.getAbsolutePath() + ":" + result.getCommandOutput());
        }

        return result.getCommandOutput();
    }

    /**
     * Sets/Modifies a versioned property on a file/directory.
     * 
     * @param path
     *            The path to have the property set/modified.
     * @param propName
     *            The property name.
     * @param propValue
     *            The property value.
     */
    public void setProperty(final File path, final String propName, final String propValue) throws CommandWrapperFault {
        final String[] cmdString = { mSubversionBinary, PROPSET, propName, propValue, path.getAbsolutePath() };
        final CommandResult result = mExecutor.runCommandAs(ScmConstants.HTTPD_USER, cmdString, path.getParentFile(),
                                                            false);

        if (result.getReturnValue() != CommandResult.RETURN_SUCCESS) {
            throw new CommandWrapperFault(result.getCommand(), "Could not set property '" + propName
                                          + "', with a value of '" + propValue + "' on " + path.getAbsolutePath());
        }
    }

    /**
     * Removes a versioned property from file/directory.
     * 
     * @param path
     *            The path to have the property removed.
     * @param propName
     *            The property to remove.
     */
    public void removeProperty(final File path, final String propName) throws CommandWrapperFault {
        final String[] cmdString = { mSubversionBinary, PROPDEL, propName, path.getAbsolutePath() };
        final CommandResult result = mExecutor.runCommandAs(ScmConstants.HTTPD_USER, cmdString, path.getParentFile(),
                                                            false);

        if (result.getReturnValue() != CommandResult.RETURN_SUCCESS) {
            throw new CommandWrapperFault(result.getCommand(), "Could not remove property '" + propName + "' from "
                                          + path.getAbsolutePath());
        }
    }

    /**
     * Updates the working copy.
     * 
     * @param path
     *            The working copy path to update.
     * @param username
     *            Username for svn up
     * @param password
     *            Password for svn up
     */
    public void update(final File path, final String username, final String password) throws CommandWrapperFault {
        final String[] cmdString = { mSubversionBinary, UPDATE, "--username", username, "--password", password,
                                     path.getAbsolutePath() };
        final CommandResult result = mExecutor.runCommandAs(ScmConstants.HTTPD_USER, cmdString, path.getParentFile(),
                                                            false);

        if (result.getReturnValue() != CommandResult.RETURN_SUCCESS) {
            throw new CommandWrapperFault(result.getCommand(), "Could not update the working copy located here: "
                                          + path.getAbsolutePath());
        }
    }

    /**
     * Reverts uncommitted changes to the file, possibly recursively.
     * 
     * @param path
     *            The working copy path to update
     * @param recursive
     *            Whether or not to issue a recursive revert
     * 
     * @throws CommandWrapperFault
     *             All exceptions
     */
    public void revert(final File path, final boolean recursive) throws CommandWrapperFault {
        final String[] cmdString = { mSubversionBinary, REVERT, (recursive ? "-R" : ""), path.getAbsolutePath() };
        final CommandResult result = mExecutor.runCommandAs(ScmConstants.HTTPD_USER, cmdString, path.getParentFile(),
                                                            false);

        if (result.getReturnValue() != CommandResult.RETURN_SUCCESS) {
            throw new CommandWrapperFault(result.getCommand(), "Could not revert the working copy path located here: "
                                          + path.getAbsolutePath());
        }
    }

    /**
     * Gets the revision property value from the passed revision for the passed url.
     * 
     * @param revision
     *            The revision
     * @param propName
     *            The revision property name
     * @param url
     *            The repository url
     * @param username
     *            The username to perform this change as
     * @param password
     *            The password for the username
     * 
     * @return String The property value
     * 
     * @throws CommandWrapperFault
     *             If there is a problem running the command or preparing to run it
     */
    public String getRevisionProperty(final int revision, final String propName, final String url,
                                      final String username, final String password) throws CommandWrapperFault {
        final String[] cmdString = { mSubversionBinary, PROPGET, "--revprop", "-r", Integer.toString(revision),
                                     "--non-interactive", "--username", username, "--password", password, propName, url };

        final CommandResult result = mExecutor.runCommandAs(ScmConstants.HTTPD_USER, cmdString, null, false);

        if (result.getReturnValue() != CommandResult.RETURN_SUCCESS) {
            final StringBuilder message = new StringBuilder();

            message.append("Could not remove the '");
            message.append(propName);
            message.append("' property for revision'");
            message.append(revision);
            message.append("' at '");
            message.append(url);
            message.append("': ");
            message.append(result.getCommandOutput());

            throw new CommandWrapperFault(result.getCommand(), message.toString());
        }

        return result.getCommandOutput();
    }

    /**
     * Adds/Sets the revision property value for the passed revision property for the passed revision for the passed
     * url.
     * 
     * @param revision
     *            The revision
     * @param propName
     *            The revision property name
     * @param propValue
     *            The new revision property value
     * @param url
     *            The repository url
     * @param username
     *            The username to perform this change as
     * @param password
     *            The password for the username
     * 
     * @throws CommandWrapperFault
     *             If there is a problem running the command or preparing to run it
     */
    public void setRevisionProperty(final int revision, final String propName, final String propValue,
                                    final String url, final String username, final String password)
    throws CommandWrapperFault {
        final String[] cmdString = { mSubversionBinary, PROPSET, "--revprop", "-r", Integer.toString(revision),
                                     "--non-interactive", "--username", username, "--password", password, propName,
                                     propValue, url };

        final CommandResult result = mExecutor.runCommandAs(ScmConstants.HTTPD_USER, cmdString, null, false);

        if (result.getReturnValue() != CommandResult.RETURN_SUCCESS) {
            final StringBuilder message = new StringBuilder();

            message.append("Could not add/update the '");
            message.append(propName);
            message.append("' property to '");
            message.append(propValue);
            message.append("' for revision'");
            message.append(revision);
            message.append("' at '");
            message.append(url);
            message.append("': ");
            message.append(result.getCommandOutput());

            throw new CommandWrapperFault(result.getCommand(), message.toString());
        }
    }

    /**
     * Remove the revision property from the passed revision for the passed url.
     * 
     * @param revision
     *            The revision
     * @param propName
     *            The revision property name
     * @param url
     *            The repository url
     * @param username
     *            The username to perform this change as
     * @param password
     *            The password for the username
     * 
     * @throws CommandWrapperFault
     *             If there is a problem running the command or preparing to run it
     */
    public void removeRevisionProperty(final int revision, final String propName, final String url,
                                       final String username, final String password) throws CommandWrapperFault {
        final String[] cmdString = { mSubversionBinary, PROPDEL, "--revprop", "-r", Integer.toString(revision),
                                     "--non-interactive", "--username", username, "--password", password, propName, url };

        final CommandResult result = mExecutor.runCommandAs(ScmConstants.HTTPD_USER, cmdString, null, false);

        if (result.getReturnValue() != CommandResult.RETURN_SUCCESS) {
            final StringBuilder message = new StringBuilder();

            message.append("Could not remove the '");
            message.append(propName);
            message.append("' property for revision'");
            message.append(revision);
            message.append("' at '");
            message.append(url);
            message.append("': ");
            message.append(result.getCommandOutput());

            throw new CommandWrapperFault(result.getCommand(), message.toString());
        }
    }
}
