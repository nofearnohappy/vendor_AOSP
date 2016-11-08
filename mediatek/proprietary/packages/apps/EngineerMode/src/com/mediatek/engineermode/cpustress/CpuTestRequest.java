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
 * MediaTek Inc. (C) 2010. All rights reserved.
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

package com.mediatek.engineermode.cpustress;

public class CpuTestRequest {

    public static final int FLAG_PACK_ALL = 3;
    public static final int FLAG_PACK_CPU_INFO = 1;
    public static final int FLAG_PACK_TEST_DATA = 2;

    private int mCpuInfoLen;
    private int mTestDataLen;
    private int mCoreNum;
    private int mCpuArch;
    private int mCpuBits;
    private int mCoreBitLen;
    private int mTestCoreLen;
    private int[] mTestCores = null;
    private int[] mTestData = null;

    public CpuTestRequest(int coreNum, int cpuArch, int cpuBits, int bitLen) {
        mCoreNum = coreNum;
        mCpuArch = cpuArch;
        mCpuBits = cpuBits;
        mCoreBitLen = bitLen;
        if (bitLen > 0) {
            if ((bitLen > 32) || (32 % bitLen != 0)) {
                throw new IllegalArgumentException("bitLen must be divisible by 32; now bitLen:" + bitLen);
            }
            int n = 32 / mCoreBitLen;
            mTestCoreLen = mCoreNum / n + ((mCoreNum % n == 0) ? 0 : 1);
            mTestCores = new int[mTestCoreLen];
        }
    }

    public final int[] packRequest(int flags) {
        int packLen = 2;
        int[] testData = null;
        if ((flags & FLAG_PACK_CPU_INFO) > 0) {
            packLen += mTestCoreLen + 1;
            mCpuInfoLen = mTestCoreLen + 1;
        }

        if ((flags & FLAG_PACK_TEST_DATA) > 0) {
            testData = getTestData();
            if (testData != null) {
                packLen += testData.length;
                mTestDataLen = testData.length;
            }
        }
        int[] pack = new int[packLen];
        int n = 0;
        pack[n++] = mCpuInfoLen;
        pack[n++] = mTestDataLen;
        if ((flags & FLAG_PACK_CPU_INFO) > 0) {
            int cpuinfo = 0;
            cpuinfo = mCoreNum & 0XFF;
            cpuinfo |= (mCoreBitLen & 0XFF) << 8;
            cpuinfo |= (mCpuArch & 0XFF) << 16;
            cpuinfo |= (mCpuBits & 0XFF) << 24;
            pack[n++] = cpuinfo;
            if (mTestCores != null) {
                for (int i = 0; i < mTestCores.length; i++) {
                    pack[n++] = mTestCores[i];
                }
            }
        }

        if ((flags & FLAG_PACK_TEST_DATA) > 0) {
            if (testData != null) {
                for (int i = 0; i < testData.length; i++) {
                    pack[n++] = testData[i];
                }
            }
        }

        return pack;
    }

    public int[] getTestData() {
        return mTestData;
    }

    public void setTestData(int[] data) {
        mTestData = data;
    }

    public void setCpuTestCore(int coreIndex, int val) {
        if (mTestCores == null) {
            return;
        }
        if (coreIndex > mCoreNum - 1 || coreIndex < 0) {
            return;
        }
        int n = 32 / mCoreBitLen;
        int index = coreIndex / n;
        int location = coreIndex % n;
        int mask = (0XFFFFFFFF >>> (32 - mCoreBitLen));
        int base = mTestCores[index] & (~(mask << (mCoreBitLen * location)));
        mTestCores[index] = ((val & mask) << (mCoreBitLen * location)) | base ;
    }

    public CpuTestRequest copy(int flags) {
        CpuTestRequest copyReq = new CpuTestRequest(mCoreNum, mCpuArch, mCpuBits, mCoreBitLen);
        if ((flags & FLAG_PACK_CPU_INFO) > 0) {
            if (mTestCores != null) {
                for (int i = 0; i < mTestCores.length; i++) {
                    copyReq.mTestCores[i] = mTestCores[i];
                }
            }
        }
        if ((flags & FLAG_PACK_TEST_DATA) > 0) {
            if (mTestData != null) {
                copyReq.mTestData = new int[mTestData.length];
                for (int i = 0; i < mTestData.length; i++) {
                    copyReq.mTestData[i] = mTestData[i];
                }
            }
        }
        return copyReq;
    }

    public boolean equals(CpuTestRequest request, int flags) {
        if ((flags & FLAG_PACK_CPU_INFO) == 0 && (flags & FLAG_PACK_TEST_DATA) == 0) {
            return false;
        }
        if ((flags & FLAG_PACK_CPU_INFO) > 0) {
            if (!(mTestCores == null && request.mTestCores == null)) {
                if (mTestCores == null || request.mTestCores == null) {
                    return false;
                }
                if (mTestCores.length != request.mTestCores.length) {
                    return false;
                }
                for (int i = 0; i < mTestCores.length; i++) {
                    int m = mTestCores[i];
                    int n = request.mTestCores[i];
                    if (m != n) {
                        return false;
                    }
                }
            }
        }

        if ((flags & FLAG_PACK_TEST_DATA) > 0) {
            if (!(mTestData == null && request.mTestData == null)) {
                if (mTestData == null || request.mTestData == null) {
                    return false;
                }
                if (mTestData.length != request.mTestData.length) {
                    return false;
                }
                for (int i = 0; i < mTestData.length; i++) {
                    int m = mTestData[i];
                    int n = request.mTestData[i];
                    if (m != n) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    public int getCpuTestCore(int coreIndex) {
        if (mTestCores == null) {
            return -1;
        }
        if (coreIndex > mCoreNum - 1 || coreIndex < 0) {
            return -1;
        }
        int val = 0;
        int n = 32 / mCoreBitLen;
        int index = coreIndex / n;
        int location = coreIndex % n;
        int mask = (0XFFFFFFFF >>> (32 - mCoreBitLen));
        val = (mTestCores[index] >>> (mCoreBitLen * location)) & mask;
        return val;
    }

    public void clearCpuTestCore() {
        if (mTestCores != null) {
            for (int i = 0; i < mTestCores.length; i++) {
                mTestCores[i] = 0;
            }
        }
    }

    public boolean isSetCpuTestCore() {
        boolean result = false;
        if (mTestCores != null) {
            for (int i = 0; i < mTestCores.length; i++) {
                if (mTestCores[i] != 0) {
                    result = true;
                    break;
                }
            }
        }
        return result;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("CpuTestRequest Dump\n");
        builder.append("coreNum:").append(mCoreNum);
        builder.append(",coreBitLen:").append(mCoreBitLen);
        builder.append(",cpuArch:").append(mCpuArch);
        builder.append(",cpuBits:").append(mCpuBits);
        builder.append("\nTestCores:[");
        if (mTestCores != null) {
            for (int i : mTestCores) {
                builder.append(String.format("%08x", i)).append(",");
            }
        } else {
            builder.append("null");
        }
        builder.append("]");
        builder.append("\nTestData:[");
        if (mTestData != null) {
            for (int i : mTestData) {
                builder.append(i).append(",");
            }
        } else {
            builder.append("null");
        }
        builder.append("]");

        return builder.toString();
    }
}
