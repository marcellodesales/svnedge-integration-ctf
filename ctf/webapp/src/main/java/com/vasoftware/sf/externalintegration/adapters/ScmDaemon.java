/*
 * $RCSfile: ScmDaemon.java,v $
 *
 * SourceForge(r) Enterprise Edition
 * Copyright 2007 CollabNet, Inc.  All rights reserved.
 * http://www.collab.net
*/

package com.vasoftware.sf.externalintegration.adapters;

import java.io.File;

import com.vasoftware.sf.externalintegration.IntegrationFault;
import com.vasoftware.sf.externalintegration.ObjectAlreadyExistsFault;

/**
 * Defines core requests which must be implemented by a daemon being in charge of SCM repositories.
 * The requests are being forwarded in the form of SOAP calls from an SCM adapter, running in the context
 * of main SFEE application.
 *
 * @author Paul Sokolovsky <psokolovsky@vasoftware.com>
 * @version $Revision: 1.4 $ $Date: 2007/05/24 00:37:28 $
 */
public interface ScmDaemon {
    /** External System ID in /etc/sourceforge.properties */
    String KEY_EXTERNAL_SYSTEM_ID = "external_system_id";

    /**
     * Verify that the current external system is configured correctly
     * @param adapterType The type of adapter that you expect the current system to support
     * @throws com.vasoftware.sf.externalintegration.IntegrationFault If the command could not execute correctly
     */
    void verifyExternalSystem(String adapterType) throws IntegrationFault;

    /**
    * Initialize external system
    *
    * @param systemId The id of the system
     * @throws IntegrationFault if an error is encountered
    */
   public void initializeExternalSystem(String systemId) throws IntegrationFault;

    /**
    * Remove external system
    *
    * @param systemId The id of the system
    * @throws IntegrationFault if an error is encountered
    */
   public void deleteExternalSystem(String systemId) throws IntegrationFault;

    /**
     * Implementation of create repository method.
     *
     * @param repositoryGroup   the group of the repository to create
     * @param repositoryDir The path to the repository
     * @param systemId     The Guid of the system this adapter is running on
     * @param repositoryBaseUrl the repository base url (wandisco subversion uses this)
     * @return the repsository filesystem path of the new repository
     * @throws IntegrationFault An error occurred while executing the command.
     * @throws ObjectAlreadyExistsFault if a repository already exists at the destination path
     */
    public String createRepository(String repositoryGroup, String repositoryDir, String systemId,
				   String repositoryBaseUrl)
	    throws IntegrationFault, ObjectAlreadyExistsFault;

    /**
     * Set the repository access level (public, gated, member, private) by changing the group ownership
     *
     * @param repositoryDir The full directory of the mounted CC vob
     * @param repositoryId  The repository's ID to use as the group for member and private access
     * @param level         the access level to change to (from RBACConstants)
     * @throws IntegrationFault An error occurred while executing the command.
     */
    void setRepositoryAccessLevel(String repositoryDir, String repositoryId, String level)
	    throws IntegrationFault;

    /**
     * Checksout the repository
     * @param repositoryPath the root path for the repository
     * @param repositoryPathFromRoot the path to the repository from the root path
     * @param destinationDirectory destination directory to checkout the repository to
     * @throws IntegrationFault if something is wrong
     */
    void checkoutRepository(String repositoryPath, String repositoryPathFromRoot,
					       File destinationDirectory)
	    throws IntegrationFault;

    /**
     * Move the repository to archive location.
     * @param repositoryPath the root path for the repository
     * @return true if archival operation succeeded, false otherwise
     * @throws IntegrationFault Any errors
     */
    Boolean archiveRepository(String repositoryPath) throws IntegrationFault;

    /**
     * Returns the path to the archive repository.
     *
     * NOTE: any changes in location or algorithm here may need to be reflected in
     * AbstractScmDeleteHttpTest.verifyRepositoryMoved(), and in the
     * RepositoryDeactivationSuccess.xml message template.
     *
     * @return the path
     */
    String getArchiveRepositoryRootPath();

    /**
     * Verify the path exists on the SCM server
     * @param externalBlackduckProjectId External blackduck project id, used as part of path
     * @param repositoryId The id of the scm server
     * @param repositoryPath The root path of the repositories on the scm server
     * @param repositoryPathFromRoot The relative path to verify
     * @throws IntegrationFault Any errors
     */
    void verifyPath(String externalBlackduckProjectId, String repositoryId,
				    String repositoryPath, String repositoryPathFromRoot)
	    throws IntegrationFault;

}
