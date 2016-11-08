/*
* Copyright (C) 2014 MediaTek Inc.
* Modification based on code covered by the mentioned copyright
* and/or permission notice(s).
*/
/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.browser;

import android.net.WebAddress;
import android.os.Debug;
import android.os.Process;
import android.os.SystemClock;
import android.util.Log;

import java.io.IOException;

/**
 * Performance analysis
 */
public class Performance {

    private static final String LOGTAG = "browser";

    private final static boolean LOGD_ENABLED =
            com.android.browser.Browser.LOGD_ENABLED;

    private static boolean mInTrace;

    // Performance probe
    private static final int[] SYSTEM_CPU_FORMAT = new int[] {
            Process.PROC_SPACE_TERM | Process.PROC_COMBINE,
            Process.PROC_SPACE_TERM | Process.PROC_OUT_LONG, // 1: user time
            Process.PROC_SPACE_TERM | Process.PROC_OUT_LONG, // 2: nice time
            Process.PROC_SPACE_TERM | Process.PROC_OUT_LONG, // 3: sys time
            Process.PROC_SPACE_TERM | Process.PROC_OUT_LONG, // 4: idle time
            Process.PROC_SPACE_TERM | Process.PROC_OUT_LONG, // 5: iowait time
            Process.PROC_SPACE_TERM | Process.PROC_OUT_LONG, // 6: irq time
            Process.PROC_SPACE_TERM | Process.PROC_OUT_LONG  // 7: softirq time
    };

    private static long mStart;
    private static long mProcessStart;
    private static long mUserStart;
    private static long mSystemStart;
    private static long mIdleStart;
    private static long mIrqStart;

    private static long mUiStart;

    static void tracePageStart(String url) {
        if (BrowserSettings.getInstance().isTracing()) {
            String host;
            try {
                WebAddress uri = new WebAddress(url);
                host = uri.getHost();
            } catch (android.net.ParseException ex) {
                host = "browser";
            }
            host = host.replace('.', '_');
            host += ".trace";
            mInTrace = true;
            Debug.startMethodTracing(host, 20 * 1024 * 1024);
        }
    }

    static void tracePageFinished() {
        if (mInTrace) {
            mInTrace = false;
            Debug.stopMethodTracing();
        }
    }

    static void onPageStarted() {
        mStart = SystemClock.uptimeMillis();
        mProcessStart = Process.getElapsedCpuTime();
        long[] sysCpu = new long[7];
        if (Process.readProcFile("/proc/stat", SYSTEM_CPU_FORMAT, null, sysCpu, null)) {
            mUserStart = sysCpu[0] + sysCpu[1];
            mSystemStart = sysCpu[2];
            mIdleStart = sysCpu[3];
            mIrqStart = sysCpu[4] + sysCpu[5] + sysCpu[6];
        }
        mUiStart = SystemClock.currentThreadTimeMillis();
    }

    static void onPageFinished(String url) {
        long[] sysCpu = new long[7];
        if (Process.readProcFile("/proc/stat", SYSTEM_CPU_FORMAT, null, sysCpu, null)) {
            String uiInfo =
                    "UI thread used " + (SystemClock.currentThreadTimeMillis() - mUiStart) + " ms";
            if (LOGD_ENABLED) {
                Log.d(LOGTAG, uiInfo);
            }
            // The string that gets written to the log
            String performanceString =
                    "It took total " + (SystemClock.uptimeMillis() - mStart)
                            + " ms clock time to load the page." + "\nbrowser process used "
                            + (Process.getElapsedCpuTime() - mProcessStart)
                            + " ms, user processes used " + (sysCpu[0] + sysCpu[1] - mUserStart)
                            * 10 + " ms, kernel used " + (sysCpu[2] - mSystemStart) * 10
                            + " ms, idle took " + (sysCpu[3] - mIdleStart) * 10
                            + " ms and irq took " + (sysCpu[4] + sysCpu[5] + sysCpu[6] - mIrqStart)
                            * 10 + " ms, " + uiInfo;
            if (LOGD_ENABLED) {
                Log.d(LOGTAG, performanceString + "\nWebpage: " + url);
            }
            if (url != null) {
                // strip the url to maintain consistency
                String newUrl = new String(url);
                if (newUrl.startsWith("http://www.")) {
                    newUrl = newUrl.substring(11);
                } else if (newUrl.startsWith("http://")) {
                    newUrl = newUrl.substring(7);
                } else if (newUrl.startsWith("https://www.")) {
                    newUrl = newUrl.substring(12);
                } else if (newUrl.startsWith("https://")) {
                    newUrl = newUrl.substring(8);
                }
                if (LOGD_ENABLED) {
                    Log.d(LOGTAG, newUrl + " loaded");
                }
            }
        }
    }

    static String encodeToJSON(Debug.MemoryInfo memoryInfo) {
        StringBuilder memoryUsage = new StringBuilder();
        memoryUsage.append("{\r\n")
                   .append("    \"Browser app (MB)\": {\r\n")
                   .append("        \"Browser\": {\r\n")
                   .append("            \"Pss\": {\r\n")
                   .append(String.format("                \"DVM\": %.2f,\r\n", memoryInfo.dalvikPss / 1024.0))
                   .append(String.format("                \"Native\": %.2f,\r\n", memoryInfo.nativePss / 1024.0))
                   .append(String.format("                \"Other\": %.2f,\r\n", memoryInfo.otherPss / 1024.0))
                   .append(String.format("                \"Total\": %.2f\r\n", memoryInfo.getTotalPss() / 1024.0))
                   .append("            },\r\n")
                   .append("            \"Private\": {\r\n")
                   .append(String.format("                \"DVM\": %.2f,\r\n", memoryInfo.dalvikPrivateDirty / 1024.0))
                   .append(String.format("                \"Native\": %.2f,\r\n", memoryInfo.nativePrivateDirty / 1024.0))
                   .append(String.format("                \"Other\": %.2f,\r\n", memoryInfo.otherPrivateDirty / 1024.0))
                   .append(String.format("                \"Total\": %.2f\r\n", memoryInfo.getTotalPrivateDirty() / 1024.0))
                   .append("            },\r\n")
                   .append("            \"Shared\": {\r\n")
                   .append(String.format("                \"DVM\": %.2f,\r\n", memoryInfo.dalvikSharedDirty / 1024.0))
                   .append(String.format("                \"Native\": %.2f,\r\n", memoryInfo.nativeSharedDirty / 1024.0))
                   .append(String.format("                \"Other\": %.2f,\r\n", memoryInfo.otherSharedDirty / 1024.0))
                   .append(String.format("                \"Total\": %.2f\r\n", memoryInfo.getTotalSharedDirty() / 1024.0))
                   .append("            }\r\n")
                   .append("        },\r\n");

        for (int i = 0; i < Debug.MemoryInfo.NUM_OTHER_STATS; ++i) {
            memoryUsage.append("        \"" + memoryInfo.getOtherLabel(i) + "\": {\r\n")
                       .append("            \"Pss\": {\r\n")
                       .append(String.format("                \"Total\": %.2f\r\n", memoryInfo.getOtherPss(i) / 1024.0))
                       .append("            },\r\n")
                       .append("            \"Private\": {\r\n")
                       .append(String.format("                \"Total\": %.2f\r\n", memoryInfo.getOtherPrivateDirty(i) / 1024.0))
                       .append("            },\r\n")
                       .append("            \"Shared\": {\r\n")
                       .append(String.format("                \"Total\": %.2f\r\n", memoryInfo.getOtherSharedDirty(i) / 1024.0))
                       .append("            }\r\n");

            if (i + 1 == Debug.MemoryInfo.NUM_OTHER_STATS) {
                memoryUsage.append("        }\r\n")
                           .append("    }\r\n")
                           .append("}\r\n");
            } else {
                memoryUsage.append("        },\r\n");
            }
        }

        return memoryUsage.toString();
    }

    /// M: Dump memory info
    static String printMemoryInfo(boolean log2File) {
        String outputFileName = "";
        Debug.MemoryInfo memoryInfo = new Debug.MemoryInfo();
        Debug.getMemoryInfo(memoryInfo);

        String memMessage = String.format("Browser Memory usage: (Total/DVM/Native/Other) \r\n Pss=%.2f/%.2f/%.2f/%.2f MB\r\n Private=%.2f/%.2f/%.2f/%.2f MB\r\n Shared=%.2f/%.2f/%.2f/%.2f MB",
                memoryInfo.getTotalPss() / 1024.0, memoryInfo.dalvikPss / 1024.0, memoryInfo.nativePss / 1024.0, memoryInfo.otherPss / 1024.0,
                memoryInfo.getTotalPrivateDirty() / 1024.0, memoryInfo.dalvikPrivateDirty / 1024.0, memoryInfo.nativePrivateDirty / 1024.0, memoryInfo.otherPrivateDirty / 1024.0,
                memoryInfo.getTotalSharedDirty() / 1024.0, memoryInfo.dalvikSharedDirty / 1024.0, memoryInfo.nativeSharedDirty / 1024.0, memoryInfo.otherSharedDirty / 1024.0);

        String otherMemMsg = "Browser other mem statistics: \r\n";
        for (int i = 0; i < Debug.MemoryInfo.NUM_OTHER_STATS; ++i) {
            otherMemMsg += " [" + String.valueOf(i) + "] " + memoryInfo.getOtherLabel(i) + ", pss=" + String.format("%.2fMB", memoryInfo.getOtherPss(i) / 1024.0) + ", private=" + String.format("%.2fMB", memoryInfo.getOtherPrivateDirty(i) / 1024.0) + ", shared=" + String.format("%.2fMB", memoryInfo.getOtherSharedDirty(i) / 1024.0) + "\r\n";
        }

        if (log2File) {
            try {
                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyyMMdd_HHmmss");
                outputFileName = "/storage/emulated/0/memDumpLog"
                        + sdf.format(new java.util.Date()) + ".txt";
                java.io.PrintWriter printWriter = new java.io.PrintWriter(outputFileName);
                printWriter.print(encodeToJSON(memoryInfo));
                printWriter.close();
            } catch (IOException ex) {
                Log.d(LOGTAG, "Failed to save memory logs to file, " + ex.getMessage());
                outputFileName = "";
            }
        } else {
            Log.d(LOGTAG, memMessage);
            Log.d(LOGTAG, otherMemMsg);
        }

        return outputFileName;
    }
}
