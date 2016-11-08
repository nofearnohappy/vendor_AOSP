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

package com.mediatek.rcs.incallui;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import com.cmcc.sso.sdk.auth.AuthnConstants;
import com.cmcc.sso.sdk.auth.AuthnHelper;
import com.cmcc.sso.sdk.auth.TokenListener;
import com.cmcc.sso.sdk.util.SsoSdkConstants;

import com.cmdm.control.util.client.ResultEntity;
import com.cmdm.control.util.client.ResultUtil;
import com.cmdm.rcs.bean.RichScrnShowingObject;
import com.cmdm.rcs.biz.RichScrnPersonBiz;

import java.util.Locale;

import org.json.JSONObject;

//This class need to porting sdk api, import sdk class and using workthread
//to call the api. Because the sdk api maybe synchronous and may cause anr.
public class RichCallAdapter {
    private static final String TAG = "RichCallAdapter";
    public static final int RES_TYPE_PIC = 0;
    public static final int RES_TYPE_GIF = 1;
    public static final int RES_TYPE_VID = 2;
    public static final int RES_TYPE_NONE = 3;

    public static final int TYPE_SDK_INIT     = 1;
    public static final int TYPE_CALL_UPDATED = 2;
    public static final int TYPE_CALL_FETCHED = 3;
    public static final int TYPE_GEO_UPDATED  = 4;
    public static final int TYPE_SYS_LOGIN    = 5;
    public static final int TYPE_NET_CHANGED  = 6;
    public static final int TYPE_SDK_UNINIT   = 7;

    //For sdk using to get token
    public static final  String  RICH_APP_ID    = "00500130";
    public static final  String  RICH_APP_KEY   = "0D64C047E6A18731";
    private static final String  RICH_SOURCE_ID = "005001";

    private  WorkHandler    mWorkHandler;
    private  HandlerThread  mHandlerThread;

    public RichCallAdapter() {
        init();
    }

    private  Handler mResultHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            WorkingArgs arg = null;
            Log.d(TAG, "mResultHandler, handleMessage, type = " + msg.what);
            switch(msg.what) {
                case TYPE_CALL_UPDATED:
                    arg = (WorkingArgs) msg.obj;
                    if (arg != null) {
                        arg.mListener.onRichCallInfoUpdated(arg.mNumber, arg.mResult);
                    }
                    break;
                case TYPE_CALL_FETCHED:
                    arg = (WorkingArgs) msg.obj;
                    if (arg != null) {
                        arg.mListener.onRichCallInfoFetched(
                                arg.mNumber, arg.mRichCallInfo, arg.mResult);
                    }
                    break;
                case TYPE_GEO_UPDATED:
                    arg = (WorkingArgs) msg.obj;
                    if (arg != null) {
                        arg.mListener.onRichCallGeoUpdated(arg.mResult);
                    }
                    break;
                case TYPE_SYS_LOGIN:
                    arg = (WorkingArgs) msg.obj;
                    if (arg != null) {
                        arg.mListener.onRichCallSyncLogin(arg.mResult);
                    }
                    break;
                default:
                    break;
            }
        }
    };

    private void init() {
        mHandlerThread = new HandlerThread("RichCallAdapter");
        mHandlerThread.start();
        mWorkHandler = new WorkHandler(mHandlerThread.getLooper());
    }

    public interface Listener {
        public void onRichCallInfoFetched(String str, RichCallInfo info, boolean succeed);
        public void onRichCallInfoUpdated(String str, boolean succeed);
        public void onRichCallGeoUpdated(boolean succeed);
        public void onRichCallSyncLogin(boolean succeed);
    }

    private class WorkHandler extends Handler {
        //TO DO: May cause memory leak, how to fix?????
        private RichScrnPersonBiz mRichScrnPersonBiz;
        private Looper mLooper;
        WorkHandler(Looper looper) {
            super(looper);
            mLooper = looper;
        }

        @Override
        public void handleMessage(Message msg) {
            Log.d(TAG, "handleMessage, type = " + msg.what);
            switch(msg.what) {
                case TYPE_SDK_INIT:
                    this.initRichCallSdk((WorkingArgs) msg.obj);
                    break;
                case TYPE_CALL_UPDATED:
                    this.updateRichCallInfo((WorkingArgs) msg.obj);
                    break;
                case TYPE_CALL_FETCHED:
                    this.downloadRichCallInfo((WorkingArgs) msg.obj);
                    break;
                case TYPE_GEO_UPDATED:
                    this.downloadGeoInfo((WorkingArgs) msg.obj);
                    break;
                case TYPE_SYS_LOGIN:
                    this.loginRichCallSystem((WorkingArgs) msg.obj);
                    break;
                case TYPE_NET_CHANGED:
                    this.changeNetworkConnection();
                    break;
                case TYPE_SDK_UNINIT:
                    this.releaseResources();
                    break;
                default:
                    break;
            }
        }

        private void initRichCallSdk(WorkingArgs arg) {
            Log.d(TAG, "initRichCallSdk");
            Context cnx = arg.mContext;
            int cacheSize = arg.mCache;
            mRichScrnPersonBiz = new RichScrnPersonBiz(cnx);
            mRichScrnPersonBiz.init(cacheSize);
        }

        private void updateRichCallInfo(WorkingArgs arg) {
            Log.d(TAG, "updateRichCallInfo");
            String number = arg.mNumber;
            String event = arg.mEvent;
            if (mRichScrnPersonBiz == null) {
                Log.d(TAG, "mRichScrnPersonBiz is null");
                return;
            }

            ResultEntity result = mRichScrnPersonBiz.DownloadRichScrnObj(number, event);
            if (result != null && result.isSuccessed()) {
                arg.mResult = true;
            } else {
                arg.mResult = false;
            }

            Message message = mResultHandler.obtainMessage(TYPE_CALL_UPDATED);
            message.obj = arg;
            mResultHandler.sendMessage(message);
        }

        private void downloadRichCallInfo(WorkingArgs arg) {
            Log.d(TAG, "downloadRichCallInfo");
            String number = arg.mNumber;
            String event = arg.mEvent;
            if (mRichScrnPersonBiz == null) {
                Log.d(TAG, "mRichScrnPersonBiz is null");
                return;
            }

            ResultUtil<RichScrnShowingObject> result = mRichScrnPersonBiz
                .GetRichScrnObj(number, event);
            if (result != null && result.isSuccessed()) {
                RichScrnShowingObject mRichScrnShowingObject = result.getAttachObj();
                RichCallInfo info = new RichCallInfo();
                info.mGreet = mRichScrnShowingObject.getGreeting();
                info.mNumber = mRichScrnShowingObject.getMissdn();
                info.mUri = mRichScrnShowingObject.localSourceUrl;
                Log.d(TAG, "downloadRichCallInfo, mUri = " + info.mUri);

                String type = mRichScrnShowingObject.getSourceType();
                if ("0".equals(type)) {
                    info.mResourceType = RES_TYPE_PIC;
                } else if ("1".equals(type)) {
                    info.mResourceType = RES_TYPE_GIF;
                } else if ("2".equals(type)) {
                    info.mResourceType = RES_TYPE_VID;
                } else {
                    info.mResourceType = RES_TYPE_NONE;
                }

                info.mGeoInfo = mRichScrnShowingObject.missdnAddress;
                arg.mRichCallInfo = info;
                arg.mResult = true;
            } else {
                arg.mResult = false;
            }

            Message message = mResultHandler.obtainMessage(TYPE_CALL_FETCHED);
            message.obj = arg;
            mResultHandler.sendMessage(message);
        }

         private void loginRichCallSystem(final WorkingArgs arg) {
             String token = arg.mToken;
             arg.mResult = false;
             Log.d(TAG, "loginRichCallSystem, token = " + token);

             if (mRichScrnPersonBiz == null) {
                 Log.d(TAG, "mRichScrnPersonBiz is null");
                 return;
             }

             ResultEntity entity = mRichScrnPersonBiz.RichScrnCMCCSSOLogin(token, RICH_SOURCE_ID);
             if (entity != null && entity.isSuccessed()) {
                 arg.mResult = true;
             }
             Log.d(TAG, "loginRichCallSystem result = " + arg.mResult);
             Message message = mResultHandler.obtainMessage(TYPE_SYS_LOGIN);
             message.obj = arg;
             mResultHandler.sendMessage(message);
        }

        private void downloadGeoInfo(WorkingArgs arg) {
            Log.d(TAG, "downloadGeoInfo");
            String event = arg.mEvent;
            if (mRichScrnPersonBiz == null) {
                Log.d(TAG, "mRichScrnPersonBiz is null");
                return;
            }

            ResultEntity result = mRichScrnPersonBiz.DownloadHomeLocRules(event);
            if (result != null && result.isSuccessed()) {
                arg.mResult = true;
            } else {
                arg.mResult = false;
            }

            Message message = mResultHandler.obtainMessage(TYPE_GEO_UPDATED);
            message.obj = arg;
            mResultHandler.sendMessage(message);
        }

        private void changeNetworkConnection() {
            Log.d(TAG, "changeNetwork");
            if (mRichScrnPersonBiz == null) {
                Log.d(TAG, "mRichScrnPersonBiz is null");
                return;
            }

            ResultEntity result = mRichScrnPersonBiz.RichScrnChangeNetWork();
            if (result != null) {
                Log.d(TAG, "changeNetwork, result = " + result.isSuccessed());
            }

        }

        private void releaseResources() {
            Log.d(TAG, "releaseResources");
            if (mRichScrnPersonBiz != null) {
                //TO DO, fix memory leak problem
                mRichScrnPersonBiz = null;
            }

            if (mLooper != null) {
                mLooper.quitSafely();
            }
        }
    }

    public static final class RichCallInfo {
        //The greeting string, for future use
        public String mGreet;
        //File location in phone
        public String mUri;
        public int    mResourceType;
        public String mGeoInfo;
        public String mNumber;

        @Override
        public String toString() {
            return String.format(Locale.US, "RichCallInfo, " +
                    "[Greeting:%s, Path:%s, ResourceType:%s, GeoInfo:%s, Number:%s]",
                    mGreet, mUri, getResourceType(mResourceType), mGeoInfo, mNumber);
        }

        public String getResourceType(int type) {
            switch(type) {
                case 0:
                    return "PIC";
                case 1:
                    return "GIF";
                case 2:
                    return "VID";
                case 3:
                default:
                    return "UNKNOW";
            }
        }

    }

    private static final class WorkingArgs {
        Context      mContext;
        int          mCache;
        String       mNumber;
        String       mEvent;
        String       mToken;
        Listener     mListener;
        RichCallInfo mRichCallInfo;
        boolean      mResult;
    }

    public void initRichCallSystem(int cacheSize, Context cnx) {
        WorkingArgs arg = new WorkingArgs();
        arg.mContext = cnx;
        arg.mCache = cacheSize;

        Message message = mWorkHandler.obtainMessage(TYPE_SDK_INIT);
        message.obj = arg;
        mWorkHandler.sendMessage(message);
    }

    public void updatedRichCallInfo(String number, String event, Listener listener) {
        WorkingArgs arg = new WorkingArgs();
        arg.mNumber = number;
        arg.mEvent = event;
        arg.mListener = listener;

        Message message = mWorkHandler.obtainMessage(TYPE_CALL_UPDATED);
        message.obj = arg;
        mWorkHandler.sendMessage(message);
    }

    public void getRichCallInfo(String number, String event, Listener listener) {
        WorkingArgs arg = new WorkingArgs();
        arg.mNumber = number;
        arg.mEvent = event;
        arg.mListener = listener;

        Message message = mWorkHandler.obtainMessage(TYPE_CALL_FETCHED);
        message.obj = arg;
        mWorkHandler.sendMessage(message);
    }

    public void updateGeoInfo(String event, Listener listener) {
        WorkingArgs arg = new WorkingArgs();
        arg.mEvent = event;
        arg.mListener = listener;

        Message message = mWorkHandler.obtainMessage(TYPE_GEO_UPDATED);
        message.obj = arg;
        mWorkHandler.sendMessage(message);
    }

    public void loginRichCallSystem(Context cnx, Listener listener) {
        final WorkingArgs arg = new WorkingArgs();
        arg.mListener = listener;

        //This authnHelper will cause memory leak of InCallActivity, we should email
        // this issue to sdk vendor to fix this problem
        AuthnHelper authnHelper = new AuthnHelper(cnx);
        AdapterTokenListener tokenListener = new AdapterTokenListener(authnHelper, arg);
        authnHelper.setDefaultUI(false);
        authnHelper.getAccessToken(RICH_APP_ID, RICH_APP_KEY, "",
            SsoSdkConstants.LOGIN_TYPE_DEFAULT, tokenListener);
    }

    public void notifyDataConnectionChange() {
        Message message = mWorkHandler.obtainMessage(TYPE_NET_CHANGED);
        mWorkHandler.sendMessage(message);
    }

    public void releaseAdapterResource() {
        removeMessages();

        Message message = mWorkHandler.obtainMessage(TYPE_SDK_UNINIT);
        mWorkHandler.sendMessage(message);
    }

    private class AdapterTokenListener implements TokenListener {
        private WorkingArgs mWorkingArgs;
        private AuthnHelper mAuthnHelper;
        public AdapterTokenListener(AuthnHelper helper, WorkingArgs args) {
            mAuthnHelper = helper;
            mWorkingArgs = args;
        }

        @Override
        public void onGetTokenComplete(JSONObject jsonobj) {
            int result = jsonobj.optInt(SsoSdkConstants.VALUES_KEY_RESULT_CODE, -1);
            Log.d(TAG, "onGetTokenComplete, result = " + result);
            if (result == AuthnConstants.CLIENT_CODE_SUCCESS) {
                final String token = jsonobj.optString(SsoSdkConstants.VALUES_KEY_TOKEN, null);
                if (token == null || token.equals("")) {
                    return;
                }

                if (mWorkHandler == null) {
                    return;
                }
                //Send to work handler to start login process
                mWorkingArgs.mToken = token;
                Message message = mWorkHandler.obtainMessage(TYPE_SYS_LOGIN);
                message.obj = mWorkingArgs;
                mWorkHandler.sendMessage(message);
                //releaseResource();
                return;
            }
            Log.d(TAG, "onGetTokenComplete, result or token error");
            //Send to result handler to notify login failed
            mWorkingArgs.mResult = false;
            Message message = mResultHandler.obtainMessage(TYPE_SYS_LOGIN);
            message.obj = mWorkingArgs;
            mResultHandler.sendMessage(message);
            //releaseResource();
        }

        //Inorder to release resource, or else will cause memory leak.
        private void releaseResource() {
            if (mAuthnHelper != null) {
                //TO DO: to fix memory leak
                //mAuthnHelper.cleanSSO(this);
                //mAuthnHelper.cancelLogin();
                mAuthnHelper = null;
            }
        }

    }

    private void removeMessages() {
        Log.d(TAG, "removeMessages");
        mWorkHandler.removeMessages(TYPE_CALL_UPDATED);
        mWorkHandler.removeMessages(TYPE_CALL_FETCHED);
        mWorkHandler.removeMessages(TYPE_GEO_UPDATED);
        mWorkHandler.removeMessages(TYPE_SYS_LOGIN);
        mWorkHandler.removeMessages(TYPE_NET_CHANGED);

        mResultHandler.removeMessages(TYPE_CALL_UPDATED);
        mResultHandler.removeMessages(TYPE_CALL_FETCHED);
        mResultHandler.removeMessages(TYPE_GEO_UPDATED);
        mResultHandler.removeMessages(TYPE_SYS_LOGIN);
    }
}
