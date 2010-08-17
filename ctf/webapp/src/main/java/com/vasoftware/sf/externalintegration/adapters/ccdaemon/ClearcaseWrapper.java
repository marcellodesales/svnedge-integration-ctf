package com.vasoftware.sf.externalintegration.adapters.ccdaemon;

import com.vasoftware.sf.externalintegration.execution.CommandExecutor;
import com.vasoftware.sf.externalintegration.execution.CommandWrapperFault;

/**
 * Class which provides a clean looking wrapper around a bunch of dirty system exec calls to Subversion.
 * 
 * @author Wei Hsu <whsu@vasoftware.com>
 * @author Jamison Gray <jgray@vasoftware.com>
 * @author Michael Jones <mjones@vasoftware.com>
 * @version $Revision: 1.1 $ $Date: 2006/09/20 19:22:47 $
 */
public class ClearcaseWrapper {
    /**
     * Default constructor.
     * 
     * @param executor
     *            the executor class for executing external commands
     */
    public ClearcaseWrapper(final CommandExecutor executor) {
        // Empty
    }

    /**
     * verify the path, hardcoded to true for now
     * 
     * @param repositoryPath
     *            the path of the repository
     * @param repositoryPathFromRoot
     *            the path of the repository, from the root
     * @throws CommandWrapperFault
     *             if a command should fail
     * @return hard coded true
     */
    public boolean verifyPath(final String repositoryPath, final String repositoryPathFromRoot)
                                                                                               throws CommandWrapperFault {
        return (true);
    }

}
