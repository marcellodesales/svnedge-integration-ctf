/*
 * CollabNet TeamForge
 * Copyright 2010 CollabNet, Inc.  All rights reserved.
 * http://www.collab.net
 */
package com.vasoftware.sf.externalintegration.adapters.subversiondaemon;

/**
 * <code>InactiveScmServerDaemonSecure</code> class contains non-functional
 * versions of the ScmServer api's which are needed to convert a CSVN 
 * instance into an integration server.
 */
class InactiveScmServerDaemon {

    /**
     * @see com.vasoftware.sf.externalintegration.adapters.subversiondaemon.SubversionScmServerDaemon#initializeExternalSystem(String)
     */
    public void initializeExternalSystem(String systemId) {
    }

    /**
     * @see com.vasoftware.sf.externalintegration.adapters.subversiondaemon.SubversionScmServerDaemon#deleteExternalSystem(String)
     */
    public void deleteExternalSystem(String systemId) {
    }

    /**
     * @see com.vasoftware.sf.externalintegration.adapters.subversiondaemon.SubversionScmServerDaemon#createRepository(String,
     *      String, String, String)
     */
    public String createRepository(String repositoryGroup,
                                   String repositoryDir, String systemId,
                                   String repositoryBaseUrl) {
        return repositoryDir;
    }

    /**
     * @see com.vasoftware.sf.externalintegration.adapters.subversiondaemon.SubversionScmServerDaemon#setupRepository(String,
     *      String, String)
     */
    public void setupRepository(String systemId,
                                String repositoryGroup, 
                                String repositoryDir) {
    }

    /**
     * @see com.vasoftware.sf.externalintegration.adapters.subversiondaemon.SubversionScmServerDaemon#verifyExternalSystem(String)
     */
    public void verifyExternalSystem(String adapterType) {
    }

    /**
     * @see com.vasoftware.sf.externalintegration.adapters.subversiondaemon.SubversionScmServerDaemon#setRepositoryAccessLevel(String,
     *      String, String)
     */
    public void setRepositoryAccessLevel(String repositoryDir,
                                         String repositoryId, 
                                         String level) {
    }

    /**
     * @see com.vasoftware.sf.externalintegration.adapters.subversiondaemon.SubversionScmServerDaemon#verifyPath(String,
     *      String, String, String)
     */
    public void verifyPath(String externalBlackduckProjectId,
                           String repositoryId, String repositoryPath,
                           String repositoryPathFromRoot) {
    }

    /**
     * @see com.vasoftware.sf.externalintegration.adapters.subversiondaemon.SubversionScmServerDaemon#checkoutRepository(String,
     *      String, java.io.File)
     */
    public void checkoutRepository(String repositoryPath,
                                   String repositoryPathFromRoot,
                                   java.io.File destinationDirectory) {
    }

    /**
     * @see com.vasoftware.sf.externalintegration.adapters.subversiondaemon.SubversionScmServerDaemon#archiveRepository(String)
     */
    public Boolean archiveRepository(String repositoryPath) {
        return false;
    }

    /**
     * @see com.vasoftware.sf.externalintegration.adapters.subversiondaemon.SubversionScmServerDaemon#getArchiveRepositoryRootPath()
     */
    public String getArchiveRepositoryRootPath() {
        return null;
    }
}
