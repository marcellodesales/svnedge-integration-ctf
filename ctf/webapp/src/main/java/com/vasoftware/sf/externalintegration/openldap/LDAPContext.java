package com.vasoftware.sf.externalintegration.openldap;

/**
 * data object encapsulating the ldap connection/schema related information
 * @author Richard Lee <rlee@vasoftware.com>
 */
public class LDAPContext {
    /** The DN of the container for users in ldap */
    private String mUserContainerDN;
    
    /** The DN of the container for groups in ldap */
    private String mGroupContainerDN;
    
    /** The ldap host */
    private String mBindHost;
    
    /** the ldap port */
    private int mBindPort;
    
    /** the login DN */
    private String mBindDN;
    
    /** the login DN's password */
    private String mBindPassword;

    /**
     * Get the DN to login and bind as
     * @return the bind dn as a string
     */
    public String getBindDN() {
	return mBindDN;
    }

    /**
     * Set the dn to bind as
     * @param bindDN the value to set the bind dn
     */
    public void setBindDN(String bindDN) {
	mBindDN = bindDN;
    }

    /**
     * Get the address of the ldap host
     * @return the ldap server's hostname as a String
     */
    public String getBindHost() {
	return mBindHost;
    }

    /**
     * Set the ldap host
     * @param bindHost the address of the ldap host
     */
    public void setBindHost(String bindHost) {
	mBindHost = bindHost;
    }

    /**
     * Get the clear text password used to login to the ldap server
     * @return the password as a string
     */
    public String getBindPassword() {
	return mBindPassword;
    }

    /**
     * Set the password used to login to the ldap server
     * @param bindPassword the password value
     */
    public void setBindPassword(String bindPassword) {
	mBindPassword = bindPassword;
    }

    /**
     * Get the port to connect to the ldap server
     * @return the port value
     */
    public int getBindPort() {
	return mBindPort;
    }

    /**
     * Set the port used for ldap connections
     * @param bindPort the port value
     */
    public void setBindPort(int bindPort) {
	mBindPort = bindPort;
    }

    /**
     * Get the ldap container's DN for creating/getting group entries
     * @return the group container's DN
     */
    public String getGroupContainerDN() {
	return mGroupContainerDN;
    }

    /**
     * Set the group container's dn
     * @param groupContainerDN the value to set
     */
    public void setGroupContainerDN(String groupContainerDN) {
	mGroupContainerDN = groupContainerDN;
    }

    /**
     * Get the ldap container's DN for creating/getting user entries
     * @return the user container's DN
     */
    public String getUserContainerDN() {
	return mUserContainerDN;
    }

    /**
     * Set the ldap container's DN for creating/getting user entries
     * @param userContainerDN the value tos et the user dn to.
     */
    public void setUserContainerDN(String userContainerDN) {
	mUserContainerDN = userContainerDN;
    }

}
