package com.cmcc.ccs.publicaccount;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import com.mediatek.rcs.pam.IDeviceApiService;
import com.mediatek.rcs.pam.IDeviceApiServiceCallback;

import org.gsma.joyn.JoynService;
import org.gsma.joyn.JoynServiceException;
import org.gsma.joyn.JoynServiceListener;
import org.gsma.joyn.JoynServiceNotAvailableException;
import org.gsma.joyn.JoynServiceRegistrationListener;

import java.util.LinkedList;
import java.util.List;

public class PublicAccountService extends JoynService {
    private static final String TAG = "PublicAccountService";

    private Context mContext;
    private long mToken;

    private IDeviceApiServiceCallback mCallback;
    private IDeviceApiService mDeviceApiService;
    private ServiceConnection mServiceConnection;

    private JoynServiceListener mListener;
    private List<PublicAccountChatListener> mEventListeners;
    private List<JoynServiceRegistrationListener> mRegistrationListeners;

    public static final String READ_PERMISSION = "com.cmcc.ccs.READ_PUBLICACCOUNT";
    public static final String WRITE_PERMISSION = "com.cmcc.ccs.WRITE_PUBLICACCOUNT";

    public PublicAccountService(Context context, JoynServiceListener listener) {
        super(context, listener);
        mContext = context;
        mListener = listener;
        mEventListeners = new LinkedList<PublicAccountChatListener>();
        mRegistrationListeners = new LinkedList<JoynServiceRegistrationListener>();
        mCallback = new IDeviceApiServiceCallback.Stub() {

            @Override
            public void onServiceConnected() throws RemoteException {
                Log.d(TAG, "onServiceConnected");
                mListener.onServiceConnected();
            }

            @Override
            public void onServiceDisconnected(int reason) throws RemoteException {
                Log.d(TAG, "onServiceDisconnected");
                mListener.onServiceDisconnected(reason);
            }

            @Override
            public void onServiceRegistered() throws RemoteException {
                Log.d(TAG, "onServiceRegistered");
                for (JoynServiceRegistrationListener listener : mRegistrationListeners) {
                    listener.onServiceRegistered();
                }
            }

            @Override
            public void onServiceUnregistered() throws RemoteException {
                Log.d(TAG, "onServiceUnregistered");
                for (JoynServiceRegistrationListener listener : mRegistrationListeners) {
                    listener.onServiceUnregistered();
                }
            }

            @Override
            public void onNewPublicAccountChat(String account, String msgId)
                    throws RemoteException {
                Log.d(TAG, "onNewPublicAccountChat, id = " + msgId);
                for (PublicAccountChatListener listener : mEventListeners) {
                    listener.onNewPublicAccountChat(account, msgId);
                }
            }

            @Override
            public void onNewCCPublicAccoutChat(String accountnumber, String msgId)
                    throws RemoteException {
                Log.d(TAG, "onNewCCPublicAccoutChat, id = " + msgId);
                for (PublicAccountChatListener listener : mEventListeners) {
                    listener.onNewCCPublicAccoutChat(accountnumber, msgId);
                }
            }

            @Override
            public void onPublicAccoutChatHistory(String publicaccount, long id)
                    throws RemoteException {
                Log.d(TAG, "onPublicAccoutChatHistory,id = " + id);
                for (PublicAccountChatListener listener : mEventListeners) {
                    listener.onPublicAccoutChatHistory(publicaccount, id);
                }
            }

            @Override
            public void onFollowPublicAccount(String account, int errType, String statusCode)
                    throws RemoteException {
                Log.d(TAG, "onFollowPublicAccount,errType = " + errType);
                for (PublicAccountChatListener listener : mEventListeners) {
                    listener.onFollowPublicAccount(account, errType, statusCode);
                }
            }

            @Override
            public void onUnfollowPublicAccount(String account, int errType, String statusCode)
                    throws RemoteException {
                Log.d(TAG, "onUnfollowPublicAccount, errType = " + errType);
                for (PublicAccountChatListener listener : mEventListeners) {
                    listener.onUnfollowPublicAccount(account, errType, statusCode);
                }
            }

            @Override
            public void onGetInfo(String account, int errType, String statusCode)
                    throws RemoteException {
                Log.d(TAG, "onGetInfo, errType = " + errType);
                for (PublicAccountChatListener listener : mEventListeners) {
                    listener.onGetInfo(account, errType, statusCode);
                }
            }

            @Override
            public void onSearch(int errType, String statusCode) throws RemoteException {
                Log.d(TAG, "onSearch, errType = " + errType);
                for (PublicAccountChatListener listener : mEventListeners) {
                    listener.onSearch(errType, statusCode);
                }
            }

            @Override
            public void onGetFollowedPublicAccount(int errType, String statusCode)
                    throws RemoteException {
                Log.d(TAG, "onGetFollowedPublicAccount, errType = " + errType);
                for (PublicAccountChatListener listener : mEventListeners) {
                    listener.onGetFollowedPublicAccount(errType, statusCode);
                }
            }

            @Override
            public void onMenuConfigUpdated(String account, String configInfo, int errType,
                    String statusCode) throws RemoteException {
                Log.d(TAG, "onMenuConfigUpdated, errType = " + errType);
                for (PublicAccountChatListener listener : mEventListeners) {
                    listener.onMenuConfigUpdated(account, configInfo, errType, statusCode);
                }
            }

            @Override
            public void onReportPublicAccount(String account, int errType, String statusCode)
                    throws RemoteException {
                Log.d(TAG, "onReportPublicAccount, errType = " + errType);
                for (PublicAccountChatListener listener : mEventListeners) {
                    listener.onReportPublicAccount(account, errType, statusCode);
                }
            }
        };

        mServiceConnection = new ServiceConnection() {
            @Override
            public void onServiceDisconnected(ComponentName name) {
                mDeviceApiService = null;
                mListener.onServiceDisconnected(JoynService.Error.INTERNAL_ERROR);
            }

            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                Log.d(TAG, "onServiceConnected");
                mDeviceApiService = IDeviceApiService.Stub.asInterface(service);
                try {
                    mToken = mDeviceApiService.addDeviceApiCallback(mCallback);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        };
    }

    @Override
    public void addServiceRegistrationListener(JoynServiceRegistrationListener listener)
            throws JoynServiceException {
        try {
            if (mDeviceApiService.isServiceConnected(mToken)) {
                mRegistrationListeners.add(listener);
            } else {
                throw new JoynServiceNotAvailableException();
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void removeServiceRegistrationListener(JoynServiceRegistrationListener listener)
            throws JoynServiceException {
        try {
            if (mDeviceApiService.isServiceConnected(mToken)) {
                mRegistrationListeners.remove(listener);
            } else {
                throw new JoynServiceNotAvailableException();
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    /**
     * 6.7.3.1
     */
    @Override
    public void connect() {
        Intent intent = new Intent("com.mediatek.rcs.pam.IDeviceApiService");
        boolean ret = mContext.bindService(intent, mServiceConnection, Context.BIND_AUTO_CREATE);
        Log.d(TAG, "connect end, bindService ret = " + ret);
    }

    /**
     * 6.7.3.2
     */
    @Override
    public void disconnect() {
        try {
            mDeviceApiService.removeDeviceApiCallback(mToken);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        mContext.unbindService(mServiceConnection);
    }

    /**
     * 6.7.3.3
     */
    public void addEventListener(PublicAccountChatListener listener) {
        mEventListeners.add(listener);
    }

    /**
     * 6.7.3.4 Question: change from "public void removeEventListener()" ?
     */
    public void removeChatListener(PublicAccountChatListener listener) {
        mEventListeners.remove(listener);
    }

    /**
     * Synchronous API. 6.7.3.5
     *
     * @param account UUID of the public account
     * @param message Body of message
     * @return
     */
    public String sendMessage(String accountnumber, String message) {
        try {
            return mDeviceApiService.sendMessage(mToken, accountnumber, message);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 6.7.3.6
     *
     */
    public boolean deleteMessage(String msgId) {
        try {
            return mDeviceApiService.deleteMessage(mToken, msgId);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * 6.7.3.7
     *
     */
    public String sendMenuMessage(String accountnumber, String menuID) {
        try {
            return mDeviceApiService.sendMenuMessage(mToken, accountnumber, menuID);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 6.7.3.8
     */
    public boolean setMessageRead(String msgId) {
        try {
            return mDeviceApiService.setMessageRead(mToken, msgId);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * 6.7.3.9
     *
     * @param account
     */
    public void getPublicAccountInfo(String account) {
        try {
            mDeviceApiService.getPublicAccountInfo(mToken, account);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    /**
     * 6.7.3.10
     *
     */
    public boolean getPublicAccountHistory(String accountnumber, String timestamp, int order,
            int pageno, int pagesize, long id) {
        try {
            return mDeviceApiService.getPublicAccountHistory(mToken, accountnumber, timestamp,
                    order, pageno, pagesize, id);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * 6.7.3.11
     *
     * @param account
     */
    public boolean getFollowedPublicAccount(int pageno, int order, int pagesize) {
        try {
            return mDeviceApiService.getFollowedPublicAccount(mToken, pageno, order, pagesize);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * 6.7.12
     *
     * @param accountnumber)
     */
    public void followPublicAccount(String accountnumber) {
        try {
            mDeviceApiService.followPublicAccount(mToken, accountnumber);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    /**
     * 6.7.3.13
     */
    public void searchPublicAccount(final String keyword, final int pageNum, final int order,
            final int pageSize) {
        try {
            mDeviceApiService.searchPublicAccount(mToken, keyword, pageNum, order, pageSize);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    /**
     * 6.7.3.14
     *
     * @param account
     */
    public void unfollowPublicAccount(String accountnumber) {
        try {
            mDeviceApiService.unfollowPublicAccount(mToken, accountnumber);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    /**
     * 6.7.3.15
     *
     * @param accountnumber
     */
    public boolean getPublicAccountStatus(String accountnumber) {
        try {
            return mDeviceApiService.getPublicAccountStatus(mToken, accountnumber);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * 6.7.3.16
     *
     * @param account
     * @param reason
     * @param description
     * @param type
     * @param data
     */
    public void reportPublicAccount(final String account, final String reason,
            final String description, final int type, final String data) {
        try {
            mDeviceApiService.reportPublicAccount(mToken, account, reason,
                    description, type, data);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    /**
     * 6.7.3.17
     *
     * @param account
     */
    public void updateMenuConfig(String accountnumber) {
        try {
            mDeviceApiService.updateMenuConfig(mToken, accountnumber);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }
}
