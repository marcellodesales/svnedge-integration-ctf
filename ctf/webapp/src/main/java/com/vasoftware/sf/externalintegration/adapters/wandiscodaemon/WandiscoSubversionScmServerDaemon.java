/*
 * CollabNet TeamForge
 * Copyright 2010 CollabNet, Inc.  All rights reserved.
 * http://www.collab.net
 */

package com.vasoftware.sf.externalintegration.adapters.wandiscodaemon;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import com.vasoftware.sf.common.configuration.GlobalOptionKeys;
import com.vasoftware.sf.externalintegration.IntegrationFault;
import com.vasoftware.sf.externalintegration.ObjectAlreadyExistsFault;
import com.vasoftware.sf.externalintegration.adapters.subversiondaemon.SubversionWrapper;
import com.vasoftware.sf.externalintegration.execution.CommandWrapperFault;
import com.vasoftware.sf.externalintegration.execution.UserAlreadyExistsFault;
import com.wandisco.webservices.scmapi_1_0.ScmType;

/**
 * The <code>WandiscoSubversionScmServerDaemon</code> provides a simple implementation of the SCM adapter for WANdisco
 * Subversion integration.
 */
@SuppressWarnings("unchecked")
public class WandiscoSubversionScmServerDaemon extends WandiscoScmServerDaemon {
    public static final String SUPPORTED_ADAPTER_TYPE = "WANdiscoSubversion";

    public static final String ALL_REGEXP = "(/.*)?";

    private final SubversionWrapper mSubversion = new SubversionWrapper(getCommandExecutor(), true);

    /**
     * @see com.vasoftware.sf.externalintegration.adapters.wandiscodaemon.WandiscoScmServerDaemon#getSupportedAdapterType()
     */
    @Override
    public String getSupportedAdapterType() {
        return SUPPORTED_ADAPTER_TYPE;
    }

    /**
     * @see com.vasoftware.sf.externalintegration.adapters.wandiscodaemon.WandiscoScmServerDaemon#getScmType()
     */
    @Override
    protected ScmType getScmType() {
        return ScmType.SVN_HTTP;
    }

    /**
     * Constructor. Instantiates the OS command executor.
     * 
     * @throws IntegrationFault
     *             Thrown if getting the command executor fails.
     */
    public WandiscoSubversionScmServerDaemon() throws IntegrationFault {
        super();
        mWandisco = new WandiscoWrapper(GlobalOptionKeys.SFMAIN_WANDISCO_SUBVERSION_PREFIX);
    }

    /**
     * @see com.vasoftware.sf.externalintegration.adapters.wandiscodaemon.WandiscoScmServerDaemon#deleteExternalSystem(java.lang.String)
     */
    @Override
    public void deleteExternalSystem(final String systemId) throws IntegrationFault {
        // delete the groups and ACLS
        super.deleteExternalSystem(systemId);
    }

    /**
     * @see com.vasoftware.sf.externalintegration.adapters.wandiscodaemon.WandiscoScmServerDaemon#setupRepository(java.lang.String, java.lang.String, java.lang.String)
     */
    @Override
    public void setupRepository(final String systemId, final String repositoryId, final String repositoryDir)
                                                                                                             throws IntegrationFault {

        super.setupRepository(systemId, repositoryId, repositoryDir);

        try {
            mSubversion.setupTriggers(systemId, repositoryDir);
        } catch (final CommandWrapperFault f) {
            throw new IntegrationFault(f);
        }
    }

    /**
     * @see com.vasoftware.sf.externalintegration.adapters.wandiscodaemon.WandiscoScmServerDaemon#setACLs(java.lang.String, java.lang.String, java.lang.String, java.lang.String)
     */
    @Override
    public void setACLs(final String repositoryDir, final String groupId, final String level, final String operationName)
                                                                                                                         throws ObjectAlreadyExistsFault,
                                                                                                                         IntegrationFault {
        super.setACLs(repositoryDir, groupId, level, operationName, ALL_REGEXP);
    }

    /**
     * @see com.vasoftware.sf.externalintegration.adapters.wandiscodaemon.WandiscoScmServerDaemon#deleteRepositoryGroupsAndAcls(java.lang.String)
     */
    @Override
    public void deleteRepositoryGroupsAndAcls(final String repositoryId) throws IntegrationFault {
        super.deleteRepositoryGroupsAndAcls(repositoryId);
    }

    /**
     * @see com.vasoftware.sf.externalintegration.adapters.SynchronizedScmServerDaemon#setPassword(java.lang.String, java.lang.String, java.lang.String)
     */
    @Override
    public void setPassword(final String username, final String password, final String cryptedPassword)
                                                                                                       throws IntegrationFault {
        // pass the encrypted password
        mWandisco.setPassword(username, cryptedPassword, false, null);
    }

    /**
     * @see com.vasoftware.sf.externalintegration.adapters.wandiscodaemon.WandiscoScmServerDaemon#addUsers(java.lang.String[], java.lang.String[])
     */
    @Override
    public String[] addUsers(final String[] usernames, final String[] md5Passwords) throws IntegrationFault {
        final List usersCreated = new ArrayList();

        for (int i = 0; i < usernames.length; i++) {
            try {
                mWandisco.createUser(usernames[i], true);
                usersCreated.add(usernames[i]);

                if (md5Passwords != null & md5Passwords[i] != null) {
                    mWandisco.setPassword(usernames[i], md5Passwords[i], false, null);
                }
            } catch (final UserAlreadyExistsFault e) {
                // do nothing
            }
        }
        return (String[]) usersCreated.toArray(new String[] {});
    }

    /**
     * @see com.vasoftware.sf.externalintegration.adapters.wandiscodaemon.WandiscoScmServerDaemon#createRepository(java.lang.String, java.lang.String, java.lang.String, java.lang.String)
     */
    @Override
    public String createRepository(final String repositoryId, final String repositoryDir, final String systemId,
                                   final String repositoryBaseUrl) throws IntegrationFault {
        return createRepository(repositoryId, repositoryDir, systemId, repositoryBaseUrl, true);

    }

    /**
     * Implementation of create repository method. Depending on the flg setupRespository, will setup or not the
     * repository
     * 
     * @param repositoryId
     *            the id of the repository to create
     * @param repositoryDir
     *            The path to the repository
     * @param systemId
     *            The Guid of the system this adapter is running on
     * @param repositoryBaseUrl
     *            the repository base url (wandisco subversion uses this)
     *@param setupRepository
     *            flag to indicate wether we want to do the setup of the repository or not
     * @return the repository filesystem path of the new repository
     * @throws IntegrationFault
     *             An error occurred while executing the command.
     */
    private String createRepository(final String repositoryId, final String repositoryDir, final String systemId,
                                    final String repositoryBaseUrl, final boolean setupRepository)
                                                                                                  throws IntegrationFault {
        String logicalPath;
        try {
            final URL url = new URL(repositoryBaseUrl);
            logicalPath = url.getPath();
        } catch (final MalformedURLException e) {
            throw new IntegrationFault("repositoryBaseUrl is not a url " + e);
        }
        // create the repository
        mWandisco.createRepository(repositoryDir, getScmType(), logicalPath);
        if (setupRepository) {
            setupRepository(systemId, repositoryId, repositoryDir);
        }
        return repositoryDir;

    }

    /**
     * Verify that the WANdisco server is up and running.
     * 
     * @param adapterType
     *            The type of adapter we are expected to be.
     * @param repositoryBaseUrl
     *            the repositoryBaseUrl
     * @param dummyRepositoryPath
     *            the path for the dummy repository
     * @throws com.vasoftware.sf.externalintegration.IntegrationFault
     *             if the command could not be executed
     */
    public void verifyExternalSystem(final String adapterType, final String repositoryBaseUrl,
                                     final String dummyRepositoryPath) throws IntegrationFault {
        super.verifyExternalSystem(adapterType);

        // create a dummy repository so the apache config gets created

        createRepository(null, dummyRepositoryPath, null, repositoryBaseUrl, false);
        // delete the dummy repository
        super.archiveRepository(dummyRepositoryPath);
    }

    /**
     * @see com.vasoftware.sf.externalintegration.adapters.ScmScmServerDaemon#getDaemonType()
     */
    @Override
    public String getDaemonType() {
        return "subversion";
    }
}
