package com.mediatek.rcs.common.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Set;
import javax.xml.parsers.ParserConfigurationException;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ImageSpan;
import android.util.Log;
import android.view.View;
import android.view.View.OnKeyListener;
import android.view.WindowManager.BadTokenException;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import cn.com.em.sdk.DownLoadEmListener;
import cn.com.em.sdk.EmShopSDK;
import cn.com.em.sdk.mode.Em;
import cn.com.em.sdk.mode.EmData;
import cn.com.em.sdk.mode.EmPackage;
import cn.com.em.sdk.mode.EmPackagef;
import cn.com.em.sdk.mode.EmSimple;
import cn.com.em.sdk.utils.*;

import com.cmcc.sso.sdk.auth.AuthnConstants;
import com.cmcc.sso.sdk.auth.AuthnHelper;
import com.cmcc.sso.sdk.auth.TokenListener;
import com.cmcc.sso.sdk.util.SsoSdkConstants;
import com.mediatek.rcs.common.binder.RCSServiceManager;
import com.mediatek.rcs.common.binder.RCSServiceManager.OnServiceChangedListener;
import com.mediatek.widget.ImageViewEx;

import org.json.JSONException;
import org.json.JSONObject;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;
import org.xmlpull.v1.XmlPullParserException;

public class EmojiShop {

    private static final String TAG = "EmojiShop";

    private static boolean mUserDataAvailable = false;
    private static EmojiShop mInstance;
    private static EmShopSDK mEmShopSDK = null;
    private static Context mHostContext;
    private static Context mPluginContext;
    private static Context mMmsAppContext;

    private static boolean mServiceEnabled = false;
    private static boolean mServiceRegistered = false;

    private static boolean mNeedUpdate = false;
    // All Emoticons Data
    private static List<EmData> mUserEmData = new ArrayList<EmData>();

    private static List<EmojiPackage> mEmPkgInfo = new ArrayList<EmojiPackage>();

    // EmData in user folder, <emId, emPath>
    private static final HashMap<String, String> mUserEmPathMap = new HashMap<String, String>();

    // EmData in user folder, <emId, packageId>
    private static final HashMap<String, String> mUserEmPkgMap = new HashMap<String, String>();

    // EmData in download folder, <emId, emPath>
    private static final HashMap<String, String> mDownloadEmMap = new HashMap<String, String>();

    // EmData in cache folder, <emId, emPath>
    private static final HashMap<String, String> mCachedEmMap = new HashMap<String, String>();

    // ArrayList store all Em package's ID
    private static ArrayList<String> mEmPackageIdList = new ArrayList<String>();

    // Listener called when getEmListData return
  //  private static OnDataReceivedListener mDataReceivedListener;

    // Listeners called when load user data complete
    private static final HashSet<OnEmDataChangedListener> mEmDataChangedlisteners =
            new HashSet<OnEmDataChangedListener>();

    // Listeners called when load em expression complete
    private static final HashSet<OnLoadExpressionListener> mLoadExpressionListeners =
            new HashSet<OnLoadExpressionListener>();

    // store the em id which was loading expression
    private static final Set<String> loadExpressionCache = new CopyOnWriteArraySet<String>();

    // em xml string matcher pattern
    public static final String mEmXmlPattern =
                    "<?xml(.*?)><vemotic?on(.*?)><sms>(.*?)</sms><eid>(.*?)</eid></vemotic?on>";

    // show 8 emoticon shop icons in each page
    private static int EM_PER_PAGE = 8;

    // all em shop icons
  //private static HashMap<String, HashMap<Integer, List<HashMap<String, Object>>>> mEmShopEmIcons;

    private static final String downloadFolder = "Download";
    private static final String CacheFolder = "Cache";
    private static final String mEmDataDir =
                    "Android/data/cn.com.expression.shop/files/EmShop/";
    private static final String mEmDownloadDir = Environment.getExternalStorageDirectory() +
                    "/" + mEmDataDir + downloadFolder + "/";
   // private static final String mEmCacheDir = mEmDataDir + CacheFolder + "/";
    private static String mEmUserDir;
    private static String mUserId;


   /**
    * Listen em data changed.
    */
    public interface OnEmDataChangedListener {
        public void onEmDataChanged();
    }

    /**
     * Add em data changed listener.
     */
    public static boolean addOnEmDataChangedListener(OnEmDataChangedListener listener) {
        return mEmDataChangedlisteners.add(listener);
    }

   /**
    * Remove em data changed listener.
    */
    public static boolean removeOnEmDataChangedListener(OnEmDataChangedListener listener) {
        return mEmDataChangedlisteners.remove(listener);
    }

    private static void notifyEmDataChanged() {
        Logger.d(TAG, "notifyEmDataChanged");
        for (OnEmDataChangedListener l : mEmDataChangedlisteners) {
            l.onEmDataChanged();
        }
    }

    /**
     * Listen load expression complete
     */
    public interface OnLoadExpressionListener {
        public void onLoadExpressionComplete(int result, String emId);
    }

    /**
     * Add load expression compoete listener.
     */
    public static boolean addOnLoadExpressionListener(OnLoadExpressionListener listener) {
        return mLoadExpressionListeners.add(listener);
    }

    /**
      * Remove load expression compoete listener.
      */
    public static boolean removeOnLoadExpressionListener(OnLoadExpressionListener listener) {
   /*     Iterator iterator = mLoadExpressionListeners.iterator();
      List<String> pList = new ArrayList<String>();

      // check values
        while (iterator.hasNext()){
            OnLoadExpressionListener l = (OnLoadExpressionListener)iterator.next();
            if (l == listener) {
                iterator.remove();
                return true;
            }
        }
        return false;*/
        return mLoadExpressionListeners.remove(listener);
    }

    private static void notifyLoadExpressionComplete(int result, String emId) {
        Logger.d(TAG, "notifyLoadExpressionComplete, result = " + result + ", emId = " + emId);
        // only load success to notify UI
        if (result != 200) {
            return;
        }
        for (OnLoadExpressionListener l : mLoadExpressionListeners) {
            l.onLoadExpressionComplete(result, emId);
        }
    }

    /**
     * Listen RCS Server state change
     */
    private static OnServiceChangedListener mOnServiceChangedListener =
                new OnServiceChangedListener() {
        @Override
        public void onServiceStateChanged(int state, final boolean activated,
                    final boolean configured, final boolean registered) {
            Log.d(TAG, "[onServiceStateChanged]: (state, activated, configured, registered): " +
                state + " " +activated + " " + configured + " "+ registered);
            boolean enable = activated && configured;
            if (mServiceEnabled != enable) {
                mServiceEnabled = enable;
            }
            if (mServiceRegistered != registered) {
                mServiceRegistered = registered;
                if (registered) {
                    String myNumber = RCSServiceManager.getInstance().getMyNumber();
                    if (myNumber != null && !myNumber.equals(mUserId)) {
                        mUserId = myNumber;
                        if (initSdk(mMmsAppContext, mUserId)) {
                            loadUserEmData();
                            // set data changed listener
                            setEmPackageChangedListener();
                            mHandler = new EngineHandler();
                        }
                    }
                }
            }
        }
    };

    /**
     * If SD card was unmounted and remounted, reload em data
     */
    private static BroadcastReceiver mMediaChangedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "mMediaChangedReceiver(), action=" + intent.getAction());
            String action = intent.getAction();
            if (action != null && action.equals(Intent.ACTION_MEDIA_MOUNTED) && mNeedUpdate) {
                if (mUserId != null) {
                    loadUserEmData();
                }
                loadDownLoadEmData();
                mNeedUpdate = false;
            } else if (action != null && action.equals(Intent.ACTION_MEDIA_UNMOUNTED)) {
                mNeedUpdate = true;
            }
        }
    };

    private EmojiShop(Context context) {
        Log.d(TAG, "EmojiShop() context = " + context);
        mMmsAppContext = context;

        RCSServiceManager.getInstance().addOnServiceChangedListener(mOnServiceChangedListener);
        IntentFilter filter = new IntentFilter(Intent.ACTION_MEDIA_MOUNTED);
        filter.addAction(Intent.ACTION_MEDIA_UNMOUNTED);
        filter.addAction(Intent.ACTION_MEDIA_EJECT);
        filter.addDataScheme("file");
        mMmsAppContext.registerReceiver(mMediaChangedReceiver, filter);

        loadDownLoadEmData();
    }

    /**
     * Init when message init
     */
    public static void init(Context context) {
        Log.d(TAG, "init() context = " + context);
        if (mInstance == null) {
            mInstance = new EmojiShop(context);
        }
    }

    /**
     * Init emoticon sdk
     */
    private static boolean initSdk(Context context, String userId) {
        Log.d(TAG, "initSdk(), context=" + context + ", userId=" + userId);
        if ((context == null) || (userId == null)) {
            Log.d(TAG, "initSdk() error, context or userId is null");
            return false;
        }
        if (!EmShopSDK.getInstance().init(context, userId)) {
            Log.d(TAG, "initSdk(), EmShopSDK init failed");
            return false;
        }
        if (!EmShopSDK.getInstance().isAvailable()) {
            Log.d(TAG, "initSdk(), EmShopAPK Not Available.");
            return false;
        }
        return true;
    }

   /**
    * Returns instance
    *
    * @return Instance
    */
    public synchronized static EmojiShop getInstance() {
        if (mInstance == null) {
            throw new RuntimeException("EmojiShop is not initiated");
        }
        return mInstance;
    }

    public static boolean isApkAvailable() {
        PackageManager pm = mMmsAppContext.getPackageManager();
        List pInfo = pm.getInstalledPackages(0);
        for (int i = 0; i < pInfo.size(); i++) {
            if (((PackageInfo)pInfo.get(i)).packageName.
                    equalsIgnoreCase("cn.com.expression.shop")) {
                return true;
            }
        }
        return false;
    }

    private static void loadDownLoadEmData() {
        loadFolderEmData(mEmDownloadDir, mDownloadEmMap);
    }

    private static void loadFolderEmData(String folderPath, Map<String, String> dataMap) {
        Log.d(TAG, "loadEmData, folderPath=" + folderPath);
        File path = new File(folderPath);
        if (!path.exists()) {
            Log.d(TAG, "path not exists, create it");
            path.mkdirs();
            return;
        }
        //Clear all cache data and re init
        dataMap.clear();
        File[] files = path.listFiles();
        for (int i = 0; i < files.length; i++) {
            if (files[i].isDirectory()) {
                continue;
            }
            String emName = files[i].getName();
            String emPath = files[i].getPath();
            Log.d(TAG, "name=" + emName);
            String emId = emName.split("\\.")[0];
            if (!dataMap.containsKey(emId)) {
                dataMap.put(emId, emPath);
            }
        }
    }

 /*   public static boolean addToCache(String emID) {
        if (mCachedEmMap.containsKey(emID)) {
            return false;
        }
        if (mUserEmPathMap.containsKey(emID)) {
            String emPath = mUserEmPathMap.get(emID);
            File file = new File(emPath);
            String name = file.getName();
            String newPath = mEmCacheDir + name;
            if (copyFile(emPath, newPath)) {
                mCachedEmMap.put(emID, newPath);
                return true;
            }
        }
        return false;
    }

    private static boolean copyFile(String oldPath, String newPath) {
        try {
            int byteSum = 0;
            int byteRead = 0;
            File file = new File(oldPath);
            if (file.exists()) {
                InputStream inStream = new FileInputStream(oldPath);
                FileOutputStream fs = new FileOutputStream(newPath);
                byte[] buffer = new byte[1024];
                int length;
                while ((byteRead = inStream.read(buffer)) != -1) {
                    byteSum += byteRead;
                    fs.write(buffer, 0, byteRead);
                }
                inStream.close();
                return true;
            }
            return false;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    */
     final static OnDataReceivedListener mDataReceivedListener = new OnDataReceivedListener() {
         @Override
         public void onDataReceived(List<EmData> allEmData) {
             Log.d(TAG, "onDataReceived, allEmData = " + allEmData);
             mEmPackageIdList.clear();
             mUserEmPathMap.clear();
             mUserEmPkgMap.clear();
             mUserEmData.clear();
             mUserEmData = allEmData;
             mEmPkgInfo.clear();

             int pkgSize = mUserEmData.size();
             for (int i = 0; i < pkgSize; i++) {
                 EmData emData = mUserEmData.get(i);

                 EmojiPackage pkgInfo = new EmojiPackage(emData);
                 mEmPkgInfo.add(pkgInfo);

                 EmPackagef epf = emData.getEmpackagef();
                 String packageId = epf.getmId();
                 mEmPackageIdList.add(packageId);
                 List<Em> emlist = emData.getEmlist();
                 for (int j = 0; j < emlist.size(); j++) {
                     if (!mUserEmPathMap.containsKey(emlist.get(j).getmId())) {
                         mUserEmPathMap.put(emlist.get(j).getmId(), emlist.get(j).getmImage());
                         mUserEmPkgMap.put(emlist.get(j).getmId(), packageId);
                     }
                 }
             }
             mUserDataAvailable = true;
             // notify UI
             notifyEmDataChanged();
         }
     };

    private static void loadUserEmData() {
        Log.d(TAG, "loadUserEmData, enter");

        try {
            EmShopSDK.getInstance().getEmListData(mDataReceivedListener);
        } catch (UninitializedException ex) {
            Log.d(TAG, "catch UninitializedException");
        } catch (AppNotInstalledException e) {
            Log.d(TAG, "catch AppNotInstalledException");
        } finally {
            // do nothing
        }
    }

    private static void setEmPackageChangedListener() {
        Log.d(TAG, "setEmPackageChangedListener");
        EmShopSDK.getInstance().setOnEmPackageChangedListener(new OnEmPackageChangedListener() {
            @Override
            public void onAdd(String idname) {
                // new em data download success
                Log.d(TAG, "onAdd packageId: " + idname);
                loadUserEmData();
            }

            @Override
            public void onDelete(String idname) {
                //em data was delete success
                Log.d(TAG, "onDelete packageId: " + idname);
                loadUserEmData();
            }
        });
    }

    final static DownLoadEmListener mDownloadEmListener = new DownLoadEmListener() {
        @Override
        public void loadComplete(String filePath) {
            Log.d(TAG, "loadComplete() filePath=" + filePath);

            File file = new File(filePath);
            String emName = file.getName();
            String emId = emName.split("\\.")[0];
            Log.d(TAG, "loadComplete() emId=" + emId);
            if (!mDownloadEmMap.containsKey(emId)) {
                mDownloadEmMap.put(emId, filePath);
            }
            if (loadExpressionCache.contains(emId)) {
                loadExpressionCache.remove(emId);
            }
            notifyLoadExpressionComplete(200, emId);
        }

        @Override
        public void loadFailure(int errorCode) {
            Log.d(TAG, "loadFailure() errorCode=" + errorCode);
            // 426: data not exit
            // 406: wrong token
            // 101: network error
            // 102: storage error, etc. SD full
            notifyLoadExpressionComplete(errorCode, null);
        }
    };

    private static String mToken = "token";
    private static final int MSG_ERROR = -1;
    private static final int MSG_SUCCESS = 0;
    private static EngineHandler mHandler;

    private static class EngineHandler extends Handler {
        @Override
        public void handleMessage(final Message msg) {
            Log.d(TAG, "handleMessage " + msg);
            switch (msg.what) {
            case MSG_ERROR:
                int errorCode = (int)msg.obj;
                Log.d(TAG, "MSG_ERROR, errorCode=" + errorCode);
                notifyLoadExpressionComplete(errorCode, null);
                break;

            case MSG_SUCCESS:
                final String emId = (String)msg.obj;
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Log.d(TAG, "emId = " + emId);
                            Log.d(TAG, "mToken = " + mToken);
                            // resolutionIndex: 1: 1024x600; 2:800x480; 3:480x320; 4: 640x360
                            EmShopSDK.getInstance().loadExpression(
                                    mEmDownloadDir, mToken, emId, "1", mDownloadEmListener);
                        } catch (FilePathNotFoundException ex) {
                            Log.d(TAG, "catch FilePathNotFoundException, e=" + ex);
                        } catch (UninitializedException e) {
                            Log.d(TAG, "catch UninitializedException, e=" + e);
                        } catch (Exception e) {
                            Log.d(TAG, "catch Exception, e=" + e);
                        } finally {
                            // do nothing
                        }
                    }
                }).start();
                break;

            default:
                super.handleMessage(msg);
                break;
            }
        }
    }

    private static boolean getToken(Activity activity, final String emID) {
        Log.d(TAG, "getToken() emID = "+ emID);
        final String appId = "01000145";
        final String appKey = "DAA231C8F8E53CC4";
        AuthnHelper authHelper = new AuthnHelper(activity);
        authHelper.setDefaultUI(true);
        try {
            authHelper.getAccessToken(appId, appKey, null,
                        SsoSdkConstants.LOGIN_TYPE_DEFAULT,
                        new TokenListener() {
                @Override
                public void onGetTokenComplete(JSONObject json) {
                    Log.d(TAG, "onGetTokenComplete");
                    if (json == null) {
                        Log.d(TAG, "json is null!");
                        if (mHandler != null) {
                            mHandler.obtainMessage(MSG_ERROR, -1).sendToTarget();
                        }
                        return;
                    }
                    Log.d(TAG, json.toString());
                    try {
                        Integer result = (Integer)json.get("resultCode");
                        if (result == AuthnConstants.CLIENT_CODE_SUCCESS) {
                            mToken = (String)json.get("token");
                            Log.d(TAG, "  resultCode:" + result
                                    + "\n  token:" + mToken
                                    + "\n  passid:" + (String) json.get("passid"));
                            if (mHandler != null) {
                                mHandler.obtainMessage(MSG_SUCCESS, emID).sendToTarget();
                            }
                            return;
                        }
                        if (mHandler != null) {
                            mHandler.obtainMessage(MSG_ERROR, result).sendToTarget();
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            });
        } catch(BadTokenException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    // resolutionIndex: 1: 1024x600; 2:800x480; 3:480x320; 4: 640x360
    private static void loadExpression(Activity activity, final String emId,
                final String resolutionIndex) {
        Log.d(TAG, "loadExpression() " + " emId=" + emId);
        getToken(activity, emId);
    }

    public void startMain() {
        Log.d(TAG, "startMain()");
        try {
            EmShopSDK emShopSDK = EmShopSDK.getInstance();
            emShopSDK.init(mMmsAppContext, mUserId);
            emShopSDK.startMain();
        } catch (UninitializedException ex) {
            Log.d(TAG, "catch UninitializedException");
        } catch (AppNotInstalledException e) {
            Log.d(TAG, "catch AppNotInstalledException");
        } finally {
            // do nothing
        }
    }

    public void startManager() {
        Log.d(TAG, "startManager()");
        try {
            EmShopSDK emShopSDK = EmShopSDK.getInstance();
            emShopSDK.init(mMmsAppContext, mUserId);
            emShopSDK.startManager();
        } catch (UninitializedException ex) {
            Log.d(TAG, "catch UninitializedException");
        } catch (AppNotInstalledException e) {
            Log.d(TAG, "catch AppNotInstalledException");
        } finally {
            // do nothing
        }
    }

    public void showDetail(String emId) {
        Log.d(TAG, "showDetail() emId=" + emId);
        try {
            EmShopSDK emShopSDK = EmShopSDK.getInstance();
            emShopSDK.init(mMmsAppContext, mUserId);
            emShopSDK.showDetail(emId);
        } catch (UninitializedException ex) {
            Log.d(TAG, "catch UninitializedException");
        } catch (AppNotInstalledException e) {
            Log.d(TAG, "catch AppNotInstalledException");
        } finally {
            // do nothing
        }
    }

    private static String createEmXml(EmSimple emSimple) {
        return EmShopSDK.getInstance().createEmXml(emSimple);
    }

    private static EmSimple parseEmXml(String emXml) {
        return EmShopSDK.getInstance().parseEmXml(emXml);
    }

   /**
     * Get the package Id which the package index is @index
     *
     * @param index: index of the package
     * @return String: package id
     */
    public static String getEmPackageId(int index) {
        if(index >= mEmPackageIdList.size() || index <= -1) {
            return null;
        }
        return mEmPackageIdList.get(index);
    }

    private static int getEmPackageIndex(String emPkgId) {
        for (int i = 0; i < mEmPackageIdList.size(); i++) {
            if (mEmPackageIdList.get(i).equals(emPkgId)) {
                return i;
            }
        }
        return -1;
    }


   /**
     * persist the em xml string  by package id and em id
     *
     * @param emPkgId: id of the package
     * @param emId: id of the em
     * @return String: em string
     */
    public static String createEmXml(String emPkgId, String emId) {
        int index = getEmPackageIndex(emPkgId);
        if (index != -1) {
            EmData emData = mUserEmData.get(index);
            List<Em> emList = emData.getEmlist();
            for (int i = 0; i < emList.size(); i++) {
                if (emList.get(i).getmId().equals(emId)) {
                    String content = emList.get(i).getmTip();
                    EmSimple emSimple = new EmSimple(content, emId);
                    return createEmXml(emSimple);
                }
            }
        }
        return null;
    }

    private Em getEmByXml(String emXml) {
        EmSimple emSimple = parseEmXml(emXml);
        final String emId = emSimple.getId();
        String pkgId = mUserEmPkgMap.get(emId);
        if (pkgId != null) {
            int index = getEmPackageIndex(pkgId);
            if (index != -1) {
                EmData emData = mUserEmData.get(index);
                List<Em> emList = emData.getEmlist();
                for (int i = 0; i < emList.size(); i++) {
                    Em em = emList.get(i);
                    if(em.getmId().equals(emId)) {
                        return em;
                    }
                }
            }
        }
        return null;
    }

    private static String getEmPathByXml(String emXml) {
        EmSimple emSimple = parseEmXml(emXml);
        String emId = emSimple.getId();
        if (mDownloadEmMap.containsKey(emId)) {
            return mDownloadEmMap.get(emId);
        }
        if(mUserEmPathMap.containsKey(emId)) {
            return mUserEmPathMap.get(emId);
        }
        Log.d(TAG, "getEmPathByXml() no found");
        return null;
    }

    public static String getEmIdByXml(String emXml) {
        EmSimple emSimple = parseEmXml(emXml);
        return emSimple.getId();
    }

    public static boolean isLocalEmoticon(String emId) {
        return mUserEmPathMap.containsKey(emId) ||
               mDownloadEmMap.containsKey(emId);
    }

    // not found the emoticon on local, download from server
    public static void loadEmIconsFromServer(Activity activity, final String emId) {
        Log.d(TAG, "loadEmIconsFromServer() activity =" + activity + ", emId=" + emId);
        // if downloading now, not download again
        if (!loadExpressionCache.contains(emId)) {
            loadExpressionCache.add(emId);
            getToken(activity, emId);
        }
    }

    public static String parseEmSmsString(String emXml) {
        if (emXml == null) {
            return null;
        }
        try {
            EmSimple emMsg = null;
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            XmlPullParser parser = factory.newPullParser();
            parser.setInput(new StringReader(emXml));
            int event = parser.getEventType();

            while(event != -1) {
                switch (event) {
                case 0:
                    emMsg = new EmSimple();
                    break;
                case 2:
                    if (emMsg != null) {
                        if ("sms".equalsIgnoreCase(parser.getName())) {
                            return parser.nextText();
                        }
                    }
                }
                event = parser.next();
            }
        } catch (XmlPullParserException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static boolean matchEmXml(String emXml) {
        Pattern pattern = Pattern.compile(mEmXmlPattern);
        Matcher matcher = pattern.matcher(emXml);
        while (matcher.find()) {
            return true;
        }
        return false;
    }

    public static Drawable loadEmImage(String emXml) {
        String path  = getEmPathByXml(emXml);
        if (path != null) {
            return Drawable.createFromPath(path);
        } else {
            return null;
        }
    }

    public static String getEmResPath(String emXml) {
        return getEmPathByXml(emXml);
    }

    public static Uri getEmResUri(String emXml) {
        String path = getEmPathByXml(emXml);
        if (path != null) {
            return Uri.fromFile(new File(path));
        }
        return null;
    }

    public static List<EmojiPackage> getAllPackageInfo() {
        if (!mUserDataAvailable && initSdk(mMmsAppContext, mUserId)) {
            loadUserEmData();
            // set data changed listener
            setEmPackageChangedListener();
            mHandler = new EngineHandler();
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return mEmPkgInfo;
    }

    // not used now
    public static EmojiPackage getPackageInfoById(String pkgId) {
        for (int i = 0; i < mEmPkgInfo.size(); i++) {
            if (mEmPkgInfo.get(i).getPkgId().equals(pkgId)) {
                return mEmPkgInfo.get(i);
            }
        }
        return null;
    }

    public static class EmojiPackage {
        private String mPkgId;
        private String mPkgIcon;
        // <emId, emPath>
        private HashMap<String, String> mEmIcons = new HashMap<String, String>();

        EmojiPackage(EmData emData) {
            EmPackagef epf = emData.getEmpackagef();
            mPkgId = epf.getmId();
            mPkgIcon = epf.getmIconColor();
            List<Em> emlist = emData.getEmlist();
            for (int i = 0; i < emlist.size(); i++) {
                mEmIcons.put(emlist.get(i).getmId(), emlist.get(i).getmImage());
            }
        }

        public String getPkgId() {
            return mPkgId;
        }

        public String getPkgIcon() {
            return mPkgIcon;
        }

        public HashMap<String, String> getEmIcons() {
            return mEmIcons;
        }
    }
}
