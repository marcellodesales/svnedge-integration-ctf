/*
 * CollabNet TeamForge
 * Copyright 2010 CollabNet, Inc.  All rights reserved.
 * http://www.collab.net
 */

package com.vasoftware.sf.common.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

/**
 * The <code>TunnelKeysUtil</code> provides methods for interacting with the SSH authorized_keys files.
 */
public class TunnelKeysUtil {
    /**
     * In authorized_keys file, replace the user's entry with the new one
     * 
     * @param authKeyFile
     *            authorized_keys file
     * @param username
     *            user that needs entry replace
     * @param keys
     *            new keys
     * @throws IOException
     *             failed to read or write file
     */
    public static synchronized void replaceUserEntry(final File authKeyFile, final String username, final String keys)
                                                                                                                      throws IOException {
        final String entry = userEntry(username, keys);

        final StringBuffer sb = new StringBuffer((int) (authKeyFile.length() + entry.length()));
        sb.append(entry); // put user's new entry at start of authorized_keys file

        if (authKeyFile.exists()) {
            // Now copy the rest of the file, minus the user's old entry
            final BufferedReader in = new BufferedReader(new FileReader(authKeyFile.getAbsolutePath()));
            try {
                final String beginBlockMarker = "# START " + username;
                final String endBlockMarker = "# END";

                String line;
                while ((line = in.readLine()) != null) {
                    // if found user's old entry, ignore lines until block end marker
                    if (line.equals(beginBlockMarker)) {
                        // Strip every line until line starts with "# END"
                        while ((line = in.readLine()) != null) {
                            // Stop if block end marker
                            if (line.equals(endBlockMarker)) {
                                break;
                            }
                        }
                    } else {
                        sb.append(line).append('\n');
                    }
                }
            } finally {
                FileUtil.close(in);
            }
        }

        FileUtil.createFile(authKeyFile, sb.toString());
    }

    /**
     * Return user entry
     * 
     * @param username
     *            user's login
     * @param userAuthorizedKeys
     *            user's authorized keys
     * @return user entry
     */
    public static String userEntry(final String username, final String userAuthorizedKeys) {
        final StringBuffer userKeys = new StringBuffer();

        appendUserEntry(userKeys, username, userAuthorizedKeys);

        return userKeys.toString();
    }

    /**
     * Append user entry to buffer
     * 
     * @param buffer
     *            String buffer
     * @param username
     *            user's login
     * @param userAuthorizedKeys
     *            user's authorized keys
     */
    public static void appendUserEntry(final StringBuffer buffer, final String username, final String userAuthorizedKeys) {
        buffer.append("# START ").append(username).append("\n");

        final String[] lines = userAuthorizedKeys.split("[\n\f\r]+");
        for (String line : lines) {
            line = line.trim();

            // only allow keys, no "#" (we use these as markers) or sshd_options
            if (line.startsWith("ssh-")) {
                buffer.append("permitopen=\"localhost:2401\" ").append(line).append("\n");
            }
        }

        buffer.append("# END\n");
    }

    /**
     * Write content to authorized_keys. The only reason for this method is so that the writes to the authorized_keys
     * file is synchronized.
     * 
     * @param authKeyFile
     *            authorized_keys file
     * @param content
     *            new content for authorized_keys file
     * @throws IOException
     *             failed to write file
     */
    public static synchronized void writeAuthorizedKeysFile(final File authKeyFile, final String content)
                                                                                                         throws IOException {
        FileUtil.createFile(authKeyFile, content);
    }
}
