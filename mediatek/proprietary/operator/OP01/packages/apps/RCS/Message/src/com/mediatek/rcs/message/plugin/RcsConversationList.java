package com.mediatek.rcs.message.plugin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.DialogInterface.OnClickListener;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.net.NetworkInfo.State;
import android.os.Handler;
import android.os.Message;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.Settings;
import android.provider.Telephony.Mms;
import android.provider.Telephony.Sms;
import android.provider.Telephony.Sms.Conversations;

import android.text.TextUtils;
import android.telephony.SubscriptionManager;
import android.util.Log;
import android.view.ActionMode;
import android.view.ContextMenu;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ContextThemeWrapper;
import android.widget.Adapter;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.HeaderViewListAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListPopupWindow;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;

import com.mediatek.mms.callback.IConversationListCallback;
import com.mediatek.mms.ipmessage.DefaultIpConversationListExt;

import com.mediatek.rcs.common.binder.RCSServiceManager;
import com.mediatek.rcs.common.binder.RCSServiceManager.OnServiceChangedListener;
import com.mediatek.rcs.common.service.Participant;
import com.mediatek.rcs.common.utils.RCSUtils;

import com.mediatek.rcs.message.cloudbackup.CloudMsgBackupRestore;
import com.mediatek.rcs.message.data.RcsProfile;
import com.mediatek.rcs.message.group.PortraitManager;
import com.mediatek.rcs.message.ui.CreateGroupActivity;
import com.mediatek.rcs.message.ui.RcsSubSelectDialog;
import com.mediatek.rcs.message.utils.RcsMessageConfig;
import com.mediatek.rcs.message.utils.RcsMessageUtils;
import com.mediatek.rcs.message.R;

/**
 * Plugin implements. response ConversationList.java in MMS host.
 *
 */
public class RcsConversationList extends DefaultIpConversationListExt implements
            OnServiceChangedListener {
    private static String TAG = "RcsConversationList";
    private Activity mHostContext = null;
    private Context mPluginContext;
    private ListView mListView;
    RCSServiceManager mServiceManager;

    private LinearLayout mNetworkStatusBar;
    private BroadcastReceiver mNetworkStateReceiver;

    private MenuItem mCreatGroupMenuItem;
    private MenuItem mStickyTopMenuItem;
    private MenuItem mCancelStickyMenuItem;

    private static final int REQUEST_CODE_SELECT_CONTACT_FOR_GROUP = 100;

    private static final String KEY_SELECTION_SIMID = "SIMID";

    public IConversationListCallback mCallback;

    private static final int MENU_GROUP_ID_RCS = 100;
    private static final int MENU_CHANGE_MODE = 101;
    private static final int MENU_CREATE_GROUP_CHAT = 1000;
    private static final int MENU_DELETE = 0;
    private static final int MENU_STICKY_TOP = 1001;
    private static final int MENU_CANCEL_STICKY = 1002;
    private static final int MENU_MY_FAVORITE = 1003;
    private static final int MENU_SPAM_MESSAGE = 1004;
    private static final int DISABLE_ALPHA = 100;
    private static final int ENABLE_ALPHA = 255;

    private Handler mUiHandler;
    private static final String ACTION_CONTACT_SELECTION =
                            "android.intent.action.contacts.list.PICKMULTIPHONEANDEMAILS";
    public static HashSet<Long> mStickyThreadsSet;
    ActionMode mActionMode;
    private boolean mServiceActived = false;
    private boolean mServiceConfigured = false;
    private View mPublicAccountView = null;

    public RcsConversationList(Context context) {
        mPluginContext = context;
    }

    @Override
    public boolean onIpConversationListCreate(Activity context, IConversationListCallback callback,
            ListView listview, LinearLayout ipEmptyView, LinearLayout networkStatusBar,
            TextView networkStatusTextView) {
        mServiceManager = RCSServiceManager.getInstance();
        mUiHandler = new Handler();
        mHostContext = context;
        mCallback = callback;
        mListView = listview;

        mNetworkStatusBar = networkStatusBar;
        if (networkStatusTextView != null) {
            networkStatusTextView.setText(mPluginContext.getString(R.string.no_internet));
        }
        mStickyThreadsSet = new HashSet<Long>();
        mServiceConfigured = mServiceManager.isServiceConfigured();
        mServiceActived = mServiceManager.isServiceActivated();
        Log.d(TAG, "configured + " + mServiceConfigured + ", serviceActived = " + mServiceActived);
        mServiceManager.addOnServiceChangedListener(this);

        if (mServiceActived) {
            Log.i(TAG, "RCS is on, add header");
            mPublicAccountView = buildPublicAccountEntry(mPluginContext);
            mListView.addHeaderView(mPublicAccountView);
        } else {
            Log.i(TAG, "RCS is off, do nothing");
        }

        SharedPreferences msgBr = mPluginContext.getSharedPreferences("message_cloud_br_preferences",
                Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = msgBr.edit();
        boolean isFirstEntry = msgBr.getBoolean("isFirstEntry", true);
        boolean isRcsReady = mServiceManager.serviceIsReady();
        Log.d(TAG, "isFirstEntry = " + isFirstEntry + "isRcsReady = " + isRcsReady);
        if (isFirstEntry) {
            editor.putBoolean("isFirstEntry", false);
            editor.commit();
            Log.d(TAG, "First Entry");
            if (isRcsReady) {
                Log.d(TAG, "First Entry, restore message from network");
                CloudMsgBackupRestore cloudMsgBackupRestore = new CloudMsgBackupRestore(context,
                        mPluginContext);
                cloudMsgBackupRestore.init();
                cloudMsgBackupRestore.firstEntryMmsRestore();
            }
        }
        return true;
    }

    private View buildPublicAccountEntry(final Context context) {
        ContextThemeWrapper themeWrapper = new ContextThemeWrapper(context,
                com.android.internal.R.style.Theme_Material_Settings);
        LayoutInflater inflater =
                    (LayoutInflater) themeWrapper.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        LinearLayout entryItem = (LinearLayout)inflater.inflate(
                R.layout.rcs_public_account_entry, null);

        Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(),
                R.drawable.ic_public_account_entry);
        ImageView image = (ImageView) entryItem.findViewById(R.id.iv_mms_entry);
        image.setImageBitmap(bitmap);
        image.setBackgroundColor(Color.BLUE);
        image.setAdjustViewBounds(true);

        entryItem.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                Intent intent = new Intent("com.mediatek.rcs.pam.activities." +
                        "MessageHistoryActivity");
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
            }
        });

        return entryItem;
    }

    private void updatePublicEntryOnStateChanged() {
        Log.i(TAG, "[OnStateChanged] state " + mServiceActived + ", view " + mPublicAccountView);
        if (mServiceActived && mPublicAccountView == null) {
            Log.i(TAG, "RCS switch on, show header");
            mPublicAccountView = buildPublicAccountEntry(mPluginContext);
            mListView.addHeaderView(mPublicAccountView);

        } else if (!mServiceActived && mPluginContext != null) {
            Log.i(TAG, "RCS switch off, hide header");
            mListView.removeHeaderView(mPublicAccountView);
            mPublicAccountView = null;

        } else {
            Log.i(TAG, "Do nothing here");
        }
    }

    public boolean onIpNeedLoadView(View emptyViewDefault) {
        Log.d(TAG, "onIpNeedLoadView ");
        RcsMessagingNotification.cancelNewGroupInviations(mHostContext);
        if (RcsMessageConfig.isServiceEnabled(mHostContext)) {
            if (mNetworkStateReceiver == null) {
                IntentFilter filter = new IntentFilter();
                filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
                mNetworkStateReceiver = new NetworkStateReceiver();
                mHostContext.registerReceiver(mNetworkStateReceiver, filter);
            }
        }
        PortraitManager.getInstance().clearAllGroupThumbnails();
        return false;
    }


    @Override
    public boolean onIpCreateOptionsMenu(Menu menu) {
        /// M: add for ipmessage menu
        MenuItem item = menu
             .add(MENU_GROUP_ID_RCS, MENU_CREATE_GROUP_CHAT, 0,
                                     mPluginContext.getString(R.string.create_group_chat))
             .setIcon(mPluginContext.getResources().getDrawable(R.drawable.ic_create_a_group_chat))
             .setTitle(mPluginContext.getString(R.string.create_group_chat));
        item.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        item.setVisible(true);
        mCreatGroupMenuItem = item;
        menu.add(0, MENU_MY_FAVORITE, 0, mPluginContext.getString(R.string.menu_my_favorite));
        menu.add(0, MENU_SPAM_MESSAGE, 0, mPluginContext.getString(R.string.menu_spam_message));
        return false;
    }

    @Override
    public boolean onIpPrepareOptionsMenu(Menu menu) {
        Log.d(TAG, "[onIpPrepareOptionsMenu]: mServiceConfigured = " +
                      mServiceConfigured + "mServiceActived = " +mServiceActived);
        if (mServiceConfigured && mServiceActived) {
            menu.setGroupVisible(MENU_GROUP_ID_RCS, true);
            menu.setGroupEnabled(MENU_GROUP_ID_RCS, true);
//            mCreatGroupMenuItem.getIcon().setAlpha(ENABLE_ALPHA);
            mCreatGroupMenuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
            mCreatGroupMenuItem.getIcon().setAlpha(
                    RcsMessageUtils.isSmsEnabled(mHostContext) ? 255 : 127);
        } else {
            menu.setGroupVisible(MENU_GROUP_ID_RCS, false);
        }
        MenuItem changeModeItem = menu.findItem(MENU_CHANGE_MODE);
        if (changeModeItem != null) {
            menu.removeItem(MENU_CHANGE_MODE);
        }
        return false;
    }

    @Override
    public boolean onIpOptionsItemSelected(MenuItem item, boolean isSmsEnabled) {
        /// M: add for ipmessage menu
        if (RcsMessageConfig.isActivated(mHostContext)) {
            Intent intent = null;
            switch (item.getItemId()) {
            case MENU_CREATE_GROUP_CHAT:
                if (isSmsEnabled) {
                    intent = new Intent("android.intent.action.contacts.list.PICKMULTIPHONES");
                    intent.setType(Phone.CONTENT_TYPE);
                    String me = RcsProfile.getInstance().getNumber();
                    if (!TextUtils.isEmpty(me)) {
                        intent.putExtra("ExistNumberArray", new String[]{me});
                    }
                    intent.putExtra("Group", true);
                    mHostContext.startActivityForResult(intent,
                                        REQUEST_CODE_SELECT_CONTACT_FOR_GROUP);
                } else {
                    Toast.makeText(mHostContext, mPluginContext.getString(R.string.mms_not_default),
                            Toast.LENGTH_SHORT).show();
                }
                break;
            case MENU_MY_FAVORITE:
                intent = new Intent("com.mediatek.rcs.message.ui.FavoritesActivity");
                intent.setPackage("com.mediatek.rcs.message");
                mHostContext.startActivity(intent);
                break;
            case MENU_SPAM_MESSAGE:
                intent = new Intent("com.mediatek.rcs.message.ui.SpamMsgActivity");
                intent.setPackage("com.mediatek.rcs.message");
                mHostContext.startActivity(intent);
                break;

            default:
                break;
            }
        }
        return false;
    }


    @Override
    public boolean onIpCreateActionMode(ActionMode mode, Menu menu) {
        Log.d(TAG, "onIpCreateActionMode()");
        mActionMode = mode;
        if (mServiceActived && mServiceConfigured) {
            mStickyTopMenuItem = menu.add(0, MENU_STICKY_TOP, 0,
                    mPluginContext.getString(R.string.menu_sticky_top));
            mStickyTopMenuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
            mCancelStickyMenuItem = menu.add(0, MENU_CANCEL_STICKY, 1,
                    mPluginContext.getString(R.string.menu_cancel_sticky));
            mCancelStickyMenuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
        }
        return false;
    }


    @Override
    public boolean onIpActionItemClicked(final ActionMode mode, MenuItem item,
                                HashSet<Long> selectedThreadIds) {
        Log.d(TAG, "onIpActionItemClicked()");
        //TODO: process action mode menu item click event here
        Object[] threadIds = selectedThreadIds.toArray();
        long thread0 = Long.valueOf(threadIds[0].toString());
     //   boolean isSticky = isThreadSticky(thread);
        switch (item.getItemId()) {
            case MENU_DELETE:
                Iterator<Long> iterator = selectedThreadIds.iterator();
                while(iterator.hasNext()) {
                    long thread = iterator.next();
                    if (isThreadSticky(thread)) {
                        mStickyThreadsSet.remove(thread);
                    }
                }
                break;
            case MENU_STICKY_TOP:
                RCSUtils.markConversationTop(mPluginContext, thread0, true);
          //      mStickyThreadsSet.add(thread0);
                mActionMode.finish();
                break;
            case MENU_CANCEL_STICKY:
                RCSUtils.markConversationTop(mPluginContext, thread0, false);
          //      mStickyThreadsSet.remove(thread0);
                mActionMode.finish();
                break;
            default:
                break;
        }
        return false;
    }

    @Override
    public boolean onIpUpdateActionMode(HashSet<Long> selectedThreadIds) {
        Log.d(TAG, "onIpUpdateActionMode()");
        // show sticky or cancel sticky
        if (mStickyTopMenuItem == null || mCancelStickyMenuItem == null) {
            return false;
        }
        if (selectedThreadIds.size() > 1) {
            if (mStickyTopMenuItem.isVisible()) {
                mStickyTopMenuItem.setEnabled(false);
            }
            if (mCancelStickyMenuItem.isVisible()) {
                mCancelStickyMenuItem.setEnabled(false);
            }
        } else if (selectedThreadIds.size() == 1) {
            Object[] threadIds = selectedThreadIds.toArray();
            long thread = Long.valueOf(threadIds[0].toString());
            boolean isSticky = isThreadSticky(thread);
            if (isSticky) {
                mStickyTopMenuItem.setVisible(false);
                mCancelStickyMenuItem.setVisible(true);
                mCancelStickyMenuItem.setEnabled(true);
            } else {
                mStickyTopMenuItem.setVisible(true);
                mStickyTopMenuItem.setEnabled(true);
                mCancelStickyMenuItem.setVisible(false);
            }
        }
        return false;
    }

    public boolean isThreadSticky(long threadId) {
        Log.d(TAG, "isThreadSticky() threadId = " + threadId);
        return mStickyThreadsSet.contains(threadId);

    }



    public void onIpActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(TAG, "onActivityResult(): requestCode=" + requestCode + ", resultCode=" + resultCode);
        if (resultCode != mHostContext.RESULT_OK) {
            Log.d(TAG, "onActivityResult(): result is not OK.");
            return;
        }
        switch (requestCode) {
        case REQUEST_CODE_SELECT_CONTACT_FOR_GROUP:
            long[] ids = data.getLongArrayExtra("com.mediatek.contacts.list.pickdataresult");
            final long[] contactsId =
                    data.getLongArrayExtra("com.mediatek.contacts.list.pickdataresult");
            Intent intent = new Intent(CreateGroupActivity.ACTIVITY_ACTION);
            intent.putExtra(CreateGroupActivity.TAG_CREATE_GROUP_BY_IDS, contactsId);
            int count = mServiceManager.getGroupChatMaxParticipantsNumber();
            Log.d(TAG, "getGroupChatMaxParticipantsNumber, result = " + count);
            if (count > 0 && contactsId.length + 1 > count) {
                String toastString =
                     mPluginContext.getString(R.string.select_group_number_exceed_toast, count-1);
                Toast.makeText(mHostContext, toastString, Toast.LENGTH_LONG).show();
                return;
            }
            mHostContext.startActivity(intent);
            break;
        default:
            Log.d(TAG, "onActivityResult(): default return.");
            return;
        }
        return;
    }


    @Override
    public void onIpDestroy() {
        /// M: add for ipmessage
        if (mNetworkStateReceiver != null) {
            mHostContext.unregisterReceiver(mNetworkStateReceiver);
            mNetworkStateReceiver = null;
        }
        mServiceManager.removeOnServiceChangedListener(this);
    }

    @Override
    public Adapter onIpQueryComplete(ListView listView) {
        Log.d(TAG, "onIpQueryComplete ");
        if ((listView.getAdapter()) instanceof HeaderViewListAdapter) {
            HeaderViewListAdapter wrappedAdapter = (HeaderViewListAdapter) listView.getAdapter();
            return wrappedAdapter.getWrappedAdapter();
        } else {
            return null;
        }
    }

    @Override
    public void onIpQueryCompleteEnd(final ListView listView, Handler handler,
            final BaseAdapter adapter) {
        int totalCount = listView.getCount();
        int headerCount = listView.getHeaderViewsCount();
        Log.i(TAG, "[onIpQueryCompleteEnd] total " + totalCount + ", header " + headerCount);

        if (headerCount > 0 && totalCount == headerCount) {
            handler.postDelayed(new Runnable() {
                public void run() {
                    adapter.notifyDataSetChanged();
                    showListView(listView, "[onIpQueryCompleteEnd]");
                }
            }, 300);
        }
    }

    @Override
    public boolean onIpQueryCompleteQueryList(final ListView listView) {
        int totalCount = listView.getCount();
        int headerCount = listView.getHeaderViewsCount();
        Log.i(TAG, "[onIpQueryCompleteQueryList] total " + totalCount + ", header " + headerCount);

        if (headerCount > 0 && totalCount == headerCount) {
            if (listView.getVisibility() != View.VISIBLE) {
                showListView(listView, "[onIpQueryCompleteQueryList]");
            }
            return true;
        } else {
            return false;
        }
    }

    private void showListView(final ListView listView, String prefix) {
        Log.d(TAG, "showListView " + prefix);
        listView.post(new Runnable() {
            @Override
            public void run() {
                View emptyView = listView.getEmptyView();
                if (emptyView != null) {
                    emptyView.setVisibility(View.GONE);
                }
                listView.setVisibility(View.VISIBLE);
                listView.requestLayout();
                listView.bringToFront();
            }
        });
    }

    // a receiver to moniter the network status.
    private class NetworkStateReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            boolean success = false;
            ConnectivityManager connManager =
             (ConnectivityManager) mHostContext.getSystemService(mHostContext.CONNECTIVITY_SERVICE);
            State state = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI).getState();
            if (State.CONNECTED == state) {
                success = true;
            }
            if (!success) {
                state = connManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE).getState();
                if (State.CONNECTED == state) {
                    success = true;
                }
            }
            showInternetStatusBar(!success);
        }
    }

    public void showInternetStatusBar(boolean show) {
        if (show) {
            mNetworkStatusBar.setVisibility(View.VISIBLE);
        } else {
            mNetworkStatusBar.setVisibility(View.GONE);
        }
    }

    @Override
    public boolean onIpCreateNewMessage() {
        long subIdinSetting = SubscriptionManager.getDefaultSmsSubId();
        if (subIdinSetting == Settings.System.SMS_SIM_SETTING_AUTO) {
            RcsSubSelectDialog subSelectDialog = new RcsSubSelectDialog(mPluginContext,
                                                                mHostContext);
            AlertDialog mDialog = subSelectDialog.showSubSelectedDialog();
            if (mDialog == null) {
                return false;
            }
            return true;
        }
        return false;
    }

    @Override
    public void onServiceStateChanged(final int state, final boolean activated,
            final boolean configured, final boolean registered) {
        Log.d(TAG, "onServiceStateChanged: activated = " + activated + ", configured = "
                + configured + ", registered = " + registered);
        mHostContext.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // TODO Auto-generated method stub
                boolean serviceEnable = activated && configured;
                if ((mServiceActived && mServiceConfigured) != serviceEnable) {
                    mHostContext.invalidateOptionsMenu();
                }
                mServiceActived = activated;
                mServiceConfigured = configured;
                updatePublicEntryOnStateChanged();
            }
        });
    }
}
