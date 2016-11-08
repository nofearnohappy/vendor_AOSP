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

#ifndef MTK_INCLUDE_BT_B3DS_H
#define MTK_INCLUDE_BT_B3DS_H

__BEGIN_DECLS

#define BTB3DS_HAL_VERSION  100

typedef enum {
    BTB3DS_BCST_STATE_NOT_BROADCASTING  = 0,
    BTB3DS_BCST_STATE_BROADCASTING      = 1,
} btb3ds_broadcasting_state_t;

typedef enum {
    BTB3DS_SYNC_STATE_NON_SYNCHRONIZABLE    = 0,
    BTB3DS_SYNC_STATE_SYNCHRONIZABLE        = 1,
} btb3ds_synchronizable_state_t;

typedef enum {
    BTB3DS_ANNOUNCEMENT_ASSOCIATION_NOTIFICATION  = 0,
    BTB3DS_ANNOUNCEMENT_BATTERY_LEVEL_REPORT      = 1,
} btb3ds_connection_announcement_t;

typedef enum {
    BTB3DS_VEDIO_MODE_3D            = 0,
    BTB3DS_VEDIO_MODE_DUAL_VIEW     = 1,
} btb3ds_vedio_mode_t;

typedef enum {
    BTB3DS_PERIOD_DYNAMIC_CALCULATED  = 0,
    BTB3DS_PERIOD_2D_MODE             = 1,
    BTB3DS_PERIOD_3D_MODE_48_HZ       = 2,
    BTB3DS_PERIOD_3D_MODE_50_HZ       = 3,
    BTB3DS_PERIOD_3D_MODE_59_94_HZ    = 4,
    BTB3DS_PERIOD_3D_MODE_60_HZ       = 5,
} btb3ds_frame_sync_period_t;

/** Callback for changing broadcasting state */
typedef void (*btb3ds_broadcasting_state_callback)(btb3ds_broadcasting_state_t state, bt_status_t error);

/** Callback for changing synchronizable state */
typedef void (*btb3ds_synchronizable_state_callback)(btb3ds_synchronizable_state_t state, bt_status_t error);

/** Callback for connection announcement
* BTB3DS_ANNOUNCEMENT_ASSOCIATION_NOTIFICATION indicates an incoming association.
* Otherwise, type will be BTB3DS_ANNOUNCEMENT_BATTERY_LEVEL_REPORT.
* 0xFF in the bettary_level field means battery level reporting is not supported.
*/
typedef void (*btb3ds_connection_announcement_callback)(const bt_bdaddr_t *bd_addr, btb3ds_connection_announcement_t type, unsigned char battery_level);

/** Callback for legacy reference protocol connection announcement */
typedef void (*btb3ds_legacy_connection_announcement_callback)(void);

typedef struct {
    size_t size;
    btb3ds_broadcasting_state_callback bcst_state_cb;
    btb3ds_synchronizable_state_callback sync_state_cb;
    btb3ds_connection_announcement_callback connect_announce_cb;
    btb3ds_legacy_connection_announcement_callback legacy_connect_announce_cb;
} btb3ds_callbacks_t;

typedef struct {
    /** set to size of this struct*/
    size_t          size;

    /**
     * Initialize the b3ds interface, registering the btb3ds callbacks and determine whether to use legacy reference protocol (TRUE or FALSE)
     * Notice: using reference protocol will force EIR to be DM1 packet
     */
    bt_status_t (*init)(const btb3ds_callbacks_t *callbacks, unsigned char enable_legacy_reference_protocol);

    /**
     * Set broadcast vedio mode (3D/Dual View), frame sync period (2D, 3D, calculated from clock or APK specifies) and panel delay (in microsecond)
     * Should do this before broadcasting
     */
    bt_status_t (*set_broadcast)(btb3ds_vedio_mode_t vedio_mode, btb3ds_frame_sync_period_t frame_sync_period, unsigned int panel_delay);

    /**
     * Start broadcasting (should set_broadcast at least once beforce starting)
     * Returned by btb3ds_broadcasting_state_callback
     */
    bt_status_t (*start_broadcast)(void);

    /**
     * Stop broadcasting
     * Returned by btb3ds_broadcasting_state_callback
     */
    bt_status_t (*stop_broadcast)(void);

    /**
     * Enter Synchronizable Mode for synchronization_train_to time slots
     * Notice: synchronization_train_to should be an even value and >= 120s (0x0002EE00)
     * Returned by btb3ds_synchronizable_state_callback
     */
    bt_status_t (*start_sync_train)(unsigned int synchronization_train_to);

    /**
     * Leave Synchronizable Mode
     * Returned by btb3ds_synchronizable_state_callback
     */
    bt_status_t (*stop_sync_train)(void);

    /**
     * Cleanup the b3ds interface
     */
    void (*cleanup)(void);
} btb3ds_interface_t;

__END_DECLS

#endif /* MTK_INCLUDE_BT_B3DS_H */

