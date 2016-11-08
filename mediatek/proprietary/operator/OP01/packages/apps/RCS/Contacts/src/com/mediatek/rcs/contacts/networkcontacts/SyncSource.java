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
 * Interface for sync source.
 * @author MTK80963
 */
public interface SyncSource {
    /**
     * Item type.
     * @see SyncSource#getItem(int, int)
     */
    public static final int ITEM_TYPE_ALL = 0;
    /**
     * Item type.
     * @see SyncSource#getItem(int, int)
     */
    public static final int ITEM_TYPE_ADD = 1;
    /**
     * Item type.
     * @see SyncSource#getItem(int, int)
     */
    public static final int ITEM_TYPE_UPDATE = 2;
    /**
     * Item type.
     * @see SyncSource#getItem(int, int)
     */
    public static final int ITEM_TYPE_DELETE = 3;

    /**
     * Value return by {@link SyncSource#getTransResult(int) getTransResult}.
     * No error.
     */
    public static final int ITEM_ERR_OK = 0;
    /**
     * Value return by {@link SyncSource#getTransResult(int) getTransResult}.
     * No error.
     */
    public static final int ITEM_ERR_ALREADY_EXISTS = -1;
    /**
     * Value return by {@link SyncSource#getTransResult(int) getTransResult}.
     * Item to be deleted or replaced not exists.
     */
    public static final int ITEM_ERR_NOT_EXISTS = -2;
    /**
     * Value return by {@link SyncSource#getTransResult(int) getTransResult}.
     * Unknown error.
     */
    public static final int ITEM_ERR_OTHER = -3;

    /**
     * @return device infomation. @see DevInfo
     */
    public DevInfo getDevInfo();

    /**
     * @return last anchor of the OMA DS protocol.
     */
    public String getLastAnchor();

    /**
     * @return next anchor of the OMA DS protocol.
     */
    public String getNextAnchor();

    /**
     * save current anchor after synchronization
     * complete successfully.
     */
    public void updateAnchor();

    /**
     * @return sync server URL.
     */
    public String getServerUri();

    /**
     * @return local URI of the sync client.
     */
    public String getLocalUri();

    /**
     * @return user name for authentication.
     */
    public String getUserName();

    /**
     * @return password for authentication.
     */
    public String getPassword();

    /**
     * @return Local URI of sync source.
     */
    public String getSourceUri();

    /**
     * @return  Target URI of sync.
     */
    public String getTargetUri();

    /**
     * @return Meta type of sync source.
     */
    public String getMetaType();

    /**
     * @return Count of all the sync items on client.
     */
    public int getAllItemCount();

    /**
     * @return Count of new items on client from last successful synchronization.
     */
    public int getNewItemCount();

    /**
     * @return Count of updated items on client from last successful synchronization.
     */
    public int getUpdateItemCount();

    /**
     * @return Count of deleted items on client from last successful synchronization.
     */
    public int getDeleteItemCount();

    /**
     * @return get specified item of all, new added or updated.
     */
    /**
     * get specified item of all, new added or updated.
     *
     * @param index index of item.
     * @param type  @see {@link #ITEM_TYPE_ADD}, etc.
     * @return the item specified.
     */
    public SyncItem getItem(int index, int type);

    /**
     * All the modifications to client data store between
     * {@linkplain #beginTransaction()} and
     * {@linkplain #endTransaction()} is in an atomic behavior, all be
     * completed successfully or none to be done. The execute sequence is as
     * below:
     * {@linkplain #beginTransaction()}
     *  {@linkplain #addItem(SyncItem)},
     *  {@linkplain #replaceItem(SyncItem)} or
     *  {@linkplain #deleteItem(SyncItem)}
     * {@linkplain #commit()}
     * {@linkplain #getTransResult(int)} for each operation.
     * {@linkplain #endTransaction()}
     *
     * @return true or false.
     */
    public boolean beginTransaction();

    /**
     * @see #beginTransaction()
     * @return true or false.
     */
    public boolean commit();

    /**
     * @see #beginTransaction()
     * @return true or false.
     */
    public boolean endTransaction();

    /**
     * @see #beginTransaction()
     * @param index index of operation.
     * @return
     * > 0 : new item added, and the return value is the id of new item
     * 0 : operation success
     * -1 : item item to be deleted not exist < 0 : other error
     */
    public int getTransResult(int index);

    /**
     * @param item item to be added.
     * @return true or false.
     */
    public boolean addItem(SyncItem item);

    /**
     * Replace item specified.
     *
     * Update the data table of one raw contact. Before you call this method,
     * you should set a raw contact id into the ContactItem item, otherwise this
     * method do nothing. Update rule: If no such contact in table, just add the
     * ContactItem item into table, otherwise, by MIME type, replace the info
     * that ContactItem item has, while stay unchanged that ContactItem item
     * does not have.
     * @param item new item to replace the one with same id.
     * @return true or false
     */
    public boolean replaceItem(SyncItem item);

    /**
     * @param key id of item to be deleted.
     * @return true or false.
     */
    public boolean deleteItem(int key);

    /**
     * @param data  item data received from transport layer which type
     * is determined by {@linkplain #getMetaType()}
     * @return sync item.
     */
    public SyncItem fromData(String data);

    /**
     * Backup current database for rollback when error occurs.
     * @return true or false.
     */
    public boolean backup();

    /**
     * Delete origin data and clear the backup.
     * @return true or false.
     */
    public boolean clearBackup();

    /**
     * Restore data to origin and clear the backup.
     * @return true or false.
     */
    public boolean rollback();

    /**
     * Check if restore is completed normally last time.
     * Close it normally if not.
     * @return true -- restore complete successfully.
     *         false -- restore complete with error.
     */
    public boolean checkBackup();

    /**
     * To do some prepare work for synchronization.
     * It's only to create cursor for backup currently.
     */
    public void startSync();

    /**
     * Clean the work done in startSync.
     */
    public void endSync();

    /**
     * Is sync data empty refreshed from server
     * @return true --  sync data from server is empty.
     *         false --sync data from server is not empty.
     */
    public boolean isEmptyRestoreFromServer();

}
