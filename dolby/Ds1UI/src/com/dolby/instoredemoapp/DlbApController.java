/******************************************************************************
 *  This program is protected under international and U.S. copyright laws as
 *  an unpublished work. This program is confidential and proprietary to the
 *  copyright owners. Reproduction or disclosure, in whole or in part, or the
 *  production of derivative works therefrom without the express permission of
 *  the copyright owners is prohibited.
 *
 *                 Copyright (C) 2011-2014 by Dolby Laboratories,
 *                             All rights reserved.
 ******************************************************************************/

package com.dolby.instoredemoapp;

import java.io.InputStream;
import java.util.ArrayList;

import android.app.Activity;
import android.content.Context;
import android.dolby.DsClient;
import android.dolby.DsClientSettings;
import android.dolby.DsConstants;
import android.dolby.IDsClientEvents;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

//import android.widget.Toast;

public class DlbApController implements IDsClientEvents {
    private static final String TAG = "DlbApController";

    private class APMessage {

        public long delayTime;
        public Message message;

        public APMessage(long time, Message msg) {
            delayTime = time;
            message = msg;
        }
    }

    private DsClient mDsClient;
    private MediaPlayer mMediaPlayer;
    private Handler mHandler;
    private InputStream mApInfoStream = null;
    // This context is used to construct Dsclient
    private Context mContext;
    private DlbApInfoExtractor mApInfoExtractor;
    private ArrayList<APMessage> mMsgList;

    private boolean mDsConnected = false;
    private boolean mPrevDsOnStat = false;
    private int mPrevProfile;
    private int mPrevIeqPreset;
    private ArrayList<DsClientSettingsData> mDsClientSettingsDataList;
    private ArrayList<Integer> mProfilesArray;

    public DlbApController(Context ctx) {
        super();
        mContext = ctx;
        mApInfoExtractor = new DlbApInfoExtractor();
        mDsClient = new DsClient();

        try {
            Log.d(TAG, "going to bind the DS service...");
            mDsClient.bindDsService((Activity) mContext);
        } catch (Exception e) {
            Log.e(TAG, "Consturction of DlbApController, bindDsService failed");
            e.printStackTrace();
            return;
        }
        mDsClient.setEventListener(this);
    }

    public void setMediaPlayer(MediaPlayer mp) {
        if (mp != null) {
            mMediaPlayer = mp;
        }
    }

    public void setHandler(Handler handler) {
        if (handler != null) {
            mHandler = handler;
        }
    }

    public void onExit() {
        mDsClient.setEventListener(null);
        try {
            Log.d(TAG, "about to unbind DS service...");
            mDsClient.unBindDsService((Activity) mContext);
        } catch (Exception e) {
            Log.e(TAG, "DlbApController.onExit(), unBindDsService failed");
        }
        mDsConnected = false;
    }

    public void sendApMessages() {
        if (mHandler != null) {
            Log.d(TAG, "the un-handled messages will be removed!");
            mHandler.removeCallbacksAndMessages(null);
        }

        Log.d(TAG, "duaration of the media is " + mMediaPlayer.getDuration());
        for (int i = 0; i < mMsgList.size(); ++i) {
            APMessage apmsg = mMsgList.get(i);
            Log.d(TAG, "will send ap msg after " + apmsg.delayTime + " millisecond");
            mHandler.sendMessageDelayed(apmsg.message, apmsg.delayTime);
        }
    }

    public void setApInfoFile(InputStream apstream) {
        mApInfoStream = apstream;
        mApInfoExtractor.setApInfoFile(mApInfoStream);
        initMsgList();
    }

    private class DsClientSettingsData {
        public int mProfile;
        public int mIeqPreset;
        public DsClientSettings mDsClientSettings;

        public DsClientSettingsData(int profile, int ieq, DsClientSettings dscs) {
            mProfile = profile;
            mIeqPreset = ieq;
            mDsClientSettings = dscs;
        }
    };

    public boolean saveCurrentDs1Data() {
        Log.d(TAG, "saveCurrentDs1Data");
        
        if (!mDsConnected) {
            Log.w(TAG, "DsClient hasn't been connected!");
            return false;
        }
        
        try {
            mPrevDsOnStat = mDsClient.getDsOn();
            mPrevProfile = mDsClient.getSelectedProfile();
            mPrevIeqPreset = mDsClient.getIeqPreset(mPrevProfile);
        } catch (Exception e) {
            Log.e(TAG, "DlbApController.saveCurrentDs1Data " + "fail to call getDsOn or getSelectedProfile or getIeqPreset");
            e.printStackTrace();
            return false;
        }
        Log.d(TAG, "mPrevDsOnStat = " + mPrevDsOnStat);
        Log.d(TAG, "mPrevProfile = " + mPrevProfile);
        Log.d(TAG, "mPrevIeqPreset = " + mPrevIeqPreset);

        mProfilesArray = new ArrayList<Integer>();
        ArrayList<AutoPilotItem> aplist = mApInfoExtractor.getAutoPilotMetadata();
        for (int i = 0; i < aplist.size(); ++i) {
            AutoPilotItem item = aplist.get(i);
            String proctl = item.getProfileControlValue();
            if (proctl.equalsIgnoreCase("unset")) {
                continue;
            } else {
                int profile = -1;
                if (proctl.equalsIgnoreCase("Movie")) {
                    profile = ConstValue.PROFILE_MOVIE;
                } else if (proctl.equalsIgnoreCase("Music")) {
                    profile = ConstValue.PROFILE_MUSIC;
                } else if (proctl.equalsIgnoreCase("Game")) {
                    profile = ConstValue.PROFILE_GAME;
                } else if (proctl.equalsIgnoreCase("Voice")) {
                    profile = ConstValue.PROFILE_VOICE;
                } else {
                    Log.e(TAG, "DlbApController.saveCurrentDs1Data, invalide profile name = " + proctl);
                }
                Integer profileInt = profile;
                if ((profile != -1) && (!mProfilesArray.contains(profileInt))) {
                    mProfilesArray.add(profileInt);
                } else {
                    continue;
                }
            }
        }
        Log.d(TAG, "mProfilesArray.size = " + mProfilesArray.size());
        Log.d(TAG, "mProfilesArray = " + mProfilesArray.toString());

        mDsClientSettingsDataList = new ArrayList<DsClientSettingsData>();
        int profileCnt = mProfilesArray.size();
        Log.d(TAG, "profileCnt = " + profileCnt);
        try {
            for (int i = 0; i < profileCnt; ++i) {
                int profile = mProfilesArray.get(i).intValue();
                int ieqPreset = mDsClient.getIeqPreset(profile);
                DsClientSettings dscs = mDsClient.getProfileSettings(profile);
                DsClientSettingsData dscsdata = new DsClientSettingsData(profile, ieqPreset, dscs);
                mDsClientSettingsDataList.add(dscsdata);
            }
        } catch (Exception e) {
            Log.e(TAG, "DlbApController.saveCurrentDs1Data, fail to call setIeqPreset");
            e.printStackTrace();
            return false;
        }
        Log.d(TAG, "the size of mDsClientSettingsDataList = " + mDsClientSettingsDataList.size());

        // reset profile settings
        for (int i = 0; i < profileCnt; ++i) {
            try {
                // Because the user-defined GEQ is not good enough, reset the
                // profile
                mDsClient.resetProfile(mProfilesArray.get(i).intValue());
            } catch (Exception e) {
                Log.e(TAG, "DlbApController.saveCurrentDs1Data, fail to call resetProfile");
                e.printStackTrace();
                return false;
            }
        }

        return true;
    }

    public void restoreAllDs1Data() {
        Log.d(TAG, "restoreAllDs1Data");

        if (mDsClientSettingsDataList == null) {
            Log.w(TAG, "mDsClientSettingsDataList is null, we haven't save the Ds1 Data before!");
            return;
        }
        
        for (int i = 0; i < mDsClientSettingsDataList.size(); ++i) {
            DsClientSettingsData dscd = mDsClientSettingsDataList.get(i);
            try {
            	mDsClient.setSelectedProfile(dscd.mProfile);
                mDsClient.setIeqPreset(dscd.mProfile, dscd.mIeqPreset);
                mDsClient.setProfileSettings(dscd.mProfile, dscd.mDsClientSettings);
            } catch (Exception e) {
                Log.e(TAG, "DlbApController.restoreAllDs1Data, fail to call setIeqPreset or setProfileSettings");
                e.printStackTrace();
                mHandler.sendEmptyMessage(ConstValue.DS1_INSTOREDEMO_QUIT);
                break;
            }
        }

        try {
            int result = mDsClient.setDsOnChecked(mPrevDsOnStat);
            // Check for failure to enable the effect
            if (result != DsConstants.DS_NO_ERROR)
            {
            	Log.e(TAG, "DlbApController.restoreAllDs1Data, setDsOnChecked failed due to return code: " + result);
            	return;
            }
            mDsClient.setSelectedProfile(mPrevProfile);
        } catch (Exception e) {
            Log.e(TAG, "DlbApController.restoreAllDs1Data," + "fail to call setDsOnChecked or setSelectedProfile or setIeqPreset");
            e.printStackTrace();
            mHandler.sendEmptyMessage(ConstValue.DS1_INSTOREDEMO_QUIT);
        }
    }

    public boolean isDsConnected() {
        return mDsConnected;
    }

    public boolean processApMessage(Message msg) {
        Log.d(TAG, "processApMessage " + msg.what);
        // Log.d(TAG, "obj = " + msg.obj);
        if (msg.obj == null) {
            Log.e(TAG, "the msg.obj is null");
            return true;
        }
        AutoPilotItem apitem = (AutoPilotItem) msg.obj;
        boolean ret = handleMasterControl(apitem.getMasterControlValue());
        Log.d(TAG, "handleMasterControl, returns " + ret);
        
        // check for master control failure and signal autopilot failure.
        if (!ret)
        	return false;        
        
        ret = handleProfileControl(apitem.getProfileControlValue());
        Log.d(TAG, "handleProfileControl, returns " + ret);
        ret = handleSurroundVirtualizer(apitem.getSurroundVirtualizerValue());
        Log.d(TAG, "handleSurroundVirtualizer, returns " + ret);
        ret = handleDialogEnhancer(apitem.getDialogEnahancerValue());
        Log.d(TAG, "handleDialogEnhancer, returns " + ret);
        ret = handleVolumeLeveler(apitem.getVolumeLevelerValue());
        Log.d(TAG, "handleVolumeLeveler, returns " + ret);
        ret = handleIntelligentEq(apitem.getIntelligenEqValue());
        Log.d(TAG, "handleIntelligentEq, returns " + ret);
        handleTextInfo(apitem.getDisplayText());
        
        return true;
    }

    private void handleTextInfo(TextInfo ti) {
        if (ConstValue.UPDATE_TEXT) {
            Log.d(TAG, "handleTextInfo, ti = " + ti);
            Message msg = mHandler.obtainMessage(ConstValue.UPDATE_TXT_MSG_ID, ti);
            mHandler.sendMessage(msg);
        }
    }

    private boolean handleIntelligentEq(String sieq) {
        /*
         * Note:At present, the lib of ds1 implemented Off, Open, Rich and
         * Focused
         */
        Log.d(TAG, "handleIntelligentEq, ieq = " + sieq);
        int ieq = -1;
        if (sieq.equalsIgnoreCase("off")) {
            ieq = ConstValue.IEQ_OFF;
        } else if (sieq.equalsIgnoreCase("Open")) {
            ieq = ConstValue.IEQ_OPEN;
        } else if (sieq.equalsIgnoreCase("Rich")) {
            ieq = ConstValue.IEQ_RICH;
        } else if (sieq.equalsIgnoreCase("Focused")) {
            ieq = ConstValue.IEQ_FOCUSED;
        } else if (sieq.equalsIgnoreCase("Warm")) {
            ieq = ConstValue.IEQ_WARM;
            // Workaround
            Log.d(TAG, "Not supported yet");
            return true;
        } else if (sieq.equalsIgnoreCase("Bright")) {
            ieq = ConstValue.IEQ_BRIGHT;
            // Workaround
            Log.d(TAG, "Not supported yet");
            return true;
        } else if (sieq.equalsIgnoreCase("Balanced")) {
            ieq = ConstValue.IEQ_BALANCED;
            // Workaround
            Log.d(TAG, "Not supported yet");
            return true;
        } else if (sieq.equalsIgnoreCase("unset")) {
            Log.d(TAG, "value does not change");
            return true;
        } else {
            Log.e(TAG, "DlbApController.handleIntelligentEq, invalid value = " + ieq);
            return false;
        }

        try {
            int profile = mDsClient.getSelectedProfile();
            mDsClient.setIeqPreset(profile, ieq);
        } catch (Exception e) {
            Log.e(TAG, "DlbApController.handleIntelligentEq, fail to call setIeqPreset");
            e.printStackTrace();
            return false;
        }
        return true;
    }

    private boolean handleVolumeLeveler(String vl) {
        Log.d(TAG, "handleVolumeLeveler, vl = " + vl);
        boolean on = false;
        if (vl.equalsIgnoreCase("on")) {
            on = true;
        } else if (vl.equalsIgnoreCase("off")) {
            on = false;
        } else if (vl.equalsIgnoreCase("unset")) {
            Log.d(TAG, "value does not change");
            return true;
        } else {
            Log.e(TAG, "DlbApController.handleVolumeLeveler, invalid value = " + vl);
            return false;
        }
        try {
            int profile = mDsClient.getSelectedProfile();
            DsClientSettings dscs = mDsClient.getProfileSettings(profile);
            dscs.setVolumeLevellerOn(on);
            mDsClient.setProfileSettings(profile, dscs);
        } catch (Exception e) {
            Log.e(TAG, "DlbApController.handleVolumeLeveler,fail to call setProfileSettings");
            e.printStackTrace();
            return false;
        }
        return true;
    }

    private boolean handleDialogEnhancer(String deh) {
        Log.d(TAG, "handleDialogEnhancer, deh = " + deh);
        boolean on = false;
        if (deh.equalsIgnoreCase("on")) {
            on = true;
        } else if (deh.equalsIgnoreCase("off")) {
            on = false;
        } else if (deh.equalsIgnoreCase("unset")) {
            Log.d(TAG, "value does not change");
            return true;
        } else {
            Log.e(TAG, "DlbApController.handleDialogEnhancer, invalid value = " + deh);
            return false;
        }

        try {
            int profile = mDsClient.getSelectedProfile();
            DsClientSettings dscs = mDsClient.getProfileSettings(profile);
            dscs.setDialogEnhancerOn(on);
            mDsClient.setProfileSettings(profile, dscs);
        } catch (Exception e) {
            Log.e(TAG, "DlbApController.handleDialogEnhancer,fail to call setProfileSettings");
            e.printStackTrace();
            return false;
        }
        return true;
    }

    private boolean handleSurroundVirtualizer(String sv) {
        Log.d(TAG, "handleSurroundVirtualizer " + sv);
        boolean on = false;
        if (sv.equalsIgnoreCase("on")) {
            on = true;
        } else if (sv.equalsIgnoreCase("off")) {
            on = false;
        } else if (sv.equalsIgnoreCase("unset")) {
            Log.d(TAG, "value does not change");
            return true;
        } else {
            Log.e(TAG, "DlbApController.handleSurroundVirtualizer, invalid value = " + sv);
            return false;
        }
        try {
            DsClientSettings dscs = mDsClient.getProfileSettings(mDsClient.getSelectedProfile());
            // TODO: Set the right virtualizer state according to whether the
            // headphone is attached or not.
            dscs.setHeadphoneVirtualizerOn(on);
            mDsClient.setProfileSettings(mDsClient.getSelectedProfile(), dscs);
        } catch (Exception e) {
            Log.e(TAG, "DlbApController.handleSurroundVirtualizer,fail to call setProfileSettings");
            e.printStackTrace();
            return false;
        }
        return true;
    }

    private boolean handleProfileControl(String proctl) {
        Log.d(TAG, "handleProfileControl, profilecontrol = " + proctl);
        int profile = -1;
        if (proctl.equalsIgnoreCase("Movie")) {
            profile = ConstValue.PROFILE_MOVIE;
        } else if (proctl.equalsIgnoreCase("Music")) {
            profile = ConstValue.PROFILE_MUSIC;
        } else if (proctl.equalsIgnoreCase("Game")) {
            profile = ConstValue.PROFILE_GAME;
        } else if (proctl.equalsIgnoreCase("Voice")) {
            profile = ConstValue.PROFILE_VOICE;
        } else if (proctl.equalsIgnoreCase("unset")) {
            Log.d(TAG, "value not change!");
            return true;
        } else {
            Log.e(TAG, "DlbApController.handleProfileControl,invalid value = " + proctl);
            return false;
        }

        try {
            mDsClient.setSelectedProfile(profile);
        } catch (Exception e) {
            Log.e(TAG, "DlbApController.handleProfileControl,fail to call setProfileSettings");
            e.printStackTrace();
            return false;
        }
        return true;
    }

    private boolean handleMasterControl(String mastercontrol) {
        Log.d(TAG, "handleMasterControl, mastercontrol = " + mastercontrol);
        boolean on = false;

        if (mastercontrol.equalsIgnoreCase("on")) {
            on = true;
        } else if (mastercontrol.equalsIgnoreCase("off")) {
            on = false;
        } else if (mastercontrol.equalsIgnoreCase("unset")) {
            Log.d(TAG, "no need to handle this");
            return true;
        } else {
            Log.e(TAG, "DlbApController.handleMasterControl, invalid value = " + mastercontrol);
            return false;
        }

        try {
            int result = mDsClient.setDsOnChecked(on);
            
            // Check for failure to enable the effect
            if (result != DsConstants.DS_NO_ERROR)
            {
            	Log.e(TAG, "DlbApController.handleMasterControl, setDsOnChecked failed due to return code: " + result);
            	return false;
            }
            
        } catch (Exception e) {
            Log.e(TAG, "DlbApController.handleMasterControl, setDsOnChecked failed");
            e.printStackTrace();
            return false;
        }

        return true;
    }

    private void initMsgList() {
        if (mMsgList != null) {
            mMsgList.clear();
            mMsgList = null;
        }
        mMsgList = new ArrayList<APMessage>();
        ArrayList<AutoPilotItem> aplist = mApInfoExtractor.getAutoPilotMetadata();
        Log.d(TAG, "aplist.length = " + aplist.size());
        for (int i = 0; i < aplist.size(); ++i) {
            AutoPilotItem apitem = aplist.get(i);
            Log.d(TAG, "obj of msg: \n" + apitem);
            Message msg = mHandler.obtainMessage(ConstValue.AP_MSG_ID, apitem);
            long delaytime = calMsgDelaytime(apitem.getTimeStamp()).longValue();
            APMessage apmsg = new APMessage(delaytime, msg);
            mMsgList.add(apmsg);
        }
    }

    private Integer calMsgDelaytime(String timestamp) {
        // The format of timestamp is HH:MM:SS:MSS e.g.02:12:00:000
        Integer ret = 0;
        String tmp = timestamp;

        // get the hour value
        int colonIdx = tmp.indexOf(':');
        if (colonIdx == -1) {
            Log.e(TAG, "the format of the timestamp is not valid");
            return -1;
        }
        String sub = tmp.substring(0, colonIdx);
        Log.d(TAG, "hour = " + sub);
        Integer hour = Integer.valueOf(sub);
        tmp = tmp.substring(colonIdx + 1, tmp.length());

        // get the minute value
        colonIdx = tmp.indexOf(':');
        sub = tmp.substring(0, colonIdx);
        Log.d(TAG, "min = " + sub);
        Integer minute = Integer.valueOf(sub);
        tmp = tmp.substring(colonIdx + 1, tmp.length());

        // get the second value
        colonIdx = tmp.indexOf(':');
        sub = tmp.substring(0, colonIdx);
        Log.d(TAG, "sec = " + sub);
        Integer second = Integer.valueOf(sub);
        tmp = tmp.substring(colonIdx + 1, tmp.length());

        // get the millisecond value
        Integer millisecond = Integer.valueOf(tmp);

        ret = hour * 60 * 60 * 1000 + minute * 60 * 1000 + second * 1000 + millisecond;
        Log.d(TAG, "time = " + ret);
        return ret;
    }

    @Override
    public void onClientConnected() {
        Log.d(TAG, "onClientConnected");
        mDsConnected = true;
        mHandler.sendEmptyMessage(ConstValue.DS1_SERVICE_CONNECTED);
    }

    @Override
    public void onClientDisconnected() {
        Log.d(TAG, "onClientDisConnected");

    }

    @Override
    public void onDsOn(boolean arg0) {

    }

    @Override
    public void onProfileNameChanged(int arg0, String arg1) {

    }

    @Override
    public void onProfileSelected(int arg0) {

    }

    @Override
    public void onProfileSettingsChanged(int arg0) {

    }

    @Override
    public void onEqSettingsChanged(int arg0, int arg1) {

    }
}
