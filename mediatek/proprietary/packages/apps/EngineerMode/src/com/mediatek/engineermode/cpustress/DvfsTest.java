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

import com.mediatek.engineermode.Elog;
import com.mediatek.engineermode.ShellExe;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
/**
 * DVFS test util and save test data.
 */
public class DvfsTest {
    private static final String TAG = "EM/CpuStress_DVFS";
    private static final String FS_CPUFREQ_OPPIDX = "/proc/cpufreq/cpufreq_oppidx";
    private static final String SET_COMMAND_FORMAT = "echo %1$d > %2$s";
    private static final String CAT = "cat ";
    private static final String REGEX_OPP_NUMBER =
                "\\bOP\\(\\d+\\s*,\\s*\\d+\\)";  //  match OP(1690000, 110000),
    private static final String REGEX_IDX =
                "\\bcpufreq\\_oppidx\\s*\\=\\s*\\d+";  //  match cpufreq_oppidx = 1

    public int mOppNumber;  // OPP num
    public int mOppCode;    // OPP select number
    public int mInterval;   // test interval
    public int mTestIndex;  // Current set idx
    public int mResultIdx;  // Current get idx
    public boolean mWantStop;
    public boolean mIsRunning;

    /**
     * Get current opp number.
     * @return the total opp number.
     */
    public static int getOppNumber() {
        Elog.v(TAG, "Enter getOppNumber: ");
        int num = 0;
        String cmd = CAT + FS_CPUFREQ_OPPIDX;
        String output = execCommand(cmd);
        if (output != null) {
            Pattern p = Pattern.compile(REGEX_OPP_NUMBER);
            Matcher matcher = p.matcher(output);
            while (matcher.find()) {
                num++;
            }
        }
        return num;
    }

    /**
     * Get current opp idx.
     * @return the opp idx value.
     */
    public static int getOppIdx() {
        Elog.v(TAG, "Enter getOppIdx: ");
        int idx = 0;
        String cmd = CAT + FS_CPUFREQ_OPPIDX;
        String output = execCommand(cmd);
        String content = null;
        if (output != null) {
            Pattern p = Pattern.compile(REGEX_IDX);
            Matcher matcher = p.matcher(output);
            while (matcher.find()) {
                content = matcher.group();
                if (content == null) {
                    continue;
                }
                Elog.v(TAG, "content: " + content);
                String[] strA = content.split("=");
                if (strA.length == 2) {
                    try {
                        idx = Integer.parseInt(strA[1].trim());
                    } catch (NumberFormatException e) {
                        Elog.e(TAG, "NumberFormatException invalid output:" + strA[1]);
                    }
                }
            }
        }
        return idx;
    }

    /**
     * Do DVFS test.
     */
    public void doDvfsTest() {
        Elog.v(TAG, "enter doDvfsTest");
        String command = null;
        command = String.format(SET_COMMAND_FORMAT, mTestIndex, FS_CPUFREQ_OPPIDX);
        execCommand(command);
        int idx = getOppIdx();
        if (idx != mTestIndex) {
            Elog.e(TAG, "set and get is not sync when write " + mTestIndex);
        }
        mResultIdx = idx;
        // find next idx
        int nextIndex = -1;
        for (int i = mTestIndex + 1; i < mOppNumber; i++) {
            if ((mOppCode & (1 << i)) != 0) {
                nextIndex = i;
                break;
            }
        }
        // test again from start
        if (nextIndex == -1) {
            for (int i = 0; i < mOppNumber; i++) {
                if ((mOppCode & (1 << i)) != 0) {
                    nextIndex = i;
                    break;
                }
            }
        }
        mTestIndex = nextIndex;
    }
    /**
     * exec shell command.
     *
     * @param cmd
     *            command string
     */
    private static String execCommand(String cmd) {
         int ret = -1;
         Elog.d(TAG, "[cmd]:" + cmd);
         try {
             ret = ShellExe.execCommand(cmd);
         } catch (IOException e) {
             Elog.e(TAG, "IOException: " + e.getMessage());
         }
         if (ret == 0) {
             String outStr = ShellExe.getOutput();
             Elog.d(TAG, "[output]: " + outStr);
             return outStr;
         }
         return null;
     }
}
