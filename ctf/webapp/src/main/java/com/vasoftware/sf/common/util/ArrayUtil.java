/*
 * CollabNet TeamForge
 * Copyright 2010 CollabNet, Inc.  All rights reserved.
 * http://www.collab.net
 */

package com.vasoftware.sf.common.util;

/**
 * The <code>ArrayUtil</code> class provides utility methods for arrays.
 */
public class ArrayUtil {
    /**
     * Convert int to byte[4]
     * 
     * @param arr
     *            byte[4]
     * @return int
     */
    public static int byte2int(final byte[] arr) {
        int res = arr[3] & 0xff;

        res = (res << 8) | (arr[2] & 0xff);
        res = (res << 8) | (arr[1] & 0xff);
        res = (res << 8) | (arr[0] & 0xff);

        return res;
    }

    /**
     * Concatenate arrays.
     * 
     * @param arr1
     *            Array1
     * @param arr2
     *            Array2
     * @return [Array1] + []Array2]
     */
    public static byte[] concatArrays(final byte[] arr1, final byte[] arr2) {
        final byte result[] = new byte[arr1.length + arr2.length];

        System.arraycopy(arr1, 0, result, 0, arr1.length);
        System.arraycopy(arr2, 0, result, arr1.length, arr2.length);

        return result;
    }

    /**
     * Return array slice
     * 
     * @param arr
     *            Array
     * @param pos
     *            From
     * @param len
     *            Len
     * @return Slice
     */
    public static byte[] extractSubArray(final byte[] arr, final int pos, final int len) {
        final byte[] result = new byte[len];

        System.arraycopy(arr, pos, result, 0, len);

        return result;
    }

    /**
     * Convert int to byte[4]
     * 
     * @param i
     *            int
     * @return byte[4]
     */
    public static byte[] int2byte(final int i) {
        final byte[] arr = new byte[4];

        arr[0] = (byte) i;
        arr[1] = (byte) (i >>> 8);
        arr[2] = (byte) (i >>> 16);
        arr[3] = (byte) (i >>> 24);

        return arr;
    }
}
