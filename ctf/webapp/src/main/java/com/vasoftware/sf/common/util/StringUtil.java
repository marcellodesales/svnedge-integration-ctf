/*
 * CollabNet TeamForge
 * Copyright 2010 CollabNet, Inc.  All rights reserved.
 * http://www.collab.net
 */

package com.vasoftware.sf.common.util;


/**
 * The <code>ArrayUtil</code> class provides utility methods for strings and for interacting with message bundles.
 */
public class StringUtil {
    /* An empty string that is usable for everyone. */
    public static final String EMPTY = "";

    /**
     * Checks if the specified string is empty (null or zero length)
     * 
     * @param s
     *            String to check.
     * @return true if string is empty, false otherwise.
     */
    public static boolean isEmpty(final String s) {
        return s == null || s.length() == 0;
    }

    /**
     * Join strings.
     * 
     * @param words
     *            Strings to join
     * @param separator
     *            Join using this separator
     * @return Joined string
     */
    public static String join(final String[] words, final String separator) {
        if (words == null) {
            return "";
        }
        return join(words, separator, words.length);
    }

    /**
     * Join strings.
     * 
     * @param words
     *            Strings to join
     * @param separator
     *            Join using this separator
     * @param index
     *            join the words array until it reaches this index
     * @return Joined string
     */
    public static String join(final String[] words, final String separator, final int index) {
        if (words == null) {
            return "";
        }
        final StringBuffer result = new StringBuffer();
        for (int i = 0; i < index; i++) {
            if (words[i] != null) {
                if (result.length() > 0) {
                    result.append(separator);
                }
                result.append(words[i]);
            }
        }
        return result.toString();
    }
}
