/*
 * CollabNet TeamForge
 * Copyright 2010 CollabNet, Inc.  All rights reserved.
 * http://www.collab.net
 */

package com.vasoftware.sf.externalintegration.adapters.wandiscodaemon;

import java.io.File;

import com.vasoftware.sf.common.configuration.GlobalOptionKeys;
import com.vasoftware.sf.externalintegration.IntegrationFault;
import com.vasoftware.sf.externalintegration.ObjectAlreadyExistsFault;
import com.vasoftware.sf.externalintegration.ScmConstants;
import com.vasoftware.sf.externalintegration.adapters.cvsdaemon.CvsWrapper;
import com.vasoftware.sf.externalintegration.execution.CommandWrapperFault;
import com.vasoftware.sf.externalintegration.execution.ObjectNotFoundFault;
import com.wandisco.webservices.scmapi_1_0.ScmType;

/**
 * The <code>WandiscoCvsScmServerDaemon</code> provides a simple implementation of the SCM adapter for WANdisco CVS
 * integration.
 */
public class WandiscoCvsScmServerDaemon extends WandiscoScmServerDaemon {
    // TODO: revisit this REGEXP
    public static final String ALL_BUT_CVSROOT_REGEXP = "/(?<!CVSROOT/).*";

    private final CvsWrapper mCvs = new CvsWrapper(getCommandExecutor(), CvsWrapper.CvsType.WANDISCO);

    public static final String SUPPORTED_ADAPTER_TYPE = "WANdiscoCVS";

    /**
     * Constructor. Instantiates the OS command executor.
     * 
     * @throws IntegrationFault
     *             Thrown if getting the command executor fails.
     */
    public WandiscoCvsScmServerDaemon() throws IntegrationFault {
        super();
        mWandisco = new WandiscoWrapper(GlobalOptionKeys.SFMAIN_WANDISCO_CVS_PREFIX);
    }

    /**
     * @see com.vasoftware.sf.externalintegration.adapters.wandiscodaemon.WandiscoScmServerDaemon#getScmType()
     */
    @Override
    protected ScmType getScmType() {
        return ScmType.CVS;
    }

    /**
     * @see com.vasoftware.sf.externalintegration.adapters.wandiscodaemon.WandiscoScmServerDaemon#setupRepository(java.lang.String, java.lang.String, java.lang.String)
     */
    @Override
    public void setupRepository(final String systemId, final String repositoryId, final String repositoryDir)
                                                                                                             throws IntegrationFault {
        super.setupRepository(systemId, repositoryId, repositoryDir);
        // create wandisco repository-admin group
        try {
            mWandisco.createGroup(repositoryId + ScmConstants.WD_ADMIN_GROUP_EXTENSION, "Admin group for "
                    + repositoryId);
        } catch (final ObjectAlreadyExistsFault e) {
            // TODO: what should be done if group already exists.. remove its
            // user?
        }
        final File repositoryDirectory = new File(repositoryDir);
        try {
            mCvs.setupTriggers(systemId, repositoryDirectory);
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

        super.setACLs(repositoryDir, groupId, level, operationName, ALL_BUT_CVSROOT_REGEXP);
    }

    /**
     * @see com.vasoftware.sf.externalintegration.adapters.wandiscodaemon.WandiscoScmServerDaemon#deleteRepositoryGroupsAndAcls(java.lang.String)
     */
    @Override
    public void deleteRepositoryGroupsAndAcls(final String repositoryId) throws IntegrationFault {
        final String adminGroupId = repositoryId + ScmConstants.WD_ADMIN_GROUP_EXTENSION;
        super.deleteRepositoryGroupsAndAcls(repositoryId);
        // delete the read, write and admin groups
        try {
            mWandisco.deleteGroup(adminGroupId);
        } catch (final ObjectNotFoundFault e) {
            // do nothing
        }

        // delete the ACLs for the admin group

        try {
            mWandisco.deleteACL(getAclExternalId(adminGroupId));
        } catch (final ObjectNotFoundFault e) {
            // do nothing
        }

    }

    /**
     * @see com.vasoftware.sf.externalintegration.adapters.wandiscodaemon.WandiscoScmServerDaemon#getSupportedAdapterType()
     */
    @Override
    public String getSupportedAdapterType() {
        return SUPPORTED_ADAPTER_TYPE;
    }

    /**
     * @see com.vasoftware.sf.externalintegration.adapters.SynchronizedScmServerDaemon#setPassword(java.lang.String, java.lang.String, java.lang.String)
     */
    @Override
    public void setPassword(final String username, final String password, final String cryptedPassword)
                                                                                                       throws IntegrationFault {
        // TODO
    }

    /**
     * @see com.vasoftware.sf.externalintegration.adapters.wandiscodaemon.WandiscoScmServerDaemon#createRepository(java.lang.String, java.lang.String, java.lang.String, java.lang.String)
     */
    @Override
    public String createRepository(final String repositoryId, final String repositoryDir, final String systemId,
                                   final String repositoryBaseUrl) throws IntegrationFault {
        // create the repository
        mWandisco.createRepository(repositoryDir, getScmType(), null);
        setupRepository(systemId, repositoryId, repositoryDir);
        return repositoryDir;

    }

    /**
     * @see com.vasoftware.sf.externalintegration.adapters.ScmScmServerDaemon#getDaemonType()
     */
    @Override
    public String getDaemonType() {
        return "cvs";
    }
}
