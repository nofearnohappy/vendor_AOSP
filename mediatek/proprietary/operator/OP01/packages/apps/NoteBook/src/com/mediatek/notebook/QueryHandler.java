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

package com.mediatek.notebook;

import android.app.ProgressDialog;
import android.content.AsyncQueryHandler;
import android.content.ContentResolver;
import android.content.res.Resources;
import android.database.Cursor;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.provider.BaseColumns;
import android.util.Log;
import android.widget.Toast;

import com.mediatek.notebook.NoteAdapter.NoteItem;
import com.mediatek.notebook.NotePad.Notes;

import java.util.ArrayList;
import java.util.List;

public class QueryHandler extends AsyncQueryHandler {
    private static final String TAG = "QueryHandler";
    private NotesList mLcontext;
    private NoteReading mRcontext;
    public NoteAdapter nadapter;
    private static final int MSG_QUERY_DONE_TO_SET_DATA = 1001;
    private static final int MSG_SETDATA_DELAY = 20;
    private Cursor mQueryCursor;
    private NoteWaitCursorView mNoteWaitProgress;
    private List<NoteItem> mList = new ArrayList<NoteItem>();
    private List<NoteItem> mListUi = new ArrayList<NoteItem>();
    private boolean cancelFlg = false;

    public QueryHandler(ContentResolver cr, NotesList context) {
        super(cr);
        mLcontext = context;
        cancelFlg = false;
        Log.d(TAG, "created from Noteslists");
        mThread.start();
    }

    public QueryHandler(ContentResolver cr, NoteReading context) {
        super(cr);
        mRcontext = context;
        Log.d(TAG, "created from NoteReading");
    }

    public void deinitQueryHandler() {
        cancelFlg = true;
        synchronized (mThread) {
            try {
                if (mHandler != null) {
                    mHandler.getLooper().quit();
                    mHandler = null;
                }
                if (mThread != null) {
                    mThread.join();
                    mThread = null;
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onQueryComplete(int token, Object cookie, Cursor cursor) {
        Log.i(TAG, "onQueryComplete: " + cursor);
        if (cancelFlg) {
            return;
        }
        mNoteWaitProgress = ((NoteWaitCursorView) cookie);
        if (cursor == null) {
            showStorageFullError();
            return;
        }
        /*Notes.sNoteCount = cursor.getCount();
        if (Notes.sNoteCount > 0) {
            mLcontext.countView.setText(String.valueOf(cursor.getCount()));
        } else {
            mLcontext.countView.setText("");
        }*/
        nadapter = mLcontext.noteadapter;
        mLcontext.setListAdapter(nadapter);
        mLcontext.invalidateOptionsMenu();

        if (mHandler == null) {
            try {
                synchronized (mThread) {
                    mThread.wait();
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        Message m =mHandler.obtainMessage(MSG_QUERY_DONE_TO_SET_DATA, cursor);
        mHandler.sendMessageDelayed(m,MSG_SETDATA_DELAY);

    }

    @Override
    public void onDeleteComplete(int token, Object cookie, int result) {
        if (token == Notes.NOTESLIST_DELETE_TOKEN) {
            mLcontext.mActionMode.finish();
            mLcontext.queryUpdateData();
        } else if (token == Notes.NOTEREADING_DELETE_TOKEN) {
            Notes.sFlagNotesNotChanged = false;
            mRcontext.finish();
        } else if (token == Notes.NOTESLIST_DELETE_ALL_TOKEN) {
            mLcontext.queryUpdateData();
        }
        if (cookie != null) {
            ((ProgressDialog) cookie).cancel();
        }
    }

    public void setData(Cursor cursor) {
        NoteItem item;
        String note;
        String createTime;
        String currentTime;
        String notegroup;
        String modifyTime;
        int id;
        mList.clear();
        try {
            if (cursor.moveToFirst()) {
                int idColumn = cursor.getColumnIndex(BaseColumns._ID);
                int titleColumn = cursor.getColumnIndex(Notes.COLUMN_NAME_NOTE);
                int createColumn = cursor
                        .getColumnIndex(Notes.COLUMN_NAME_CREATE_DATE);
                int groupColumn = cursor.getColumnIndex(Notes.COLUMN_NAME_GROUP);
                int modifyColumn = cursor
                        .getColumnIndex(Notes.COLUMN_NAME_MODIFICATION_DATE);
                do {
                    if (cancelFlg) {
                        break;
                    }
                    id = cursor.getInt(idColumn);
                    note = cursor.getString(titleColumn);
                    createTime = cursor.getString(createColumn);
                    notegroup = cursor.getString(groupColumn);
                    modifyTime = cursor.getString(modifyColumn);
                    item = nadapter.new NoteItem();
                    item.id = id;
                    item.note = note;
                    NoteReading nr = new NoteReading();
                    currentTime = nr.currentDay(createTime, mLcontext);
                    item.modify_time = modifyTime;
                    item.create_time = currentTime;
                    item.notegroup = getGroup(notegroup);
                    mList.add(item);
                } while (cursor.moveToNext());
            }
        } catch (OutOfMemoryError e) {
            showStorageFullError();
        } catch (IllegalStateException e) {
            showStorageFullError();
        } finally {
            Log.d(TAG, "close cursor: " + cursor);
            cursor.close();
            cursor = null;
            mListUi.clear();
            mListUi.addAll(mList);
            Log.d(TAG, "list cnt=" + mList.size());
        }
    }


    public String getGroup(String i) {
        Resources resource;
        resource = mLcontext.getResources();
        String groupWork = (String) resource.getString(R.string.menu_work);
        String groupPersonal = (String) resource
                .getString(R.string.menu_personal);
        String groupFamily = (String) resource.getString(R.string.menu_family);
        String groupStudy = (String) resource.getString(R.string.menu_study);
        if (i.equals("1")) {
            return groupWork;
        } else if (i.equals("2")) {
            return groupPersonal;
        } else if (i.equals("3")) {
            return groupFamily;
        } else if (i.equals("4")) {
            return groupStudy;
        } else {
            return "";
        }
    }

    private void showStorageFullError() {
        mLcontext.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mNoteWaitProgress.stopWaitCursor();
                Toast.makeText(mLcontext, R.string.sdcard_full, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private Handler mHandler;
    private Thread mThread = new Thread() {
        @Override
        public void run() {
            Looper.prepare();
            mHandler = new Handler() {
                @Override
                public void handleMessage(Message msg) {
                    switch (msg.what) {
                        case MSG_QUERY_DONE_TO_SET_DATA:
                            mLcontext.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    if (mNoteWaitProgress.getQueryStatus()!= true){
                                         mNoteWaitProgress.startWaitCursor();
                                    }
                                }
                            });
                            setData((Cursor)msg.obj);
                            mLcontext.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    nadapter.list.clear();
                                    nadapter.list.addAll(mListUi);
                                    nadapter.notifyDataSetChanged();
                                    Notes.sNoteCount = mListUi.size();
                                    if (Notes.sNoteCount > 0) {
                                        mLcontext.countView.setText(String.valueOf(mListUi.size()));
                                    } else {
                                        mLcontext.countView.setText("");
                                    }
                                    mNoteWaitProgress.stopWaitCursor();
                                    mLcontext.invalidateOptionsMenu();
                                }
                            });
                            break;
                    default:
                        break;
                    }

                }
            };
            synchronized (mThread) {
                notify();
            }
            Looper.loop();
        }
    };
}
