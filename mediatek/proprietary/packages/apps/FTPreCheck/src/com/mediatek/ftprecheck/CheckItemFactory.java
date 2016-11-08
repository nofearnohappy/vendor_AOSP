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

package com.mediatek.ftprecheck;

import android.content.Context;

public class CheckItemFactory {

    /**
     * Create check item by checkItemName.
     * @param c Context of UI.
     * @param checkItemName Name of check item.
     * @param checkType Behavior type of check item.
     * @param conditionValues Values of condition, through which
     * check item can judge if condition is satisfied.
     * You should know clearly the structure of conditionValues
     * is [Condition.mValue, Condition.mValueLeft, Condition.mValueRight],
     * so you can get it right in child class of CheckItemBase.
     * @return The corresponding check item.
     */
    public static CheckItemBase getCheckItem(Context c, String checkItemName,
            String checkType, Object... conditionValues) {

        if (checkItemName.equals(c.getString(R.string.SINR))) {
            return new CheckSINR(c, checkType, conditionValues);

        } else if (checkItemName.equals(c.getString(R.string.RSRP))) {
            return new ChecRSRP(c, checkType, conditionValues);

        } else if (checkItemName.equals(c.getString(R.string.subframe_ratio))) {
            return new CheckSubFrame(c, checkType, conditionValues);

        } else if (checkItemName.equals(c.getString(R.string.ssp))) {
            return new CheckSSP(c, checkType, conditionValues);

        } else if (checkItemName.equals(c.getString(R.string.freq_band))) {
            return new CheckFreqBand(c, checkType);

        } else if (checkItemName.equals(c.getString(R.string.neighbor_cell_4g))) {
            return new Check4gNeighborCell(c, checkType);

        } else if (checkItemName.equals(c.getString(R.string.neighbor_cell_3g))) {
            return new Check3gNeighborCell(c, checkType);

        } else if (checkItemName.equals(c.getString(R.string.neighbor_cell_2g))) {
            return new Check2gNeighborCell(c, checkType);

        } else if (checkItemName.equals(c.getString(R.string.redirection_4g))) {
            return new Check4gRedirection(c, checkType);

        } else if (checkItemName.equals(c.getString(R.string.redirection_3g))) {
            return new Check3gRedirection(c, checkType);

        } else if (checkItemName.equals(c.getString(R.string.measure_cell_3g))) {
            return new Check3gMeasureCell(c, checkType);

        } else if (checkItemName.equals(c.getString(R.string.measure_cell_4g))) {
            return new Check4gMeasureCell(c, checkType);

        } else if (checkItemName.equals(c.getString(R.string.la))) {
            return new CheckLA(c, checkType);

        } else if (checkItemName.equals(c.getString(R.string.lte_rat_change))) {
            return new CheckLteRatChange(c, checkType);

        } else if (checkItemName.equals(c.getString(R.string.rat_change))) {
            return new CheckRatChange(c, checkType);

        } else if (checkItemName.equals(c.getString(R.string.regi_network))) {
            return new CheckRegiNetwork(c, checkType, conditionValues);

        } else if (checkItemName.equals(c.getString(R.string.IPO))) {
            return new CheckIPO(c, checkType);

        } else if (checkItemName.equals(c.getString(R.string.battery))) {
            return new CheckBattery(c, checkType);

        } else if (checkItemName.equals(c.getString(R.string.unlock_screen))) {
            return new CheckScreenUnlock(c, checkType);

        } else if (checkItemName.equals(c.getString(R.string.hang_up_by_power))) {
            return new CheckHangupByPower(c, checkType);

        } else if (checkItemName.equals(c.getString(R.string.wifi_ap))) {
            return new CheckWifiAp(c, checkType);

        } else if (checkItemName.equals(c.getString(R.string.rat_mode))) {
            return new CheckRatMode(c, checkType);

        } else {
            throw new RuntimeException("No such check item: " + checkItemName);
        }
    }
}