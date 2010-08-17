package com.vasoftware.sf.externalintegration;

import com.vasoftware.sf.common.SoapClientHelper;
import com.vasoftware.sf.common.logger.Logger;
import com.vasoftware.sf.common.util.DecryptLicenseUtil;
import com.vasoftware.sf.common.util.ScmPermissionsCallback;

/**
 * Implementation of <code>ScmPermissionsCallback</code> which will use SOAP
 * to retrieve the user's repository permissions from the
 * <code>ScmListener</code>.
 *
 * @since 5.5
 */
public class SoapPermissionsCallbackImpl implements ScmPermissionsCallback {
    private static final Logger smLogger = Logger.getLogger(SoapPermissionsCallbackImpl.class);
    private String mWebAppInternalUrl = null;

    /**
     * Constructor.
     *
     * @param webAppInternalUrl The full url to the app server.
     */
    public SoapPermissionsCallbackImpl(String webAppInternalUrl) {
        this.mWebAppInternalUrl = webAppInternalUrl;
    }
    
    /**
     * @see com.vasoftware.sf.common.util.ScmPermissionsCallback#retrieveScmPermissions(String, String, String)
     */
    public String[] retrieveScmPermissions(String username, String systemId, String repositoryName) {
        SoapClientHelper soapHelper = null;
        String requestKey = DecryptLicenseUtil.createScmRequestKey();
        Object[] soapParams = new Object[] {requestKey, username, systemId, repositoryName};
        String[] perms = null;

        try {
            soapHelper = new SoapClientHelper(mWebAppInternalUrl + "/sf-soap/services/ScmListener");

            smLogger.info("ScmListener.getRolePaths(" + username + ", " + systemId + ", " + repositoryName + ")");

            perms = (String[])soapHelper.invoke("getRolePaths", soapParams);
        } catch (Exception e) {
            throw new RuntimeException(e); // Can happen if systemId and/or repositoryName are invalid or if
                                           // there is a server communication problem.
        }

        return perms;
    }
}
