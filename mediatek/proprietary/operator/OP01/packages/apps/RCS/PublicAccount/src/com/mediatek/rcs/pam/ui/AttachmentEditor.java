/*
 * Copyright (C) 2008 Esmertec AG.
 * Copyright (C) 2008 The Android Open Source Project
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

package com.mediatek.rcs.pam.ui;

import android.content.Context;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;

/**
 * This is an embedded editor/view to add photos and sound/video clips into a
 * multimedia message.
 */
public class AttachmentEditor extends LinearLayout {
    private static final String TAG = "AttachmentEditor";

    static final int MSG_EDIT_SLIDESHOW = 1;
    static final int MSG_SEND_SLIDESHOW = 2;
    static final int MSG_PLAY_SLIDESHOW = 3;
    static final int MSG_REPLACE_IMAGE = 4;
    static final int MSG_REPLACE_VIDEO = 5;
    static final int MSG_REPLACE_AUDIO = 6;
    static final int MSG_PLAY_VIDEO = 7;
    static final int MSG_PLAY_AUDIO = 8;
    static final int MSG_VIEW_IMAGE = 9;
    static final int MSG_REMOVE_ATTACHMENT = 10;

    private final Context mContext;
    private Handler mHandler;

    private SlideViewInterface mView;
    private boolean mCanSend;
    private Button mSendButton;

    public AttachmentEditor(Context context, AttributeSet attr) {
        super(context, attr);
        mContext = context;
    }

    /**
     * Returns true if the attachment editor has an attachment to show.
     */
    public boolean update(PaWorkingMessage msg) {
        return true;
    }

    public void setHandler(Handler handler) {

    }

    public void setCanSend(boolean enable) {

    }

    private void updateSendButton() {

    }

    public void hideView() {

    }

    private View getStubView(int stubId, int viewId) {
        View view = findViewById(viewId);
        return view;
    }

    private class MessageOnClick implements OnClickListener {
        private int mWhat;

        public MessageOnClick(int what) {

        }

        public void onClick(View v) {

        }
    }

    private SlideViewInterface createView() {
        return null;
    }

    /**
     * What is the current orientation?
     */
    private boolean inPortraitMode() {
        return true;
    }

    private SlideViewInterface createMediaView(int stub_view_id,
            int real_view_id, int view_button_id, int replace_button_id,
            int remove_button_id, int view_message, int replace_message,
            int remove_message) {
        LinearLayout view = (LinearLayout) getStubView(stub_view_id,
                real_view_id);

        return (SlideViewInterface) view;
    }

    private SlideViewInterface createSlideshowView(boolean inPortrait) {
        return null;
    }
}
