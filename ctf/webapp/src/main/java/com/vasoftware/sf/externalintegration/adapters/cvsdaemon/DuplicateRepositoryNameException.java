/*
 * $RCSfile: DuplicateRepositoryNameException.java,v $
 *
 * SourceForge(r) Enterprise Edition
 * Copyright 2007 CollabNet, Inc.  All rights reserved.
 * http://www.collab.net
 */
package com.vasoftware.sf.externalintegration.adapters.cvsdaemon;

import com.vasoftware.sf.common.SfApplicationException;

/**
 * Exception <code>DuplicateRepositoryNameException</code> is thrown when the specified repository could not be created
 * 
 * @author Richard Lee <rlee@vasoftware.com>
 * @author Priyanka Dhanda <pdhanda@vasoftware.com>
 * @version $Revision: 1.6 $ $Date: 2007/05/24 00:37:27 $
 */
@SuppressWarnings("serial")
public class DuplicateRepositoryNameException extends SfApplicationException {
    /**
     * Error code for the exception.
     */
    protected static final String CODE_DUPLICATE_REPOSITORY_NAME = "Duplicate Repository Name: ";

    /**
     * Constructor for the exception.
     * 
     * @param repositoryName
     *            Name of the repository that failed to create.
     */
    public DuplicateRepositoryNameException(final String repositoryName) {
        super(CODE_DUPLICATE_REPOSITORY_NAME + repositoryName);
    }
}
