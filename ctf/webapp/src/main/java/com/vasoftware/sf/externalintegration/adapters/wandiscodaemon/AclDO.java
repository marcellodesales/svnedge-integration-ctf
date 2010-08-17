package com.vasoftware.sf.externalintegration.adapters.wandiscodaemon;

/**
 * Created by IntelliJ IDEA.
 * User: arosset
 * Date: Dec 19, 2006
 * Time: 10:45:46 AM
 * To change this template use File | Settings | File Templates.
 */
public class AclDO {
    String mAclId;
    boolean mAllow;
    String mPrivilege;
    String mUserGroupPattern;
    boolean mIsGroup;
    String mIPAddressPattern;
    String mFilePattern;
    String mBranch;

    /**
     * constructor
     * @param aclId the aclId
     * @param allow is it a allow rule or deny rule
     * @param privilege	the privilege
     * @param userGroupPattern the user or group pattern
     * @param isGroup	  true if it is a group
     * @param ipAddressPattern the ipaddress pattern
     * @param filePattern      the filePattern
     * @param branch the branchPattern
     */
    public AclDO(String aclId, boolean allow, String privilege, String userGroupPattern, boolean isGroup,
		 String ipAddressPattern, String filePattern, String branch) {
	this.mAclId = aclId;
	this.mAllow = allow;
	this.mPrivilege = privilege;
	this.mUserGroupPattern = userGroupPattern;
	this.mIsGroup = isGroup;
	this.mIPAddressPattern = ipAddressPattern;
	this.mFilePattern = filePattern;
	this.mBranch = branch;

    }

    /**
     * get the acl external id
     * @return the acl external id
     */
    public String getAclId() {
	return mAclId;
    }

    /**
     * returns the type of rule
     * @return the type of rule (true= allow rule, false = deny rule)
     */
    public boolean isAllow() {
	return mAllow;
    }

    /**
     * return the privilege
     * @return the privilege (=READ, WRITE, LIST)
     */
    public String getPrivilege() {
	return mPrivilege;
    }

    /**
     * returns the user group pattern
     * @return the user group pattern
     */
    public String getUserGroupPattern() {
	return mUserGroupPattern;
    }

    /**
     * returns wether this acl applies to a group or not
     * @return true if it an acl for a group
     */
    public boolean isIsGroup() {
	return mIsGroup;
    }

    /**
     * returns the IP address pattern
     * @return the IP address pattern
     */
    public String getIPAddressPattern() {
	return mIPAddressPattern;
    }

    /**
     * returns the file pattern
     * @return the file pattern
     */
    public String getFilePattern() {
	return mFilePattern;
    }

    /**
     * returns the branch pattern
     * @return the branch Patterm
     */
    public String getBranch() {
	return mBranch;
    }

}
