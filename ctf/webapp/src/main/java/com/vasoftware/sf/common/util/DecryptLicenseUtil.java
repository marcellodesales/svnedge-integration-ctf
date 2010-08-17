/*
 * CollabNet TeamForge
 * Copyright 2010 CollabNet, Inc.  All rights reserved.
 * http://www.collab.net
 */

package com.vasoftware.sf.common.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.Random;

import org.apache.axis.encoding.Base64;

import com.vasoftware.sf.common.SfSystemException;
import com.vasoftware.sf.common.configuration.GlobalOptionKeys;
import com.vasoftware.sf.common.configuration.SfGlobalOptions;
import com.vasoftware.sf.common.configuration.SfGlobalOptionsManager;

/**
 * The <code>DecryptLicenseUtil</code> class provides utilities for license decryption.
 */
public class DecryptLicenseUtil {
    private static final String SCM_DEFAULT_SHARED_SECRET = "xnaskdxy*B R^qbiwgd";
    private static final String SCM_KEY_SEED = "kaboomastringa";
    private static Random smRandomGen = new Random(new Date().getTime());

    public static final int SCM_TIMESTAMP_SALT;

    static {
        int salt = 0x34A49f41;
        try {
            final MessageDigest digester = MessageDigest.getInstance("SHA-1");
            digester.update(SCM_DEFAULT_SHARED_SECRET.getBytes());
            final byte[] hash = digester.digest();
            salt = ArrayUtil.byte2int(hash);
        } catch (final NoSuchAlgorithmException e) {
            ;
        }
        SCM_TIMESTAMP_SALT = salt;
    }

    /**
     * Create a one-time key for SCM SOAP call.
     * 
     * @return A key
     */
    public static String createScmRequestKey() {
        final int rand = smRandomGen.nextInt();
        final int timestamp = (int) (new Date().getTime() / 1000);

        final byte[] key = DecryptLicenseUtil.makeScmRequestKey(rand, timestamp);
        final String requestKey = Base64.encode(key);
        return requestKey;
    }

    /**
     * Make a one-time key for SCM SOAP call.
     * 
     * @param rnd
     *            Random data, "seed" of key (must change all the time!)
     * @param timestamp
     *            Key timestamp
     * @return A key as a byte sequence
     */
    public static byte[] makeScmRequestKey(final int rnd, final int timestamp) {
        final SfGlobalOptions options = SfGlobalOptionsManager.getOptions();
        String password = options.getOption(GlobalOptionKeys.SFMAIN_INTEGRATION_SECURITY_SHARED_SECRET);
        if (password == null) {
            password = SCM_DEFAULT_SHARED_SECRET;
        }

        final byte[] rndBytes = ArrayUtil.int2byte(rnd);
        final byte[] timeBytes = ArrayUtil.int2byte(timestamp ^ SCM_TIMESTAMP_SALT);

        MessageDigest digester = null;
        try {
            digester = MessageDigest.getInstance("SHA-1");
        } catch (final NoSuchAlgorithmException e) {
            throw new SfSystemException(e);
        }
        digester.update(password.getBytes());
        digester.update(rndBytes);
        digester.update(timeBytes);
        digester.update(password.getBytes());
        digester.update(SCM_KEY_SEED.getBytes());

        final byte[] key = digester.digest();
        return ArrayUtil.concatArrays(ArrayUtil.concatArrays(rndBytes, timeBytes), key);
    }
}
