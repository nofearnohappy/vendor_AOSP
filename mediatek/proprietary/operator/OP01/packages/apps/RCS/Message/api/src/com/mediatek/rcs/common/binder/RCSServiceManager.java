package com.mediatek.rcs.common.binder;

import java.util.HashSet;

import org.gsma.joyn.JoynServiceConfiguration;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.os.IBinder;
import android.os.RemoteException;
import android.telephony.PhoneNumberUtils;
import android.telephony.SubscriptionManager;
import android.text.TextUtils;
import android.util.Log;

import com.mediatek.rcs.common.IBurnMessageCapabilityListener;
import com.mediatek.rcs.common.IFileSpamReportListener;
import com.mediatek.rcs.common.ISpamReportListener;
import com.mediatek.rcs.common.IpMessageConsts;
import com.mediatek.rcs.common.IpMessageConsts.FeatureId;
import com.mediatek.rcs.common.RCSMessageManager;
import com.mediatek.rcs.common.provider.GroupMemberData;
import com.mediatek.rcs.common.provider.GroupChatCache;
import com.mediatek.rcs.common.provider.GroupChatData;
import com.mediatek.rcs.common.provider.GroupChatUtils;
import com.mediatek.rcs.common.service.IRCSChatService;
import com.mediatek.rcs.common.service.IRCSChatServiceListener;
import com.mediatek.rcs.common.utils.ContextCacher;
import com.mediatek.rcs.common.utils.Logger;
import com.mediatek.rcs.common.utils.RCSUtils;


public class RCSServiceManager {

    private static final String TAG = "RCSServiceManager";

    private final String RCS_CONFIGURATION_CHANGED_ACTION
                                            = "com.orangelabs.rcs.CONFIGURATION_STATUS_TO_APP";
    private final String RCS_SERVICE_STATUS_CHANGED_ON_ACTION
                                            = "com.mediatek.intent.rcs.stack.LaunchService";
    private final String RCS_SERVICE_STATUS_CHANGED_OFF_ACTION
                                            = "com.mediatek.intent.rcs.stack.StopService";
    private final String RCS_SERVICE_NOTIFY_ACTION = "org.gsma.joyn.action.VIEW_SETTINGS";
    private final String RCS_SERVICE_REGISTRERAD_STATUS
                                          = "com.mediatek.rcs.message.REGISTRATION_STATUS_CHANGE";
//    public static final String RCS_SERVICE_MANAGER_READY
//                                                   = "com.mediatek.rcs.message.service.initiated";
    public static final String RCS_SYNC_GROUP_CHATS = "com.mediatek.rcs.message.SYNC_CHATS";
    //add to manifest as a action.
    public static final String RCS_SYNC_GROUP_CHATS_DONE
                                            = "com.mediatek.rcs.message.SYNC_CHATS_DONE";
    public static final String RCS_SERVICE_STATE_ACTION = "com.mediatek.rcs.message.SERVICE_STATE";
    public static final String RCS_SERVICE_STATE_CONFIGUREAD = "configured";
    public static final String RCS_SERVICE_STATE_ACTIVATED = "activated";
    public static final String RCS_SERVICE_STATE_REGISTERED = "registered";

    private final static int RCS_CORE_LOADED = 0;
    private final static int RCS_CORE_STARTED = 2;
    private final static int RCS_CORE_STOPPED = 3;
    private final static int RCS_CORE_IMS_CONNECTED = 4;
    private final static int RCS_CORE_IMS_DISCONNECTED = 9;

    private boolean mServiceActivated = false; //whether service is on or off
    private boolean mServiceConfigured = false;//whether configuration completed
    private boolean mServiceRegistered = false; // whether service registered
    private String mNumber = "";

    private static RCSServiceManager sInstance = null;
    private IRCSChatService mChatService = null;
    private Context mContext = null;
    private IRCSChatServiceListener mListener = null;

    public static final int RCS_SERVICE_STATE_CONNECTING = 1;
    public static final int RCS_SERVICE_STATE_CONNECTTED = 2;
    public static final int RCS_SERVICE_STATE_DISCONNECTTED = 3;
    private int mState;

    private HashSet<INotifyListener> mNotifyListeners = new HashSet<INotifyListener>();

    private HashSet<IBurnMessageCapabilityListener> mBurnMsgCapListeners =
                                                new HashSet<IBurnMessageCapabilityListener>();

    private HashSet<ISpamReportListener> mSpamReportListeners =
        new HashSet<ISpamReportListener>();
    private HashSet<IFileSpamReportListener> mFileSpamReportListeners =
        new HashSet<IFileSpamReportListener>();

    private HashSet<OnServiceChangedListener> mServiceChangedListeners =
            new HashSet<RCSServiceManager.OnServiceChangedListener>();

    private RCSServiceManager(Context context) {
        Logger.d(TAG, "call constructor");
        mContext = context;
        bindRemoteService();
        mServiceActivated = JoynServiceConfiguration.isServiceActivated(mContext);
        mNumber = getMyAccountNumber();
        mServiceConfigured = getServiceConfigurationState();
        IntentFilter filter = new IntentFilter(RCS_CONFIGURATION_CHANGED_ACTION);
        filter.addAction(RCS_SERVICE_STATUS_CHANGED_ON_ACTION);
        filter.addAction(RCS_SERVICE_STATUS_CHANGED_OFF_ACTION);
        filter.addAction(RCS_SERVICE_NOTIFY_ACTION);
        filter.addAction(RCS_SERVICE_REGISTRERAD_STATUS);
        context.registerReceiver(mRecever, filter);
        broadcastConfigurationChanged();
    }

    private ServiceConnection mConnection = new ServiceConnection() {

        public void onServiceConnected(ComponentName name, IBinder service) {
            Logger.d(TAG, "service connect!!");
            mChatService = IRCSChatService.Stub.asInterface(service);
            mListener = new RCSChatServiceListener();
            try {
                mChatService.addRCSChatServiceListener(mListener);
//                List<String> chatIds = RCSDataBaseUtils.getAvailableGroupChatIds();
//                mChatService.startGroups(chatIds);
                RCSMessageManager.getInstance().deleteLastBurnedMessage();
                updateServiceRegistrationState();
            } catch (RemoteException e) {
                e.printStackTrace();
            }
            mState = RCS_SERVICE_STATE_CONNECTTED;
            notifyServiceStateChanged();
            updateRcsProvisioningData();
        }

        public void onServiceDisconnected(ComponentName name) {
            Logger.d(TAG, "service disconnect!!");
            mChatService = null;
            bindRemoteService();
        }
    };

    private BroadcastReceiver mRecever = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.d(TAG, "mRecever: " + action);
            if (action == null) {
                return;
            }
            if (action.equals(RCS_CONFIGURATION_CHANGED_ACTION)) {
                updateRcsProvisioningData();
                updateServiceConfigurationState();
                asyncGroupChat();
            } else if (action.equals(RCS_SERVICE_STATUS_CHANGED_ON_ACTION)) {
                setServiceActivated(true);
            } else if (action.equals(RCS_SERVICE_STATUS_CHANGED_OFF_ACTION)) {
                setServiceActivated(false);
            } else if (action.equals(RCS_SERVICE_NOTIFY_ACTION)) {
                int state = intent.getIntExtra("label_enum", -1);
                Logger.d(TAG, "state = " + state);
                if (state == RCS_CORE_LOADED || state == RCS_CORE_STARTED ||
                        state == RCS_CORE_STOPPED ||
                        state == RCS_CORE_IMS_CONNECTED ||state == RCS_CORE_IMS_DISCONNECTED) {
                    updateServiceState();
                }
                if (state == RCS_CORE_LOADED) {
                    // after stack loaded, update provisioning data.
                    updateRcsProvisioningData();
                }
            } else if (action.equals(RCS_SERVICE_REGISTRERAD_STATUS)) {
                boolean registered = intent.getBooleanExtra("status", false);
                setServiceRegistrationState(registered);
                asyncUpdateAccountNumber(registered);
            }
        }
    };

    public interface INotifyListener {
        void notificationsReceived(Intent intent);
    }
    /**
     * Get service Listeners.
     * @return IRCSChatServiceListener
     */
    public IRCSChatServiceListener getServiceListener() {
        return mListener;
    }

    /**
     * Regist notify listener.
     * @param listener INotifyListener
     */
    public void registNotifyListener(INotifyListener listener) {
        mNotifyListeners.add(listener);
    }

    /**
     * UnregistNotifyListener.
     * @param listener INotifyListener
     */
    public void unregistNotifyListener(INotifyListener listener) {
        mNotifyListeners.remove(listener);
    }

    /**
     * registBurnMsgCapListener.
     * @param listener IBurnMessageCapabilityListener
     */
    public void registBurnMsgCapListener(IBurnMessageCapabilityListener listener) {
        mBurnMsgCapListeners.add(listener);
    }

    /**
     * unregistBurnMsgCapListener.
     * @param listener IBurnMessageCapabilityListener
     */
    public void unregistBurnMsgCapListener(IBurnMessageCapabilityListener listener) {
        mBurnMsgCapListeners.remove(listener);
    }

    /**
     * callNotifyListeners.
     * @param intent
     */
    public void callNotifyListeners(Intent intent) {
        Logger.d(TAG, "callNotifyListeners, action=" + intent.getAction());
        for (INotifyListener listener : mNotifyListeners) {
            listener.notificationsReceived(intent);
        }
    }

    /**
     * callBurnCapListener.
     * @param contact
     * @param result
     */
    public void callBurnCapListener(String contact, boolean result) {
        for (IBurnMessageCapabilityListener listener : mBurnMsgCapListeners) {
            listener.onRequestBurnMessageCapabilityResult(contact, result);
        }
    }

    /**
     * service is ready.
     * @return  return true if service is registered or return false.
     */
    public boolean serviceIsReady() {
        Log.d(TAG, "serviceIsReady() entry: " + mServiceRegistered);
        return mServiceRegistered;
    }

    /**
     * get self account number, if not configured, return ""
     * @return
     */
    public String getMyNumber() {
        Logger.d(TAG, "myNumber = " + mNumber);
        return mNumber;
    }

    private String getMyAccountNumber() {
        String publicUri = new JoynServiceConfiguration().getPublicUri(mContext);
        String number = "";
        if (!TextUtils.isEmpty(publicUri)) {
            if (publicUri.indexOf('@') != -1) {
                number = publicUri.substring(publicUri.indexOf(':') + 1, publicUri.indexOf('@'));
            } else {
                number = publicUri.substring(publicUri.indexOf(':') + 1);
            }
        }
        Logger.d(TAG, "getMyAccountNumber = " + number);
        return number;
    }

    private void asyncUpdateAccountNumber(boolean registered) {
        if (registered) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    mNumber = getMyAccountNumber();
                }
            }).start();
        }
    }

    private void updateServiceActivatedState() {
        boolean activate = JoynServiceConfiguration.isServiceActivated(mContext);;
        if (mServiceActivated != activate) {
            mServiceActivated = activate;
            notifyServiceStateChanged();
        }
    }

    private void updateServiceConfigurationState() {
        boolean configured = getServiceConfigurationState();
        if (mServiceConfigured != configured) {
            mServiceConfigured = configured;
            notifyServiceStateChanged();
        }
    }

    private boolean getServiceConfigurationState() {
        return new JoynServiceConfiguration().getConfigurationState(mContext);
    }

    private void updateServiceRegistrationState() {
        boolean registered = false;
        try {
            if (mChatService != null) {
                registered = mChatService.getRegistrationStatus();
            }
            setServiceRegistrationState(registered);
        } catch (RemoteException e) {
            e.printStackTrace();
            Logger.d(TAG, "updateServiceRegistrationState: e = " + e);
        }
    }

    private void updateServiceState() {
        boolean needNotify = false;
        boolean activated = JoynServiceConfiguration.isServiceActivated(mContext);
        if (mServiceActivated != activated) {
            mServiceActivated = activated;
            needNotify = true;
        }

        boolean configured = getServiceConfigurationState();
        if (mServiceConfigured != configured) {
            mServiceConfigured = configured;
            needNotify = true;
        }

        try {
            if (mChatService != null) {
                boolean registed = mChatService.getRegistrationStatus();
                if (mServiceRegistered != registed) {
                    mServiceRegistered = registed;
                    needNotify = true;
                }
                //TODO: temp process, should remove the code after auto configuration @{
                if (mServiceRegistered == true) {
                    if (mServiceConfigured != true) {
                        mServiceConfigured = true;
                        needNotify = true;
                    }
                }
                // @}
            }
        } catch (RemoteException e) {
            e.printStackTrace();
            Logger.d(TAG, "updateServiceState: e = " + e);
        }
        Log.d(TAG, "updateServiceState: needNotify = " + needNotify);
        if (needNotify) {
            notifyServiceStateChanged();
        }
    }

    /**
     * whether service is enabled.
     * @return true if service is activated and configured
     */
    public boolean isServiceEnabled() {
        return (mServiceConfigured && mServiceActivated);
    }

    private void bindRemoteService() {
        Logger.d(TAG, "bindRemoteService");
        Intent intent = new Intent("com.mediatek.rcs.messageservice.chat.RCSMessageChatService");
        intent.setPackage("com.mediatek.rcs.messageservice");
        mContext.startService(intent);
        mContext.bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
        mState = RCS_SERVICE_STATE_CONNECTING;
        notifyServiceStateChanged();
    }

    private void asyncGroupChat() {
        new Thread(new Runnable() {

            @Override
            public void run() {
                syncGroupChat();
            }

        }).start();
    }

    private void syncGroupChat() {
        Logger.d(TAG, "syncGroupChat start");
        GroupChatCache cache = GroupChatCache.getInstance();
        if (cache == null) {
            GroupChatCache.createInstance(ContextCacher.getPluginContext());
            cache = GroupChatCache.getInstance();
        }
        mNumber = getMyAccountNumber();
        ContentResolver resolver = ContextCacher.getPluginContext().getContentResolver();
        int subId = RCSUtils.getRCSSubId();
        String threadSelection =
                "(status<>" + subId + " AND status>0) " +
                " OR " +
                "(status=" + IpMessageConsts.GroupActionList.GROUP_STATUS_INVITING +
                        " AND sub_id<>" + subId + ")";
        Cursor cursor = resolver.query(GroupChatData.CONTENT_URI,
                               GroupChatUtils.GROUP_CHAT_PROJECTION, threadSelection, null, null);
        try {
            while (cursor.moveToNext()) {
                int status = cursor.getInt(cursor.getColumnIndex(GroupChatData.KEY_STATUS));
                String chatId = cursor.getString(cursor.getColumnIndex(GroupChatData.KEY_CHAT_ID));
                int threadSubId = cursor.getInt(cursor.getColumnIndex(GroupChatData.KEY_SUB_ID));
                if (status == IpMessageConsts.GroupActionList.GROUP_STATUS_INVITING
                        && threadSubId != subId) {
                    GroupChatCache.getInstance().updateStatusByChatId(
                            chatId, IpMessageConsts.GroupActionList.GROUP_STATUS_INVALID);
                    continue;
                }
                String selection = GroupMemberData.COLUMN_CHAT_ID + "='" + chatId + "'";
                Cursor memberCursor = resolver.query(GroupMemberData.CONTENT_URI,
                        RCSUtils.PROJECTION_GROUP_MEMBER, selection, null, null);
                try {
                    boolean validGroup = false;
                    while (memberCursor.moveToNext()) {
                        String number = memberCursor.getString(
                                memberCursor.getColumnIndex(GroupMemberData.COLUMN_CONTACT_NUMBER));
                        if (PhoneNumberUtils.compare(number, mNumber)) {
                            GroupChatCache.getInstance().updateStatusByChatId(chatId, subId);
                            validGroup = true;
                            break;
                        }
                    }
                    if (!validGroup) {
                        GroupChatCache.getInstance().updateStatusByChatId(chatId,
                                IpMessageConsts.GroupActionList.GROUP_STATUS_INVALID);
                    }
                } finally {
                    if (memberCursor != null) {
                        memberCursor.close();
                    }
                }
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        Logger.d(TAG, "syncGroupChat end");
    }

    private void setServiceActivated(boolean activated) {
        Logger.d(TAG, "setServiceActivated: " + activated);
        if (mServiceActivated != activated) {
            mServiceActivated = activated;
            notifyServiceStateChanged();
        }
    }

    public int getServiceState() {
        return mState;
    }

    public boolean isServiceActivated() {
        return mServiceActivated;
    }

    private void setServiceRegistrationState(boolean registered) {
        Logger.d(TAG, "setServiceRegistrationState: " + registered);
        if (mServiceRegistered != registered) {
            mServiceRegistered = registered;
            if (mState == RCS_SERVICE_STATE_CONNECTTED) {
                //only service binded to notify registration state
                notifyServiceStateChanged();
            }
        }
    }

    private void notifyServiceStateChanged() {
        Logger.d(TAG, "notifyServiceStateChanged: mServiceActivated = " + mServiceActivated +
                ", mServiceConfigured = " + mServiceConfigured +
                ", mServiceRegistered = " + mServiceRegistered);
        for (OnServiceChangedListener l : mServiceChangedListeners) {
            l.onServiceStateChanged(mState, mServiceActivated,
                    mServiceConfigured, mServiceRegistered);
        }
        broadcastConfigurationChanged();
    }

    /**
     *  Is Service configured.
     * @return return true is configuration success full or turn false
     */
    public boolean isServiceConfigured() {
        return mServiceConfigured;
    }

    public boolean isFeatureSupported(int featureId) {
        Log.d(TAG, "isFeatureSupported() featureId is " + featureId);
        switch (featureId) {
            case FeatureId.FILE_TRANSACTION:
            case FeatureId.EXTEND_GROUP_CHAT:
            case FeatureId.GROUP_MESSAGE:
                return true;
            case FeatureId.PARSE_EMO_WITHOUT_ACTIVATE:
            case FeatureId.CHAT_SETTINGS:
            case FeatureId.ACTIVITION:
            case FeatureId.ACTIVITION_WIZARD:
            case FeatureId.MEDIA_DETAIL:
            case FeatureId.TERM:
             case FeatureId.EXPORT_CHAT:
            case FeatureId.APP_SETTINGS:
                  return false;
            default:
                return false;
        }
    }

    /**
     * CreateManager instance.
     * @param context Context
     */
    public static void createManager(Context context) {
        Logger.d(TAG, "createManager, entry");
        if (sInstance == null) {
            sInstance = new RCSServiceManager(context);
        }
    }

    /**
     * Get instance.
     * @return RCSServiceManager instance
     */
    public static RCSServiceManager getInstance() {
        if (sInstance == null) {
            throw new RuntimeException("need call createManager to create instance");
        }
        return sInstance;
    }

    /**
     * Get chat Service.
     * This function should not be called in main thread,
     * or it will block onServiceConnected
     * @return IRCSChatService
     */
    public IRCSChatService getChatService() {
        int i = 0;
        while (mChatService == null && i++ < 3) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return mChatService;
    }

    /**
     * GetBurnMsgCap.
     * @param contact
     * @return
     */
    public boolean getBurnMsgCap(String contact) {
        Logger.d(TAG, "getBurnMsgCap for " + contact);
        boolean cap = false;
        try {
            if (mChatService != null) {
                mChatService.getBurnMessageCapability(contact);
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return cap;
    }

    @Override
    public void finalize() {
        try {
            Logger.d(TAG, "finalize()!!");
            mContext.unbindService(mConnection);
            mContext.unregisterReceiver(mRecever);
            super.finalize();
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    /**
     * Listen rcs service state changed.
     * @author
     *
     */
    public interface OnServiceChangedListener {
        /**
         *
         * @param state whether service state
         * @param activated  whether service is turn on, true is on, false is off
         * @param configured whether service is configured, true is configured
         * @param registered whether service is registered
         */
        public void onServiceStateChanged(int state, boolean activated,
                boolean configured, boolean registered);
    }

    /**
     * Add service changed listener.
     * @param l OnServiceChangedListener
     * @return  return true if add successful or return false
     */
    public boolean addOnServiceChangedListener(OnServiceChangedListener l) {
        return mServiceChangedListeners.add(l);
    }

    /**
     * Remove service changed listener.
     * @param l OnServiceChangedListener
     * @return  return true if remove successful or return false
     */
    public boolean removeOnServiceChangedListener(OnServiceChangedListener l) {
        return mServiceChangedListeners.remove(l);
    }

    private void broadcastConfigurationChanged() {
        Intent intent = new Intent(RCS_SERVICE_STATE_ACTION);
        intent.putExtra(RCS_SERVICE_STATE_ACTIVATED, mServiceActivated);
        intent.putExtra(RCS_SERVICE_STATE_CONFIGUREAD, mServiceConfigured);
        intent.putExtra(RCS_SERVICE_STATE_REGISTERED, mServiceRegistered);
        mContext.sendBroadcast(intent);
    }

    /**
     * registSpamReportListener.
     * @param listener ISpamReportListener
     */
    public void registSpamReportListener(ISpamReportListener listener) {
        Log.d(TAG, "[spam-report] registSpamReportListener listener: "+listener  );
        mSpamReportListeners.add(listener);
    }

    /**
     * unregistSpamReportListener.
     * @param listener ISpamReportListener
     */
    public void unregistSpamReportListener(ISpamReportListener listener) {
        Log.d(TAG, "[spam-report] unregistSpamReportListener listener: "+listener  );
        mSpamReportListeners.remove(listener);
    }

    /**
     * handleSpamReportResult.
     * @param contact
     * @param result
     */
    public void handleSpamReportResult(String contact, String msgId, int errorcode) {
        Log.d(TAG, "[spam-report] handleSpamReportResult contact: "+contact+
                " msgId: "+msgId+ " errorcode: "+errorcode  );
        for (ISpamReportListener listener : mSpamReportListeners) {
            listener.onSpamReportResult(contact, msgId, errorcode);
        }
    }

    /**
     * registFileSpamReportListener.
     * @param listener IFileSpamReportListener
     */
    public void registFileSpamReportListener(IFileSpamReportListener listener) {
        Log.d(TAG, "[spam-report] registFileSpamReportListener listener: "+listener  );
        mFileSpamReportListeners.add(listener);
    }

    /**
     * unregistFileSpamReportListener.
     * @param listener IFileSpamReportListener
     */
    public void unregistFileSpamReportListener(IFileSpamReportListener listener) {
        Log.d(TAG, "[spam-report] unregistFileSpamReportListener listener: "+listener  );
        mFileSpamReportListeners.remove(listener);
    }

    /**
     * handleFileSpamReportResult.
     * @param contact
     * @param result
     */
    public void handleFileSpamReportResult(String contact, String msgId, int errorcode) {
        Log.d(TAG, " [spam-report] handleFileSpamReportResult: #" + contact +
                " ,#" + msgId + " ,#" + errorcode);
        for (IFileSpamReportListener listener : mFileSpamReportListeners) {
            listener.onFileSpamReportResult(contact, msgId, errorcode);
        }
    }

    /**
     * Get the max participants number of Group chat.
     * @return the max participants number of Group chat, return 0 if error happens.
     */
     public int getGroupChatMaxParticipantsNumber() {
         int number = 0;
         int subId = RCSUtils.getRCSSubId();
         Logger.d(TAG, "[getGroupChatMaxParticipantsNumber] subId = " + subId);
         if (SubscriptionManager.isValidSubscriptionId(subId)) {
             SharedPreferences sp = mContext.getSharedPreferences(SP_CONFIGRATION_DATA,
                     Context.MODE_WORLD_READABLE);
             number = sp.getInt(KEY_GROUPCHAT_MAX_PARTICIPANT_NUMBER + subId, 0);
         }
         Logger.d(TAG, "[getGroupChatMaxParticipantsNumber] = " + number);
         return number;
     }

     /**
      * Get file transfer max size.
      * @return the max size of file transfer
      */
     public long getFileTransferMaxSize() {
         long maxSize = 0;
         int subId = RCSUtils.getRCSSubId();
         Logger.d(TAG, "[getFileTransferMaxSize] subId = " + subId);
         if (SubscriptionManager.isValidSubscriptionId(subId)) {
             SharedPreferences sp = mContext.getSharedPreferences(SP_CONFIGRATION_DATA,
                     Context.MODE_WORLD_READABLE);
             maxSize = sp.getLong(KEY_FILETRANSFER_MAX_SIZE + subId, 0);
         }
         Logger.d(TAG, "[getFileTransferMaxSize] = " + maxSize);
         return maxSize;
     }

     private static final String SP_CONFIGRATION_DATA = "RCSCONFIG";
     private static final String KEY_GROUPCHAT_MAX_PARTICIPANT_NUMBER =
                                                     "groupChatMaxParcipantsNumber_";
     private static final String KEY_FILETRANSFER_MAX_SIZE = "fileTransferMaxSize_";
     private void updateRcsProvisioningData() {
         int groupChatMaxParcipantsNumber = 0;
         long fileTransferMaxSize = 0;
         if (mChatService == null) {
             Logger.d(TAG, "[updateRcsProvisioningData] chatService is null");
             return;
         }
         int subId = RCSUtils.getRCSSubId();
         if (!SubscriptionManager.isValidSubscriptionId(subId)) {
             Logger.d(TAG, "[updateRcsProvisioningData] subId is invalid: " + subId);
             return;
         }
         try {
             groupChatMaxParcipantsNumber = mChatService.getGroupChatMaxParticipantsNumber();
             fileTransferMaxSize = mChatService.getRcsFileTransferMaxSize();
         } catch (RemoteException e) {
             // TODO: handle exception
             e.printStackTrace();
         }
         Logger.d(TAG, "[updateRcsProvisioningData]:groupChatMaxParcipantsNumber = " +
         groupChatMaxParcipantsNumber + ", fileTransferMaxSize=" + fileTransferMaxSize);
         if (groupChatMaxParcipantsNumber == 0 && fileTransferMaxSize == 0) {
             return;
         }
         SharedPreferences sp = mContext.getSharedPreferences(SP_CONFIGRATION_DATA,
                 Context.MODE_WORLD_READABLE | Context.MODE_WORLD_WRITEABLE);
         Editor editor = sp.edit();
         if (groupChatMaxParcipantsNumber > 0) {
             editor.putInt(KEY_GROUPCHAT_MAX_PARTICIPANT_NUMBER + subId,
                                                 groupChatMaxParcipantsNumber);
         }
         if (fileTransferMaxSize > 0) {
             editor.putLong(KEY_FILETRANSFER_MAX_SIZE + subId, fileTransferMaxSize);
         }
         editor.commit();
     }
}
