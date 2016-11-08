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

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;

/**
 * Subclass of {@link ImapString} used for non literals.
 */
public class ImapSimpleString extends ImapString {
    private String mString;
    public static final Charset ASCII = Charset.forName("US-ASCII");

    /* package */  ImapSimpleString(String string) {
        mString = (string != null) ? string : "";
    }

    @Override
    public void destroy() {
        mString = null;
        super.destroy();
    }

    @Override
    public String getString() {
        return mString;
    }

    @Override
    public InputStream getAsStream() {
        return new ByteArrayInputStream(toAscii(mString));
    }

    @Override
    public String toString() {
        // Purposefully not return just mString, in order to prevent using it instead of getString.
        return "\"" + mString + "\"";
    }

    // RCS:IMAP porting
    /** Converts a String to ASCII bytes.
     * @param s .
     * @return .
     */
    public static byte[] toAscii(String s) {
        return encode(ASCII, s);
    }

    private static byte[] encode(Charset charset, String s) {
        if (s == null) {
            return null;
        }
        final ByteBuffer buffer = charset.encode(CharBuffer.wrap(s));
        final byte[] bytes = new byte[buffer.limit()];
        buffer.get(bytes);
        return bytes;
    }

}
