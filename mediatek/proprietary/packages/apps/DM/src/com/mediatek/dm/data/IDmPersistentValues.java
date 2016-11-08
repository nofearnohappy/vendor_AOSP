/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein
 * is confidential and proprietary to MediaTek Inc. and/or its licensors.
 * Without the prior written permission of MediaTek inc. and/or its licensors,
 * any reproduction, modification, use or disclosure of MediaTek Software,
 * and information contained herein, in whole or in part, shall be strictly prohibited.
 *
 * MediaTek Inc. (C) 2010. All rights reserved.
 *
 * BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 * THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
 * RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER ON
 * AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL WARRANTIES,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR NONINFRINGEMENT.
 * NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH RESPECT TO THE
 * SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY, INCORPORATED IN, OR
 * SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES TO LOOK ONLY TO SUCH
 * THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO. RECEIVER EXPRESSLY ACKNOWLEDGES
 * THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES
 * CONTAINED IN MEDIATEK SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK
 * SOFTWARE RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
 * STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S ENTIRE AND
 * CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE RELEASED HEREUNDER WILL BE,
 * AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE MEDIATEK SOFTWARE AT ISSUE,
 * OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE CHARGE PAID BY RECEIVER TO
 * MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 * The following software/firmware and/or related documentation ("MediaTek Software")
 * have been modified by MediaTek Inc. All revisions are subject to any receiver's
 * applicable license agreements with MediaTek Inc.
 */

package com.mediatek.dm.data;

import com.redbend.vdm.DownloadDescriptor;

public interface IDmPersistentValues {
    long getMaxSize();

    long getDownloadedSize();

    void setDownloadedSize(long size);

    long getSize();

    int getDLSessionStatus();

    void setDLSessionStatus(int status);

    DownloadDescriptor getDownloadDescriptor();

    void setDownloadDescriptor(DownloadDescriptor dd);

    // public void deleteDlInfo();
    void deleteDeltaPackage();

    DownloadInfo getDownloadInfo();

    int MSG_NETWORKERROR = 0;
    int MSG_NEWVERSIONDETECTED = 1;
    int MSG_NONEWVERSIONDETECTED = 2;
    int MSG_DLPKGCONFIRMED = 3;
    int MSG_DLPKGUPGRADE = 4;
    int MSG_DLPKGCOMPLETE = 5;
    int MSG_DLPKGCANCELLED = 6;
    int MSG_DLPKGPAUSED = 7;
    int MSG_DLPKGRESUME = 8;
    int MSG_NIARECIEVED = 9;
    int MSG_NIACONFIRMED = 10;
    int MSG_DMSESSIONCOMPLETED = 11;
    int MSG_DMSESSIONABORTED = 12;
    int MSG_DLSESSIONABORTED = 13;
    int MSG_BOOTSTRAPSESSIONCOMPLETED = 14;
    int MSG_BOOTSTRAPSESSIONABORTED = 15;
    int MSG_CONNECTTIMEOUT = 16;
    int MSG_AUTHENTICATION = 17;
    int MSG_OTHERERROR = 18;
    int MSG_DLPKGSTARTED = 19;
    int MSG_VERIFYING_PKG = 20;
    int MSG_PROCESS_NEXT_NIA = 21;

    int MSG_SCOMO_CONFIRM_DOWNLOAD = 200;
    int MSG_SCOMO_CONFIRM_INSTALL = 201;
    int MSG_SCOMO_EXEC_INSTALL = 202;
    int MSG_DM_SESSION_COMPLETED = 203;
    int MSG_DM_SESSION_ABORTED = 204;
    int MSG_SCOMO_DL_SESSION_COMPLETED = 205;
    int MSG_SCOMO_DL_SESSION_ABORTED = 206;
    int MSG_SCOMO_DL_PKG_UPGRADE = 207;
    int MSG_SCOMO_DL_SESSION_START = 208;
    // public static final int MSG_SCOMO_DL_SESSION_PAUSED = 209;
    // public static final int MSG_SCOMO_DL_SESSION_RESUMED = 210;

    int MSG_USERMODE_INVISIBLE = 16;
    int MSG_USERMODE_VISIBLE = 17;
    int MSG_USERMODE_INTERACT = 18;
    int MSG_NIASESSION_START = 19;
    int MSG_NIASESSION_CANCLE = 20;
    int MSG_DMSESSION_START = 21;
    int MSG_DMSESSION_CANCLE = 22;
    int MSG_NIASESSION_INVALID = 23;
    int MSG_NIA_ALERT_1102 = 24;



    int MSG_WAP_CONNECTION_ALREADY_EXIST = 100;
    int MSG_WAP_CONNECTION_FAILED = 101;
    int MSG_WAP_CONNECTION_APN_TYPE_NOT_AVAILABLE = 102;
    int MSG_WAP_CONNECTION_SUCCESS = 103;
    int MSG_WAP_CONNECTION_TIMEOUT = 104;

    /**
     * DL session state, flow may be STATE_QUERY_NEW_VERSION -> STATE_NEW_VERSION_DETECTED ->
     * STATE_START_TO_DOWNLOAD -> STATE_DOWNLOADING -> STATE_PAUSE_DOWNLOAD -> STATE_RESUME_DOWNLOAD
     * -> STATE_START_TO_DOWNLOAD -> STATE_DOWNLOADING -> STATE_PAUSE_DOWNLOAD ->
     * STATE_CANCEL_DOWNLOAD -> STATE_NOT_DOWNLOAD if download complete, STATE_DL_PKG_COMPLETE ->
     * STATE_UPDATE_RUNNING -> STATE_UPDATE_COMPLETE -> STATE_NOT_DOWNLOAD
     */
    int STATE_QUERY_NEW_VERSION = 0;
    int STATE_NEW_VERSION_DETECTED = 1;
    int STATE_START_TO_DOWNLOAD = 2;
    int STATE_DOWNLOADING = 3;
    int STATE_CANCEL_DOWNLOAD = 4;
    int STATE_PAUSE_DOWNLOAD = 5;
    int STATE_RESUME_DOWNLOAD = 6;
    int STATE_DL_PKG_COMPLETE = 7;
    int STATE_UPDATE_RUNNING = 8;
    int STATE_UPDATE_COMPLETE = 9;

    int STATE_VERIFY_FAIL = 10;
    int STATE_VERIFY_NO_STORAGE = 11;
    int STATE_VERIFYING_PKG = 12;
    int STATE_NOT_DOWNLOAD = 100;
    // public static final int STATE_BACKKEYCLICKED = 8;

    /**
     * DL session trigger methods
     */
    int SERVER = 0;
    int CLIENT_PULL = 1; // foreground
    int CLIENT_POLLING = 2; // background

    /**
     * DM session state
     */
    int STATE_DM_NO_ACTION = 100;
    int STATE_DM_USERMODE_INVISIBLE = 16;
    int STATE_DM_USERMODE_VISIBLE = 17;
    int STATE_DM_USERMODE_INTERACT = 18;
    int STATE_DM_USREMODE_CANCLE = 23;
    int STATE_DM_NIA_START = 19;
    int STATE_DM_NIA_ALERT = 11;
    /**
     * DM NIA Session aborted or DL Session aborted
     */
    int STATE_DM_NIA_CANCLE = 20;
    int STATE_DM_NIA_COMPLETE = 25;
    int STATE_DM_SESSION_START = 21;
    int STATE_DM_SESSION_CANCLE = 22;

    int STATE_DM_DETECT_WAP = 24;
    int STATE_DM_WAP_CONNECT_SUCCESS = 26;
    int STATE_DM_WAP_CONNECT_TIMEOUT = 27;

    // public static final int STATE_DOWNING_BKGTOFORE=28;

    String DELTA_FILE_NAME = "delta.zip";
    String SCOMO_FILE_NAME = "scomo.zip";
    String RESUME_FILE_NAME = "dlresume.dat";
    String RESUME_SCOMO_FILE_NAME = "scomoresume.dat";
}
