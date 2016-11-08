/*
* Copyright (C) 2014 MediaTek Inc.
* Modification based on code covered by the mentioned copyright
* and/or permission notice(s).
*/
/*
 * Copyright (C) 2010 The Android Open Source Project
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

package com.mediatek.rcs.message.cloudbackup.store.imap;

import android.util.Log;
//import com.google.common.annotations.VisibleForTesting;
import com.mediatek.rcs.message.cloudbackup.utils.FixedLengthInputStream;
import com.mediatek.rcs.message.cloudbackup.utils.ProgressListener;
//import com.mediatek.rcs.message.cloudbackup.utils.Folder.MessageRetrievalListener;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;

/**
 * Subclass of {@link ImapString} used for literals backed by an in-memory byte array.
 */
public class ImapMemoryLiteral extends ImapString {
    private static final String LOG_TAG = "RcsBR/ImapMemoryLiteral";
    public static final Charset ASCII = Charset.forName("US-ASCII");

    private byte[] mData;

    //@VisibleForTesting
    /* package */ ImapMemoryLiteral(FixedLengthInputStream in) throws IOException {
        // We could use ByteArrayOutputStream and IOUtils.copy, but it'd perform an unnecessary
        // copy....
        mData = new byte[in.getLength()];
        int pos = 0;
        while (pos < mData.length) {
            int read = in.read(mData, pos, mData.length - pos);
            if (read < 0) {
                break;
            }
            pos += read;
        }
        if (pos != mData.length) {
            Log.w(LOG_TAG, "");
        }
    }

    /** M: added UI update processing.
     * @param in
     * @param listener
     * @throws IOException
     */
    ImapMemoryLiteral(FixedLengthInputStream in, ProgressListener listener)
            throws IOException {
        // We could use ByteArrayOutputStream and IOUtils.copy, but it'd perform
        // an unnecessary copy....
        mData = new byte[in.getLength()];
        int size = in.getLength();
        int pos = 0;
        long lastPct = -1;
        while (pos < mData.length) {
            int read = in.read(mData, pos, mData.length - pos);
            if (read < 0) {
                break;
            }
            pos += read;
            final int pct = (int) ((pos * 100) / size);
            /*
             * callback to update ui progress. Loading data from server
             * finished, but not send finished callback. Waiting finished
             * decoding the file. Callback only if we've read at least 1% more,
             * We don't want to spam the app.
             */
            if (listener != null && size != 0 && pos < size && lastPct < pct) {
                listener.updateProgress(0, size, pos);
                lastPct = pct;
            }
        }
        if (pos != mData.length) {
            Log.w(LOG_TAG, "");
        }
    }

    @Override
    public void destroy() {
        mData = null;
        super.destroy();
    }

    @Override
    public String getString() {
        return fromAscii(mData);
    }

    @Override
    public InputStream getAsStream() {
        return new ByteArrayInputStream(mData);
    }

    @Override
    public String toString() {
        return String.format("{%d byte literal(memory)}", mData.length);
    }

    // RCS:IMAP porting
    /** Builds a String from ASCII bytes.
     * @param b .
     * @return .
     */
    public static String fromAscii(byte[] b) {
        return decode(ASCII, b);
    }

    private static String decode(Charset charset, byte[] b) {
        if (b == null) {
            return null;
        }
        final CharBuffer cb = charset.decode(ByteBuffer.wrap(b));
        return new String(cb.array(), 0, cb.length());
    }


}
