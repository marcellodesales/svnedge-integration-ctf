/*
 * $RCSfile $
 * SourceForge(r) Enterprise Edition
 * Copyright 2007 CollabNet, Inc.  All rights reserved.
 * http://www.collab.net
 */

package com.vasoftware.sf.externalintegration.adapters;

import java.io.File;
import java.net.MalformedURLException;
import java.rmi.RemoteException;

import javax.xml.rpc.ServiceException;

import org.apache.axis.AxisFault;

import com.vasoftware.sf.common.SoapClientHelper;
import com.vasoftware.sf.common.configuration.GlobalOptionKeys;
import com.vasoftware.sf.common.configuration.SfGlobalOptions;
import com.vasoftware.sf.common.configuration.SfGlobalOptionsManager;
import com.vasoftware.sf.common.logger.Logger;
import com.vasoftware.sf.externalintegration.BlackduckConstants;
import com.vasoftware.sf.externalintegration.IntegrationFault;
import com.vasoftware.sf.externalintegration.execution.CommandExecutor;
import com.vasoftware.sf.externalintegration.execution.CommandExecutorException;
import com.vasoftware.sf.externalintegration.execution.CommandResult;
import com.vasoftware.sf.externalintegration.execution.CommandWrapperFault;
import com.vasoftware.sf.externalintegration.execution.ExecutionUtil;

/**
 * A process running in a thread that walks through all steps of a blackduck analysis
 *
 * @author Dominick Bellizzi <dbellizzi@vasoftware.com>
 * @version $Revision: 1.11 $ $Date: 2007/05/24 00:37:28 $
 */
public class BlackduckAnalysis implements Runnable {
    private static Logger smLogger = Logger.getLogger(BlackduckAnalysis.class);

    private ScmDaemon mScmDaemon;
    private String mHostName;
    private int mPort;
    private String mUsername;
    private String mPassword;
    private String mExternalBlackduckProjectId;
    private String mRepositoryPath;
    private String mRepositoryPathFromRoot;
    private String mStatus;
    private boolean mCancelRequested;
    private String mBlackduckRepositoryId;
    private CommandExecutor mExecutor;

    /**
     * Create a blackduck analysis
     * @param scmDaemon The Scm Daemon calling, to be used to checkout the repository
     * @param hostName The host name of the blackduck server
     * @param port The port of the blackduck server
     * @param username The username to log in to blackduck with
     * @param password The password to log in to blackduck with
     * @param blackduckRepositoryId The blackduck repository id that the analysis is linked to.
     * @param externalBlackduckProjectId The external blackduck project id to report against
     * @param repositoryPath The repository root path on disk
     * @param repositoryPathFromRoot The relative path to analyze, from the repositoryPath
     * @throws CommandExecutorException thrown if there was a problem getting the executor.
     */
    public BlackduckAnalysis(ScmDaemon scmDaemon, String hostName, int port, String username,
				   String password, String blackduckRepositoryId,  String externalBlackduckProjectId,
				   String repositoryPath, String repositoryPathFromRoot)
	    throws CommandExecutorException {
	mScmDaemon = scmDaemon;
	mHostName = hostName;
	mPort = port;
	mUsername = username;
	mPassword = password;
	mExternalBlackduckProjectId = externalBlackduckProjectId;
	mRepositoryPath = repositoryPath;
	mRepositoryPathFromRoot = repositoryPathFromRoot;
	mStatus = BlackduckAnalysisManager.STATUS_STARTED;
	mBlackduckRepositoryId = blackduckRepositoryId;

	mExecutor = ExecutionUtil.getCommandExecutor();
    }

    /**
     * Run the blackduck analysis
     * @see Runnable#run()
     */
    public void run() {
	boolean inException = true;
	try {
	    smLogger.debug("Starting analysis for " + mExternalBlackduckProjectId);
	    BlackduckWrapper blackduck = new BlackduckWrapper(mExecutor);

	    File blackduckSourceRoot = ScmScmServerDaemon.getBlackduckSourceRoot();
	    if (!blackduckSourceRoot.exists()) {
		mExecutor.createPath(blackduckSourceRoot);
		mExecutor.setOwnerToRwxOthersRxPermissions("nobody", blackduckSourceRoot, true);
	    }

	    File workingDirectory = new File(blackduckSourceRoot, mExternalBlackduckProjectId);
	    if (!workingDirectory.exists()) {
		mExecutor.createPath(workingDirectory);
		mExecutor.setUserOnPath("nobody", workingDirectory);
	    }

	    setStatus(BlackduckAnalysisManager.STATUS_CHECKOUT);
	    mScmDaemon.checkoutRepository(mRepositoryPath, mRepositoryPathFromRoot, workingDirectory);
	    if (isCancelRequested()) {
		return;
	    }

	    setStatus(BlackduckAnalysisManager.STATUS_LOGIN);
	    blackduck.login(mHostName, mPort, mUsername, mPassword, workingDirectory);
	    if (isCancelRequested()) {
		return;
	    }

	    setStatus(BlackduckAnalysisManager.STATUS_ATTACH);
	    blackduck.attachProject(mExternalBlackduckProjectId, workingDirectory);
	    if (isCancelRequested()) {
		return;
	    }

	    setStatus(BlackduckAnalysisManager.STATUS_ANALYZE);
	    Process blackduckAnalysisProcess = blackduck.beginAnalysis(workingDirectory);

	    // While the analysis is running, check if a cancel has been requested
	    while (!finished(blackduckAnalysisProcess) && !isCancelRequested()) {
		try {
		    Thread.sleep(5000); // 5 seconds
		} catch (InterruptedException e) {
		    // ignore
		}
	    }

	    if (blackduckAnalysisProcess.exitValue() != CommandResult.RETURN_SUCCESS) {
		throw new CommandWrapperFault("bdstool --expert-mode --rescan aggressive analyze", 
					      "Blackduck analysis failed");
	    }

	    if (isCancelRequested()) {
		blackduckAnalysisProcess.destroy();
		return;
	    }

	    setStatus(BlackduckAnalysisManager.STATUS_UPLOAD);
	    blackduck.uploadAnalysis(workingDirectory);
	    smLogger.debug("Finished analysis for " + mExternalBlackduckProjectId);
	    inException = false;
	} catch (IntegrationFault f) {
	    smLogger.error("Error in blackduck analysis", f);
	    setStatus(BlackduckAnalysisManager.STATUS_EXCEPTION);
	} catch (CommandWrapperFault commandWrapperFault) {
	    smLogger.error("Error in blackduck analysis", commandWrapperFault);
	    setStatus(BlackduckAnalysisManager.STATUS_EXCEPTION);
	} finally {
	    String status = BlackduckConstants.ANALYSIS_STATUS_COMPLETED;
	    if (isCancelRequested()) {
		status = BlackduckConstants.ANALYSIS_STATUS_CANCELLED;
		smLogger.debug("Interrupted analysis of " + mExternalBlackduckProjectId);
	    } else if (inException) {
		status = BlackduckConstants.ANALYSIS_STATUS_ERROR;
		smLogger.warn("Error in analysis of " + mExternalBlackduckProjectId);
	    }

	    // Report back to sourceforge
	    notifyAnalysisComplete(mBlackduckRepositoryId, status);

	    // Thread is about to finish, clean it from the manager
	    BlackduckAnalysisManager manager = BlackduckAnalysisManager.getManager();
	    manager.cleanupThread(mExternalBlackduckProjectId);
	}
    }

    /**
     * Check to see if a process is finished.
     * @param blackduckAnalysisProcess The process to check
     * @return Finished or not
     */
    private boolean finished(Process blackduckAnalysisProcess) {
	try {
	    blackduckAnalysisProcess.exitValue();
	    return true;
	} catch (IllegalThreadStateException e) {
	    return false;
	}
    }

    /**
     * Cancel the running process
     */
    public void cancel() {
	mCancelRequested = true;
    }

    /**
     * Check to see if cancelling is requested
     * @return true or false
     */
    private boolean isCancelRequested() {
	return mCancelRequested;
    }

    /**
     * Set the status of the current process
     * @param status The status
     */
    private void setStatus(String status) {
	mStatus = status;
    }

    /**
     * Get the status of the current process
     * @return The status
     */
    public String getStatus() {
	return mStatus;
    }


    /**
     * Set the analysis status to completed after the BlackDuckAnalysys thread is done with its analysis
     * @param blackDuckRepositoryId The blackduck repository id for which the analysis was being run
     * @param status The status of the analysis after completion
     */
    protected void notifyAnalysisComplete(String blackDuckRepositoryId, String status) {
	SfGlobalOptions config = SfGlobalOptionsManager.getOptions();

        String soapServerUrl = config.getOption(GlobalOptionKeys.SFMAIN_SOAP_API_SERVER_URL);
	try {
	    SoapClientHelper soapHelper = new SoapClientHelper(soapServerUrl + "/ce-soap60/services/ScmListener");

	    soapHelper.invoke("notifyAnalysisComplete", new Object[]{blackDuckRepositoryId, status});
        } catch (AxisFault e) {
	    smLogger.error("Error while calling notifyAnalysisComplete for BlackDuck repository: " + blackDuckRepositoryId,
			   e);
	} catch (ServiceException e) {
	    smLogger.error("Error while calling notifyAnalysisComplete for BlackDuck repository: " + blackDuckRepositoryId,
			   e);
	} catch (RemoteException e) {
	    smLogger.error("Error while calling notifyAnalysisComplete for BlackDuck repository: " + blackDuckRepositoryId,
			   e);
        } catch (MalformedURLException e) {
	    smLogger.error("Error while calling notifyAnalysisComplete for BlackDuck repository: " + blackDuckRepositoryId,
			   e);
	}
    }



}
