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

package com.mediatek.atv;

import com.mediatek.mtvbase.ChannelManager;
import com.mediatek.mtvbase.ChannelManager.ChannelHolder;
import com.mediatek.mtvbase.ChannelManager.ChannelTableEmpty;
import android.util.Log;


public class AtvChannelManager implements ChannelManager {
    private static final String TAG = "ATV/AtvChannelManager";

    private AtvService mService;
    public AtvChannelManager(AtvService s) {
        mService = s;
    }

    /*Do channel scan.*/
    public void channelScan(int mode, int area) {
        mService.channelScan(mode, area);
    }

    /*Stop channel scan.*/
    public void  stopChannelScan() {
        mService.stopChannelScan();
    }

    /*Clear channel table in driver.*/
    public void  clearChannelTable() {
        mService.clearChannelTable();
    }

    /*Get channel entry.*/
    public void setChannelEntry(int ch, Object entry) {
        mService.setChannelTable(ch, ((AtvChannelEntry) entry).packedEntry);
    }


    /*gets the signal strength.*/
    public int getSignalStrengh() {
        int signal = mService.getSignalStrength();
        Log.i("@M_" + TAG, "getSignalStrengh signal = " + signal);

        if (signal < 26) {
            return 0;
        } else if (signal < 45) {
            return 1;
        } else if (signal < 64) {
            return 2;
        } else if (signal < 83) {
            return 3;
        } else if (signal < 101) {
            return 4;
        } else {
            return 0;
        }
    }

    public void changeChannel(int ch) {
        if (ch >= 0) {
            mService.changeChannel(ch);
        }
    }

    public class AtvChannelEntry extends ChannelEntry {
        public long packedEntry;
    }

    public void initChannelTable(ChannelHolder holder) throws ChannelTableEmpty {
        AtvChannelEntry[] table = (AtvChannelEntry[]) holder.getChannelTable(MTV_ATV, this);
        for (int i = 0; i < table.length; i++) {
            Log.i("@M_" + TAG, "ch = " + table[i].ch + " entry = " + table[i].packedEntry);
            mService.setChannelTable(table[i].ch, table[i].packedEntry);
        }
    }
}


