package com.mediatek.contacts.plugin;

import java.util.List;
import java.util.Collections;
import java.util.Comparator;

import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.app.AlertDialog;
import android.app.Activity;
import android.app.Fragment;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.PorterDuff;
import android.provider.CallLog.Calls;
import android.provider.ContactsContract.Intents.Insert;
import android.provider.ContactsContract.ProviderStatus;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.text.method.DialerKeyListener;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.Menu;
import android.widget.EditText;

import com.mediatek.common.PluginImpl;
import com.mediatek.contacts.ext.DefaultOp01Extension;
import com.mediatek.contacts.list.ContactGroupListActivity;
import com.mediatek.contacts.list.ContactListMultiChoiceActivity;
import com.mediatek.contacts.list.PhoneAndEmailsPickerFragment;
import com.mediatek.internal.telephony.ITelephonyEx;
import com.mediatek.op01.plugin.R;

@PluginImpl(interfaceName="com.mediatek.contacts.ext.IOp01Extension")
public class OP01Extension extends DefaultOp01Extension {
    private static final String TAG = "OP01Extension";
    private Context mContext;
    private static Context mContextHost;
    private static final int MENU_ID_BASE = 9999;
    private static final int MENU_SIM_STORAGE = MENU_ID_BASE + 1;
    private static final int MENU_ID_BLACKLIST = MENU_ID_BASE + 2;
    private static final int MENU_ID_GROUP = MENU_ID_BASE + 3;
    private static final String BLACKLIST_ACTIVITY_INTENT =
                                    "com.mediatek.rcs.blacklist.BlacklistManagerActivity";
    private static final int CALL_ARROW_ICON_RES = R.drawable.ic_call_arrow;
    private static final int CALL_AUTOREJECT_ICON_RES = R.drawable.ic_call_autoreject_arrow;

    public OP01Extension(Context context) {
        mContext = context;
    }

    @Override
    public void addOptionsMenu(Context context, Menu menu) {
        Log.i(TAG, "addOptionsMenu");
        mContextHost = context;
        MenuItem item = menu.findItem(MENU_SIM_STORAGE);
        List<SubscriptionInfo> simInfos = SubscriptionManager.from(mContext)
                .getActiveSubscriptionInfoList();
        if (item == null && simInfos != null && simInfos.size() > 0) {
            String string = mContext.getResources().getString(R.string.look_simstorage);
            menu.add(0, MENU_SIM_STORAGE, 0, string).setOnMenuItemClickListener(
                new MenuItem.OnMenuItemClickListener() {
                    public boolean onMenuItemClick(MenuItem item) {
                        ShowSimCardStorageInfoTask.showSimCardStorageInfo(mContext);
                        return true;
                    }
            });
        }

        MenuItem itemBlacklist = menu.findItem(MENU_ID_BLACKLIST);
        if (itemBlacklist == null) {
            String string = mContext.getResources().getString(R.string.menu_blacklist);
            menu.add(0, MENU_ID_BLACKLIST, 0, string).setOnMenuItemClickListener(
                new MenuItem.OnMenuItemClickListener() {

                    public boolean onMenuItemClick(MenuItem item) {
                        Intent intent = new Intent(BLACKLIST_ACTIVITY_INTENT);
                        intent.setClassName("com.mediatek.rcs.blacklist",
                                    "com.mediatek.rcs.blacklist.BlacklistManagerActivity");
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        try {
                            mContext.startActivity(intent);
                        } catch (ActivityNotFoundException e) {
                            Log.i(TAG, "not found application");
                            e.printStackTrace();
                        }
                        return true;
                    }
            });
        }
    }

    public static class ShowSimCardStorageInfoTask extends AsyncTask<Void, Void, Void> {
        private static ShowSimCardStorageInfoTask sInstance = null;
        private boolean mIsCancelled = false;
        private boolean mIsException = false;
        private String mDlgContent = null;
        private Context mContext = null;

        public static void showSimCardStorageInfo(Context context) {
            Log.i(TAG, "[ShowSimCardStorageInfoTask]_beg");
            if (sInstance != null) {
                sInstance.cancel();
                sInstance = null;
            }
            sInstance = new ShowSimCardStorageInfoTask(context);
            sInstance.execute();
            Log.i(TAG, "[ShowSimCardStorageInfoTask]_end");
        }

        public ShowSimCardStorageInfoTask(Context context) {
            mContext = context;
            Log.i(TAG, "[ShowSimCardStorageInfoTask] onCreate()");
        }

        @Override
        protected Void doInBackground(Void... args) {
            Log.i(TAG, "[ShowSimCardStorageInfoTask]: doInBackground_beg");
            List<SubscriptionInfo> simInfos = getSortedInsertedSimInfoList();
            if (!mIsCancelled && (simInfos != null) && simInfos.size() > 0) {
                StringBuilder build = new StringBuilder();
                int simId = 0;
                for (SubscriptionInfo simInfo : simInfos) {
                    if (simId > 0) {
                        build.append("\n\n");
                    }
                    simId++;
                    int[] storageInfos = null;
                    build.append(simInfo.getDisplayName());
                    build.append(":\n");
                    try {
                        ITelephonyEx phoneEx = ITelephonyEx.Stub.asInterface(ServiceManager
                              .checkService("phoneEx"));
                        if (!mIsCancelled && phoneEx != null) {
                            storageInfos = phoneEx.getAdnStorageInfo(simInfo.getSubscriptionId());
                            if (storageInfos == null) {
                                mIsException = true;
                                Log.i(TAG, " storageInfos is null");
                                return null;
                            }
                            Log.i(TAG, "[ShowSimCardStorageInfoTask] infos: "
                                    + storageInfos.toString());
                        } else {
                            Log.i(TAG, "[ShowSimCardStorageInfoTask]: phone = null");
                            mIsException = true;
                            return null;
                        }
                    } catch (RemoteException ex) {
                        Log.i(TAG, "[ShowSimCardStorageInfoTask]_exception: " + ex);
                        mIsException = true;
                        return null;
                    }
                    build.append(mContext.getResources().getString(R.string.dlg_simstorage_content,
                            storageInfos[1], storageInfos[0]));
                    if (mIsCancelled) {
                        return null;
                    }
                }
                mDlgContent = build.toString();
            }
            Log.i(TAG, "[ShowSimCardStorageInfoTask]: doInBackground_end");
            return null;
        }

        public void cancel() {
            super.cancel(true);
            mIsCancelled = true;
            Log.i(TAG, "[ShowSimCardStorageInfoTask]: mIsCancelled = true");
        }

        @Override
        protected void onPostExecute(Void v) {
            if (mContextHost instanceof Activity) {
                Log.i(TAG, "[onPostExecute]: activity find");
                Activity activity = (Activity) mContextHost;
                if (activity.isFinishing()) {
                    Log.i(TAG, "[onPostExecute]: activity finish");
                    mIsCancelled = false;
                    mIsException = false;
                    sInstance = null;
                    return;
                }
            }

            Drawable icon = mContext.getResources().getDrawable(R.drawable.ic_menu_look_simstorage_holo_light);
            String string = mContext.getResources().getString(R.string.look_simstorage);
            sInstance = null;
            if (!mIsCancelled && !mIsException) {
                new AlertDialog.Builder(mContextHost).setIcon(icon).setTitle(string).setMessage(mDlgContent).setPositiveButton(
                       android.R.string.ok, null).setCancelable(true).create().show();
            }
            mIsCancelled = false;
            mIsException = false;
        }

         /**
             * Sort sim storage information by slot.
             * @return List<SubscriptionInfo>.
             */
        public List<SubscriptionInfo> getSortedInsertedSimInfoList() {
            List<SubscriptionInfo> ls = SubscriptionManager.from(mContext)
                    .getActiveSubscriptionInfoList();
            if (ls != null) {
                Collections.sort(ls, new Comparator<SubscriptionInfo>() {
                    @Override
                    public int compare(SubscriptionInfo arg0, SubscriptionInfo arg1) {
                        return (arg0.getSimSlotIndex() - arg1.getSimSlotIndex());
                    }
                });
            }
            return ls;
        }
    }

    @Override
    public int getMultiChoiceLimitCount(int defaultCount) {
        Log.i(TAG, "[getMultiChoiceLimitCount]");
        return 5000;
    }

    @Override
    public String formatNumber(String number, Bundle bundle) {
        String result = number;
        if (bundle != null) {
            final CharSequence data = bundle.getCharSequence(Insert.PHONE);
            if (data != null && TextUtils.isGraphic(data)) {
                String phone = data.toString();
                Log.i(TAG, "[formatNumber] orignal: " + phone);
                if (phone != null && !TextUtils.isEmpty(phone)) {
                    phone = phone.replaceAll(" ", "");
                    Log.i(TAG, "[formatNumber]" + phone);
                    bundle.putString(Insert.PHONE, phone);
                }
            }
            return result;
        }
        if (result != null && !TextUtils.isEmpty(result)) {
            result = result.replaceAll(" ", "");
        }
        Log.i(TAG, "[formatNumber]" + result);
        return result;
    }

    @Override
    public boolean areContactAvailable(Integer providerStatus) {
        Log.i(TAG, "[areContactAvailable] providerStatus: " + providerStatus);
        return (providerStatus != null)
                && (providerStatus.equals(ProviderStatus.STATUS_NORMAL) ||
                providerStatus.equals(ProviderStatus.STATUS_EMPTY));
    }

    @Override
    public Drawable getArrowIcon(int type, Drawable callArrowIcon) {
        Log.i(TAG, "[getArrowIcon] type: " + type);
        Drawable callArrow = null;
        Resources res = mContext.getResources();
        switch (type) {
            case Calls.INCOMING_TYPE:
                callArrow = res.getDrawable(CALL_ARROW_ICON_RES).mutate();
                callArrow.setColorFilter(res.getColor(R.color.call_arrow_green),
                        PorterDuff.Mode.MULTIPLY);
                break;
            case Calls.MISSED_TYPE:
                callArrow = res.getDrawable(CALL_ARROW_ICON_RES).mutate();
                callArrow.setColorFilter(res.getColor(R.color.call_arrow_red),
                        PorterDuff.Mode.MULTIPLY);
                break;
            case Calls.OUTGOING_TYPE:
                Bitmap original = BitmapFactory.decodeResource(res, CALL_ARROW_ICON_RES);
                Bitmap rotated = Bitmap.createBitmap(
                        original.getWidth(), original.getHeight(), Bitmap.Config.ARGB_8888);
                Canvas tempCanvas = new Canvas(rotated);
                tempCanvas.rotate(180f, (float)original.getWidth()/2,
                        (float)original.getHeight()/2);
                tempCanvas.drawBitmap(original, 0, 0, null);
                callArrow = new BitmapDrawable(res,rotated);
                callArrow.setColorFilter(res.getColor(R.color.call_arrow_green),
                        PorterDuff.Mode.MULTIPLY);
                break;
            case Calls.AUTO_REJECT_TYPE:
                callArrow = res.getDrawable(CALL_AUTOREJECT_ICON_RES).mutate();
                break;
        }
        return callArrow;
    }

    @Override
    public void addGroupMenu(final Context context, Menu menu, Fragment fragment){
        Log.i(TAG, "[addGroupMenu]");
        if (fragment instanceof PhoneAndEmailsPickerFragment) {
            MenuItem item = menu.findItem(MENU_ID_GROUP);
            if(item == null) {
                String text = mContext.getResources().getString(R.string.groupsLabel);
                item = menu.add(0, MENU_ID_GROUP, 0, text);
                MenuItem.OnMenuItemClickListener listener = new MenuItem.OnMenuItemClickListener(){
                    @Override
                    public boolean onMenuItemClick(MenuItem item){
                        if (item.getItemId() == MENU_ID_GROUP) {
                            Intent intent = new Intent(context,
                                    ContactGroupListActivity.class);
                            Activity activity = (Activity)context;
                            activity.startActivityForResult(intent,
                                    ContactListMultiChoiceActivity.
                                    CONTACTGROUPLISTACTIVITY_RESULT_CODE);
                            return true;
                        }
                        return false;
                    }
                };
                item.setOnMenuItemClickListener(listener);
            }
        }
    }

    @Override
    public void setViewKeyListener(EditText fieldView) {
        Log.i(TAG, "[setViewKeyListener] fieldView : " + fieldView);
        if (fieldView != null) {
            fieldView.setKeyListener(SIMKeyListener.getInstance());
        } else {
            Log.e(TAG, "[setViewKeyListener]fieldView is null");
        }
    }

    public static class SIMKeyListener extends DialerKeyListener {
        private static SIMKeyListener sKeyListener;
        /**
         * The characters that are used.
         *
         * @see KeyEvent#getMatch
         * @see #getAcceptedChars
         */
        public static final char[] CHARACTERS = new char[] { '0', '1', '2',
            '3', '4', '5', '6', '7', '8', '9', '+', '*', '#', 'P', 'W', 'p', 'w', ',', ';'};

        @Override
        protected char[] getAcceptedChars() {
            return CHARACTERS;
        }

        public static SIMKeyListener getInstance() {
            if (sKeyListener == null) {
                sKeyListener = new SIMKeyListener();
            }
            return sKeyListener;
        }

    }
}
