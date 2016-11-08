package com.mediatek.rcs.pam;

import android.R.color;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import org.gsma.joyn.JoynService;

import java.util.concurrent.ConcurrentHashMap;


/**
 * This is the wrapper of public account service for client.
 */
public class PAService {
    public static final int INTERNAL_ERROR = JoynService.Error.INTERNAL_ERROR;
    private static final String TAG = Constants.TAG_PREFIX + "PAService";
    private Context mAppContext;
    private IPAService mServiceImpl = null;
    private ServiceConnection mServiceConnection;
    private static PAService mInstance = null;
    private ConcurrentHashMap<Long, IPAServiceCallback> mCallbacks =
                        new ConcurrentHashMap<Long, IPAServiceCallback>();
    private ConcurrentHashMap<Long, IPAServiceCallback> mWeekCallbacks =
            new ConcurrentHashMap<Long, IPAServiceCallback>();

    private PAService(final Context context, final ServiceConnectNotify callback) {

        mServiceConnection = new ServiceConnection() {

            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                Log.d(TAG, "onServiceConnected");
                mServiceImpl = IPAService.Stub.asInterface(service);
                callback.onServiceConnected();
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                Log.d(TAG, "onServiceDisconnected");
                mServiceImpl = null;
                mInstance = null;
                for (IPAServiceCallback callback : mCallbacks.values()) {
                    try {
                        callback.onServiceDisconnected(INTERNAL_ERROR);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }

                for (IPAServiceCallback callback : mWeekCallbacks.values()) {
                    try {
                        callback.onServiceDisconnected(INTERNAL_ERROR);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }
            }
        };

        mAppContext = context.getApplicationContext();
        Intent intent = new Intent();
        Log.d(TAG, "appContext info:" + mAppContext.getApplicationInfo());
        intent.setClass(mAppContext, PAServiceImpl.class);
        boolean ret = mAppContext.bindService(intent,
            mServiceConnection, Context.BIND_AUTO_CREATE);
        Log.d(TAG, "appContext(" + mAppContext + ") bindService() ret=" + ret);
    }

    public interface ServiceConnectNotify {
        void onServiceConnected();
    }

    public static synchronized void init(Context context, ServiceConnectNotify callback) {
        Log.d(TAG, "init()");
        if (mInstance == null) {
            Log.d(TAG, "new PAService()");
            mInstance = new PAService(context, callback);
        } else {
            Log.d(TAG, "init() callback directly");
            callback.onServiceConnected();
        }
    }

    public static synchronized PAService getInstance() {
        Log.d(TAG, "getInstance()=" + mInstance);
        return mInstance;
    }

    public long registerCallback(IPAServiceCallback callback, boolean isWeekLink) {

        try {
            long token = mServiceImpl.registerCallback(callback);
            if (isWeekLink) {
                mWeekCallbacks.put(token, callback);
            } else {
                mCallbacks.put(token, callback);
            }
            Log.d(TAG, "PAServiceImpl connected with token: " +
                        token + ", waiting for RCS connection.");
            return token;
        } catch (RemoteException e) {
            Log.e(TAG, "Remote exception happened during registering callback");
            try {
                callback.onServiceDisconnected(JoynService.Error.INTERNAL_ERROR);
            } catch (RemoteException e1) {
                e1.printStackTrace();
            }
            return Constants.INVALID;
        }
    }

    public void registerAck(long token) {
        try {
            if (null != mServiceImpl) {
                mServiceImpl.registerAck(token);
            } else {
                Log.e(TAG, "registerAck() but mServiceImpl is null");
            }
        } catch (RemoteException e) {
            Log.e(TAG, "Remote exception happened during registerAck");
            if (null != mCallbacks.get(token)) {
                try {
                    mCallbacks.get(token).onServiceDisconnected(JoynService.Error.INTERNAL_ERROR);
                } catch (RemoteException e1) {
                    e1.printStackTrace();
                }
            } else if (null != mWeekCallbacks.get(token)) {
                try {
                    mWeekCallbacks.get(token).
                        onServiceDisconnected(JoynService.Error.INTERNAL_ERROR);
                } catch (RemoteException e1) {
                    e1.printStackTrace();
                }
            }
        }
    }

    public void unregisterCallback(long token) {
        try {
            if (null != mServiceImpl) {
                mServiceImpl.unregisterCallback(token);
            } else {
                Log.e(TAG, "unregisterCallback() but mServiceImpl is null");
            }
        } catch (RemoteException e) {
            Log.e(TAG, "Remote exception happened during unregisterCallback");
            if (null != mCallbacks.get(token)) {
                try {
                    mCallbacks.get(token).onServiceDisconnected(JoynService.Error.INTERNAL_ERROR);
                } catch (RemoteException e1) {
                    e1.printStackTrace();
                }
            } else if (null != mWeekCallbacks.get(token)) {
                try {
                    mWeekCallbacks.get(token).
                        onServiceDisconnected(JoynService.Error.INTERNAL_ERROR);
                } catch (RemoteException e1) {
                    e1.printStackTrace();
                }
            }
        } finally {
            mCallbacks.remove(token);
            mWeekCallbacks.remove(token);
            if (mCallbacks.size() == 0) {
                Log.e(TAG, "unbindService");
                mInstance = null;
                for(Long tokenn : mWeekCallbacks.keySet()) {
                    try {
                        mServiceImpl.unregisterCallback(tokenn);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }
                for (IPAServiceCallback callback : mWeekCallbacks.values()) {
                    try {
                        callback.onServiceDisconnected(INTERNAL_ERROR);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }
                mAppContext.unbindService(mServiceConnection);
            }
        }
    }

    // TODO use gsma termial api exception instead
    public boolean isServiceConnected(long token) {
        try {
            return mServiceImpl != null && mServiceImpl.isServiceConnected();
        } catch (RemoteException e) {
            throw new Error(e);
        }
    }

    private void checkConnectStatus(long token) {
        if (token == Constants.INVALID) {
            throw new Error("Invalid token");
        }
        try {
            if (mServiceImpl == null || !mServiceImpl.isServiceConnected()) {
                throw new Error("Service not connected");
            }
        } catch (RemoteException e) {
            throw new Error(e);
        }
    }

    // TODO use GSMA terminal api exception instead
    public boolean isServiceRegistered(long token) {
        if (!isServiceConnected(token)) {
            throw new Error("Service is not connected.");
        }
        try {
            return mServiceImpl.isServiceRegistered();
        } catch (RemoteException e) {
            throw new Error(e);
        }
    }

    /**
     * Send message to public account specified by accountId.
     *
     * @param accountId
     *            Account ID in provider. Cannot be Constants.INVALID.
     * @param message
     *            Message to sent. Cannot be null.
     * @param system
     *            Whether this message is system message. System messages cannot
     *            be queried from provider unless you add a special query
     *            parameter.
     * @return The message being sent.
     */
    public long sendMessage(long token, long accountId, String message, boolean system) {
        checkConnectStatus(token);
        if (accountId == Constants.INVALID || message == null) {
            throw new Error("Invalid parameter");
        }
        try {
            return mServiceImpl.sendMessage(token, accountId, message, system);
        } catch (RemoteException e) {
            throw new Error(e);
        }
    }

    /**
     * Resend a failed message.
     * 
     * @param messageId
     *            the message ID to resend
     */
    public void resendMessage(long token, long messageId) {
        checkConnectStatus(token);
        try {
            mServiceImpl.resendMessage(token, messageId);
        } catch (RemoteException e) {
            throw new Error(e);
        }
    }

    public long sendImage(long token, long accountId, String path, String thumbnailPath) {
        checkConnectStatus(token);
        try {
            return mServiceImpl.sendImage(token, accountId, path, thumbnailPath);
        } catch (RemoteException e) {
            throw new Error(e);
        }
    }

    public long sendAudio(long token, long accountId, String path, int duration) {
        checkConnectStatus(token);
        try {
            return mServiceImpl.sendAudio(token, accountId, path, duration);
        } catch (RemoteException e) {
            throw new Error(e);
        }
    }

    public long sendVideo(long token, long accountId,
                String path, String thumbnailPath, int duration) {
        checkConnectStatus(token);
        try {
            return mServiceImpl.sendVideo(token, accountId, path, thumbnailPath, duration);
        } catch (RemoteException e) {
            throw new Error(e);
        }
    }

    public long sendGeoLoc(long token, long accountId, String data) {
        checkConnectStatus(token);
        try {
            return mServiceImpl.sendGeoLoc(token, accountId, data);
        } catch (RemoteException e) {
            throw new Error(e);
        }
    }

    public long sendVcard(long token, long accountId, String data) {
        checkConnectStatus(token);
        try {
            return mServiceImpl.sendVcard(token, accountId, data);
        } catch (RemoteException e) {
            throw new Error(e);
        }
    }

    public void complainSpamMessage(long token, long messageId) {
        checkConnectStatus(token);
        try {
            mServiceImpl.complainSpamMessage(token, messageId);
        } catch (RemoteException e) {
            throw new Error(e);
        }
    }

    public boolean deleteMessage(long token, long messageId) {
        checkConnectStatus(token);
        try {
            return mServiceImpl.deleteMessage(token, messageId);
        } catch (RemoteException e) {
            e.printStackTrace();
            return false;
        }
    }

    public long deleteMessageByAccount(long token, long accountId) {
        checkConnectStatus(token);
        try {
            return mServiceImpl.deleteMessageByAccount(token, accountId);
        } catch (RemoteException e) {
            throw new Error(e);
        }
    }

    public long setFavourite(long token, long messageId, int index) {
        checkConnectStatus(token);
        try {
            return mServiceImpl.setFavourite(token, messageId, index);
        } catch (RemoteException e) {
            throw new Error(e);
        }
    }

    public long subscribe(long token, String id) {
        checkConnectStatus(token);
        try {
            return mServiceImpl.subscribe(token, id);
        } catch (RemoteException e) {
            throw new Error(e);
        }
    }

    public long unsubscribe(long token, String id) {
        checkConnectStatus(token);
        try {
            return mServiceImpl.unsubscribe(token, id);
        } catch (RemoteException e) {
            throw new Error(e);
        }
    }

    public long getSubscribedList(long token, int order, int pageSize, int pageNumber) {
        checkConnectStatus(token);
        try {
            return mServiceImpl.getSubscribedList(token, order, pageSize, pageNumber);
        } catch (RemoteException e) {
            throw new Error(e);
        }
    }

    public long getDetails(long token, String uuid, String timestamp) {
        checkConnectStatus(token);
        try {
            return mServiceImpl.getDetails(token, uuid, timestamp);
        } catch (RemoteException e) {
            throw new Error(e);
        }
    }

    public long getMenu(long token, String uuid, String timestamp) {
        checkConnectStatus(token);
        try {
            return mServiceImpl.getMenu(token, uuid, timestamp);
        } catch (RemoteException e) {
            throw new Error(e);
        }
    }

    public long downloadObject(long token, long requestId, String url, int type) {
        checkConnectStatus(token);
        Log.d(TAG, "downloadObject(" + url + ", " + type + ")");
        try {
            return mServiceImpl.downloadObject(token, requestId, url, type);
        } catch (RemoteException e) {
            throw new Error(e);
        }
    }

    public void cancelDownload(long token, long cancelId) {
        checkConnectStatus(token);
        try {
            mServiceImpl.cancelDownload(cancelId);
        } catch (RemoteException e) {
            throw new Error(e);
        }
    }

    public long setAcceptStatus(long token, String uuid, int acceptStatus) {
        checkConnectStatus(token);
        try {
            return mServiceImpl.setAcceptStatus(token, uuid, acceptStatus);
        } catch (RemoteException e) {
            throw new Error(e);
        }
    }

    public long getMaxFileTransferSize(long token) {
        checkConnectStatus(token);
        try {
            return mServiceImpl.getMaxFileTransferSize();
        } catch (RemoteException e) {
            throw new Error(e);
        }
    }
}
