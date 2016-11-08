/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein is
 * confidential and proprietary to MediaTek Inc. and/or its licensors. Without
 * the prior written permission of MediaTek inc. and/or its licensors, any
 * reproduction, modification, use or disclosure of MediaTek Software, and
 * information contained herein, in whole or in part, shall be strictly
 * prohibited.
 *
 * MediaTek Inc. (C) 2015. All rights reserved.
 *
 * BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 * THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
 * RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER
 * ON AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL
 * WARRANTIES, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR
 * NONINFRINGEMENT. NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH
 * RESPECT TO THE SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY,
 * INCORPORATED IN, OR SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES
 * TO LOOK ONLY TO SUCH THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO.
 * RECEIVER EXPRESSLY ACKNOWLEDGES THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO
 * OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES CONTAINED IN MEDIATEK
 * SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK SOFTWARE
 * RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
 * STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S
 * ENTIRE AND CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE
 * RELEASED HEREUNDER WILL BE, AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE
 * MEDIATEK SOFTWARE AT ISSUE, OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE
 * CHARGE PAID BY RECEIVER TO MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 * The following software/firmware and/or related documentation ("MediaTek
 * Software") have been modified by MediaTek Inc. All revisions are subject to
 * any receiver's applicable license agreements with MediaTek Inc.
 */

package com.mediatek.bluetoothgatt.characteristic;

import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

/**
 * Public APIs for the Bluetooth GATT characteristic format convert to JAVA format.
 */
public class FormatUtils {
    private static final String TAG = "FormatUtils";
    private static final boolean DBG = true;
    private static final boolean VDBG = true;

    /*
     * Byte Size of Format types.
     */
    public static final int BOOLEAN_SIZE = 1;
    public static final int BIT2_SIZE = 1;
    public static final int NIBBLE_SIZE = 1;
    public static final int BIT4_SIZE = 1;
    public static final int BIT8_SIZE = 1;
    public static final int BIT16_SIZE = 2;
    public static final int BIT24_SIZE = 3;
    public static final int BIT32_SIZE = 4;

    public static final int UINT8_SIZE = 1;
    public static final int UINT12_SIZE = 2;
    public static final int UINT16_SIZE = 2;
    public static final int UINT24_SIZE = 3;
    public static final int UINT32_SIZE = 4;
    public static final int UINT40_SIZE = 5;
    public static final int UINT48_SIZE = 6;
    public static final int UINT64_SIZE = 8;
    public static final int UINT128_SIZE = 16;

    public static final int SINT8_SIZE = 1;
    public static final int SINT12_SIZE = 2;
    public static final int SINT16_SIZE = 2;
    public static final int SINT24_SIZE = 3;
    public static final int SINT32_SIZE = 4;
    public static final int SINT48_SIZE = 6;
    public static final int SINT64_SIZE = 8;
    public static final int SINT128_SIZE = 16;

    public static final int FLOAT32_SIZE = 4;
    public static final int FLOAT64_SIZE = 8;
    public static final int SFLOAT_SIZE = 2;
    public static final int FLOAT_SIZE = 4;

    /*
     * Bit Size of Format types.
     */
    public static final int BOOLEAN_BIT_SIZE = 1;
    public static final int BIT2_BIT_SIZE = 2;
    public static final int NIBBLE_BIT_SIZE = 4;
    public static final int BIT4_BIT_SIZE = 4;
    public static final int BIT8_BIT_SIZE = 8;
    public static final int BIT16_BIT_SIZE = 16;
    public static final int BIT24_BIT_SIZE = 24;
    public static final int BIT32_BIT_SIZE = 32;

    public static final int UINT8_BIT_SIZE = 8;
    public static final int UINT12_BIT_SIZE = 12;
    public static final int UINT16_BIT_SIZE = 16;
    public static final int UINT24_BIT_SIZE = 24;
    public static final int UINT32_BIT_SIZE = 32;
    public static final int UINT40_BIT_SIZE = 40;
    public static final int UINT48_BIT_SIZE = 48;

    public static final int SINT8_BIT_SIZE = 8;
    public static final int SINT12_BIT_SIZE = 12;
    public static final int SINT16_BIT_SIZE = 16;
    public static final int SINT24_BIT_SIZE = 24;
    public static final int SINT32_BIT_SIZE = 32;
    public static final int SINT48_BIT_SIZE = 48;

    public static final int FLOAT32_BIT_SIZE = 32;
    public static final int FLOAT64_BIT_SIZE = 64;
    public static final int SFLOAT_BIT_SIZE = 16;
    public static final int FLOAT_BIT_SIZE = 32;

    public static final int FLOAT_POSITIVE_INFINITY = 0x007FFFFE;
    public static final int FLOAT_NAN = 0x007FFFFF;
    public static final int FLOAT_NRES = 0x00800000;
    public static final int FLOAT_RESERVED_VALUE = 0x00800001;
    public static final int FLOAT_NEGATIVE_INFINITY = 0x00800002;

    public static final int FIRST_RESERVED_VALUE = FLOAT_POSITIVE_INFINITY;

    public static final float INFINITY = 0x0200;
    public static final float NAN = 0x0001;
    static final float RESERVED_FLOAT_VALUES[] = {INFINITY, NAN, NAN, NAN, -INFINITY};

    public static final int SFLOAT_POSITIVE_INFINITY = 0x07FE;
    public static final int SFLOAT_NAN = 0x07FF;
    public static final int SFLOAT_NRES = 0x0800;
    public static final int SFLOAT_RESERVED_VALUE = 0x0801;
    public static final int SFLOAT_NEGATIVE_INFINITY = 0x0802;

    public static final int FLOAT_MANTISSA_MAX = 0x007FFFFD;
    public static final int SFLOAT_MANTISSA_MAX = 0x07FD;
    // 2 ** 7 - 1
    public static final int FLOAT_EXPONENT_MAX = 127;
    public static final int FLOAT_EXPONENT_MIN = -128;
    public static final int FLOAT_PRECISION = 10000000;

    public static final int SFLOAT_EXPONENT_MAX = 7;
    public static final int SFLOAT_EXPONENT_MIN = -8;
    public static final int SFLOAT_PRECISION = 10000000;

    /*
     * String Format
     */
    public static final String UTF8_STRING = "UTF-8";
    public static final String UTF16_STRING = "UTF-16";

    /**
     * UUID16 to UUID128 convert function.
     *
     * @param value UUID16
     * @return UUID128
     */
    public static String uuid16ToUuid128(String value) {
        return "0000" + value + "-0000-1000-8000-00805F9B34FB";
    }

    /**
     * UUID32 to UUID128 convert function.
     *
     * @param value UUID32
     * @return UUID128
     */
    public static String uuid32ToUuid128(String value) {
        return value + "-0000-1000-8000-00805F9B34FB";
    }

    /*
     * bit8 to int convert function
     */
    static int bit8ToInt(byte[] value) {
        return uint8ToInt(value);
    }

    /*
     * int to bit8 convert function
     */
    static byte[] intToBit8(int value) {
        return intToUint8(value);
    }

    /*
     * (uint8/uint12/uint16/uint24) to (int) convert function
     * (int) to (uint8/uint12/uint16/uint24) convert function
     */

    static int uint8ToInt(byte[] value) {
        return uintToInt(value, UINT8_BIT_SIZE);
    }

    static byte[] intToUint8(int value) {
        return intToUint(value, UINT8_BIT_SIZE);
    }

    static int uint12ToInt(byte[] value) {
        return uintToInt(value, UINT12_BIT_SIZE);
    }

    static byte[] intToUint12(int value) {
        return intToUint(value, UINT12_BIT_SIZE);
    }

    static int uint16ToInt(byte[] value) {
        return uintToInt(value, UINT16_BIT_SIZE);
    }

    static byte[] intToUint16(int value) {
        return intToUint(value, UINT16_BIT_SIZE);
    }

    static int uint24ToInt(byte[] value) {
        return uintToInt(value, UINT24_BIT_SIZE);
    }

    static byte[] intToUint24(int value) {
        return intToUint(value, UINT24_BIT_SIZE);
    }

    private static int uintToInt(byte[] value, int uintSize) {
        int rt = 0;
        switch(uintSize) {
            case UINT8_BIT_SIZE:
                rt = (value[0] & 0xff);
                break;
            case UINT12_BIT_SIZE:
                rt = (value[0] & 0xff) + ((value[1] & 0x0f) << 8);
                break;
            case UINT16_BIT_SIZE:
                rt = (value[0] & 0xff) + ((value[1] & 0xff) << 8);
                break;
            case UINT24_BIT_SIZE:
                rt = (value[0] & 0xff) + ((value[1] & 0xff) << 8) + ((value[2] & 0xff) << 16);
                break;
            default:
                Log.e(TAG, "Impossible case!! uintToInt(), uintSize=" + uintSize);
                break;
        }

        return rt;
    }

    private static byte[] intToUint(int value, int uintSize) {
        byte[] rt = new byte[(uintSize + 7) / 8];

        switch(uintSize) {
            case UINT8_BIT_SIZE:
                rt[0] = (byte) (value & 0xff);
                break;
            case UINT12_BIT_SIZE:
                rt[0] = (byte) (value & 0xff);
                rt[1] = (byte) ((value >> 8) & 0x0f);
                break;
            case UINT16_BIT_SIZE:
                rt[0] = (byte) (value & 0xff);
                rt[1] = (byte) ((value >> 8) & 0xff);
                break;
            case UINT24_BIT_SIZE:
                rt[0] = (byte) (value & 0xff);
                rt[1] = (byte) ((value >> 8) & 0xff);
                rt[2] = (byte) ((value >> 16) & 0xff);
                break;
            default:
                Log.e(TAG, "Impossible case!! intToUint(" + value + ", " + uintSize + ")");
                break;
        }

        return rt;
    }


    /*
     * (uint32/uint40/uint48) to (long) convert function
     * (long) to (uint32/uint40/uint48) convert function
     */

    static long uint32ToLong(byte[] value) {
        return uintToLong(value, UINT32_BIT_SIZE);
    }

    static byte[] longToUint32(long value) {
        return longToUint(value, UINT32_BIT_SIZE);
    }

    static long uint40ToLong(byte[] value) {
        return uintToLong(value, UINT40_BIT_SIZE);
    }

    static byte[] longToUint40(long value) {
        return longToUint(value, UINT40_BIT_SIZE);
    }

    static long uint48ToLong(byte[] value) {
        return uintToLong(value, UINT48_BIT_SIZE);
    }

    static byte[] longToUint48(long value) {
        return longToUint(value, UINT48_BIT_SIZE);
    }

    private static long uintToLong(byte[] value, int uintSize) {
        long rt = 0;
        switch(uintSize) {
            case UINT32_BIT_SIZE:
                rt = ((long) value[0] & 0xffL) + (((long) value[1] & 0xffL) << 8) +
                         (((long) value[2] & 0xffL) << 16) + (((long) value[3] & 0xffL) << 24);
                break;
            case UINT40_BIT_SIZE:
                rt = ((long) value[0] & 0xffL) + (((long) value[1] & 0xffL) << 8) +
                        (((long) value[2] & 0xffL) << 16) + (((long) value[3] & 0xffL) << 24) +
                        (((long) value[4] & 0xffL) << 32);
                break;
            case UINT48_BIT_SIZE:
                rt = ((long) value[0] & 0xffL) + (((long) value[1] & 0xffL) << 8) +
                        (((long) value[2] & 0xffL) << 16) + (((long) value[3] & 0xffL) << 24) +
                        (((long) value[4] & 0xffL) << 32) + (((long) value[5] & 0xffL) << 40);
                break;
            default:
                Log.e(TAG, "Impossible case!! uintToLong(), uintSize=" + uintSize);
                break;
        }

        return rt;
    }

    private static byte[] longToUint(long value, int uintSize) {
        byte[] rt = new byte[(uintSize + 7) / 8];

        switch(uintSize) {
            case UINT32_BIT_SIZE:
                rt[0] = (byte) (value & 0xff);
                rt[1] = (byte) ((value >> 8) & 0xff);
                rt[2] = (byte) ((value >> 16) & 0xff);
                rt[3] = (byte) ((value >> 24) & 0xff);
                break;
            case UINT40_BIT_SIZE:
                rt[0] = (byte) (value & 0xff);
                rt[1] = (byte) ((value >> 8) & 0xff);
                rt[2] = (byte) ((value >> 16) & 0xff);
                rt[3] = (byte) ((value >> 24) & 0xff);
                rt[4] = (byte) ((value >> 32) & 0xff);
                break;
            case UINT48_BIT_SIZE:
                rt[0] = (byte) (value & 0xff);
                rt[1] = (byte) ((value >> 8) & 0xff);
                rt[2] = (byte) ((value >> 16) & 0xff);
                rt[3] = (byte) ((value >> 24) & 0xff);
                rt[4] = (byte) ((value >> 32) & 0xff);
                rt[5] = (byte) ((value >> 40) & 0xff);
                break;
            default:
                Log.e(TAG, "Impossible case!! intToUint(" + value + ", " + uintSize + ")");
                break;
        }

        return rt;
    }


    /*
     * (int8/int12/int16/int24/int32) to (int) convert function
     * (int) to (int8/int12/int16/int24/int32) convert function
     */

    static int sint8ToInt(byte[] value) {
        return sintToInt(value, SINT8_BIT_SIZE);
    }

    static byte[] intToSint8(int value) {
        return intToSint(value, SINT8_BIT_SIZE);
    }

    static int sint12ToInt(byte[] value) {
        return sintToInt(value, SINT12_BIT_SIZE);
    }

    static byte[] intToSint12(int value) {
        return intToSint(value, SINT12_BIT_SIZE);
    }

    static int sint16ToInt(byte[] value) {
        return sintToInt(value, SINT16_BIT_SIZE);
    }

    static byte[] intToSint16(int value) {
        return intToSint(value, SINT16_BIT_SIZE);
    }

    static int sint24ToInt(byte[] value) {
        return sintToInt(value, SINT24_BIT_SIZE);
    }

    static byte[] intToSint24(int value) {
        return intToSint(value, SINT24_BIT_SIZE);
    }

    static int sint32ToInt(byte[] value) {
        return sintToInt(value, SINT32_BIT_SIZE);
    }

    static byte[] intToSint32(int value) {
        return intToSint(value, SINT32_BIT_SIZE);
    }

    private static int sintToInt(byte[] value, int sintSize) {
        int rt = 0;
        switch(sintSize) {
            case SINT8_BIT_SIZE:
                rt = unsignedToSigned((value[0] & 0xff), SINT8_BIT_SIZE);
                break;
            case SINT12_BIT_SIZE:
                rt = unsignedToSigned((value[0] & 0xff) + ((value[1] & 0x0f) << 8),
                        SINT12_BIT_SIZE);
                break;
            case SINT16_BIT_SIZE:
                rt = unsignedToSigned((value[0] & 0xff) + ((value[1] & 0xff) << 8),
                        SINT16_BIT_SIZE);
                break;
            case SINT24_BIT_SIZE:
                rt = unsignedToSigned((value[0] & 0xff) + ((value[1] & 0xff) << 8) +
                        ((value[2] & 0xff) << 16), SINT24_BIT_SIZE);
                break;
            case SINT32_BIT_SIZE:
                rt = unsignedToSigned((value[0] & 0xff) + ((value[1] & 0xff) << 8) +
                        ((value[2] & 0xff) << 16) + ((value[3] & 0xff) << 24), SINT32_BIT_SIZE);
                break;
            default:
                Log.e(TAG, "Impossible case!! sintToInt(), sintSize=" + sintSize);
                break;
        }

        return rt;
    }

    private static byte[] intToSint(int value, int sintSize) {
        byte[] rt = new byte[(sintSize + 7) / 8];

        switch(sintSize) {
            case SINT8_BIT_SIZE:
                value = intToSignedBits(value, SINT8_BIT_SIZE);
                rt[0] = (byte) (value & 0xff);
                break;
            case SINT12_BIT_SIZE:
                value = intToSignedBits(value, UINT12_BIT_SIZE);
                rt[0] = (byte) (value & 0xff);
                rt[1] = (byte) ((value >> 8) & 0x0f);
                break;
            case SINT16_BIT_SIZE:
                value = intToSignedBits(value, UINT16_BIT_SIZE);
                rt[0] = (byte) (value & 0xff);
                rt[1] = (byte) ((value >> 8) & 0xff);
                break;
            case SINT24_BIT_SIZE:
                value = intToSignedBits(value, UINT24_BIT_SIZE);
                rt[0] = (byte) (value & 0xff);
                rt[1] = (byte) ((value >> 8) & 0xff);
                rt[2] = (byte) ((value >> 16) & 0xff);
                break;
            case SINT32_BIT_SIZE:
                value = intToSignedBits(value, UINT32_BIT_SIZE);
                rt[0] = (byte) (value & 0xff);
                rt[1] = (byte) ((value >> 8) & 0xff);
                rt[2] = (byte) ((value >> 16) & 0xff);
                rt[3] = (byte) ((value >> 24) & 0xff);
                break;
            default:
                Log.e(TAG, "Impossible case!! intToSint(" + value + ", " + sintSize + ")");
                break;
        }

        return rt;
    }


    /*
     * (UTF-8 String / UTF-16 String) to (String) convert function
     * (String) to (UTF-8 String / UTF-16 String) convert function
     */

    static String utf8sToString(byte[] value) {
        return new String(value, StandardCharsets.UTF_8);
    }

    static byte[] stringToUtf8s(String value) {
        if (value == null) {
            String sr = "";
            return sr.getBytes(StandardCharsets.UTF_8);
        } else {
            return value.getBytes(StandardCharsets.UTF_8);
        }
    }

    static String utf16sToString(byte[] value) {
        return new String(value, StandardCharsets.UTF_16);
    }

    static byte[] stringToUtf16s(String value) {
        if (value == null) {
            String sr = "";
            return sr.getBytes(StandardCharsets.UTF_16);
        } else {
            return value.getBytes(StandardCharsets.UTF_16);
        }
    }

    static float sfloatToFloat(byte[] value) {
        float result;
        int mantissa = (value[0] & 0xff) +
                ((value[1] & 0x0f) << 8);
        int exponent = unsignedToSigned(unsignedByteToInt(value[1]) >> 4, 4);

        result = (float) (mantissa * Math.pow(10.0f, exponent));
        return result;
    }

    static byte[] floatToSfloat(float value) {
        byte[] result = new byte[2];
        boolean signed = value > 0;
        double mantissa = Math.abs(value);
        int exponent = 0; // Note: 10**x exponent, not 2**x

        // scale down if number needs more precision
        double smantissa = Math.round(mantissa * SFLOAT_PRECISION);
        double rmantissa = Math.round(mantissa) * SFLOAT_PRECISION;
        double mdiff = Math.abs(smantissa - rmantissa);
        while (mdiff > 0.5 && exponent > SFLOAT_EXPONENT_MIN &&
                (mantissa * 10) <= SFLOAT_MANTISSA_MAX) {
            mantissa *= 10;
            --exponent;
            smantissa = Math.round(mantissa * SFLOAT_PRECISION);
            rmantissa = Math.round(mantissa) * SFLOAT_PRECISION;
            mdiff = Math.abs(smantissa - rmantissa);
        }


        int intMantissa = (int) Math.round((signed ? 1 : -1) * mantissa);

        byte[] mantissaByte = ByteBuffer.allocate(4).putInt(intMantissa).array();

        byte[] exponentByte = ByteBuffer.allocate(4).putInt(exponent).array();

        result[0] = mantissaByte[3];

        // Last byte is exponent
        result[1] = (byte) (mantissaByte[2] | ((exponentByte[3] & 0x000F) << 4));

        return result;
    }

    static float floatToFloat(byte[] value) {
        float result;
        int mantissa = (value[0] & 0xff) +
                ((value[1] & 0xff) << 8) +
                ((value[2] & 0xff) << 16);
        int expoent = value[3];

        if (mantissa >= FIRST_RESERVED_VALUE &&
                mantissa <= FLOAT_NEGATIVE_INFINITY) {
            result = RESERVED_FLOAT_VALUES[mantissa - FIRST_RESERVED_VALUE];
        } else {
            if (mantissa >= 0x800000) {
                mantissa = -((0xFFFFFF + 1) - mantissa);
            }
            result = (float) (mantissa * Math.pow(10.0f, expoent));
        }

        return result;
    }

    static byte[] floatToFloat(float value) {
        byte[] result = new byte[4];
        boolean signed = value > 0;
        double mantissa = Math.abs(value);
        int exponent = 0; // Note: 10**x exponent, not 2**x

        // scale down if number needs more precision
        double smantissa = Math.round(mantissa * FLOAT_PRECISION);
        double rmantissa = Math.round(mantissa) * FLOAT_PRECISION;
        double mdiff = Math.abs(smantissa - rmantissa);
        while (mdiff > 0.5 && exponent > FLOAT_EXPONENT_MIN &&
                (mantissa * 10) <= FLOAT_MANTISSA_MAX) {
            mantissa *= 10;
            --exponent;
            smantissa = Math.round(mantissa * FLOAT_PRECISION);
            rmantissa = Math.round(mantissa) * FLOAT_PRECISION;
            mdiff = Math.abs(smantissa - rmantissa);
        }


        int intMantissa = (int) Math.round((signed ? 1 : -1) * mantissa);

        byte[] mantissaByte = ByteBuffer.allocate(4).putInt(intMantissa).array();

        byte[] exponentByte = ByteBuffer.allocate(4).putInt(exponent).array();

        for (int i = 0; i < 3; i++) { // Size of mantissaByte
            result[i] = mantissaByte[mantissaByte.length - i - 1];
        }

        result[3] = exponentByte[3]; // Last byte is exponent

        return result;
    }

    /*
     * bit8 range check function
     */
    static boolean bit8RangeCheck(int value) {
        return uint8RangeCheck(value);
    }

    /*
     * uint8 range check function
     */
    static boolean uint8RangeCheck(int value) {
        return (value >= 0 && value <= 0xFF);
    }

    /*
     * uint12 range check function
     */
    static boolean uint12RangeCheck(int value) {
        return (value >= 0 && value <= 0xFFF);
    }

    /*
     * uint16 range check function
     */
    static boolean uint16RangeCheck(int value) {
        return (value >= 0 && value <= 0xFFFF);
    }

    /*
     * uint24 range check function
     */
    static boolean uint24RangeCheck(int value) {
        return (value >= 0 && value <= 0xFFFFFF);
    }

    /*
     * uint32 range check function
     */
    static boolean uint32RangeCheck(long value) {
        return (value >= 0 && value <= 0xFFFFFFFFL);
    }

    /*
     * uint40 range check function
     */
    static boolean uint40RangeCheck(long value) {
        return (value >= 0 && value <= 0xFFFFFFFFFFL);
    }

    /*
     * uint48 range check function
     */
    static boolean uint48RangeCheck(long value) {
        return (value >= 0 && value <= 0xFFFFFFFFFFFFL);
    }

    /*
     * sint8 range check function
     */
    static boolean sint8RangeCheck(int value) {
        return (value >= (0 - 0x80) && value <= (0x80 - 1));
    }

    /*
     * sint12 range check function
     */
    static boolean sint12RangeCheck(int value) {
        return (value >= (0 - 0x800) && value <= (0x800 - 1));
    }

    /*
     * sint16 range check function
     */
    static boolean sint16RangeCheck(int value) {
        return (value >= (0 - 0x8000) && value <= (0x8000 - 1));
    }

    /*
     * sint24 range check function
     */
    static boolean sint24RangeCheck(int value) {
        return (value >= (0 - 0x800000) && value <= (0x800000 - 1));
    }

    /*
     * sint32 range check function
     */
    static boolean sint32RangeCheck(int value) {
        return true;
    }

    /*
     * SFLOAT range check function
     */
    static boolean sfloatRangeCheck(float value) {
        return (value > (0 - SFLOAT_NEGATIVE_INFINITY) && value < SFLOAT_POSITIVE_INFINITY);
    }

    /*
     * FLOAT range check function
     */
    static boolean floatRangeCheck(float value) {
        return (value > (0 - FLOAT_NEGATIVE_INFINITY) && value < FLOAT_POSITIVE_INFINITY);
    }

    /**
     * Convert an unsigned integer value to a two's-complement encoded
     * signed value.
     */
    private static int unsignedToSigned(int unsigned, int size) {
        if ((unsigned & (1 << size - 1)) != 0) {
            unsigned = -1 * ((1 << size - 1) - (unsigned & ((1 << size - 1) - 1)));
        }
        return unsigned;
    }

    /**
     * Convert an integer into the signed bits of a given length.
     */
    private static int intToSignedBits(int i, int size) {
        if (i < 0) {
            i = (1 << size - 1) + (i & ((1 << size - 1) - 1));
        }
        return i;
    }

    /**
     * Convert a signed byte to an unsigned int.
     */
    private static int unsignedByteToInt(byte b) {
        return b & 0xFF;
    }

    /**
     * Convert byte[] to Hex string.
     *
     * @param value byte[] value
     * @return Hex string
     */
    public static String toHexString(byte[] value) {
        if (value == null) {
            return "[ null ]";
        }

        String str;
        str = "[ ";
        for (int i = 0; i < value.length; i++) {
            str += String.format("%02X ", value[i]);
        }
        str += "], Length=" + value.length;

        return str;
    }

}
