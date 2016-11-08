package com.mediatek.smartmotion.sensor;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.mediatek.sensorhub.SensorHubManager;
import com.mediatek.sensorhub.Action;
import com.mediatek.sensorhub.Condition;
import com.mediatek.sensorhub.ContextInfo;
import com.mediatek.smartmotion.MainActivity;

public class SensorHubClient {
    private Context mContext;
    private SensorHubManager mSensorHubManager;
    private static final String TAG = "SensorHubClient";

    public static final int TYPE_QUICKANSWER = 0;
    public static final int TYPE_EASYREJECT = 1;
    public static final int TYPE_SMARTSILENT = 2;
    public static final int TYPE_INPOCKET = 3;
    public static final String SENSOR_TYPE = "sensor_type";

    public SensorHubClient(Context context) {
        mContext = context;
        if (mContext != null) {
            mSensorHubManager = (SensorHubManager)mContext.getSystemService(
                    SensorHubManager.SENSORHUB_SERVICE);
        }
    }

    public int addRequest(int type) {
        Log.i(TAG, "addRequest:" + type);
        if (mSensorHubManager == null) {
            Log.i(TAG, "mSensorHubManager is null");
            return -1;
        }
        if (mSensorHubManager == null || !mSensorHubManager.getContextList().contains(type)) {
            Log.i(TAG, "mSensorHubManager does not contain this type");
            return -1;
        }

        Condition.Builder builder = new Condition.Builder();
        Condition condition = null;
        switch (type) {
        case ContextInfo.Type.PICK_UP:
            condition = builder.createCondition(ContextInfo.Pickup.VALUE, Condition.OP_EQUALS, 1);
            break;
        case ContextInfo.Type.SHAKE:
            condition = builder.createCondition(ContextInfo.Shake.VALUE, Condition.OP_EQUALS, 1);
            break;
        case ContextInfo.Type.FACING:
            if (MainActivity.sDemoMode) {
                condition = builder.createCondition(ContextInfo.Facing.FACE_DOWN,
                        Condition.OP_EQUALS, 1);
            } else {
                condition = builder.createCondition(ContextInfo.Facing.FACE_DOWN,
                        Condition.OP_GREATER_THAN_OR_EQUALS, 1);
            }
            break;
        case ContextInfo.Type.CARRY:
            condition = builder.createCondition(ContextInfo.Carry.IN_POCKET, Condition.OP_EQUALS, 1);
            break;
        case ContextInfo.Type.PEDOMETER:
            condition = builder.createCondition(ContextInfo.Pedometer.TOTAL_COUNT,
                    Condition.OP_GREATER_THAN_OR_EQUALS, 1000);
            break;
        case ContextInfo.Type.USER_ACTIVITY:
            Condition c1 = builder.createCondition(ContextInfo.UserActivity.CURRENT_STATE,
                Condition.OP_EQUALS, ContextInfo.UserActivity.State.ON_FOOT);
            Condition c2 = builder.createCondition(ContextInfo.UserActivity.CONFIDENCE,
                Condition.OP_GREATER_THAN_OR_EQUALS, 60);
            condition = builder.combineWithAnd(c1, c2);
            break;
        default:
            break;
        }
        Intent intent = new Intent(mContext, SensorIntentService.class);
        PendingIntent callbackIntent = PendingIntent.getService(
                mContext, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        Action action = null;
        if (MainActivity.sDemoMode) {
            action = new Action(callbackIntent, false, true);
        } else {
            action = new Action(callbackIntent, true, false);
        }
        int res = mSensorHubManager.requestAction(condition, action);
        Log.i(TAG, "addRequest: rid=" + res);
        return res;
    }

    public void cancelRequest(int rid) {
        if (mSensorHubManager == null || !mSensorHubManager.cancelAction(rid)) {
            Log.i(TAG, "cancelRequest failed: " + rid);
        }
    }
}
