package com.vasoftware.sf.externalintegration.openldap;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import com.novell.ldap.LDAPAttribute;
import com.novell.ldap.LDAPAttributeSet;
import com.novell.ldap.LDAPConnection;
import com.novell.ldap.LDAPEntry;
import com.novell.ldap.LDAPException;
import com.novell.ldap.LDAPModification;
import com.novell.ldap.LDAPSearchConstraints;
import com.novell.ldap.LDAPSearchResults;

/**
 * Class which encapsulates the OpenLDAP calls through novell's JLDAP libraries.
 */
public class LDAPUtil {
    /** The well known port of an ldap service */
    public static final int LDAP_PORT = LDAPConnection.DEFAULT_PORT;
    /** The version of ldap that this implementation uses */
    public static final int LDAP_VERSION = LDAPConnection.LDAP_V3;
    /** The attributes to get when looking up a user */
    private static final String[] USER_ATTRIBS = new String[] { "uid", "userPassword", "uidNumber", "gidNumber",
                                                               "homeDirectory", "loginShell" };
    /** The attributes to get when looking up a group */
    private static final String[] GROUP_ATTRIBS = new String[] { "gid", "gidNumber" };

    /**
     * Characters used for base64 encoding
     */
    private static final String BASE64_CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";

    /**
     * Base64 encode a bunch of bytes
     * 
     * @param message
     *            the base64 encoded version of the bytes
     * @return the base64 message as a string
     */
    public static String base64(final byte[] message) {
        final ByteBuffer buffer = ByteBuffer.wrap(message);
        final StringBuffer result = new StringBuffer();
        while (buffer.remaining() > 0) {
            int triplet = 0;
            int count = 4;
            if (buffer.remaining() == 1) {
                final int last = 0xFF & buffer.get();
                triplet = last << 16;
                count = 2;
            } else if (buffer.remaining() == 2) {
                final int lastTwo = 0xFFFF & buffer.getShort();
                triplet = lastTwo << 8;
                count = 3;
            } else {
                final int firstTwo = 0xFFFF & buffer.getShort();
                final int third = 0xFF & buffer.get();
                triplet = (firstTwo << 8) + third;
            }

            for (int i = 0; i < count; i++) {
                final int shifts = (3 - i) * 6;
                final int mask = 0x3F << shifts;
                final int offset = (triplet & mask) >> shifts;
                result.append(BASE64_CHARACTERS.charAt(offset));
            }
        }

        switch (message.length % 3) {
            case 2:
                result.append("=");
                break;
            case 1:
                result.append("==");
                break;
            default: // nothing
        }

        return result.toString();
    }

    /**
     * Get a connection to the LDAP server given an LDAPContext object
     * 
     * @param context
     *            the ldap context containing the host/bind information
     * @return a bound and connected ldap connection
     * @throws LDAPException
     *             thrown if either the connection or the binding fail
     */
    private static LDAPConnection getConnection(final LDAPContext context) throws LDAPException {
        final LDAPConnection result = new LDAPConnection();
        result.connect(context.getBindHost(), context.getBindPort());
        result.bind(LDAP_VERSION, context.getBindDN(), context.getBindPassword().getBytes());

        return result;
    }

    /**
     * Add an entry to the ldap implementation with a specific dn and map of attributes. If you want an attribute to
     * have multiple values, then that attribute's map entry should contain an array of values, where each array element
     * corresponds to a possible value of the entry.
     * 
     * @param context
     *            the context to bind to
     * @param dn
     *            the dn to create
     * @param attributes
     *            the attributes to put on the entry
     * @throws LDAPException
     *             thrown if the entry already exists, or some other problem has occured
     */
    @SuppressWarnings("unchecked")
    private static void addEntry(final LDAPContext context, final String dn, final Map attributes) throws LDAPException {
        final LDAPAttributeSet attributeSet = new LDAPAttributeSet();

        for (final Iterator iterator = attributes.keySet().iterator(); iterator.hasNext();) {
            final Object key = iterator.next();
            final Object value = attributes.get(key);

            if (value == null) {
                continue; // don't add null attributes
            }

            LDAPAttribute attribute;

            if (value instanceof String[]) {
                attribute = new LDAPAttribute(key.toString(), (String[]) value);
            } else {
                attribute = new LDAPAttribute(key.toString(), value.toString());
            }

            attributeSet.add(attribute);
        }

        final LDAPEntry entry = new LDAPEntry(dn, attributeSet);
        final LDAPConnection connection = getConnection(context);

        connection.add(entry);
        connection.disconnect();
    }

    // private static void performBulkOperation(LDAPContext context, LDAPMessage[] requests)
    // throws LDAPException {
    // ArrayList operationList = new ArrayList();
    // for (int i = 0; i < requests.length; i++) {
    // LDAPLburpRequest request = new LDAPLburpRequest(requests[i]);
    // operationList.add(request);
    // }
    //	
    // LDAPLburpRequest[] operationArray = (LDAPLburpRequest[]) operationList.toArray(new LDAPLburpRequest[]{});
    // LburpOperationRequest bulkRequest = new LburpOperationRequest(operationArray, 0);
    //	
    // LDAPConnection connection = getConnection(context);
    // connection.extendedOperation(bulkRequest);
    // connection.disconnect();
    // }

    /**
     * Modify an entry by performing a specified operation on the entry's attributes.
     * 
     * @param context
     *            the context with connect information
     * @param operation
     *            operation to perform, see LDAPModification for constants
     * @param dn
     *            the dn for the object to modify
     * @param attributes
     *            the attributes to operate on
     * @throws LDAPException
     *             thrown if operation is illegal, or if connection fails
     */
    @SuppressWarnings("unchecked")
    private static void modifyAttributes(final LDAPContext context, final int operation, final String dn,
                                         final Map attributes) throws LDAPException {
        final ArrayList<LDAPModification> modificationList = new ArrayList<LDAPModification>();

        for (final Iterator iterator = attributes.keySet().iterator(); iterator.hasNext();) {
            final Object key = iterator.next();
            final Object value = attributes.get(key);

            if (value == null) {
                continue; // don't add null attributes
            }

            LDAPAttribute attribute;

            if (value instanceof String[]) {
                attribute = new LDAPAttribute(key.toString(), (String[]) value);
            } else {
                attribute = new LDAPAttribute(key.toString(), value.toString());
            }

            modificationList.add(new LDAPModification(operation, attribute));
        }

        final LDAPModification[] modificationArray = modificationList.toArray(new LDAPModification[] {});

        final LDAPConnection connection = getConnection(context);
        connection.modify(dn, modificationArray);
        connection.disconnect();
    }

    // private static void addAttributes(LDAPContext context, String dn, Map attributes) throws LDAPException {
    // modifyAttributes(context, LDAPModification.ADD, dn, attributes);
    // }

    /**
     * Replace existing attributes on an entry.
     * 
     * @param context
     *            the ldap context with connectivity info
     * @param dn
     *            the dn of the entry to modify
     * @param attributes
     *            the attributes to replace
     * @throws LDAPException
     *             thrown if operation was illegal or connection failed.
     */
    @SuppressWarnings("unchecked")
    private static void replaceAttributes(final LDAPContext context, final String dn, final Map attributes)
                                                                                                           throws LDAPException {
        modifyAttributes(context, LDAPModification.REPLACE, dn, attributes);
    }

    // private static void deleteAttributes(LDAPContext context, String dn, Map attributes) throws LDAPException {
    // modifyAttributes(context, LDAPModification.DELETE, dn, attributes);
    // }

    /**
     * Given a context, return the DN for a user's uid
     * 
     * @param context
     *            the ldap context
     * @param uid
     *            the uid whose dn is needed
     * @return the user's dn as a string
     */
    private static String getUserDN(final LDAPContext context, final String uid) {
        return "uid=" + uid + ", " + context.getUserContainerDN();
    }

    /**
     * Given a context, return the DN for a group's gid
     * 
     * @param context
     *            the ldap context
     * @param gid
     *            the gid whose dn is needed
     * @return the group's dn as a string
     */
    private static String getGroupDN(final LDAPContext context, final String gid) {
        return "cn=" + gid + ", " + context.getGroupContainerDN();
    }

    /**
     * Return an LDAP Object's attributes
     * 
     * @param context
     *            the ldap connection context
     * @param dn
     *            the dn to retrieve
     * @param attribs
     *            the attributes of the object to retrieve
     * @return a Map containing the attributes of the object.
     * @throws LDAPException
     *             thrown if object/attrib does not exist, bad permission, or if the connection failed.
     */
    @SuppressWarnings("unchecked")
    private static Map<String, String[]> getEntry(final LDAPContext context, final String dn, final String[] attribs)
                                                                                                                     throws LDAPException {
        final LDAPConnection connection = getConnection(context);
        final LDAPEntry entry = connection.read(dn, attribs);

        connection.disconnect();

        final Map<String, String[]> result = new TreeMap<String, String[]>();

        for (final Iterator iterator = entry.getAttributeSet().iterator(); iterator.hasNext();) {
            final LDAPAttribute ldapAttribute = (LDAPAttribute) iterator.next();

            result.put(ldapAttribute.getName(), ldapAttribute.getStringValueArray());
        }

        return result;
    }

    /**
     * Search for objects in ldap given a context, searchdn, and filters. Return a two dimensional array of entries
     * where each element in the first dimension is an array containing the attributes specified by the attributes
     * variable.
     * 
     * @param context
     *            the ldap connection context
     * @param searchDn
     *            the dn to perform the search on
     * @param filter
     *            the filter
     * @param attributes
     *            which attributes to retrieve
     * @return an array of array of attributes
     * @throws LDAPException
     *             thrown if searching ldap fails.
     */
    private static String[][][] search(final LDAPContext context, final String searchDn, final String filter,
                                       final String[] attributes) throws LDAPException {
        final LDAPConnection connection = getConnection(context);
        final LDAPSearchConstraints constraints = new LDAPSearchConstraints();

        constraints.setMaxResults(0); // don't limit the number of values

        final LDAPSearchResults searchResult = connection.search(searchDn, LDAPConnection.SCOPE_SUB, filter,
                                                                 attributes, false, constraints);
        final ArrayList<String[][]> tupleList = new ArrayList<String[][]>();

        while (searchResult.hasMore()) {
            final LDAPEntry entry = searchResult.next();
            final String[][] attributeValues = new String[attributes.length][];

            for (int i = 0; i < attributes.length; i++) {
                final LDAPAttribute attribute = entry.getAttribute(attributes[i]);

                attributeValues[i] = attribute.getStringValueArray();
            }

            tupleList.add(attributeValues);
        }

        return tupleList.toArray(new String[][][] {});
    }

    /**
     * List all usernames in the ldap database
     * 
     * @param context
     *            ldap database context
     * @return an array of all usernames known in ldap
     * @throws LDAPException
     *             thrown on error.
     */
    public static String[] listAllUsers(final LDAPContext context) throws LDAPException {
        final String[][][] userUidValues = search(context, context.getUserContainerDN(), "(objectClass=posixAccount)",
                                                  new String[] { "uid" });

        final String[] result = new String[userUidValues.length];
        for (int i = 0; i < result.length; i++) {
            result[i] = userUidValues[i][0][0]; // There should be exactly one username value per tuple
        }

        return result;
    }

    /**
     * Given a list of usernames, return an array of login shells for each user. if the user does not exist in ldap,
     * null is returned in the corresponding array entry.
     * 
     * @param context
     *            ldap database context
     * @param usernames
     *            array of usernames to lookup the login shell for.
     * @return an array of all usernames known in ldap
     * @throws LDAPException
     *             thrown on error.
     */
    public static String[] listLoginShellByUser(final LDAPContext context, final String[] usernames)
                                                                                                    throws LDAPException {
        final String[][][] userUidValues = search(context, context.getUserContainerDN(), "(objectClass=posixAccount)",
                                                  new String[] { "uid", "loginShell" });
        final TreeMap<String, String> userNameToShell = new TreeMap<String, String>();

        for (int i = 0; i < userUidValues.length; i++) {
            userNameToShell.put(userUidValues[i][0][0], userUidValues[i][1][0]);
        }

        final String[] result = new String[usernames.length];
        for (int i = 0; i < result.length; i++) {
            // We have to tolower this because all usernames coming back will be lowercase
            result[i] = userNameToShell.get(usernames[i].toLowerCase());
        }

        return result;
    }

    /**
     * Remove an LDAP object
     * 
     * @param context
     *            the context containing connection information
     * @param dn
     *            the dn of the object to remove
     * @throws LDAPException
     *             thrown if connection fails, object doesn't exist, or bad permission
     */
    public static void deleteEntry(final LDAPContext context, final String dn) throws LDAPException {
        final LDAPConnection connection = getConnection(context);
        connection.delete(dn);
        connection.disconnect();
    }

    /**
     * Add a new user object to the ldap database.
     * 
     * @param context
     *            ldap context holding connect info
     * @param uid
     *            the username of the user being created
     * @param passwordCrypted
     *            the crypted password for the user being created
     * @param shell
     *            the user's login shell
     * @param home
     *            the user's home directory
     * @param uidNumber
     *            the numeric id for the user
     * @param gidNumber
     *            the numeric group id of the user's primary group
     * @throws LDAPException
     *             thrown on error
     */
    @SuppressWarnings("unchecked")
    public static void addUser(final LDAPContext context, final String uid, final String passwordCrypted,
                               final String shell, final String home, final int uidNumber, final int gidNumber)
                                                                                                               throws LDAPException {
        final TreeMap attributes = new TreeMap();

        final String uidLowerCase = uid.toLowerCase();

        attributes.put("objectClass", new String[] { "account", "posixAccount" });
        attributes.put("cn", uidLowerCase);
        attributes.put("userPassword", "{crypt}" + passwordCrypted);
        attributes.put("uid", uidLowerCase);
        attributes.put("uidNumber", Integer.toString(uidNumber));
        attributes.put("gidNumber", Integer.toString(gidNumber));
        attributes.put("homeDirectory", home);
        attributes.put("loginShell", shell);

        final String dn = getUserDN(context, uidLowerCase);

        addEntry(context, dn, attributes);
    }

    /**
     * Return a user's data from LDAP for a specific user's username
     * 
     * @param context
     *            the ldap context
     * @param uid
     *            the username of the user to retrieve
     * @return A map of the ldap entry's attributes for the username
     * @throws LDAPException
     *             thrown on error
     */
    public static Map<String, String[]> getUser(final LDAPContext context, final String uid) throws LDAPException {
        final String dn = getUserDN(context, uid.toLowerCase());

        return getEntry(context, dn, USER_ATTRIBS);
    }

    /**
     * Update a user's record in ldap
     * 
     * @param context
     *            the ldap context
     * @param uid
     *            the user's username
     * @param passwordCrypted
     *            the password value for the user
     * @param shell
     *            the user's login shell
     * @param home
     *            the user's home directory
     * @param uidNumber
     *            the user's numeric id
     * @param gidNumber
     *            the user's primary group's numeric id
     * @throws LDAPException
     *             thrown on error
     */
    public static void modifyUser(final LDAPContext context, final String uid, final String passwordCrypted,
                                  final String shell, final String home, final int uidNumber, final int gidNumber)
                                                                                                                  throws LDAPException {
        final TreeMap<String, String> attributes = new TreeMap<String, String>();

        final String uidLowerCase = uid.toLowerCase();

        attributes.put("objectClass", "posixAccount");
        attributes.put("cn", uidLowerCase);
        if (passwordCrypted != null) {
            attributes.put("userPassword", "{crypt}" + passwordCrypted);
        }
        attributes.put("uid", uidLowerCase);
        attributes.put("uidNumber", Integer.toString(uidNumber));
        attributes.put("gidNumber", Integer.toString(gidNumber));
        attributes.put("homeDirectory", home);
        attributes.put("loginShell", shell);

        final String dn = getUserDN(context, uidLowerCase);

        replaceAttributes(context, dn, attributes);
    }

    /**
     * Set only a user's password.
     * 
     * @param context
     *            the ldap context
     * @param uid
     *            the user's username
     * @param passwordCrypted
     *            the crypt hash of password to set
     * @throws LDAPException
     *             thrown on error
     */
    public static void setUserPasswordCrypt(final LDAPContext context, final String uid, final String passwordCrypted)
                                                                                                                      throws LDAPException {
        if (passwordCrypted == null) {
            return;
        }

        final String dn = getUserDN(context, uid.toLowerCase());
        final TreeMap<String, String> attributes = new TreeMap<String, String>();

        attributes.put("userPassword", "{crypt}" + passwordCrypted);
        replaceAttributes(context, dn, attributes);
    }

    /**
     * Set the user's login shell
     * 
     * @param context
     *            the ldap context
     * @param uid
     *            the user's username
     * @param loginShell
     *            the login shell the user should have
     * @throws LDAPException
     *             thrown on error
     */
    public static void setUserShell(final LDAPContext context, final String uid, final String loginShell)
                                                                                                         throws LDAPException {
        final String dn = getUserDN(context, uid.toLowerCase());
        final TreeMap<String, String> attributes = new TreeMap<String, String>();

        attributes.put("loginShell", loginShell);

        replaceAttributes(context, dn, attributes);
    }

    /**
     * Get the user's home directory
     * 
     * @param context
     *            the ldap context
     * @param uid
     *            the user's username
     * @return the user's home directory as a String
     * @throws LDAPException
     *             thrown on error
     */
    public static String getUserHomeDirectory(final LDAPContext context, final String uid) throws LDAPException {
        final Map<String, String[]> userData = getUser(context, uid.toLowerCase());
        final String[] userPassword = userData.get("homeDirectory");

        if (userPassword == null || userPassword.length < 1) {
            return null;
        }

        return userPassword[0];
    }

    /**
     * Get the user's login shell
     * 
     * @param context
     *            the ldap context
     * @param uid
     *            the user's username
     * @return the user's login shell as a String
     * @throws LDAPException
     *             thrown on error
     */
    public static String getUserShell(final LDAPContext context, final String uid) throws LDAPException {
        final Map<String, String[]> userData = getUser(context, uid.toLowerCase());
        final String[] loginShell = userData.get("loginShell");

        if (loginShell == null || loginShell.length < 1) {
            return null;
        }

        return loginShell[0];
    }

    /**
     * Delete a user from the ldap database
     * 
     * @param context
     *            the ldap context
     * @param uid
     *            the username of the user to delete
     * @throws LDAPException
     *             thrown on error
     */
    public static void deleteUser(final LDAPContext context, final String uid) throws LDAPException {
        final String dn = getUserDN(context, uid.toLowerCase());

        deleteEntry(context, dn);
    }

    /**
     * Remove a unix group from the ldap database
     * 
     * @param context
     *            the ldap context
     * @param gid
     *            the group name
     * @throws LDAPException
     *             thrown on error
     */
    public static void deleteGroup(final LDAPContext context, final String gid) throws LDAPException {
        final String dn = getGroupDN(context, gid);

        deleteEntry(context, dn);
    }

    /**
     * Add a new group to the ldap database
     * 
     * @param context
     *            the ldap context
     * @param gid
     *            the name of the group to add
     * @param gidNumber
     *            the numeric id of the group to add
     * @throws LDAPException
     *             thrown on error
     */
    public static void addGroup(final LDAPContext context, final String gid, final int gidNumber) throws LDAPException {
        final TreeMap<String, String> attributes = new TreeMap<String, String>();

        attributes.put("cn", gid);
        attributes.put("objectClass", "posixGroup");
        attributes.put("gidNumber", Integer.toString(gidNumber));

        final String dn = getGroupDN(context, gid);

        addEntry(context, dn, attributes);
    }

    /**
     * List all users who are members of a group
     * 
     * @param context
     *            the ldapcontext
     * @param gid
     *            the group whose members are requested
     * @return an array of Strings representing the usernames of all users in that group
     * @throws LDAPException
     *             thrown on error
     */
    public static String[] listUsersInGroup(final LDAPContext context, final String gid) throws LDAPException {
        final String dn = getGroupDN(context, gid);

        final Map<String, String[]> members = getEntry(context, dn, new String[] { "memberUid" });

        final String[] memberStrings = members.get("memberUid");
        if (memberStrings == null || memberStrings.length < 1) {
            return new String[] {};
        }

        return memberStrings;
    }

    /**
     * Get a group's ldap entry
     * 
     * @param context
     *            the ldap context
     * @param gid
     *            the name of the group to retrieve
     * @return a map of the group's attribtues
     * @throws LDAPException
     *             thrown on error
     */
    public static Map<String, String[]> getGroup(final LDAPContext context, final String gid) throws LDAPException {
        final String dn = getGroupDN(context, gid);

        return getEntry(context, dn, GROUP_ATTRIBS);
    }

    /**
     * Return the numeric id of a group
     * 
     * @param context
     *            the ldap context
     * @param gid
     *            the name of the group whose numeric id is needed
     * @return the numeric id of the group
     * @throws LDAPException
     *             thrown on error
     */
    public static int getGroupIdNumber(final LDAPContext context, final String gid) throws LDAPException {
        final Map<String, String[]> userData = getGroup(context, gid);
        final String[] gidNumber = userData.get("gidNumber");

        if (gidNumber == null || gidNumber.length < 1) {
            return -1;
        }

        try {
            return Integer.parseInt(gidNumber[0]);
        } catch (final NumberFormatException e) {
            return -1;
        }
    }

    /**
     * Add a number of users to a group.
     * 
     * @param context
     *            the ldap context
     * @param groupId
     *            the group to add these users to
     * @param userIds
     *            the usernames of the users to add
     * @throws LDAPException
     *             thrown on error
     */
    public static void addUsersToGroup(final LDAPContext context, final String groupId, final String[] userIds)
                                                                                                               throws LDAPException {
        final String groupDN = getGroupDN(context, groupId);
        final String[] members = listUsersInGroup(context, groupId);
        final ArrayList<String> memberList = new ArrayList<String>(Arrays.asList(members));
        final Map<String, String[]> groupModMap = new TreeMap<String, String[]>();

        for (int i = 0; i < userIds.length; i++) {
            final String uidLowerCase = userIds[i].toLowerCase();

            if (!memberList.contains(uidLowerCase)) {
                memberList.add(uidLowerCase);
            }
        }

        groupModMap.put("memberUid", memberList.toArray(new String[] {}));
        replaceAttributes(context, groupDN, groupModMap);
    }

    /**
     * Remove multiple users from a group
     * 
     * @param context
     *            the ldap context
     * @param gid
     *            the group to remove users from
     * @param uids
     *            the usernames of the users to remove
     * @throws LDAPException
     *             thrown on error
     */
    public static void removeUsersFromGroup(final LDAPContext context, final String gid, final String[] uids)
                                                                                                             throws LDAPException {
        final String dn = getGroupDN(context, gid);
        final String[] members = listUsersInGroup(context, gid);
        final ArrayList<String> memberList = new ArrayList<String>(Arrays.asList(members));
        final Map<String, String[]> groupModMap = new TreeMap<String, String[]>();

        for (int i = 0; i < uids.length; i++) {
            final String uidLowerCase = uids[i].toLowerCase();

            if (memberList.contains(uidLowerCase)) {
                memberList.remove(uidLowerCase);
            }
        }

        groupModMap.put("memberUid", memberList.toArray(new String[] {}));
        replaceAttributes(context, dn, groupModMap);
    }

}
