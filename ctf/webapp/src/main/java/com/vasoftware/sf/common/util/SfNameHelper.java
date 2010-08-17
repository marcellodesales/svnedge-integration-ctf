/*
 * CollabNet TeamForge
 * Copyright 2010 CollabNet, Inc.  All rights reserved.
 * http://www.collab.net
 */

package com.vasoftware.sf.common.util;

/**
 * The <code>SfNameHelper</code> class provides methods for handling TeamForge names.
 */
public class SfNameHelper {
    /* The string that is used to delimit tokens in auto-generated names */
    public static final char NAME_TOKEN_DELIMETER_CHAR = '_';

    /* The regex matching disallowed characters */
    private static final String DISALLOWED_CHARACTERS = "[^a-zA-Z0-9_]";

    /**
     * Validation function for the type handler. An object should be passed in to be checked. The validation can be
     * complex or simple type but failure should return false.
     * 
     * @param sfName
     *            The object being validated
     * @return true if the object conforms to the desired form
     */
    public static boolean validate(final String sfName) {
        if (sfName == null) {
            return false;
        }
        final String valid = convertToValidSfName(sfName, true);
        return (sfName.equals(valid));
    }

    /**
     * @see SfNameHelper#convertToValidSfName(String, boolean, String)
     */
    public static String convertToValidSfName(final String value, final boolean preserveCase) {
        return convertToValidSfName(value, preserveCase, DISALLOWED_CHARACTERS);
    }

    /**
     * Convert any input string into a valid SfName, or return null if it's not possible to create an SfName out of the
     * input value. Illegal characters are removed, and the result is shortened to 32 characters.
     * 
     * If any characters outside the Unicode "Basic Latin" character set are present, we return null rather than attempt
     * to create a name from the remaining characters.
     * 
     * @param value
     *            the input value
     * @param preserveCase
     *            if true, we maintain the original case; otherwise, the string is converted to lower case (more
     *            suitable for a folder name conversion from a title).
     * @param disallowedCharacters
     *            Regexp pattern for characters which are not alloweddd in name (to be replaced with underscores)
     * @return the resulting SfName string, or null if one cannot be made from it.
     */
    protected static String convertToValidSfName(final String value, final boolean preserveCase,
                                                 final String disallowedCharacters) {
        if (value == null) {
            return null;
        }
        // first, validate that there are no characters outside BASIC_LATIN. If there are, we return null.
        final char[] input = value.toCharArray();
        for (int i = 0; i < input.length; i++) {
            final char asCharacter = input[i];

            // if this is outside the unicde "basic latin" char set, then we don't bother trying to create
            // a usable folder name.
            if (Character.UnicodeBlock.of(asCharacter) != Character.UnicodeBlock.BASIC_LATIN) {
                return null;
            }
        }

        String output = value;
        if (!preserveCase) {
            output = output.toLowerCase();
        }

        // anything that is not a letter, digit or underscore is converted to underscore.
        output = output.replaceAll(disallowedCharacters, "_");

        /* Replace multiple underscores with a single one */
        output = output.replaceAll("_+", String.valueOf(NAME_TOKEN_DELIMETER_CHAR));

        /* Trim leading and trailing underscores */
        output = output.replaceAll("^_*", "");
        output = output.replaceAll("_*$", "");

        // shorten to 32 characters if longer than that.
        if (output.length() > 32) {
            output = output.substring(0, 32);
            /* Trim lany additional trailing underscores */
            output = output.replaceAll("_*$", "");
        }

        // if that didn't leave anything useful, we return null.
        if (output.length() == 0) {
            return null;
        } else {
            return output;
        }
    }
}
