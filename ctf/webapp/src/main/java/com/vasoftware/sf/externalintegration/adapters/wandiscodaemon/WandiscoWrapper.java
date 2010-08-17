package com.vasoftware.sf.externalintegration.adapters.wandiscodaemon;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.rmi.RemoteException;

import javax.xml.rpc.ServiceException;
import javax.xml.rpc.holders.BooleanHolder;
import javax.xml.rpc.holders.StringHolder;

import org.apache.axis.AxisFault;
import org.apache.axis.client.Stub;

import com.vasoftware.sf.common.configuration.SfGlobalOptions;
import com.vasoftware.sf.common.configuration.SfGlobalOptionsManager;
import com.vasoftware.sf.externalintegration.IntegrationFault;
import com.vasoftware.sf.externalintegration.ObjectAlreadyExistsFault;
import com.vasoftware.sf.externalintegration.execution.ObjectNotFoundFault;
import com.vasoftware.sf.externalintegration.execution.UserAlreadyExistsFault;
import com.wandisco.webservices.scmapi_1_0.ACLManagement;
import com.wandisco.webservices.scmapi_1_0.ACLManagementService;
import com.wandisco.webservices.scmapi_1_0.ACLManagementServiceLocator;
import com.wandisco.webservices.scmapi_1_0.RepositoryManagement;
import com.wandisco.webservices.scmapi_1_0.RepositoryManagementService;
import com.wandisco.webservices.scmapi_1_0.RepositoryManagementServiceLocator;
import com.wandisco.webservices.scmapi_1_0.ScmType;
import com.wandisco.webservices.scmapi_1_0.SystemManagement;
import com.wandisco.webservices.scmapi_1_0.SystemManagementService;
import com.wandisco.webservices.scmapi_1_0.SystemManagementServiceLocator;
import com.wandisco.webservices.scmapi_1_0.UserGroupManagement;
import com.wandisco.webservices.scmapi_1_0.UserGroupManagementService;
import com.wandisco.webservices.scmapi_1_0.UserGroupManagementServiceLocator;


/**
 * Class which provides a clean looking wrapper around a bunch of soap calls to Wandisco.
 *
 * @author Helen Chen <hchen@vasoftware.com>
 * @author Wei Hsu <whsu@vasoftware.com>
 * @version $Revision: 1.28 $ $Date: 2007/01/17 00:07:58 $
 */
public class WandiscoWrapper {
    private static final char XML_LIST_DELIMITER = ' ';
    private static final String SPACE_PATTERN = "[\\s]+";

    private String mWandiscoHost;
    private String mWandiscoPort;

    private ACLManagement mWdAcl;
    private UserGroupManagement mWdUserGroup;
    private RepositoryManagement mWdRepository;
    private SystemManagement mWdSystem;
    private String mLoginToken;


    /**
     * Default constructor for CvsWrapper.
     *
     * @param propertyPrefix prefix for host and port property
     */
    public WandiscoWrapper(String propertyPrefix) {
	SfGlobalOptions options = SfGlobalOptionsManager.getOptions();
	mWandiscoHost = options.getOption(propertyPrefix + ".host");
	mWandiscoPort = options.getOption(propertyPrefix + ".port");
    }

    /**
     * Verify that we can initiate some sort of connection with the wandisco server via soap.
     *
     * @throws RemoteException if something goes wrong
     */
    public void verifyConnection() throws RemoteException {
	getSystemPort().getVersion(0);
    }

    /**
     * Create a group on the WD syste,
     *
     * @param groupName   the group name
     * @param description the description for that group
     * @throws IntegrationFault	 if something goes wrong
     * @throws ObjectAlreadyExistsFault if the user already exists
     */

    public void createGroup(String groupName, String description) throws IntegrationFault, ObjectAlreadyExistsFault {
	try {
	    int completionMessage = getUserGroupPort().createGroup(groupName, description, null);

	    if (completionMessage != 0) {
		throw new IntegrationFault("createGroup(" + groupName + ") failed");
	    }
	} catch (Throwable t) {
	    if (t instanceof AxisFault) {
		String faultString = ((AxisFault) t).getFaultString();
		if (faultString != null && faultString.indexOf("already exists") != -1) {
		    throw new ObjectAlreadyExistsFault(faultString);
		}
	    }
	    throw new IntegrationFault("createGroup(" + groupName + ") failed", t);
	}

    }

    /**
     * Create a group on the WD system,
     *
     * @param groupId  the group name
     * @param userList the list of users we want to add to this group
     * @throws IntegrationFault if something goes wrong
     */

    public void addUsersToGroup(String[] userList, String groupId) throws IntegrationFault {
	try {
	    //formatXMLList is used to workaround axis1.1 cannot format xs:list limitation.
	    int completionMessage = getUserGroupPort().addUsersToGroup(groupId, formatXMLList(userList));

	    if (completionMessage != 0) {
		throw new IntegrationFault("addUsersToGroup(" + groupId + ") failed");
	    }
	} catch (Throwable e) {
	    throw new IntegrationFault("addUsersToGroup(" + groupId + ") failed", e);
	}

    }

    /**
     * Set a user's password to a given value
     *
     * @param username	the username of the person whose password is to be changed
     * @param password	the value to set the password to.
     * @param isClearPassword if it is a clear password or not
     * @param repositoryPath  the repository path (used for cvs)
     * @throws IntegrationFault if something goes wrong
     */

    public void setPassword(String username, String password, boolean isClearPassword, String repositoryPath)
	    throws IntegrationFault {
	try {
	    int completionMessage = getUserGroupPort().setPassword(username, password, isClearPassword, repositoryPath);

	    if (completionMessage != 0) {
		throw new IntegrationFault("setPassword(" + username + ") failed");
	    }
	} catch (Throwable e) {
	    throw new IntegrationFault("setPassword(" + username + ") failed", e);
	}

    }

    /**
     * Remove the users from the given group
     *
     * @param groupId  the group name
     * @param userList the list of users we want to remove from this group
     * @throws IntegrationFault if something goes wrong
     */

    public void removeUsersFromGroup(String[] userList, String groupId) throws IntegrationFault {
	try {
	    //formatXMLList is used to workaround axis1.1 cannot format xs:list limitation.
	    int completionMessage = getUserGroupPort().deleteUsersGroupAssociation(groupId, formatXMLList(userList));

	    if (completionMessage != 0) {
		throw new IntegrationFault("removeUsersFromGroup(" + groupId + ") failed");
	    }
	} catch (Throwable t) {
	    throw new IntegrationFault("removeUsersFromGroup(" + groupId + ") failed", t);
	}
    }


    /**
     * Delete a user on the WD system,
     *
     * @param username the group name
     * @throws ObjectNotFoundFault if the user doesn't exist on WD system
     * @throws IntegrationFault    if something goes wrong
     */

    public void deleteUser(String username) throws IntegrationFault, ObjectNotFoundFault {
	try {
	    int completionMessage = getUserGroupPort().deleteUser(username, true);
	    if (completionMessage != 0) {
		throw new IntegrationFault("deleteUser(" + username + ") failed");
	    }
	} catch (Throwable t) {
	    if (t instanceof AxisFault) {
		String faultString = ((AxisFault) t).getFaultString();
		if (faultString != null && (faultString.indexOf("does not exist") != -1 ||
			faultString.indexOf("has already been deleted") != -1)) {
		    throw new ObjectNotFoundFault(username);
		}
	    }
	    throw new IntegrationFault("deleteUser(" + username + ") failed", t);
	}
    }


    /**
     * Delete all the users on the WD system,
     *
     * @throws IntegrationFault if something goes wrong
     */

    public void deleteAllUsers() throws IntegrationFault {
	try {
	     //flag sets to true for subversion will delete the user from htpasswd

	    int completionMessage = getUserGroupPort().deleteAllUsers(true);
	    if (completionMessage != 0) {
		throw new IntegrationFault("deleteAllUsers failed");
	    }
	} catch (RemoteException t) {
	    throw new IntegrationFault("deleteAllUsers failed", t);
	}
    }


    /**
     * Makes the given group contain exactly the usernames include in usernames
     *
     * @param groupId  the group name
     * @param userList the list of users we want to add to this group
     * @throws IntegrationFault if something goes wrong
     */

    public void modifyUsersGroupAssociation(String[] userList, String groupId)
	    throws IntegrationFault {
	try {
	    // we are deleting all the exiting users in the group
	    String [] existingUserList = listGroupMembers(groupId);

	    if (existingUserList != null) {
		removeUsersFromGroup(existingUserList, groupId);
	    }

	    if (userList != null && userList.length > 0) {
		// now adding the requested userList to the group
		//formatXMLList is used to workaround axis1.1 cannot format xs:list limitation.
		int completionMessage = getUserGroupPort().addUsersToGroup(groupId, formatXMLList(userList));

		if (completionMessage != 0) {
		    throw new IntegrationFault("modifyUsersGroupAssociation(" + groupId + ") failed");
		}
	    }
	} catch (RemoteException e) {
	    throw new IntegrationFault("modifyUsersGroupAssociation(" + groupId + ") failed", e);
	}
    }

    /**
     * Create an ACL on the WD system
     *
     * @param allow	    is it a allow rule or deny rule
     * @param privilege	the privilege
     * @param userGroupPattern the user or group pattern
     * @param filePattern      the filePattern
     * @param isGroup	  true if it is a group
     * @param aclName	  the external id for the ACL
     * @throws IntegrationFault if something goes wrong
     */
    public void createACLIfNotExist(boolean allow, String privilege, String userGroupPattern, String filePattern,
				    boolean isGroup, String aclName)
	    throws IntegrationFault {
	try {
	    createACL(allow, privilege, userGroupPattern, filePattern, isGroup, aclName);
	} catch (ObjectAlreadyExistsFault e) {
	    //do nothing
	}
    }

    /**
     * Create an ACL on the WD system
     *
     * @param allow	    is it a allow rule or deny rule
     * @param privilege	the privilege
     * @param userGroupPattern the user or group pattern
     * @param filePattern      the filePattern
     * @param isGroup	  true if it is a group
     * @param aclName	  the external id for the ACL
     * @throws ObjectAlreadyExistsFault if the acl already exists
     * @throws IntegrationFault	 if something goes wrong
     */

    public void createACL(boolean allow, String privilege, String userGroupPattern, String filePattern,
			  boolean isGroup, String aclName)
	    throws IntegrationFault, ObjectAlreadyExistsFault {
	try {
	    int completionMessage = getAclPort().modifyACL(allow, privilege, userGroupPattern, isGroup, ".*",
		    filePattern, ".*", aclName);
	    if (completionMessage != 0) {
		throw new IntegrationFault("createACL(" + aclName + ") failed");
	    }
	} catch (Throwable t) {
	    if (t instanceof AxisFault) {
		String faultString = ((AxisFault) t).getFaultString();
		if (faultString != null && (faultString.indexOf("already exists") != -1 ||
			faultString.indexOf("ignoring duplicate") != -1)) {
		    throw new ObjectAlreadyExistsFault(aclName);
		}
	    }
	    throw new IntegrationFault("createACL(" + aclName + ") failed", t);
	}
    }

    /**
     * list the users of a WD group
     *
     * @param groupId the group we want to query
     * @return the users
     * @throws ObjectNotFoundFault if the group doesn't exist
     * @throws IntegrationFault    if something goes wrong
     */

    public String[] listGroupMembers(String groupId) throws IntegrationFault, ObjectNotFoundFault {
	try {
	    // parseXMLList is used to workaround axis1.1 cannot parse xs:list limitation.
	    return parseXMLList(getUserGroupPort().queryUsers(groupId));
	} catch (Throwable t) {
	    if (t instanceof AxisFault) {
		String faultString = ((AxisFault) t).getFaultString();
		if (faultString != null &&
			(faultString.indexOf("does not exist") != -1)) {
		    throw new ObjectNotFoundFault(groupId);
		}
	    }
	    throw new IntegrationFault("listGroupMembers(" + groupId + ") failed", t);
	}
    }

    /**
     * Create a group on the WD syste,
     *
     * @param repositoryPath repository folderpath which has the repository name
     * @param scmType	SCM type (either ScmType.CVS or ScmType.SVN_HTTP)
     * @param logicalPath    the logical path (used for svn)
     * @throws IntegrationFault if something goes wrong
     */

    public void createRepository(String repositoryPath, ScmType scmType, String logicalPath)
	    throws IntegrationFault {
	int returnCode;
	File f = new File(repositoryPath);
	try {
	    // If owner is set to null, WD will use default user
	    returnCode = getRepositoryPort().createRepository(f.getParent(), logicalPath, f.getName(), scmType, null);
	    if (returnCode != 0) {
		throw new IntegrationFault("createRepository(" + repositoryPath + ") failed");
	    }
	} catch (Throwable t) {
	    throw new IntegrationFault("createRepository(" + repositoryPath + ") failed", t);
	}
    }

    /**
     * Create  users on the WD system,
     *
     * @param userIds     the list of users
     * @throws IntegrationFault if something goes wrong
     */

    public void createUsers(String[] userIds) throws IntegrationFault {
	try {
	    for (int i = 0; i < userIds.length; i++) {
		int completionMessage = getUserGroupPort().createUser(userIds[i], null, null, null);
		if (completionMessage != 0) {
		    throw new IntegrationFault("createUsers(" + userIds + ") failed");
		}
	    }
	} catch (RemoteException e) {
	    throw new IntegrationFault("createUsers(" + userIds + ") failed", e);
	}

    }


    /**
     * Create a user on the WD system,
     *
     * @param userId      the group name
     * @param addToSystem : whether we should create an accoutn for the user
     * @throws UserAlreadyExistsFault if the user already exists
     * @throws IntegrationFault       if something goes wrong
     */

    public void createUser(String userId, boolean addToSystem) throws IntegrationFault, UserAlreadyExistsFault {
	try {
	    int completionMessage = getUserGroupPort().createUser(userId, null, null, null);
	    if (completionMessage != 0) {
		throw new IntegrationFault("createUser(" + userId + ") failed");
	    }
	} catch (Throwable t) {
	    if (t instanceof AxisFault) {
		String faultString = ((AxisFault) t).getFaultString();
		if (faultString != null && faultString.indexOf("already exists") != -1) {
		    throw new UserAlreadyExistsFault(faultString);
		}
	    }
	    throw new IntegrationFault("createUser(" + userId + ") failed", t);
	}

    }

    /**
     * Finds out if a user exists on WD system
     *
     * @param userId the group name
     * @return true if the user exists on WD system
     * @throws IntegrationFault if something goes wrong
     */

    public boolean userExists(String userId) throws IntegrationFault {
	try {
	    return getUserGroupPort().userExists(userId);
	} catch (Throwable t) {
	    throw new IntegrationFault("userExists(" + userId + ") failed", t);
	}
    }

    /**
     * Finds out if a group exists on WD system
     *
     * @param groupId the group name
     * @return true if the user exists on WD system
     * @throws IntegrationFault if something goes wrong
     */

    public boolean groupExists(String groupId) throws IntegrationFault {
	try {
	    return getUserGroupPort().groupExists(groupId);
	} catch (Throwable t) {
	    throw new IntegrationFault("groupExists(" + groupId + ") failed", t);
	}
    }

    /**
     * Create an authorized_keys file in the user's home directory.
     *
     * @param username       the username to create the file in
     * @param authorizedKeys the authorized keys file contents
     * @throws IntegrationFault thrown if the operation fails.
     */
    public void createAuthorizedKeysFile(String username, String authorizedKeys) throws IntegrationFault {
	try {
	    getUserGroupPort().setSSHAuthorizedKeys(username, authorizedKeys);
	} catch (Throwable t) {
	    throw new IntegrationFault("createAuthorizedKeysFile(" + username + ") failed", t);
	}
    }

    /**
     * Archive the repository
     *
     * @param repositoryPath the root path for the repository
     * @param archiveRoot    the archive root path for the repository
     * @throws IntegrationFault if any errors
     */
    public void archiveRepository(String repositoryPath, String archiveRoot) throws IntegrationFault {
	try {
	    int completionStatus = getRepositoryPort().archiveRepository(repositoryPath, archiveRoot);
	    if (completionStatus != 0) {
		throw new IntegrationFault("archiveRepository(" + repositoryPath + ") failed");
	    }
	} catch (Throwable t) {
	    throw new IntegrationFault("archiveRepository(" + repositoryPath + ") failed", t);
	}
    }

    /**
     * Delete a group
     *
     * @param groupName the group name
     * @throws ObjectNotFoundFault if the group doesn't exist on WD system
     * @throws IntegrationFault    if any errors
     */
    public void deleteGroup(String groupName) throws IntegrationFault, ObjectNotFoundFault {
	try {
	    int completionStatus = getUserGroupPort().deleteGroup(groupName);
	    if (completionStatus != 0) {
		throw new IntegrationFault("deleteGroup(" + groupName + ") failed");
	    }
	} catch (Throwable t) {
	    if (t instanceof AxisFault) {
		String faultString = ((AxisFault) t).getFaultString();
		if (faultString != null && (faultString.indexOf("does not exist") != -1 ||
			faultString.indexOf("is already deleted") != -1)) {
		    throw new ObjectNotFoundFault(groupName);
		}
	    }
	    throw new IntegrationFault("deleteGroup(" + groupName + ") failed", t);
	}
    }

    /**
     * Delete an ACL
     *
     * @param aclId the aclId we want to delete
     * @throws ObjectNotFoundFault if the user doesn't exist on WD system
     * @throws IntegrationFault    if any errors
     */
    public void deleteACL(String aclId) throws IntegrationFault, ObjectNotFoundFault {
	try {
	    int completionStatus = getAclPort().deleteACL(aclId);
	    if (completionStatus != 0) {
		throw new IntegrationFault("deleteAcl(" + aclId + ") failed");
	    }
	} catch (Throwable t) {
	    if (t instanceof AxisFault) {
		String faultString = ((AxisFault) t).getFaultString();
		if (faultString != null && faultString.indexOf("does not exist") != -1) {
		    throw new ObjectNotFoundFault(aclId);
		}
	    }
	    throw new IntegrationFault("deleteAcl(" + aclId + ") failed", t);
	}
    }

    /**
     * Queries an ACL based on its external id
     *
     * @param aclId the aclId we want to query
     * @return the aclDO found
     * @throws ObjectNotFoundFault if the acl doesn't exist on WD system
     * @throws IntegrationFault    if any errors
     */
    public AclDO queryACL(String aclId) throws IntegrationFault, ObjectNotFoundFault {
	BooleanHolder allow = new BooleanHolder();
	StringHolder privilege = new StringHolder();
	StringHolder userGroupPattern = new StringHolder();
	BooleanHolder isGroup = new BooleanHolder();
	StringHolder filePattern = new StringHolder();
	StringHolder IPAddressPattern = new StringHolder();
	StringHolder branch = new StringHolder();
	try {
	    getAclPort().queryACL(new StringHolder(aclId), allow, privilege, userGroupPattern, isGroup,
		    IPAddressPattern, filePattern, branch);
	    return new AclDO(aclId, allow.value, privilege.value, userGroupPattern.value, isGroup.value,
		    IPAddressPattern.value, filePattern.value, branch.value);

	} catch (Throwable t) {
	    if (t instanceof AxisFault) {
		String faultString = ((AxisFault) t).getFaultString();
		if (faultString != null && faultString.indexOf("does not exist") != -1) {
		    throw new ObjectNotFoundFault(aclId);
		}
	    }
	    throw new IntegrationFault("queryACL(" + aclId + ") failed", t);
	}
    }

    /**
     * Finds out if a user has permissions or not
     *
     * @param userId    the group name
     * @param privilege the privilege ie READ, WRITE
     * @param files     the list of files for which we are checking
     * @param tag       the list of tags for which we are checking
     * @return true if the user exists on WD system
     * @throws IntegrationFault if something goes wrong
     */

    public boolean hasPermissions(String userId, String privilege, String[] files, String[] tag)
	    throws IntegrationFault {
	try {
	    return getAclPort().isAccessAllowed(userId, ".*", privilege, formatXMLList(files), formatXMLList(tag));
	} catch (Throwable t) {
	    throw new IntegrationFault("hasPermissions for user(" + userId + ") failed", t);
	}
    }

    /**
     * Gets the port for the ACLManagement.
     *
     * @return the ACL management port
     * @throws RemoteException if the port cannot be created
     */
    private ACLManagement getAclPort() throws RemoteException {
	if (mWdAcl == null) {
	    try {
		URL portAddress = getServiceUrl("/soap/scm/aclmanagement");
		ACLManagementService wdAclService = new ACLManagementServiceLocator();
		try {
		    mWdAcl = (ACLManagement) configureStub((Stub) wdAclService.getACLManagement(portAddress), doLogin());
		} catch (ServiceException e) {
		    throw new RemoteException("Failed to connect to " + portAddress, e);
		}
	    } catch (MalformedURLException e) {
		throw new RemoteException("Malformed URL exception", e);
	    }
	}
	return mWdAcl;
    }

    /**
     * Gets the port for the ACLManagement.
     *
     * @return the ACL port
     * @throws RemoteException if the port cannot be created
     */
    private UserGroupManagement getUserGroupPort() throws RemoteException {
	if (mWdUserGroup == null) {
	    try {
		URL portAddress = getServiceUrl("/soap/scm/usergroupmanagement");
		UserGroupManagementService wdAclService = new UserGroupManagementServiceLocator();
		try {
		    mWdUserGroup = (UserGroupManagement)
			    configureStub((Stub) wdAclService.getUserGroupManagement(portAddress), doLogin());
		} catch (ServiceException e) {
		    throw new RemoteException("Failed to connect to " + portAddress, e);
		}
	    } catch (MalformedURLException e) {
		throw new RemoteException("Malformed URL exception", e);
	    }
	}
	return mWdUserGroup;
    }

    /**
     * Get the list of groups to which a user belongs to
     *
     * @param username the user name
     * @return the list of groups to which this user belongs
     * @throws IntegrationFault if something goes wrong
     */

    public String[] queryGroups(String username) throws IntegrationFault {
	try {
	    return parseXMLList(getUserGroupPort().queryGroups(username));
	} catch (Throwable t) {
	    throw new IntegrationFault("queryGroups(" + username + ") failed", t);
	}
    }


    /**
     * Gets the port for the RepositoryManagement.
     *
     * @return the repository management port
     * @throws RemoteException if the port cannot be created
     */
    private RepositoryManagement getRepositoryPort() throws RemoteException {
	if (mWdRepository == null) {
	    try {
		URL portAddress = getServiceUrl("/soap/scm/repositorymanagement");
		RepositoryManagementService wdRepositoryService = new RepositoryManagementServiceLocator();
		try {
		    mWdRepository = (RepositoryManagement) configureStub((Stub) wdRepositoryService.
			    getrepositoryManagement(portAddress), doLogin());
		} catch (ServiceException e) {
		    throw new RemoteException("Failed to connect to " + portAddress, e);
		}
	    } catch (MalformedURLException e) {
		throw new RemoteException("Malformed URL exception", e);
	    }
	}

	return mWdRepository;
    }

    /**
     * Gets the port for the SystemManagement.
     *
     * @return the system management port
     * @throws RemoteException if the port cannot be created
     */
    private SystemManagement getSystemPort() throws RemoteException {
	if (mWdSystem == null) {
	    try {
		URL portAddress = getServiceUrl("/soap/scm/systemmanagement");
		SystemManagementService wdSystemService = new SystemManagementServiceLocator();
		try {
		    mWdSystem = wdSystemService.getSystemManagement(portAddress);
		} catch (ServiceException e) {
		    throw new RemoteException("Failed to connect to " + portAddress, e);
		}
		((Stub) mWdSystem)._setProperty(org.apache.axis.AxisEngine.PROP_DOMULTIREFS, Boolean.FALSE);
	    } catch (MalformedURLException e) {
		throw new RemoteException("Malformed URL exception", e);
	    }
	}
	return mWdSystem;

    }

    /**
     * Configure the stub.
     *
     * @param stub       the stub to be configured
     * @param loginToken the loginToken
     * @return the configured stub
     */
    private Stub configureStub(Stub stub, String loginToken) {
	// TODO figure out if this is necessary
	// Multirefs support in Axis 1.4 is incomplete, SOAP request does not validate with schema with multirefs,
	// so disable
	stub._setProperty(org.apache.axis.AxisEngine.PROP_DOMULTIREFS, Boolean.FALSE);

	// Need the security header to go past the WANdisco authorization check, normally would obtain a
	// loginNonce by invoking loginAsAdmin
	stub.setHeader("http://http://webservices.wandisco.com/headers", "wandiscosecurityheader", loginToken);
	return stub;
    }

    /**
     * Creates the URL based on the host and port and location
     *
     * @param location the location of the service
     * @return url
     * @throws MalformedURLException if the url is mal formed
     */
    private URL getServiceUrl(String location) throws MalformedURLException {

	return new URL(new StringBuffer().append("http://").append(mWandiscoHost).append(":").append(mWandiscoPort).
		append(location).toString());
    }

    /**
     * This operation is used to login as an administrator in a WANdisco node
     *
     * @return the loginToken
     * @throws RemoteException if the url is mal formed
     */
    private String doLogin() throws RemoteException {
	if (mLoginToken == null) {
	    mLoginToken = getSystemPort().loginAsAdmin("root", "wandisco");
	}
	return mLoginToken;

    }

    /**
     * Convert the string array to XML list, which is a string separated by delimiter.
     * TODO: this method is used to workaround the axis1.1 limitation for handling "xs:list".
     * If we move to axis1.4, this method is not required.
     *
     * @param vals the string array
     * @return xml list
     */
    private static String formatXMLList(String [] vals) {
	StringBuffer stringBuffer = new StringBuffer();

	for (int i = 0; i < vals.length; i++) {
	    String val = vals[i];

	    if (val != null && val.length() > 0) {
		if (stringBuffer.length() > 0) {
		    stringBuffer.append(XML_LIST_DELIMITER);
		}
		stringBuffer.append(val);
	    }
	}

	return stringBuffer.toString();
    }

    /**
     * Convert the XML list string to string array.
     * TODO: this method is used to workaround the axis1.1 limitation for handling "xs:list".
     * If we move to axis1.4, this method is not required.
     *
     * @param value the xml list string
     * @return string array
     */
    private static String [] parseXMLList(String value) {
	if (value == null || value.equals("")) {
	    return null;
	}
	return value.split(SPACE_PATTERN);
    }


}
