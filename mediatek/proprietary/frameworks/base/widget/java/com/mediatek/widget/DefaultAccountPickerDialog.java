/*
 * Copyright (C) 2011-2014 MediaTek Inc.
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

package com.mediatek.widget;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import com.mediatek.widget.CustomAccountRemoteViews.AccountInfo;
import com.mediatek.internal.R;

import java.util.ArrayList;
import java.util.List;

public class DefaultAccountPickerDialog extends DialogFragment {
    public final static String TAG = "DefaultAccountPickerDialog";
    private final static String ACCOUNT_INFO_LIST_KEY = "AccountInfoList";

    private final static int NO_ITEM_SELECT = -1;
    private int mSelection = NO_ITEM_SELECT;

    public DefaultAccountPickerDialog() {}

    /**
     * Build a DefaultAccountPickerDialog instance.
     * @param context the context to show the dialog
     * @return instance of DefaultAccountPickerDialog
     */
    public static DefaultAccountPickerDialog build(Context context) {
        DefaultAccountPickerDialog dialogFragment = new DefaultAccountPickerDialog();
        return dialogFragment;
    }

    /**
     * Set data to the Dialog, data is a list of {@link #AccountInfo}
     * @param data items to display
     * @return the dialog itself
     */
    public DefaultAccountPickerDialog setData(List<AccountInfo> data) {
        Bundle b = new Bundle();
        // clone data array, don't modify original data
        ArrayList<AccountInfo> accountInfo = new ArrayList<AccountInfo>();
        for (AccountInfo ai : data) {
            accountInfo.add(new AccountInfo(ai.getIconId(), ai.getIcon(), ai.getLabel(),
                    ai.getNumber(), ai.getIntent(), ai.isActive(), ai.isSimAccount()));
        }
        b.putParcelableArrayList(ACCOUNT_INFO_LIST_KEY, accountInfo);
        setArguments(b);
        return this;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.choose_account);
        final DefaultAccountPickerAdapter adapter = new DefaultAccountPickerAdapter(getActivity());

        if (getArguments().containsKey(ACCOUNT_INFO_LIST_KEY)) {
            List<AccountInfo> data = getArguments().getParcelableArrayList(ACCOUNT_INFO_LIST_KEY);
            adapter.setItemData(data);
            Log.d(TAG, "onCreateDialog get data form args : " + data);
        }

        mSelection = adapter.getActivePosition();
        builder.setSingleChoiceItems(adapter, mSelection, new OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                mSelection = which;
                adapter.setActiveStatus(mSelection);
                Log.d(TAG, "onClick position: " + mSelection);

                adapter.notifyDataSetChanged();
            }
        });

        builder.setNegativeButton(android.R.string.cancel, new OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        builder.setPositiveButton(android.R.string.ok, new OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (mSelection == NO_ITEM_SELECT) {
                    Log.d(TAG, "--- No item is selected ---");
                    return;
                }

                Intent intent = adapter.getItem(mSelection).getIntent();
                if (intent != null && getActivity() != null) {
                    Log.d(TAG, "sent broadcast: " + mSelection);
                    getActivity().sendBroadcast(intent);
                }
            }
        });
        return builder.create();
    }
}
