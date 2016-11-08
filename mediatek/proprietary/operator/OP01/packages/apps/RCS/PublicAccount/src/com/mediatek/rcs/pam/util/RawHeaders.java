package com.mediatek.rcs.pam.util;

/* Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *      http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License. */
import java.util.ArrayList;
import java.util.List;

/**
 * * The HTTP status and unparsed header fields of a single HTTP message. Values
 * * are represented as uninterpreted strings; use {@link RequestHeaders} and *
 * {@link ResponseHeaders} for interpreted headers. This class maintains the *
 * order of the header fields within the HTTP message. * *
 * <p>
 * This class tracks fields line-by-line. A field with multiple comma- *
 * separated values on the same line will be treated as a field with a single *
 * value by this class. It is the caller's responsibility to detect and split *
 * on commas if their field permits multiple values. This simplifies use of *
 * single-valued fields whose values routinely contain commas, such as cookies *
 * or dates. * *
 * <p>
 * This class trims whitespace from values. It never returns values with *
 * leading or trailing whitespace.
 */
public final class RawHeaders {
    private final List<String> mNamesAndValues = new ArrayList<String>(20);
    private String mStatusLine;
    private int mHttpMinorVersion = 1;
    private int mResponseCode = -1;
    private String mResponseMessage;

    /**
     * init function.
     */
    public RawHeaders() {
    }

    /**
     * Sets the response status line (like "HTTP/1.0 200 OK") or request line
     * (like "GET / HTTP/1.1").
     *
     * @param statusLine the first line in http response
     */
    public void setStatusLine(String statusLine) {
        statusLine = statusLine.trim();
        this.mStatusLine = statusLine;
        if (statusLine == null || !statusLine.startsWith("HTTP/")) {
            return;
        }
        statusLine = statusLine.trim();
        int mark = statusLine.indexOf(" ") + 1;
        if (mark == 0) {
            return;
        }
        if (statusLine.charAt(mark - 2) != '1') {
            this.mHttpMinorVersion = 0;
        }
        int last = mark + 3;
        if (last > statusLine.length()) {
            last = statusLine.length();
        }
        this.mResponseCode = Integer.parseInt(statusLine.substring(mark, last));
        if (last + 1 <= statusLine.length()) {
            this.mResponseMessage = statusLine.substring(last + 1);
        }
    }

    /**
     * get http result status.
     *
     * @return the status line
     */
    public String getStatusLine() {
        return mStatusLine;
    }

    /**
     * Returns the HTTP status code.
     *
     * @return HTTP status code or -1 if it is unknown.
     */
    public int getResponseCode() {
        return mResponseCode;
    }

    /**
     * Add an HTTP header line containing a field name, a literal colon, and a
     * value.
     *
     * @param line the HTTP header line
     */
    public void addLine(String line) {
        int index = line.indexOf(":");
        if (index == -1) {
            add("", line);
        } else {
            add(line.substring(0, index), line.substring(index + 1));
        }
    }

    /** * Add a field with the specified value. */
    private void add(String fieldName, String value) {
        if (fieldName == null) {
            throw new IllegalArgumentException("fieldName == null");
        }
        if (value == null) { /*
                             *  * Given null values, the RI sends a malformed
                             * field line like * "Accept\r\n". For platform
                             * compatibility and HTTP compliance, we * print a
                             * warning and ignore null values.
                             */
            return;
        }
        mNamesAndValues.add(fieldName);
        mNamesAndValues.add(value.trim());
    }

    /**
     * Returns the last value corresponding to the specified field, or null.
     *
     * @param fieldName the tag of the value
     *
     * @return the value of the tag
     */
    public String get(String fieldName) {
        for (int i = mNamesAndValues.size() - 2; i >= 0; i -= 2) {
            if (fieldName.equalsIgnoreCase(mNamesAndValues.get(i))) {
                return mNamesAndValues.get(i + 1);
            }
        }
        return null;
    }
}
