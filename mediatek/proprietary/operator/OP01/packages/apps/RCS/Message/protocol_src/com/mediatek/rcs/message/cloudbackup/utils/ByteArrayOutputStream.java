/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.mediatek.rcs.message.cloudbackup.utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

/**
 * This class implements an output stream in which the data is
 * written into a byte array. The buffer automatically grows as data
 * is written to it.
 * <p>
 * The data can be retrieved using <code>toByteArray()</code> and
 * <code>toString()</code>.
 * <p>
 * Closing a <tt>ByteArrayOutputStream</tt> has no effect. The methods in
 * this class can be called after the stream has been closed without
 * generating an <tt>IOException</tt>.
 * <p>
 * This is an alternative implementation of the java.io.ByteArrayOutputStream
 * class. The original implementation only allocates 32 bytes at the beginning.
 * As this class is designed for heavy duty it starts at 1024 bytes. In contrast
 * to the original it doesn't reallocate the whole memory block but allocates
 * additional mBuffers. This way no mBuffers need to be garbage collected and
 * the contents don't have to be copied to the new buffer. This class is
 * designed to behave exactly like the original. The only exception is the
 * deprecated toString(int) method that has been ignored.
 *
 * @author <a href="mailto:jeremias@apache.org">Jeremias Maerki</a>
 * @author Holger Hoffstatte
 * @version $Id: ByteArrayOutputStream.java 610010 2008-01-08 14:50:59Z niallp $
 */
public class ByteArrayOutputStream extends OutputStream {

    /** A singleton empty byte array. */
    private static final byte[] EMPTY_BYTE_ARRAY = new byte[0];

    /** The list of mBuffers, which grows and never reduces. */
    private List<byte[]> mBuffers = new ArrayList<byte[]>();
    /** The index of the current buffer. */
    private int mCurrentBufferIndex;
    /** The total mCount of bytes in all the filled mBuffers. */
    private int mFilledBufferSum;
    /** The current buffer. */
    private byte[] mCurrentBuffer;
    /** The total mCount of bytes written. */
    private int mCount;

    /**
     * Creates a new byte array output stream. The buffer capacity is
     * initially 1024 bytes, though its size increases if necessary.
     */
    public ByteArrayOutputStream() {
        this(1024);
    }

    /**
     * Creates a new byte array output stream, with a buffer capacity of
     * the specified size, in bytes.
     *
     * @param size  the initial size
     */
    public ByteArrayOutputStream(int size) {
        if (size < 0) {
            throw new IllegalArgumentException(
                "Negative initial size: " + size);
        }
        needNewBuffer(size);
    }

    /**
     * Return the appropriate <code>byte[]</code> buffer
     * specified by index.
     *
     * @param index  the index of the buffer required
     * @return the buffer
     */
    private byte[] getBuffer(int index) {
        return mBuffers.get(index);
    }

    /**
     * Makes a new buffer available either by allocating
     * a new one or re-cycling an existing one.
     *
     * @param newcount  the size of the buffer if one is created
     */
    private void needNewBuffer(int newcount) {
        if (mCurrentBufferIndex < mBuffers.size() - 1) {
            //Recycling old buffer
            mFilledBufferSum += mCurrentBuffer.length;

            mCurrentBufferIndex++;
            mCurrentBuffer = getBuffer(mCurrentBufferIndex);
        } else {
            //Creating new buffer
            int newBufferSize;
            if (mCurrentBuffer == null) {
                newBufferSize = newcount;
                mFilledBufferSum = 0;
            } else {
                newBufferSize = Math.max(
                    mCurrentBuffer.length << 1,
                    newcount - mFilledBufferSum);
                mFilledBufferSum += mCurrentBuffer.length;
            }

            mCurrentBufferIndex++;
            mCurrentBuffer = new byte[newBufferSize];
            mBuffers.add(mCurrentBuffer);
        }
    }

    /**
     * Write the bytes to byte array.
     * @param b the bytes to write
     * @param off The start offset
     * @param len The number of bytes to write
     */
    @Override
    public void write(byte[] b, int off, int len) {
        if ((off < 0)
                || (off > b.length)
                || (len < 0)
                || ((off + len) > b.length)
                || ((off + len) < 0)) {
            throw new IndexOutOfBoundsException();
        } else if (len == 0) {
            return;
        }
        synchronized (this) {
            int newcount = mCount + len;
            int remaining = len;
            int inBufferPos = mCount - mFilledBufferSum;
            while (remaining > 0) {
                int part = Math.min(remaining, mCurrentBuffer.length - inBufferPos);
                System.arraycopy(b, off + len - remaining, mCurrentBuffer, inBufferPos, part);
                remaining -= part;
                if (remaining > 0) {
                    needNewBuffer(newcount);
                    inBufferPos = 0;
                }
            }
            mCount = newcount;
        }
    }

    /**
     * Write a byte to byte array.
     * @param b the byte to write
     */
    @Override
    public synchronized void write(int b) {
        int inBufferPos = mCount - mFilledBufferSum;
        if (inBufferPos == mCurrentBuffer.length) {
            needNewBuffer(mCount + 1);
            inBufferPos = 0;
        }
        mCurrentBuffer[inBufferPos] = (byte) b;
        mCount++;
    }

    /**
     * Writes the entire contents of the specified input stream to this
     * byte stream. Bytes from the input stream are read directly into the
     * internal mBuffers of this streams.
     *
     * @param in the input stream to read from
     * @return total number of bytes read from the input stream
     *         (and written to this stream)
     * @throws IOException if an I/O error occurs while reading the input stream
     * @since Commons IO 1.4
     */
    public synchronized int write(InputStream in) throws IOException {
        int readCount = 0;
        int inBufferPos = mCount - mFilledBufferSum;
        int n = in.read(mCurrentBuffer, inBufferPos, mCurrentBuffer.length - inBufferPos);
        while (n != -1) {
            readCount += n;
            inBufferPos += n;
            mCount += n;
            if (inBufferPos == mCurrentBuffer.length) {
                needNewBuffer(mCurrentBuffer.length);
                inBufferPos = 0;
            }
            n = in.read(mCurrentBuffer, inBufferPos, mCurrentBuffer.length - inBufferPos);
        }
        return readCount;
    }

    /**
     * Return the current size of the byte array.
     * @return the current size of the byte array
     */
    public synchronized int size() {
        return mCount;
    }

    /**
     * Closing a <tt>ByteArrayOutputStream</tt> has no effect. The methods in
     * this class can be called after the stream has been closed without
     * generating an <tt>IOException</tt>.
     *
     * @throws IOException never (this method should not declare this exception
     * but it has to now due to backwards compatability)
     */
    @Override
    public void close() throws IOException {
        //nop
    }

    /**
     * @see java.io.ByteArrayOutputStream#reset()
     */
    public synchronized void reset() {
        mCount = 0;
        mFilledBufferSum = 0;
        mCurrentBufferIndex = 0;
        mCurrentBuffer = getBuffer(mCurrentBufferIndex);
    }

    /**
     * Writes the entire contents of this byte stream to the
     * specified output stream.
     *
     * @param out  the output stream to write to
     * @throws IOException if an I/O error occurs, such as if the stream is closed
     * @see java.io.ByteArrayOutputStream#writeTo(OutputStream)
     */
    public synchronized void writeTo(OutputStream out) throws IOException {
        int remaining = mCount;
        for (int i = 0; i < mBuffers.size(); i++) {
            byte[] buf = getBuffer(i);
            int c = Math.min(buf.length, remaining);
            out.write(buf, 0, c);
            remaining -= c;
            if (remaining == 0) {
                break;
            }
        }
    }

    /**
     * Gets the curent contents of this byte stream as a byte array.
     * The result is independent of this stream.
     *
     * @return the current contents of this output stream, as a byte array
     * @see java.io.ByteArrayOutputStream#toByteArray()
     */
    public synchronized byte[] toByteArray() {
        int remaining = mCount;
        if (remaining == 0) {
            return EMPTY_BYTE_ARRAY;
        }
        byte newbuf[] = new byte[remaining];
        int pos = 0;
        for (int i = 0; i < mBuffers.size(); i++) {
            byte[] buf = getBuffer(i);
            int c = Math.min(buf.length, remaining);
            System.arraycopy(buf, 0, newbuf, pos, c);
            pos += c;
            remaining -= c;
            if (remaining == 0) {
                break;
            }
        }
        return newbuf;
    }

    /**
     * Gets the curent contents of this byte stream as a string.
     * @return the contents of the byte array as a String
     * @see java.io.ByteArrayOutputStream#toString()
     */
    @Override
    public String toString() {
        return new String(toByteArray());
    }

    /**
     * Gets the curent contents of this byte stream as a string
     * using the specified encoding.
     *
     * @param enc  the name of the character encoding
     * @return the string converted from the byte array
     * @throws UnsupportedEncodingException if the encoding is not supported
     * @see java.io.ByteArrayOutputStream#toString(String)
     */
    public String toString(String enc) throws UnsupportedEncodingException {
        return new String(toByteArray(), enc);
    }

}
