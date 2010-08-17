/*
 * CollabNet TeamForge
 * Copyright 2010 CollabNet, Inc.  All rights reserved.
 * http://www.collab.net
 */

package com.vasoftware.sf.common.util;

import com.vasoftware.sf.common.logger.Logger;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.rmi.server.UID;

/**
 * The <code>GuidGenerator</code> class provides utility methods for generating CollabNet TeamForge GUIDs.
 */
public class GuidGenerator {
    /* Host IP address */
    private static String smHostIpAddress;

    /* Hex table. Two dimensional array of hex chars used for conversion between bytes and their hex values. */
    private static final char[][] HEX_TABLE = new char[256][2];

    /* Length of type code */
    public static final int TYPE_CODE_LENGTH = 4;

    /* The logger */
    private static final Logger smLogger = Logger.getLogger(GuidGenerator.class);

    static {
        char[] hex = new char[]{'0', '1', '2', '3', '4', '5', '6', '7',
                                '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};
        int byteValue = 0;
        for (char digit0 = 0; digit0 < 16; digit0++) {
            for (char digit1 = 0; digit1 < 16; digit1++) {
                HEX_TABLE[byteValue][0] = hex[digit0];
                HEX_TABLE[byteValue][1] = hex[digit1];
                byteValue++;
            }
        }

        /**
         * Note: The host prefix is computed once at server startup,
         * not each time a primary key is generated.
         */
        try {
            InetAddress inetAddress = InetAddress.getLocalHost();
            byte[] addressBytes = inetAddress.getAddress();
            StringBuffer buffer = new StringBuffer();

            appendHex(buffer, addressBytes);
            smHostIpAddress = buffer.toString();

            // make sure it starts with a digit
            if (!Character.isDigit(smHostIpAddress.charAt(0))) {
                String digitChar = Integer.toString((int) (Math.random() * 10));

                smHostIpAddress = digitChar.substring(0, 1) + smHostIpAddress.substring(1);
            }
        } catch (Exception exception) {
            /**
             * Not sure what can be done if an exception occurs here.
             */
            String message = "GUID Generator failed to initialize.";

            smLogger.fatal(message, exception);

            throw new InternalError(exception.toString());
        }
    }

    /**
     * Appends the hex equivalent of each byte in an array to a stringbuffer.
     *
     * @param buffer the buffer to which the value should be appended
     * @param bytes the bytes to be converted to hex and appended
     */
    private static void appendHex(StringBuffer buffer, byte[] bytes) {
        for (int index = 0; index < bytes.length; index++) {
            appendHex(buffer, bytes[index]);
        }
    }

    /**
     * Generate a guid of type "guid"
     * 
     * @return a full guid (28-byte) generated with the supplied typecode
     */
    public static String newGuid() {
        return newGuid("guid");
    }

    /**
     * Generates a guid of the supplied type.
     * 
     * @param typeCode
     *            a guid prefix TYPE_CODE_LENGTH characters in length
     * @return a full guid (28-byte) generated with the supplied typecode
     */
    public static String newGuid(final String typeCode) {
        validateTypeCode(typeCode);
        final StringBuffer buffer = new StringBuffer();
        buffer.append(typeCode);

        try {
            // First the host IP address
            buffer.append(smHostIpAddress);

            // A UID consists of:
            // 4 bytes = the current process id (actually, a memory address
            // on the local host guaranteed to be unique to the
            // current process)
            // 8 bytes = timestamp in milliseconds since Jan 1, 1970 UTC
            // (but only need the bottom 6 bytes since the top 2
            // bytes will remain zero for nearly 9000 years)
            // 2 bytes = a count guaranteed to be unique within a given second
            final UID uid = new UID();
            byte[] uidBytes = null;
            final ByteArrayOutputStream byteStream = new ByteArrayOutputStream(18);
            try {
                final DataOutputStream dataStream = new DataOutputStream(byteStream);
                try {
                    uid.write(dataStream);
                } finally {
                    dataStream.close();
                }
                uidBytes = byteStream.toByteArray();
            } finally {
                byteStream.close();
            }

            // (Skip the process ID)
            // (Skip the top 2 bytes of the timestamp)
            // Bottom six bytes of timestamp plus the unique count
            for (int index = 6; index < 14; index++) {
                appendHex(buffer, uidBytes[index]);
            }
        } catch (final IOException exception) {
            throw new InternalError(exception.toString());
        }

        return buffer.toString();
    }

    /**
     * 
     * Validate a typeCode (not null and correct length)
     * 
     * @param typeCode
     *            to be validated.
     */
    private static void validateTypeCode(final String typeCode) {
        if (typeCode == null) {
            throw new IllegalArgumentException("GUID type not specified.");
        } else if (typeCode.length() != TYPE_CODE_LENGTH) {
            final String message = "Invalid GUID type \"" + typeCode + "\" specified.";
            throw new IllegalArgumentException(message);
        }
    }

    /**
     * Appends the hex equivalent of a bytevalue to a stringbuffer.
     * 
     * @param buffer
     *            the buffer to which the value should be appended
     * @param byteValue
     *            the byte to be converted to hex and appended
     */
    private static void appendHex(final StringBuffer buffer, final byte byteValue) {
        final int intValue = byteValue >= 0 ? byteValue : byteValue + 256;
        final char[] hex = HEX_TABLE[intValue];

        buffer.append(hex[0]);
        buffer.append(hex[1]);
    }
}
