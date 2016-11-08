package com.mediatek.ppl.ui;

import java.util.List;

import com.mediatek.ppl.PplService;
import com.mediatek.ppl.PplService.InternalControllerBinder;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.MenuItem;

abstract public class PplBasicActivity extends Activity {

    private final static String TAG = "PPL/PplBasicActivity";

    protected final static int STATE_INIT = 0;
    protected final static int STATE_CONNECTED = 1;
    protected final static int STATE_DISCONN = 2;

    protected final static int PROPERTY_CLEAR = 0x0000;
    protected final static int PROPERTY_NEED_SERVICE  = 0x0001;
    protected final static int PROPERTY_HAS_ACTIONBAR = 0x0002;
    protected final static int PROPERTY_QUIT_BACKGROUND = 0x004;


    protected InternalControllerBinder mBinder = null;
    protected ServiceConnection mServiceConnection = null;
    protected EventReceiver mEventReceiver = null;

    private int mProperty = PROPERTY_CLEAR;

    protected class EventReceiver extends BroadcastReceiver {

        IntentFilter mIntentFilter;
        public EventReceiver() {
            mIntentFilter = new IntentFilter();
        }

        public void destroy() {
            if (mIntentFilter.countActions() > 0) {
                unregisterReceiver(this);
            }

        }

        @Override
        public void onReceive(Context context, Intent intent) {
            for (int i = 0; i < mIntentFilter.countActions(); i++) {
                String in = mIntentFilter.getAction(i);
                if (null == intent.getAction()) {
                    return;
                }                
                if (intent.getAction().toString().equals(in)) {
                    finish();
                }
            }
        }

        public void addAction(String action) {
            mIntentFilter.addAction(action);
        }

        public void registerIntents() {
            if (mIntentFilter.countActions() > 0) {
                registerReceiver(this, mIntentFilter);
            }
        }
    }


    @Override
    protected void onCreate(Bundle saveInstanceState) {
        super.onCreate(saveInstanceState);

        final Bundle instanceState = saveInstanceState;

        mEventReceiver = new EventReceiver();

        onPropertyConfig();

        if (hasProperty(PROPERTY_HAS_ACTIONBAR)) {
            getActionBar().setDisplayHomeAsUpEnabled(true);
        }

        onRegisterEvent();
        onPrepareLayout();
        onInitLayout();

        if (hasProperty(PROPERTY_NEED_SERVICE)) {
            Intent intent = new Intent(PplService.Intents.PPL_MANAGER_SERVICE);
            intent.setClass(this, PplService.class);
            intent.setPackage("com.mediatek.ppl");
            mServiceConnection = new ServiceConnection() {
                @Override
                public void onServiceConnected(ComponentName name, IBinder binder) {
                    Log.i(TAG, "onServiceConnected");
                    mBinder = (InternalControllerBinder) binder;
                    onPplServiceConnected(instanceState);
                }

                public void onServiceDisconnected(ComponentName name) {
                    Log.i(TAG, "onServiceDisconnected");
                    onPplServiceDisconnected();
                    finish();
                }
            };

            bindService(intent,  mServiceConnection, BIND_AUTO_CREATE);
        }

        mEventReceiver.registerIntents();
    }

    @Override
    public void onStop() {
        super.onStop();
        if (hasProperty(PROPERTY_QUIT_BACKGROUND)) {
            ActivityManager am = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
            List<ActivityManager.RunningTaskInfo> taskInfo = am.getRunningTasks(5);
            int taskId = getTaskId();
            Log.i(TAG, "OurTask id: " + taskId);
            if (null == taskInfo) {
                return;
            }            
            int currentTaskId = -1;
            if (taskInfo.size() > 0) {
                currentTaskId = taskInfo.get(0).id;
            }
            Log.i(TAG, "Current Task Id: " + taskId);
            if (currentTaskId != taskId) {
                finish();
            }
        }
    }

    @Override
    public void onDestroy() {

        mEventReceiver.destroy();

        if (null != mBinder) {
            mBinder = null;
        }

        if (null != mServiceConnection) {
            unbindService(mServiceConnection);
            mServiceConnection = null;
        }

        super.onDestroy();
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (android.R.id.home == item.getItemId()) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    protected void gotoActivity(Context context, Class<?> cls) {
        Intent intent = new Intent();
        intent.setClass(context, cls);
        startActivity(intent);
    }

    protected void gotoActivity(Context context, Class<?> cls, String name, boolean value) {
        Intent intent = new Intent();
        intent.setClass(this, cls);
        intent.putExtra(name, value);
        startActivity(intent);
    }

    protected void setProperty(int property) {
        mProperty = property;
    }

    private boolean hasProperty(int property) {
        return (mProperty & property) > 0;
    }

    protected void onPropertyConfig() {
        setProperty(PROPERTY_NEED_SERVICE | PROPERTY_HAS_ACTIONBAR);
    }

    abstract protected void onRegisterEvent();

    abstract protected void onPrepareLayout();

    abstract protected void onInitLayout();

    protected void onPplServiceConnected(Bundle saveInstanceState){};

    protected void onPplServiceDisconnected(){};

}
