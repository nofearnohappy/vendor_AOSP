package com.mediatek.rcs.message.proxy;

import java.util.HashSet;
import java.util.List;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;

import com.mediatek.rcs.common.IpMessage;
import com.mediatek.rcs.message.aidl.IMessageSender;


public class RcsProxyManager {
    private static final String TAG = "RcsProxyManager";
    private static RcsProxyManager sInstance;
    private Context mPluginContext;
    protected IMessageSender mSenderService;
    private static HashSet<MessageSenderListener> mMessageSenderListeners =
                                new HashSet<RcsProxyManager.MessageSenderListener>();
    private static final String SERVICE_ACTION = "com.mediatek.rcs.EmptyService";
    private static final String SERVICE_PACKAGE_NAME = "com.android.mms";
    private static final String TAG_SERVICE = "service";
    private static final String SERVICE_MESSAGE_SENDER = "servcie_IMessageSender";

    /**
     * Get Proxy Service Intent.
     * @return Intent
     */
    public static Intent getRcsProxyServiceIntent() {
        Intent intent = new Intent(SERVICE_ACTION);
        intent.setPackage(SERVICE_PACKAGE_NAME);
        return intent;
    }

    /**
     * Get Message sender Intent.
     * @param context Context
     * @return Intent
     */
    public static Intent createMessageSenderIntent(Context context) {
        Intent intent = new Intent(SERVICE_ACTION);
        intent.putExtra(TAG_SERVICE, SERVICE_MESSAGE_SENDER);
        intent.setPackage(SERVICE_PACKAGE_NAME);
        return intent;
    }

    public static void init(Context pluginContext) {
        if (sInstance == null) {
            synchronized (RcsProxyManager.class) {
                if (sInstance == null) {
                    sInstance = new RcsProxyManager(pluginContext);
                }
            }
        }
    }

    public static RcsProxyManager getInstance() {
        return sInstance;
    }

    public static void deInit(Context context) {
        if (sInstance != null) {
            sInstance.destory(context);
        }
    }

    private void destory(Context context) {
        if (mSenderService != null) {
            context.unbindService(mSenderServiceConnection);
        }
    }

    private RcsProxyManager(Context pluginContext) {
        mPluginContext = pluginContext;


        Intent intent = createMessageSenderIntent(pluginContext);
        boolean ret = mPluginContext.bindService(intent, mSenderServiceConnection, Context.BIND_AUTO_CREATE);
        Log.d(TAG, "[bindService] result = " + ret);
    }

    private ServiceConnection mSenderServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceDisconnected(ComponentName name) {
            // TODO Auto-generated method stub
            Log.d(TAG, "[onServiceDisconnected] name = " + name);
            for (MessageSenderListener l : mMessageSenderListeners) {
                l.onMessageSenderDestoryed(mSenderService);
            }
            mSenderService = null;
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            // TODO Auto-generated method stub
            Log.d(TAG, "[onServiceConnected] name = " + name);
            mSenderService = IMessageSender.Stub.asInterface(service);
            for (MessageSenderListener l : mMessageSenderListeners) {
                l.onMessageSenderCreated(mSenderService);
            }
        }
    };

    public static IMessageSender getMessageSender() {
        if (sInstance == null) {
            return null;
        }
        return sInstance.mSenderService;
    }

    public static boolean addMessageSenderListener(MessageSenderListener listener) {
        return mMessageSenderListeners.add(listener);
    }

    public static boolean removeMessageSenderListener(MessageSenderListener listener) {
        return mMessageSenderListeners.remove(listener);
    }

    public interface MessageSenderListener {
        public void onMessageSenderCreated(IMessageSender messageSender);
        public void onMessageSenderDestoryed(IMessageSender messageSender);
    }

    public void sendIpMessage(List<String> contacts, IpMessage message) {

    }


}
