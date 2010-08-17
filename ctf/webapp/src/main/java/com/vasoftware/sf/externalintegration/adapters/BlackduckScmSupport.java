/*
 * $RCSfile: BlackduckScmSupport.java,v $
 *
 * SourceForge(r) Enterprise Edition
 * Copyright 2007 CollabNet, Inc.  All rights reserved.
 * http://www.collab.net
*/

package com.vasoftware.sf.externalintegration.adapters;

import com.vasoftware.sf.externalintegration.IntegrationFault;

/**
 * Interface which an SCM daemon must implement so its repositories can undergo Blackduck analysis.
 * 
 * @author Paul Sokolovsky <psokolovsky@vasoftware.com>
 * @version $Revision: 1.2 $ $Date: 2007/05/24 00:37:28 $
 */
public interface BlackduckScmSupport {
    /**
     * Begin a blackduck analysis on the specified repository and report the results back for processing
     * @param hostName hostname of the scm server
     * @param port port for the scm server
     * @param username username to log in as
     * @param password password for the username
     * @param blackduckRepositoryId The id of the blackduck repository
     * @param externalBlackduckProjectId the id blackduck project
     * @param repositoryPath The base path of the repository
     * @param repositoryPathFromRoot The relative path of the code to be analyzed
     * @throws com.vasoftware.sf.externalintegration.IntegrationFault if the operation causes an error
     */
    void beginBlackduckAnalysis(String hostName, int port, String username, String password,
				       String blackduckRepositoryId,
				       String externalBlackduckProjectId, String repositoryPath,
				       String repositoryPathFromRoot)
	    throws IntegrationFault;

    /**
     * Cancel a blackduck analysis
     * @param externalBlackduckProjectId The analysis to cancel
     */
    void cancelBlackduckAnalysis(String externalBlackduckProjectId);

    /**
     * Get the blackduck analysis status
     * @param externalBlackduckProjectId The analysis to get the status of
     * @return the status
     */
    String getBlackduckAnalysisStatus(String externalBlackduckProjectId);

    /**
     * Cleanup the checked out repositories and blackduck files
     * @param externalBlackduckProjectId The blackduck project id
     * @throws com.vasoftware.sf.externalintegration.IntegrationFault If something goes wrong
     */
    void cleanupBlackduckRepository(String externalBlackduckProjectId) throws IntegrationFault;

    /**
     * Check if blackduck is enabled on this scm server
     * @param hostName The hostname of the blackduck server
     * @param port The port of the blackduck server
     * @param username The username of the admin on blackduck
     * @param password The password of the admin on blackduck
     * @throws com.vasoftware.sf.externalintegration.IntegrationFault If blackduck is not enabled on this server
     */
    void isBlackduckEnabled(String hostName, int port, String username, String password)
	    throws IntegrationFault;
}
