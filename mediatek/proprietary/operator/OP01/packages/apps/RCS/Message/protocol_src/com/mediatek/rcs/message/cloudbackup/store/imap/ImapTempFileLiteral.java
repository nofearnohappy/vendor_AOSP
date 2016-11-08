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

import com.mediatek.rcs.message.cloudbackup.utils.FixedLengthInputStream;
import com.mediatek.rcs.message.cloudbackup.utils.IOUtils;
import com.mediatek.rcs.message.cloudbackup.utils.ProgressListener;
import com.mediatek.rcs.message.cloudbackup.utils.TempDirectory;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;

/**
 * Subclass of {@link ImapString} used for literals backed by a temp file.
 */
public class ImapTempFileLiteral extends ImapString {
    private static final String LOG_TAG = "RcsBR/ImapTempFileLiteral";
    public static final Charset ASCII = Charset.forName("US-ASCII");
    /* package for test */ final File mFile;

    /** Size is purely for toString(). */
    private final int mSize;

    //@VisibleForTesting
    /* package */  ImapTempFileLiteral(FixedLengthInputStream stream) throws IOException {
        mSize = stream.getLength();
        mFile = File.createTempFile("imap", ".tmp", TempDirectory.getTempDirectory());

        // Unfortunately, we can't really use deleteOnExit(), because temp filenames are random
        // so it'd simply cause a memory leak.
        // deleteOnExit() simply adds filenames to a static list and the list will never shrink.
        // mFile.deleteOnExit();
        OutputStream out = new FileOutputStream(mFile);
        IOUtils.copy(stream, out);
        out.close();
    }

    /** M: added UI update processing.
     * @param in
     * @param listener
     * @throws IOException
     */
    ImapTempFileLiteral(FixedLengthInputStream stream,
            ProgressListener listener) throws IOException {
        mSize = stream.getLength();
        mFile = File.createTempFile("imap", ".tmp", TempDirectory.getTempDirectory());

        // Unfortunately, we can't really use deleteOnExit(), because temp filenames are random
        // so it'd simply cause a memory leak.
        // deleteOnExit() simply adds filenames to a static list and the list will never shrink.
        // mFile.deleteOnExit();
        OutputStream out = new FileOutputStream(mFile);
        try {
            // IOUtils.copy(stream, out);
            byte[] buffer = new byte[4 * 1024];
            int size = stream.getLength();
            int count = 0;
            int n = 0;
            long lastCallbackPct = -1;
            while (-1 != (n = stream.read(buffer))) {
                out.write(buffer, 0, n);
                count += n;
                final int pct = (int) ((count * 100) / size);
                /*
                 * callback to update ui progress. Loading data from server
                 * finished, but not send finished callback. Waiting finished
                 * decoding the file. Callback only if we've read at least 1% more,
                 * We don't want to spam the app.
                 */
                if (listener != null && size != 0 && count < size && lastCallbackPct < pct) {
                    listener.updateProgress(0, size, count);
                    lastCallbackPct = pct;
                }
            }
        } finally {
            out.close();
        }
    }

    /**
     * Make sure we delete the temp file.
     *
     * We should always be calling {@link ImapResponse#destroy()}, but it's here as a last resort.
     */
    //TODO:
    /*
    protected void finalize() throws Throwable {
        try {
            destroy();
        } finally {
            super.finalize();
        }
    }
    */

    @Override
    public InputStream getAsStream() {
        checkNotDestroyed();
        try {
            return new FileInputStream(mFile);
        } catch (FileNotFoundException e) {
            // It's probably possible if we're low on storage and the system clears the cache dir.
            Log.w(LOG_TAG, "ImapTempFileLiteral: Temp file not found");

            // Return 0 byte stream as a dummy...
            return new ByteArrayInputStream(new byte[0]);
        }
    }

    @Override
    public String getString() {
        checkNotDestroyed();
        try {
            byte[] bytes = IOUtils.toByteArray(getAsStream());
            // Prevent crash from OOM; we've seen this, but only rarely and not reproducibly
            if (bytes.length > ImapResponseParser.LITERAL_KEEP_IN_MEMORY_THRESHOLD) {
                throw new IOException();
            }
            return fromAscii(bytes);
        } catch (IOException e) {
            Log.w(LOG_TAG, "ImapTempFileLiteral: Error while reading temp file", e);
            return "";
        }
    }

    @Override
    public void destroy() {
        if (!isDestroyed() && mFile.exists()) {
            mFile.delete();
        }
        super.destroy();
    }

    @Override
    public String toString() {
        return String.format("{%d byte literal(file)}", mSize);
    }

    /**
     * @return if temp file exists.
     */
    public boolean tempFileExistsForTest() {
        return mFile.exists();
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
