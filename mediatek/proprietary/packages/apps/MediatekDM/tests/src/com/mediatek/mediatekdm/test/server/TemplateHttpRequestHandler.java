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

package com.mediatek.mediatekdm.test.server;

import android.content.Context;
import android.util.Log;

import com.android.internal.telephony.PhoneConstants;
import com.mediatek.mediatekdm.test.server.MockServerService.TestResult;
import com.mediatek.telephony.TelephonyManagerEx;

import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.MethodNotSupportedException;
import org.apache.http.entity.FileEntity;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;

public class TemplateHttpRequestHandler implements HttpRequestHandler {
    private static final String TAG = "MDMTest/TemplateHttpRequestHandler";
    private static final String INPUT_FILENAME = "input.xml";
    private static final String OUTPUT_FILENAME = "output.xml";
    private final String mTemplateFolder;
    private final String mPathPrefix;
    private final Context mContext;
    private int mTotalRequest = 0;
    private IControlPanel mControlPanel;

    public static interface IControlPanel {
        void notify(int result);

        int getCount();

        void incCount();

        boolean testRetry();
    }

    public TemplateHttpRequestHandler(String templateFolder, String pathPrefix, Context context,
            IControlPanel notifier) {
        super();
        mTemplateFolder = templateFolder;
        mPathPrefix = pathPrefix;
        mContext = context;
        mControlPanel = notifier;
        try {
            mTotalRequest = mContext.getAssets().list(templateFolder).length;
            if (notifier.testRetry()) {
                mTotalRequest += 1;
            }
        } catch (IOException e) {
            throw new Error(e);
        }
    }

    public void handle(HttpRequest request, HttpResponse response, HttpContext context)
            throws HttpException, IOException {
        HttpEntity entity = null;
        SimpleSyncMLAccessor accessor = null;

        String method = request.getRequestLine().getMethod().toUpperCase(Locale.ENGLISH);
        Log.d(TAG, "Http method is : " + method);
        if (!method.equals("GET") && !method.equals("HEAD") && !method.equals("POST")) {
            throw new MethodNotSupportedException(method + " method not supported");
        }

        String target = request.getRequestLine().getUri();
        Log.d(TAG, "Http target is : " + target);

        if (request instanceof HttpEntityEnclosingRequest) {
            mControlPanel.incCount();
            Log.d(TAG, "Request #" + mControlPanel.getCount() + ", Total #" + mTotalRequest);
            entity = ((HttpEntityEnclosingRequest) request).getEntity();
            accessor = new SimpleSyncMLAccessor();
            accessor.parse(entity.getContent());
            // check IMEI
            String imei = accessor.getSourceLocURI();
            String systemImei = "IMEI:"
                    + TelephonyManagerEx.getDefault().getDeviceId(PhoneConstants.SIM_ID_1);
            if (!systemImei.equals(imei)) {
                throw new Error("IMEIs do not match in request #" + mControlPanel.getCount()
                        + " : (" + imei + ", " + systemImei + ")");
            }
            boolean verifyResult = verify(accessor, mControlPanel.getCount());
            if (!verifyResult) {
                // write to file for debug
                FileOutputStream fos = mContext.openFileOutput(buildFilename(INPUT_FILENAME), 0);
                accessor.write(fos);
                fos.close();
                // return error
                response.setStatusCode(HttpStatus.SC_INTERNAL_SERVER_ERROR);
                Log.d(TAG, "Input verification failed.");
                mControlPanel.notify(TestResult.Fail);
            } else {
                // prepare response
                String templateFilename = mTemplateFolder + File.separator
                        + Integer.toString(mControlPanel.getCount()) + ".xml";
                InputStream is = mContext.getAssets().open(templateFilename);
                accessor = new SimpleSyncMLAccessor();
                accessor.parse(is);
                accessor.setTargetLocURI(systemImei);
                FileOutputStream fos = mContext.openFileOutput(buildFilename(OUTPUT_FILENAME), 0);
                accessor.write(fos);
                fos.close();
                response.setStatusCode(HttpStatus.SC_OK);
                FileEntity body = new FileEntity(
                        mContext.getFileStreamPath(buildFilename(OUTPUT_FILENAME)), entity
                                .getContentType().getValue());
                response.setEntity(body);
                Log.d(TAG, "Response assembly done.");
                if (mControlPanel.getCount() == mTotalRequest) {
                    mControlPanel.notify(TestResult.Success);
                }
            }
        } else {
            throw new Error("Entity required.");
        }
    }

    private boolean verify(SimpleSyncMLAccessor input, final int index) {
        return true;
    }

    private String buildFilename(String filename) {
        return mPathPrefix + "_" + filename;
    }
}
