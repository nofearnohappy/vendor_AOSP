package com.hesine.nmsg.thirdparty;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;

import com.hesine.hstat.HstatSdk;
import com.hesine.nmsg.Application;
import com.hesine.nmsg.R;
import com.hesine.nmsg.business.bean.ServiceInfo;

public class Statistics {
    private static final String EVENT_ID_RECEIVE_MSG_SUCCESS = "10000001";
    private static final String EVENT_ID_RECEIVE_PN_NOTIFICATION = "10000002";
    private static final String EVENT_ID_CLICK_APP = "10000003";
    private static final String EVENT_ID_MSG_READ = "10000004";
    private static final String EVENT_ID_MSG_SHARE = "10000005";
    private static final String EVENT_ID_SERVICE_ACCOUNT = "10000006";
    private static final String EVENT_ID_MSG_SEND = "10000007";
    private static final String EVENT_ID_DELETE_THREADS = "10000008";
    private static final String EVENT_ID_UUID = "10000009";
    private static final String EVENT_ID_ACCOUNT_STATUS = "10000010";
    private static final String EVENT_ID_REQUEST_MSG = "10000011";
    private static final String EVENT_ID_APK_LIST_INFO = "10000012";
    private static final String EVENT_ID_RECEIVE_MSG_FAIL = "10000013";
    private static final String EVENT_ID_MSG_SEND_SUCCESS = "10000014";
    private static final String EVENT_ID_MSG_SEND_FAIL = "10000015";
    
    private static final String EVENT_LABLE_RECEIVE_MSG_SUCCESS = Application.getInstance()
            .getString(R.string.statistics_receive_msg_success);
    private static final String EVENT_LABLE_RECEIVE_PN_NOTIFICATION = Application.getInstance()
            .getString(R.string.statistics_receive_pn_norification);
    private static final String EVENT_LABLE_CLICK_APP = Application.getInstance().getString(
            R.string.statistics_app_click);
    private static final String EVENT_LABLE_MSG_READ = Application.getInstance().getString(
            R.string.statistics_msg_read);
    private static final String EVENT_LABLE_MSG_SHARE = Application.getInstance().getString(
            R.string.statistics_msg_share);
    private static final String EVENT_LABLE_SERVICE_ACCOUNT = Application.getInstance().getString(
            R.string.statistics_service_account);
    private static final String EVENT_LABLE_MSG_SEND = Application.getInstance().getString(
            R.string.statistics_msg_read);
    private static final String EVENT_LABLE_DELETE_THREADS = Application.getInstance().getString(
            R.string.statistics_delete_threads);
    private static final String EVENT_LABLE_UUID = Application.getInstance().getString(
            R.string.statistics_uuid);
    private static final String EVENT_LABLE_ACCOUNT_STATUS = Application.getInstance().getString(
            R.string.statistics_account_status);
    private static final String EVENT_LABLE_REQUEST_MSG = Application.getInstance()
            .getString(R.string.statistics_request_msg);
    private static final String EVENT_LABLE_APK_LIST_INFO = Application.getInstance()
            .getString(R.string.statistics_apk_list_info);
    private static final String EVENT_LABLE_RECEIVE_MSG_FAIL = Application.getInstance()
            .getString(R.string.statistics_receive_msg_fail);

    private static final String EVENT_LABLE_MSG_SEND_SUCCESS = Application.getInstance().getString(
            R.string.statistics_msg_send_success);
    
    private static final String EVENT_LABLE_MSG_SEND_FAIL = Application.getInstance().getString(
            R.string.statistics_msg_send_fail);
    
    public static final class OpenType {
        public static final String SMS_LIST = "1";
        public static final String NOTIFICATION = "2";
        public static final String SEARCH_LIST = "3";
        public static final String SYSTEM_SETTING = "4";
    }

    public static final class ShareChannel {
        public static final String WEIXIN = "1";
        public static final String FACEBOOK = "2";
        public static final String TWITTER = "3";
        public static final String WEIBO = "4";
    }

    public static final class ThreadDeleteType {
        public static final String CV = "conversationList";
        public static final String CLEAR_MSG = "clearMsg";
    }

    private static final String KEY_MSG_ID = "msgid";
    private static final String KEY_MSG_TYPE = "msgtype";
    private static final String KEY_UUID = "UUID";
    private static final String KEY_CONTACT_ID = "contactId";
    private static final String KEY_ENTRANCE_TYPE = "entranceType";
    private static final String KEY_MSG_SUB_ID = "msgSubId";
    private static final String KEY_MSG_SEND_TYPE = "sendMsgType";
    private static final String KEY_MSG_SHARE_CHANNEL = "shareChannel";
    private static final String KEY_THREAD_DELETE_TYPE = "threadDeleteType";
    private static final String KEY_GET_MSG_FAIL_REASON = "failReason";
    private static final String KEY_GET_MSG_RETRY_TIME = "retryTime";
    
    public static Statistics mInstance = null;

    public static Statistics getInstance() {
        if (mInstance == null) {
            mInstance = new Statistics();
        }
        return mInstance;
    }

    public void receiveMsgSuccess(String msgId, String msgType) {
        Map<String, String> kv = new HashMap<String, String>();
        kv.put(KEY_MSG_ID, msgId);
        kv.put(KEY_MSG_TYPE, msgType);
        HstatSdk.onEvent(Application.getInstance(), EVENT_ID_RECEIVE_MSG_SUCCESS,
                EVENT_LABLE_RECEIVE_MSG_SUCCESS, kv);
    }
    
    public void receiveMsgFail(List<String> msgIds,int failReason, int retryTime) {
        for (String msgid : msgIds) {
            Map<String, String> kv = new HashMap<String, String>();
            kv.put(KEY_MSG_ID, msgid);
            kv.put(KEY_GET_MSG_FAIL_REASON, String.valueOf(failReason));
            kv.put(KEY_GET_MSG_RETRY_TIME, String.valueOf(retryTime));
            HstatSdk.onEvent(Application.getInstance(), EVENT_ID_RECEIVE_MSG_FAIL,
                    EVENT_LABLE_RECEIVE_MSG_FAIL, kv);
        }
    }

    public void receivePnNotification(List<String> msgIds) {
        for (String msgid : msgIds) {
            Map<String, String> kv = new HashMap<String, String>();
            kv.put(KEY_MSG_ID, msgid);
            HstatSdk.onEvent(Application.getInstance(), EVENT_ID_RECEIVE_PN_NOTIFICATION,
                    EVENT_LABLE_RECEIVE_PN_NOTIFICATION, kv);
        }
    }

    public void requestMsgs(List<String> msgIds) {
        for (String msgid : msgIds) {
            Map<String, String> kv = new HashMap<String, String>();
            kv.put(KEY_MSG_ID, msgid);
            HstatSdk.onEvent(Application.getInstance(), EVENT_ID_REQUEST_MSG,
                    EVENT_LABLE_REQUEST_MSG, kv);
        }
    }
    
    public void appClick(String conactID, String entranceType) {
        Map<String, String> kv = new HashMap<String, String>();
        kv.put(KEY_CONTACT_ID, conactID);
        kv.put(KEY_ENTRANCE_TYPE, entranceType);
        HstatSdk.onEvent(Application.getInstance(), EVENT_ID_CLICK_APP, EVENT_LABLE_CLICK_APP, kv);

    }

    public void msgRead(String msgId, String msgSubId) {
        Map<String, String> kv = new HashMap<String, String>();
        kv.put(KEY_MSG_ID, msgId);
        if (!msgSubId.isEmpty()) {
            kv.put(KEY_MSG_SUB_ID, msgSubId);
        }
        HstatSdk.onEvent(Application.getInstance(), EVENT_ID_MSG_READ, EVENT_LABLE_MSG_READ, kv);
    }

    public void msgShare(String msgId, int msgSubId, String shareChannel) {
        if (msgId.isEmpty()) {
            return;
        }
        Map<String, String> kv = new HashMap<String, String>();
        kv.put(KEY_MSG_ID, msgId);
        if (msgSubId >= 0) {
            kv.put(KEY_MSG_SUB_ID, String.valueOf(msgSubId));
        }
        kv.put(KEY_MSG_SHARE_CHANNEL, shareChannel);
        HstatSdk.onEvent(Application.getInstance(), EVENT_ID_MSG_SHARE, EVENT_LABLE_MSG_SHARE, kv);
    }

    public void accountIsExistInPhoneBook(List<ServiceInfo> accountList) {
        Map<String, String> kv = new HashMap<String, String>();
        for (ServiceInfo account : accountList) {
            kv.put(account.getAccount(), String.valueOf(account.getIsExist()));
        }
        HstatSdk.onEvent(Application.getInstance(), EVENT_ID_SERVICE_ACCOUNT,
                EVENT_LABLE_SERVICE_ACCOUNT, kv);
    }

    public void msgSend(String msgId, String msgTye) {
        Map<String, String> kv = new HashMap<String, String>();
        kv.put(KEY_MSG_ID, msgId);
        kv.put(KEY_MSG_SEND_TYPE, msgTye);
        HstatSdk.onEvent(Application.getInstance(), EVENT_ID_MSG_SEND, EVENT_LABLE_MSG_SEND, kv);
    }
    
    public void msgSendSuccess(String msgId, String msgTye) {
        Map<String, String> kv = new HashMap<String, String>();
        kv.put(KEY_MSG_ID, msgId);
        kv.put(KEY_MSG_SEND_TYPE, msgTye);
        HstatSdk.onEvent(Application.getInstance(), EVENT_ID_MSG_SEND_SUCCESS, EVENT_LABLE_MSG_SEND_SUCCESS, kv);
    }
    
    public void msgSendFail(String msgId, String msgTye) {
        Map<String, String> kv = new HashMap<String, String>();
        kv.put(KEY_MSG_ID, msgId);
        kv.put(KEY_MSG_SEND_TYPE, msgTye);
        HstatSdk.onEvent(Application.getInstance(), EVENT_ID_MSG_SEND_FAIL, EVENT_LABLE_MSG_SEND_FAIL, kv);
    }

    public void threadsDelete(String accountName, String type) {
        Map<String, String> kv = new HashMap<String, String>();
        kv.put(KEY_CONTACT_ID, accountName);
        kv.put(KEY_THREAD_DELETE_TYPE, type);
        HstatSdk.onEvent(Application.getInstance(), EVENT_ID_DELETE_THREADS, EVENT_LABLE_DELETE_THREADS,
                kv);
    }

    public void uuid(String uuid) {
        Map<String, String> kv = new HashMap<String, String>();
        kv.put(KEY_UUID, uuid);
        HstatSdk.onEvent(Application.getInstance(), EVENT_ID_UUID, EVENT_LABLE_UUID, kv);
    }

    public void accountStatus(String account, String status) {
        Map<String, String> kv = new HashMap<String, String>();
        kv.put(account, status);
        HstatSdk.onEvent(Application.getInstance(), EVENT_ID_ACCOUNT_STATUS, EVENT_LABLE_ACCOUNT_STATUS,
                kv);
    }
    
    public void apkListInfo() {
        ArrayList<Map<String, String>> appList = new ArrayList<Map<String, String>>();
        List<PackageInfo> packages = Application.getInstance().getPackageManager()
                .getInstalledPackages(0);

        for (int i = 0; i < packages.size(); i++) {
            PackageInfo packageInfo = packages.get(i);
            Map<String, String> kv = new HashMap<String, String>();
            kv.put("appName",
                    packageInfo.applicationInfo.loadLabel(
                            Application.getInstance().getPackageManager()).toString());
            kv.put("versionName", packageInfo.versionName);
            kv.put("lastUpdateTime", packageInfo.lastUpdateTime + "");
            kv.put("firstInstallTime", packageInfo.firstInstallTime + "");
            if ((packageInfo.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) == 0) {
                kv.put("isSystem", "false");
            }else{
                kv.put("isSystem", "true");
            }
            appList.add(kv);
        }

        HstatSdk.onEvent(Application.getInstance(), EVENT_ID_APK_LIST_INFO, EVENT_LABLE_APK_LIST_INFO,
                appList);
    }
}