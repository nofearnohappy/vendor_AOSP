/*
* This software/firmware and related documentation ("MediaTek Software") are
* protected under relevant copyright laws. The information contained herein
* is confidential and proprietary to MediaTek Inc. and/or its licensors.
* Without the prior written permission of MediaTek inc. and/or its licensors,
* any reproduction, modification, use or disclosure of MediaTek Software,
* and information contained herein, in whole or in part, shall be strictly prohibited.
*/
/* MediaTek Inc. (C) 2014. All rights reserved.
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

package com.mediatek.rcs.contacts.profileapp;

import android.content.Context;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListPopupWindow;

import com.mediatek.rcs.contacts.R;

import java.util.ArrayList;


public class ProfilePhotoHandler {
    /* Profile photo action mode */
    public static final int PHOTO_ACTION_MODE_NEW = 100;
    public static final int PHOTO_ACTION_MODE_UPDATE = 101;

    public static ListPopupWindow createPopupMenu(Context context, View parent, int mode, final ProfilePhotoHandleListener listener) {

        final ListPopupWindow popMenu = new ListPopupWindow(context);
        popMenu.setAnchorView(parent);
        final ArrayList<ChoiceListItem> choiceList = new ArrayList<ChoiceListItem>();
        if (mode == PHOTO_ACTION_MODE_NEW) {
            choiceList.add(new ChoiceListItem(ChoiceListItem.ID_CHOOSE_PICTURE,
                    context.getString(R.string.photo_action_choose_photo)));
            choiceList.add(new ChoiceListItem(ChoiceListItem.ID_TAKE_PHOTO,
                    context.getString(R.string.photo_action_take_photo)));
        } else if (mode == PHOTO_ACTION_MODE_UPDATE) {
            //choiceList.add(new ChoiceListItem(ChoiceListItem.ID_DELETE_PHOTO,
            //        context.getString(R.string.photo_action_delete)));
            choiceList.add(new ChoiceListItem(ChoiceListItem.ID_CHOOSE_NEW_PICTURE,
                    context.getString(R.string.photo_action_choose_new_photo)));
            choiceList.add(new ChoiceListItem(ChoiceListItem.ID_TAKE_NEW_PHOTO,
                    context.getString(R.string.photo_action_take_new_photo)));
        }

        AdapterView.OnItemClickListener clickListener = new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                int id = choiceList.get(i).getId();
                switch (id) {
                    case ChoiceListItem.ID_CHOOSE_PICTURE:
                    case ChoiceListItem.ID_CHOOSE_NEW_PICTURE:
                        listener.onChoosePhotoChosen();
                        break;
                    case ChoiceListItem.ID_TAKE_PHOTO:
                    case ChoiceListItem.ID_TAKE_NEW_PHOTO:
                        listener.onTakePhotoChosen();
                        break;
                    case ChoiceListItem.ID_DELETE_PHOTO:
                        listener.onDeletePhotoChosen();
                        break;
                    default:
                        break;
                }
                if (popMenu != null && popMenu.isShowing()) {
                    popMenu.dismiss();
                }

            }
        };
        ListAdapter adapter = new ArrayAdapter<ChoiceListItem>(context,
                R.layout.select_dialog_item_material, choiceList);
        popMenu.setAdapter(adapter);
        popMenu.setOnItemClickListener(clickListener);
        return popMenu;
    }

    /* Profile photo handler list item data. */
    public static class ChoiceListItem {
        public static final int ID_CHOOSE_PICTURE = 0;
        public static final int ID_TAKE_PHOTO = 1;
        public static final int ID_DELETE_PHOTO = 2;
        public static final int ID_CHOOSE_NEW_PICTURE = 3;
        public static final int ID_TAKE_NEW_PHOTO = 4;

        private int id;
        private String description;

        public ChoiceListItem(int id, String description) {
            this.description = description;
            this.id = id;
        }

        @Override
        public String toString() {
            return description;
        }

        public int getId() {
            return id;
        }

    }

    /* Profile photo handler listener communicate with UI */
    interface ProfilePhotoHandleListener {

        void onChoosePhotoChosen();

        void onTakePhotoChosen();

        void onDeletePhotoChosen();
    }

}
