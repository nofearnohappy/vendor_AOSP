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

/*****************************************************************************
*  Copyright Statement:
*  --------------------
*  This software is protected by Copyright and the information contained
*  herein is confidential. The software may not be copied and the information
*  contained herein may not be used or disclosed except with the written
*  permission of MediaTek Inc. (C) 2005
*
*  BY OPENING THIS FILE, BUYER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
*  THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
*  RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO BUYER ON
*  AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL WARRANTIES,
*  EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF
*  MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR NONINFRINGEMENT.
*  NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH RESPECT TO THE
*  SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY, INCORPORATED IN, OR
*  SUPPLIED WITH THE MEDIATEK SOFTWARE, AND BUYER AGREES TO LOOK ONLY TO SUCH
*  THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO. MEDIATEK SHALL ALSO
*  NOT BE RESPONSIBLE FOR ANY MEDIATEK SOFTWARE RELEASES MADE TO BUYER'S
*  SPECIFICATION OR TO CONFORM TO A PARTICULAR STANDARD OR OPEN FORUM.
*
*  BUYER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S ENTIRE AND CUMULATIVE
*  LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE RELEASED HEREUNDER WILL BE,
*  AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE MEDIATEK SOFTWARE AT ISSUE,
*  OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE CHARGE PAID BY BUYER TO
*  MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE. 
*
*  THE TRANSACTION CONTEMPLATED HEREUNDER SHALL BE CONSTRUED IN ACCORDANCE
*  WITH THE LAWS OF THE STATE OF CALIFORNIA, USA, EXCLUDING ITS CONFLICT OF
*  LAWS PRINCIPLES.  ANY DISPUTES, CONTROVERSIES OR CLAIMS ARISING THEREOF AND
*  RELATED THERETO SHALL BE SETTLED BY ARBITRATION IN SAN FRANCISCO, CA, UNDER
*  THE RULES OF THE INTERNATIONAL CHAMBER OF COMMERCE (ICC).
*
*****************************************************************************/
/*******************************************************************************
 *
 * Filename:
 * ---------
 *   FmOverBtService.java
 *
 * Project:
 * --------
 *   BT Project
 *
 * Description:
 * ------------
 *   This file is used to provide service for communication between FMRadio and A2DP Service
 *
 * Author:
 * -------
 *   Yi Zeng
 *
 *==============================================================================
 *             HISTORY
 * Below this line, this part is controlled by PVCS VM. DO NOT MODIFY!!
 *------------------------------------------------------------------------------
 * $Revision: 
 * $Modtime:
 * $Log: 
 *------------------------------------------------------------------------------
 * Upper this line, this part is controlled by PVCS VM. DO NOT MODIFY!!
 *==============================================================================
 *******************************************************************************/
package com.mediatek.bluetooth.fmOverBt;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.bluetooth.BluetoothA2dp;

import android.util.Log;
import android.os.IBinder;
import android.os.Binder;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.SystemProperties;
import android.os.RemoteException;




public class FmOverBtService extends Service {
	
    private static final String TAG = "BluetoothFmOverBtService";
    private static final boolean DBG = true;
    private static IntentFilter mIntentFilter;
	
    // FM Radio Service // TODO: maybe remove
    public static final int FMSTART_SUCCESS = 0; 	// fm over bt start success
    public static final int FMSTART_FAILED  = 1; 	// fm over bt start failed
    public static final int FMSTART_ALREADY = 2; 	// fm already over bt
    public static final int FMSTOP_SUCCESS  = 3; 	// fm over bt start success
    public static final int FMSTOP_FAILED   = 4; 	// fm over bt start failed
    public static final int FMSTOP_ALREADY  = 5; 	// fm already over bt
    public static final int FMSTOPPED_IND   = 6; 	// a2dp is disconnected

    // TODO: plan to change the intent extra data to fm state, then close can send confirm message to fm radio
    public static final int FM_STATE_STARTED  = 10; 	// fm over bt is opened
    public static final int FM_STATE_STARTING = 11; 	// fm over bt is opening
    public static final int FM_STATE_STOPING  = 12;  	// fm over bt is closing
    public static final int FM_STATE_STOPPED  = 13; 	// fm over bt is closed

    public static final int A2DP_STATE_CONNECTED     = 14;
    public static final int A2DP_STATE_DISCONNECTED  = 15;
	
    public static final int FM_EVT_START    = 20;
    public static final int FM_EVT_STOP     = 21;
    public static final int FM_EVT_IND      = 22; /* report the state of a2dp */

	public static final int FM_RADIO_STATE_OFF = 30;
	public static final int FM_RADIO_STATE_ON = 31;
	
    private int     mA2dpState     = 0;
    private int     mFmOverBtState = 13;
	private int		mFmRadioState = 30;
    private boolean mIsInitiated   = false;
    private Context mContext;

    public static final String ACTION_FM_OVER_BT_CONTROLLER = "android.server.fmoverbt.action.FM_OVER_BT_CONTROLLER";
    public static final String MSG_FM_POWER_UP              = "com.mediatek.FMRadio.FMRadioService.ACTION_TOA2DP_FM_POWERUP";
    public static final String MSG_FM_POWER_DOWN            = "com.mediatek.FMRadio.FMRadioService.ACTION_TOA2DP_FM_POWERDOWN";
    public static final String EXTRA_RESULT_STATE           = "android.bluetooth.fmoverbt.extra.RESULT_STATE";

    // load the jni library
    static {
        System.loadLibrary("extfmoverbt_jni");
    }

	
    @Override
    public void onCreate() {
    
        if (DBG) log("Starting FM over BT Service");

        if (!mIsInitiated) { 
            if (DBG) log("Starting FM over BT Service --");
            if (!initNative())
            {
                if (DBG) log("Could not init native functions.");
                return;			
            }			
            mIsInitiated = true;
        }
        else {
            if (DBG) log("Already started, just return!");
            return;
        }
  
        mIntentFilter = new IntentFilter();
        mContext = getApplicationContext();
        mIntentFilter.addAction(MSG_FM_POWER_UP);
        mIntentFilter.addAction(MSG_FM_POWER_DOWN);
        mContext.registerReceiver(mReceiver, mIntentFilter);
		
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        if (DBG) log("FM over BT Service is started");
		
        return 0;
    }

    @Override
    public void onStart(Intent intent, int startId)
    {
        if (DBG) log("FM over BT Service onstart");

    }

    @Override
    public void onDestroy() {
	
        if (DBG) log("Destroyed FM over BT Service");
		
        cleanupNative();
        mIsInitiated = false;

        mContext.unregisterReceiver(mReceiver);
    }

    @Override
    public IBinder onBind(Intent intent) {

        if (DBG) log("onBind()");
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {

        if (DBG) log("onUnBind()");
        return super.onUnbind(intent);
    }

    @Override
    public void onRebind(Intent intent) {

        if (DBG) log("onUnBind()");
        super.onRebind(intent);
    }
    /********************************************
    *
    *function name:
    *
    *    onFmOverBtJniCallback
    *
    *description:
    *
    *    used to receive the callback result from JNI
    *
    *parameters:
    *
    *    event [int] : start or stop operation
    *
    *    result [int] : the result of fm operation
    *
    *********************************************/
    private synchronized void onFmOverBtJniCallback(int event, int result) {	
        if (DBG) log("Event:" + event + " ,result:" + result + " ,fm state: " + mFmOverBtState);

        switch (event) {
            case FM_EVT_START:
            {
            switch (mFmOverBtState) {
                case FM_STATE_STARTED:
                case FM_STATE_STARTING:
                    if (result == FMSTART_SUCCESS) 
                    {
                        mFmOverBtState = FM_STATE_STARTED;
                    }
                    else
                    {
                        mFmOverBtState = FM_STATE_STOPPED;
                    }
					fmSendIntent(result);
                    break;

                default: /* failed when result is unsuccess or state error */
                    fmSendIntent(FMSTART_FAILED);
                    mFmOverBtState = FM_STATE_STOPPED;
                    break;
            }
				
            break;
            }
			
            case FM_EVT_STOP:
            {
            switch (mFmOverBtState) {
                case FM_STATE_STOPPED:
                case FM_STATE_STOPING:
					fmSendIntent(result);
					mFmOverBtState = FM_STATE_STOPPED;
                    break;

                default:
                    fmSendIntent(FMSTOP_FAILED);
					mFmOverBtState = FM_STATE_STOPPED;
                    break;
            }
				
            break;
            }

            case FM_EVT_IND: 
            {
                if (result == A2DP_STATE_DISCONNECTED) /* a2dp is disconnected  */
                {
                    mA2dpState = A2DP_STATE_DISCONNECTED;
					mFmOverBtState = FM_STATE_STOPPED;
					
					fmSendIntent(FMSTOPPED_IND);
                }
				else if(result == A2DP_STATE_CONNECTED)          /* a2dp is connected  */
                {
                    mA2dpState = A2DP_STATE_CONNECTED;
					if(mFmRadioState == FM_RADIO_STATE_ON)//FM radio already powered on
						startFmOverBt();
                }
				else
				{
					if(DBG) log("Invalid result" + result);
				}
                break;
            }
				
            default:
                break;
        }
    }

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() 
    {
        @Override
        public void onReceive(Context context, Intent intent) 
        {
            String action = intent.getAction();
			if(action == null)
			{
				if(DBG) log("get action failed.");
				return;
			}
            if (action.equals(MSG_FM_POWER_UP)) 
            { 
                if (DBG) log("received fm power on intent.");
				mFmRadioState = FM_RADIO_STATE_ON;
                if (0 == SystemProperties.getInt("bt.fmoverbt", 0)) 
                {
                    if (DBG) log("BT chip does not support Fm Over Bt, return.");

                    fmSendIntent(FMSTART_FAILED);
                    return;
                }

                if (DBG) log("FMoverBT state: " + mFmOverBtState + "A2DP state: " + mA2dpState);
                
                switch (mFmOverBtState) {
                    case FM_STATE_STARTED:
                        fmSendIntent(FMSTART_ALREADY);
                        break;
						
                    case FM_STATE_STARTING:
                        if (DBG) log("fm over bt is starting, ignore");
                        break;
						
                    case FM_STATE_STOPPED:
						if(mA2dpState == A2DP_STATE_CONNECTED)
						{
                        	startFmOverBt();
						}
						else
						{
							if (DBG) log("A2DP not connected");
						}
                        break;

                    default:
                        fmSendIntent(FMSTART_FAILED);
                        break;
                }
            } 
            else if (action.equals(MSG_FM_POWER_DOWN)) 
            {
                if (DBG) log("received fm power down intent.");
				if (DBG) log("FMoverBT state: " + mFmOverBtState + "A2DP state: " + mA2dpState);
                mFmRadioState = FM_RADIO_STATE_OFF;
                switch (mFmOverBtState) {
                    case FM_STATE_STARTED:
					case FM_STATE_STARTING:
                        stopFmOverBt();
                        break;
						
                    case FM_STATE_STOPPED:
                        fmSendIntent(FMSTOP_ALREADY);
                        break;

                    case FM_STATE_STOPING:
                        if (DBG) log("fm over bt is stoping, ignore");
                        break;

                    default:
                        fmSendIntent(FMSTOP_FAILED);
                        break;
                }
            }
        }
    };

    private class FMServiceDeathHandler implements IBinder.DeathRecipient {
        private IBinder mCb; // To be notified of client's death

        FMServiceDeathHandler(IBinder cb) {
            mCb = cb;
        }

        public void binderDied() {
            if (DBG) log("FMServiceDeathHandler::binderDied");

            if (FM_STATE_STARTED == mFmOverBtState) {
                stopFmOverBt();
            } else {
                if (DBG) log("FM was Power down,ignore.");
            }
        }

        public IBinder getBinder() {
            return mCb;
        }
    }

    private void fmSendIntent(int mFmResult) {
        if (mIsInitiated == false)
		{
		    log("fm over bt does not init.");
			return;
		}
        Intent intent = new Intent(FmOverBtService.ACTION_FM_OVER_BT_CONTROLLER);
        intent.putExtra(FmOverBtService.EXTRA_RESULT_STATE, mFmResult);
		if(mContext != null)//To resolve ALPS01401372 Null Pointer Java Exception
        	mContext.sendBroadcast(intent);
    }

    private static void log(String msg) {
        Log.d(TAG, msg);
    }

    private final IFmOverBtService.Stub mBinder = new IFmOverBtService.Stub() {
        public boolean setAudioPathToAudioTrack(IBinder cb) {
            FMServiceDeathHandler hdl = new FMServiceDeathHandler(cb);

            try {
                cb.linkToDeath(hdl, 0);
            } catch (RemoteException e) {
                // client has already died!
                if (DBG) log("[FMoverBT service]setAudioPathToAudioTrack could not link to " + cb + " binder death.");
                return false;
            }

            return true;
        }

        public int getState()
        {
            return mA2dpState;
        }
    };
	
    private int startFmOverBt()
    {
        int ret = 0;

        if (!mIsInitiated) return ret;
		
        ret = startNative();

        if (ret == 0)
        {
            if (DBG) log("start native fail.");
			 
            fmSendIntent(FMSTART_FAILED);
            mFmOverBtState = FM_STATE_STOPPED;
			 
            return ret;
        }

        mFmOverBtState = FM_STATE_STARTING;
        if (DBG) log("starting native.");
		
        return ret;
    }

    private int stopFmOverBt()
    {
        int ret = 0;

        if (!mIsInitiated) return ret;
		
        ret = stopNative();

        if (ret == 0)
        {
            if (DBG) log("stop native fail.");
			 
            fmSendIntent(FMSTOP_FAILED);
			 
            return ret;
        }

        mFmOverBtState = FM_STATE_STOPING;
        if (DBG) log("stoping native.");
		
        return ret;
    }

    private native boolean initNative();
    private native void cleanupNative();
    private native int startNative();
    private native int stopNative();
}
