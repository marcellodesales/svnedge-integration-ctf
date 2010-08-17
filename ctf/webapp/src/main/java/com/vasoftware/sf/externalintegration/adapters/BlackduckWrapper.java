package com.vasoftware.sf.externalintegration.adapters;

import java.io.File;

import com.vasoftware.sf.common.configuration.GlobalOptionKeys;
import com.vasoftware.sf.common.configuration.SfGlobalOptions;
import com.vasoftware.sf.common.configuration.SfGlobalOptionsManager;
import com.vasoftware.sf.common.logger.Logger;
import com.vasoftware.sf.externalintegration.execution.CommandExecutor;
import com.vasoftware.sf.externalintegration.execution.CommandResult;
import com.vasoftware.sf.externalintegration.execution.CommandWrapperFault;

/**
 * Class which provides a clean looking wrapper around a bunch of dirty system exec calls to CVS.
 *
 * @author Richard Lee <rlee@vasoftware.com>
 * @version $Revision: 1.9 $ $Date: 2005/06/07 18:50:38 $
 */
public class BlackduckWrapper {
    private static final Logger smLogger = Logger.getLogger(BlackduckWrapper.class);

    private String mBdtoolBinary;
    private CommandExecutor mExecutor;

    private static final String LOGIN = "login";
    private static final String NEW_PROJECT = "new-project";
    private static final String ANALYZE = "analyze";
    private static final String UPLOAD = "upload";
    private static final String LIST_VERSIONS = "list-versions";

    /**
     * Default constructor for CvsWrapper.
     *
     * @param executor the executor class for executing external commands
     */
    public BlackduckWrapper(CommandExecutor executor) {
	SfGlobalOptions options = SfGlobalOptionsManager.getOptions();
	mBdtoolBinary = options.getOption(GlobalOptionKeys.SFMAIN_INTEGRATION_EXECUTABLES_BDSTOOL);
	mExecutor = executor;
    }

    /**
     * Gets the command that this wrapper executes.
     *
     * @return String - the command executed as a String.
     */
    public String getCommand() {
	return mBdtoolBinary;
    }

    /**
     * Logs the user into the Black duck server
     * @param hostName host of the blackduck server
     * @param port port for the blackduck server
     * @param username username to log in as
     * @param password password of user
     * @param path working directory to run the command in
     * @throws CommandWrapperFault if something goes wrong
     */
    public void login(String hostName, int port, String username, String password, File path)
	    throws CommandWrapperFault {

	try {
	    String cmdString[] = {mBdtoolBinary, "--server", hostName + ":" + port,
				  "--user", username, "--password", password, LOGIN};
	    CommandResult result = mExecutor.runCommand(cmdString, null, path);

	    if (result.getReturnValue() != CommandResult.RETURN_SUCCESS) {
		// don't show the actual command run, since it contains the blackduck password
		throw new CommandWrapperFault(mBdtoolBinary + " " + LOGIN, result.getCommandOutput());
	    }
	} catch (CommandWrapperFault e) {
	    // don't show the actual command run, since it contains the blackduck password
	    smLogger.error("Blackduck error running " + mBdtoolBinary + " " + LOGIN);
	    throw new CommandWrapperFault(mBdtoolBinary + " " + LOGIN, "Login failed");
	}
    }

    /**
     * Creates a new blackduck description file for a blackduck project to it.
     * @param externalBlackduckProjectId the blackduck project to attach
     * @param path working directory to run the command in
     * @throws CommandWrapperFault if something goes wrong
     */
    public void attachProject(String externalBlackduckProjectId, File path) throws CommandWrapperFault {
	smLogger.info("Blackduck attach project for " + externalBlackduckProjectId + " started in " + path);

	try {
	    File blackduckXml = new File(path, "blackduck.xml");
	    if (blackduckXml.exists()) {
		smLogger.info("Found existing blackduck project at " + blackduckXml.getAbsolutePath());
	    } else {
		String cmdString[] = {mBdtoolBinary, NEW_PROJECT, "--attach", externalBlackduckProjectId};
		CommandResult result = mExecutor.runCommand(cmdString, null, path);

		if (result.getReturnValue() != CommandResult.RETURN_SUCCESS) {
		    throw new CommandWrapperFault(result.getCommand(), result.getCommandOutput());
		}
	    }
	} catch (CommandWrapperFault e) {
	    smLogger.error("Blackduck attach project error: " + e.getMessage());
	    throw e;
	}
    }

    /**
     * Run blackduck analysis
     *
     * @param path the path to create the repository on.
     * @return The running analysis process
     * @throws CommandWrapperFault An error occurred while executing the command.
     */
    public Process beginAnalysis(File path) throws CommandWrapperFault {
        smLogger.info("Blackduck begin analysis started in " + path);
	String cmdString[] = {mBdtoolBinary, "--expert-mode", "--rescan", "aggressive", ANALYZE};
	return mExecutor.runCommandAsync(cmdString, null, path);
    }

    /**
     * Stores the current blackduck project descriptor onto the blackduck server
     * @param path working directory where the commands run in
     * @throws CommandWrapperFault if something goes wrong
     */
    public void uploadAnalysis(File path) throws CommandWrapperFault {
	smLogger.info("Blackduck begin upload started in " + path);

	try {
	    String cmdString[] = {mBdtoolBinary, UPLOAD};
	    CommandResult result = mExecutor.runCommand(cmdString, null, path);

	    if (result.getReturnValue() != CommandResult.RETURN_SUCCESS) {
		throw new CommandWrapperFault(result.getCommand(), result.getCommandOutput());
	    }
	} catch (CommandWrapperFault e) {
	    smLogger.error("Blackduck upload error: " + e.getMessage());
	    throw e;
	}
    }

    /**
     * Get the Blackduck version for a Blackduck library component
     * @param library The component to get the version for
     * @param hostName The hostname of the blackduck server
     * @param port The port of the blackduck server
     * @param username The username to connect to blackduck as
     * @param password The password to connect with
     * @return The version string for the library component
     * @throws CommandWrapperFault If there was an error executing the command
     */
    public String getVersion(String library, String hostName, int port, String username, String password)
	    throws CommandWrapperFault {
	smLogger.info("Getting blackduck bdstool client version");

	login(hostName, port, username, password, null);
	try {
	    String cmdString[] = {mBdtoolBinary, LIST_VERSIONS};
	    CommandResult result = mExecutor.runCommand(cmdString, null, null);

	    if (result.getReturnValue() != CommandResult.RETURN_SUCCESS) {
		throw new CommandWrapperFault(result.getCommand(), result.getCommandOutput());
	    }
	    String results = result.getCommandOutput();

	    String searchString = library + " version:";
	    int startPosition = results.indexOf(searchString);
	    if (startPosition == -1) {
		throw new CommandWrapperFault(result.getCommand(),
					      "Could not find " + library + " in version string");
	    }

	    int endPosition = results.indexOf("\n", startPosition + searchString.length());
	    if (endPosition == -1) {
		endPosition = results.length();
	    }
	    String version = results.substring(startPosition + searchString.length(), endPosition).trim();
	    return version;
	} catch (CommandWrapperFault e) {
	    smLogger.error("Blackduck version error: " + e.getMessage());
	    throw e;
	}
    }
}
