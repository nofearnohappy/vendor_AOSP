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

package com.mediatek.rcs.contacts.networkcontacts;


/**
 * Constants definitions.
 * @author MTK81350
 *
 */
public class Const {
    public static final boolean TEMP_DEBUG = true;
    /**
     * action definition.
     *
     * @author MTK81350
     *
     */
    public static class Action {
        /**
         * Start {@link SyncService} to trigger an auto backup.
         */
        public static final String AUTO_BACKUP = "com.mediatek.rcs.autobackup";
        /**
         * Timer expires.
         */
        public static final String TIME_SET = "android.intent.action.TIME_SET";
        /**
         * Check if restore complete normally last time.
         */
        public static final String CHECK_RESTORE = "android.intent.action.CHECK_RESTORE";
        /**
         * Boot complete.
         */
        public static final String BOOT_COMPLETED = "android.intent.action.BOOT_COMPLETED";

        /**
         * Start {@link SyncService} to trigger an auto backup on contacts modification.
         */
        public static final String REGISTER_CONTACTS_OBSERVER =
                "intent.action.rcs.register.contactsobserver";

        /**
         * Start {@link SyncService} to stop auto backup triggered by contacts modification.
         */
        public static final String UNREGISTER_CONTACTS_OBSERVER =
                "intent.action.rcs.unregister.contactsobserver";

        /**
         * turn on/off RCS service, broadcast intent action
         */
        public static final String LAUNCH_RCS_SERVICE =
                "com.mediatek.intent.rcs.stack.LaunchService";
        public static final String STOP_RCS_SERVICE =
                "com.mediatek.intent.rcs.stack.StopService";

    }
}
