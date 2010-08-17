/*
 * CollabNet TeamForge
 * Copyright 2010 CollabNet, Inc.  All rights reserved.
 * http://www.collab.net
 */

package com.vasoftware.sf.externalintegration.adapters;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Properties;

import org.apache.axis.encoding.Base64;

import com.vasoftware.sf.common.SfSystemException;
import com.vasoftware.sf.common.configuration.GlobalOptionKeys;
import com.vasoftware.sf.common.configuration.SfGlobalOptions;
import com.vasoftware.sf.common.configuration.SfGlobalOptionsManager;
import com.vasoftware.sf.common.configuration.SfPaths;
import com.vasoftware.sf.common.logger.Logger;
import com.vasoftware.sf.common.util.ArrayUtil;
import com.vasoftware.sf.common.util.DecryptLicenseUtil;
import com.vasoftware.sf.common.util.FileUtil;
import com.vasoftware.sf.externalintegration.IntegrationFault;
import com.vasoftware.sf.externalintegration.ScmConstants;
import com.vasoftware.sf.externalintegration.execution.ExecutionUtil;

/**
 * Abstract parent of an SCM daemon
 * @author Dominick Bellizzi <dbellizzi@vasoftware.com>
 * @version $Revision: 1.31 $ $Date: 2007/05/24 00:37:28 $
 *
 * @sf.integration-soap-server
 */
public abstract class ScmScmServerDaemon extends BlackduckScmServerDaemon implements ScmDaemon {
    private static final Logger smLogger = Logger.getLogger(ScmScmServerDaemon.class);

    public static final String EXTERNAL_SYSTEM_ID_KEY = "external_system_id";

    /** Fault string for when the path could not be verified **/
    public static final String INVALID_PATH = "path doesn't exist";

    /**
     * Subdirectory under SOURCEFORGE_HOME to where archived repositories will be moved,
     * unless an override location is specified.
     */
    private static final String ARCHIVE_SUBDIRECTORY = "/var/scm-archive";

    private int mMaxRequestDelay;

    /**
     * An abstract scm server parent class
     * @throws IntegrationFault If the command did not execute correctly
     */
    public ScmScmServerDaemon() throws IntegrationFault {
        super();

        final SfGlobalOptions options = SfGlobalOptionsManager.getOptions();
        try {
            mMaxRequestDelay = Integer.parseInt(options.getOption(GlobalOptionKeys.SFMAIN_INTEGRATION_SECURITY_CHECK_TIMESTAMP));
        } catch (final NumberFormatException e) {
            mMaxRequestDelay = 0;
        }

        setScmToProcess(this);
    }

    /**
     * Returns the daemon type, which is used for daemon-specific entries in the sourceforge.properties file.
     * 
     * @return The SCM daemon type
     */
    public abstract String getDaemonType();

    /**
     * Initialize external system
     *
     * @param systemId The id of the system
     * @throws IntegrationFault if an error is encountered
     */
    public void initializeExternalSystem(final String systemId) throws IntegrationFault {
        if (ExecutionUtil.isWindows() && !isWindowsSupported()) {
            throw new IntegrationFault("This integration server SCM type is not supported on Windows.");
        }
    }

    /**
     * Remove external system
     *
     * @param systemId The id of the system
     * @throws IntegrationFault if an error is encountered
     */
    public void deleteExternalSystem(final String systemId) throws IntegrationFault {
        // do nothing
    }

    /**
     * Returns whether or not the daemon supports being run on Windows
     * which defaults to 'false' unless overridden by the subclass.
     * 
     * @return Whether or not the daemon implements Windows support.
     */
    public boolean isWindowsSupported() {
        return false;
    }

    /**
     * Verify request key.
     * @param key Request key
     * @throws IntegrationFault Key verification failed - unathorized request
     */
    protected void verifyKey(final String key) throws IntegrationFault {
        if (smLogger.isDebugEnabled()) {
            smLogger.debug("verifyKey(" + key + ")");
        }
        final byte[] unKey = Base64.decode(key);
        final int rnd = ArrayUtil.byte2int(unKey);
        int requestTimestamp = ArrayUtil.byte2int(ArrayUtil.extractSubArray(unKey, 4, 4));
        requestTimestamp ^= DecryptLicenseUtil.SCM_TIMESTAMP_SALT;
        final int myTimestamp = (int)(new Date().getTime() / 1000);
        if (smLogger.isDebugEnabled()) {
            smLogger.debug("rnd=" + rnd + ", myTS=" + myTimestamp + ", rTS=" + requestTimestamp +
                           ", diff=" + (myTimestamp - requestTimestamp));
        }
        // Check that key valid at all
        final byte[] myKey = DecryptLicenseUtil.makeScmRequestKey(rnd, requestTimestamp);
        if (!Arrays.equals(unKey, myKey)) {
            throw new IntegrationFault("Security exception");
        }

        // Now check that it's not expired
        if (mMaxRequestDelay > 0 && Math.abs(myTimestamp - requestTimestamp) > mMaxRequestDelay) {
            throw new IntegrationFault("Security timing exception");
        }
    }

    /**
     * Writing the external system id to /etc/sourceforge.properties
     * @param systemId The id of the external system
     * @param repositoryBaseKeyPrefix start of key to repository base
     */
    protected void initializeExternalSystemId(final String systemId) {
        final File configFile = scmPropertiesFile();
        final Properties properties = new Properties();
        InputStream propertyInputStream = null;

        try {
            propertyInputStream = new FileInputStream(configFile);
            properties.load(propertyInputStream);
        } catch (final IOException e) {
            // ignore
        } finally {
            FileUtil.close(propertyInputStream);
        }

        // See if there is an existing system id defined.  If so, make sure it matches the current system id
        final String oldSystemId = (String) properties.get(EXTERNAL_SYSTEM_ID_KEY);

        if (oldSystemId != null && !systemId.equals(oldSystemId)) {
            throw new SfSystemException("Unable to initialize external system. Another integration already exists.");
        }

        // If we already have a system id and it matches the current one, there is nothing to do here.
        if (oldSystemId != null) {
            return;
        }

        // We have a new system that is being initialized so write out the new property
        properties.setProperty(EXTERNAL_SYSTEM_ID_KEY, systemId);

        FileWriter writer = null;

        try {
            writer = new FileWriter(configFile, true);
            writer.write("\n" + EXTERNAL_SYSTEM_ID_KEY + "=" + systemId + "\n");
        } catch (final IOException e) {
            throw new SfSystemException("Unable to write properties", e);
        } finally {
            FileUtil.close(writer);
        }
    }

    /**
     * Delete the external system id from the .scm.properties file in the repository base directory.
     */
    protected void deleteExternalSystemId() {
        final File configFile = scmPropertiesFile();

        configFile.delete();
    }

    /**
     * Return .scm.properties file located in repository base directory which is configured in the
     * sourceforge.proeperties file.  An example configuration key for Subversion would be subversion.repository_base.
     * 
     * @return The file object corresponding to the .scm.properties
     */
    private File scmPropertiesFile() {
        return new File(getRepositoryRootPath() + "/.scm.properties");
    }

    /**
     * @see ScmDaemon#archiveRepository(String)
     */
    public Boolean archiveRepository(final String repositoryPath) throws IntegrationFault {
        final File repoDir = getRepositoryDirFromCTFRepositoryPath(repositoryPath);

        if (!repoDir.exists()) {
            smLogger.warn("archiveRepository: repository directory [" + repoDir +
            "] does not exist.");
            return Boolean.TRUE;
        }

        final String archiveRoot = getArchiveRepositoryRootPath();
        final File archiveRootFile = new File(archiveRoot);

        if (!archiveRootFile.exists()) {
            if (!archiveRootFile.mkdirs()) {
                smLogger.warn("archiveRepository: SCM archive directory [" + archiveRootFile +
                "] does not exist and could not be created.");
                return Boolean.FALSE;
            }
        }

        final DateFormat formatter = new SimpleDateFormat("yyyyMMdd'_'HHmmss");
        final String archiveFilename = repoDir.getName() + "-" + formatter.format(new Date()) + ".tar.gz";

        try {
            FileUtil.createTarGzOfDirectory(repoDir.getAbsolutePath(), archiveRoot + File.separator + archiveFilename);
            FileUtil.deleteDir(repoDir);

            return Boolean.TRUE;
        } catch (Exception e) {
            smLogger.warn("Error tarring SCM repository, left in place.", e);

            return Boolean.FALSE;
        }
    }

    /**
     * @see ScmDaemon#getArchiveRepositoryRootPath()
     */
    public String getArchiveRepositoryRootPath() {
        final SfGlobalOptions options = SfGlobalOptionsManager.getOptions();

        String archiveRoot = options.getOption(GlobalOptionKeys.SFMAIN_INTEGRATION_REPOSITORY_ARCHIVE_ROOT);

        if (archiveRoot == null) {
            archiveRoot = SfPaths.sourceforgeHome();
            archiveRoot += ARCHIVE_SUBDIRECTORY;
        }
        return archiveRoot;
    }

    /**
     * Returns the full path to the directory where the repositories will be created.  This is configured in the
     * sourceforge.properties via the <scm_daemon_type>.repository_base key.  If this key is not present in the
     * sourceforge.properties file, null is returned.
     * 
     * @return The full path to the directory where repositories will be created.
     */
    public String getRepositoryRootPath() {
        final SfGlobalOptions options = SfGlobalOptionsManager.getOptions();

        return options.getOption(getRepositoryBaseKey());
    }

    /**
     * Returns the key used to identify the repository root path in the sourceforge.properties file.
     * 
     * @return The key used to identify the path to the repository root
     */
    public String getRepositoryBaseKey() {
        return getDaemonType() + ".repository_base";
    }

    /**
     * As of CTF 5.4, there is no support internally in TeamForge for Windows filesystem paths.  To work around
     * this, we basically let the user configure whatever they want in TeamForge for the repository path.
     * TeamForge will then create the full repository path by concatenating the repository root to the
     * repository name.  On Unix, this works fine but on Windows, we get a Unixy path that must be treated
     * specially.
     * 
     * @param ctfRepositoryPath The repository path as generated by CTF.
     * 
     * @return The file corresponding to the where the repository exists
     * 
     * @throws IntegrationFault If {@link #getRepositoryRootPath()} return null
     */
    public File getRepositoryDirFromCTFRepositoryPath(String ctfRepositoryPath) throws IntegrationFault {
        File repositoryDirFile;

        if (ExecutionUtil.isWindows() && ctfRepositoryPath.startsWith(ScmConstants.FAKE_WINDOWS_SCMROOT)) {
            String repoRoot = getRepositoryRootPath();
            String repoName = FileUtil.getLastFileSystemPathSegment(ctfRepositoryPath);

            if (repoRoot == null) {
                throw new IntegrationFault("Your sourceforge.properties file is missing a necessary configuration " +
                                           "item: " + getRepositoryBaseKey() + ".  Please set the key to the full " +
                                           "path where new repositories should be created.");
            }

            repositoryDirFile = new File(repoRoot, repoName);
        } else {
            repositoryDirFile = new File(ctfRepositoryPath);
        }

        return repositoryDirFile;
    }

    /**
     * As of CTF 5.4, there is no support internally in TeamForge for Windows filesystem paths.  When sending repository
     * paths back to CTF, we need to convert the Windows path to the Unixy path expected/supported by CTF.
     * 
     * @param repositoryDir The file corresponding with the real repository directory
     * 
     * @return The unixy path CTF expects/supports on Windows or the file's absolute path otherwise
     */
    public String getCTFRepositoryPathFromRepositoryDir(File repositoryDir) {
        String repositoryPath = repositoryDir.getAbsolutePath();

        if (ExecutionUtil.isWindows() && !repositoryPath.startsWith(ScmConstants.FAKE_WINDOWS_SCMROOT)) {
            repositoryPath = ScmConstants.FAKE_WINDOWS_SCMROOT + "/" +
                FileUtil.getLastFileSystemPathSegment(repositoryPath);
        }

        return repositoryPath;
    }
}
