package com.mediatek.rcs.pam.ui.conversation;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.AsyncQueryHandler;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SqliteWrapper;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.util.Log;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.Toast;

import com.mediatek.rcs.pam.Constants;
import com.mediatek.rcs.pam.PAService;
import com.mediatek.rcs.pam.R;
import com.mediatek.rcs.pam.SimpleServiceCallback;
import com.mediatek.rcs.pam.model.MessageContent;
import com.mediatek.rcs.pam.model.ResultCode;
import com.mediatek.rcs.pam.provider.PAContract.MessageColumns;
import com.mediatek.rcs.pam.ui.messageitem.MessageData;

public class PaMultiDeleteActivity extends ListActivity {

    public static final String TAG = "Pa/PaMultiDeleteActivity";

    private static final int MESSAGE_LIST_QUERY_TOKEN = 9527;

    private PAServiceCallback mPAServiceCallback;
    private PAService mPAService;
    private long mToken = Constants.INVALID;
    private int mPASrvState;

    private ListView mMsgListView;
    public PaMessageListAdapter mMsgListAdapter;

    private BackgroundQueryHandler mBackgroundQueryHandler;

    private boolean mIsSelectedAll;
    private int mDeleteCount = 0;
    private int mDeleteRunningCount = 0;
    private boolean mIsDeleting = false;

    private SelectActionMode mSelectActionMode;
    private ActionMode mSelectMode;
    private Button mChatSelect;

    private long mAccountId;

    private boolean mShowLastestMsg = false;

    private static HashMap<String, Long> sClickedViewAndTime = new HashMap<String, Long>();
    private int mOldCursorCount = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.multi_delete_list_screen);
        setProgressBarVisibility(false);

        Log.d(TAG, "onCreate is called");

        mAccountId = getIntent().getLongExtra(PaComposeActivity.ACCOUNT_ID, 0);
        if (mAccountId <= 0) {
            Log.e(TAG, "mAccountId invalid:" + mAccountId);
            finish();
        }

        mMsgListView = getListView();
        mMsgListView.setDivider(null);
        mMsgListView.setDividerHeight(getResources().getDimensionPixelOffset(
                R.dimen.ipmsg_message_list_divider_height));
        initMessageList();
        initActivityState(savedInstanceState);

        setupActionBar();
        mBackgroundQueryHandler = new BackgroundQueryHandler(
                getContentResolver());
        float textSize = PaComposeActivity.DEFAULT_TEXT_SIZE;
        try {
            Context context = createPackageContext("com.android.mms",
                    Context.CONTEXT_IGNORE_SECURITY);
            SharedPreferences sp = context.getSharedPreferences(
                    "com.android.mms_preferences", Context.MODE_WORLD_READABLE
                            | Context.MODE_MULTI_PROCESS);
            textSize = sp.getFloat("message_font_size", 18);
        } catch (NameNotFoundException e) {
            Log.d(TAG, e.getMessage());
        }
        setTextSize(textSize);

        mShowLastestMsg = true;

        initPAService();

    }

    public void setTextSize(float size) {
        if (mMsgListAdapter != null) {
            mMsgListAdapter.setTextSize(size);
        }
        if (mMsgListView != null
                && mMsgListView.getVisibility() == View.VISIBLE) {
            int count = mMsgListView.getChildCount();
            for (int i = 0; i < count; i++) {
                PaMessageListItem item = (PaMessageListItem) mMsgListView
                        .getChildAt(i);
                if (item != null) {
                    item.setBodyTextSize(size);
                }
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        startMsgListQuery();
        mIsSelectedAll = false;
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume is called");
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        Log.d(TAG, "onConfigurationChanged " + newConfig);
        super.onConfigurationChanged(newConfig);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        Log.d(TAG, "onSaveInstanceSstate is called");
        Log.d(TAG, "Selected Number: " + mMsgListAdapter.getSelectedNumber());

        Log.d(TAG, "List Count: " + mMsgListAdapter.getCount());
        if (mMsgListAdapter != null) {
            if (mMsgListAdapter.getSelectedNumber() == 0) {
                return;
            } else {
                long[] checkedArray = new long[mMsgListAdapter
                        .getSelectedNumber()];
                Iterator<Entry<Long, Boolean>> iter = mMsgListAdapter
                        .getItemList().entrySet().iterator();
                int i = 0;
                while (iter.hasNext()) {
                    @SuppressWarnings("unchecked")
                    Map.Entry<Long, Boolean> entry = iter.next();
                    if (entry.getValue()) {
                        checkedArray[i] = entry.getKey();
                        i++;
                    }
                }
                outState.putLongArray("select_list", checkedArray);
            }
        }
    }

    @Override
    protected void onListItemClick(ListView parent, View view, int position,
            long id) {
        if (view != null) {
            ((PaMessageListItem) view).onMessageListItemClick();
        }
    }

    private void initActivityState(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            long[] selectedItems = savedInstanceState
                    .getLongArray("select_list");
            if (selectedItems != null) {
                mMsgListAdapter.setItemsValue(true, selectedItems);
            }
        }
    }

    private void setupActionBar() {
        mSelectActionMode = new SelectActionMode();
        mSelectMode = startActionMode(mSelectActionMode);
        ActionBar actionBar = getActionBar();
        actionBar.setDisplayShowHomeEnabled(false);
        actionBar.setDisplayHomeAsUpEnabled(false);
        actionBar.setDisplayShowTitleEnabled(false);
    }

    private void initMessageList() {
        Log.d(TAG, "initMessageList is called");

        if (mMsgListAdapter != null) {
            Log.d(TAG, "initMessageList is not null");
            return;
        }

        mMsgListAdapter = new PaMessageListAdapter(this, null, mMsgListView,
                true, null);
        mMsgListAdapter.mIsDeleteMode = true;
        mMsgListAdapter.setMsgListItemHandler(mMessageListItemHandler);
        mMsgListAdapter.setOnDataSetChangedListener(mDataSetChangedListener);
        mMsgListView.setAdapter(mMsgListAdapter);
        mMsgListView.setItemsCanFocus(false);
        mMsgListView.setVisibility(View.VISIBLE);
    }

    private void startMsgListQuery() {
        mBackgroundQueryHandler.cancelOperation(MESSAGE_LIST_QUERY_TOKEN);

        try {
            mBackgroundQueryHandler.postDelayed(new Runnable() {
                public void run() {
                    mBackgroundQueryHandler.startQuery(
                            MESSAGE_LIST_QUERY_TOKEN, mAccountId,
                            MessageColumns.CONTENT_URI,
                            MessageContent.sFullProjection,
                            MessageColumns.ACCOUNT_ID + "=" + mAccountId
                                    + " AND " + MessageColumns.STATUS + "<"
                                    + Constants.MESSAGE_STATUS_DRAFT, null,
                            null);
                }
            }, 50);
        } catch (SQLiteException e) {
            SqliteWrapper.checkSQLiteException(this, e);
        }
    }

    private void markCheckedState(boolean checkedState) {
        mMsgListAdapter.setItemsValue(checkedState, null);
        int count = mMsgListView.getChildCount();
        PaMessageListItem item = null;

        Iterator<Entry<Long, Boolean>> iter = mMsgListAdapter.getItemList()
                .entrySet().iterator();
        Cursor cursor = mMsgListAdapter.getCursor();
        if (cursor == null) {
            Log.d(TAG, "markCheckedState cursor is null");
            return;
        }

        for (int i = 0; i < count; i++) {
            item = (PaMessageListItem) mMsgListView.getChildAt(i);
            if (item == null || item.getMessageData() == null) {
                continue;
            }
            item.setSelectedBackground(checkedState);
        }

        updateSelectCount();
    }

    private int getSelectedCount() {
        return mMsgListAdapter.getSelectedNumber();
    }

    private void confirmMultiDeleteMsgDialog(
            final MultiDeleteMsgListener listener) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.delete_message)
                .setMessage(R.string.confirm_delete_selected_messages)
                .setIconAttribute(android.R.attr.alertDialogIcon)
                .setCancelable(true)
                .setPositiveButton(R.string.delete_message, listener)
                .setNegativeButton(android.R.string.cancel, null).show();
    }

    private ProgressDialog getProgressDialog() {
        ProgressDialog dialog = new ProgressDialog(this);

        dialog.setCancelable(false);
        dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        dialog.setMessage(getString(R.string.deleting));
        dialog.setMax(1);

        return dialog;
    }

    private class MultiDeleteMsgListener implements OnClickListener {

        @Override
        public void onClick(DialogInterface arg0, int arg1) {

            if (!isPASrvReady()) {
                Toast.makeText(PaMultiDeleteActivity.this,
                        R.string.service_not_ready, Toast.LENGTH_SHORT).show();
                return;
            }

            mBackgroundQueryHandler.setProgressDialog(getProgressDialog());
            mBackgroundQueryHandler.showProgressDialog();

            Log.d(TAG, "Delete meesages. mIsSelectedAll=" + mIsSelectedAll
                    + ". selectNumber=" + mMsgListAdapter.getSelectedNumber()
                    + ". totalNumber=" + mMsgListAdapter.getAllNumber());
            mIsDeleting = true;

            if (mIsSelectedAll) {
                Log.d(TAG, "Delete all messages");
                mPAService.deleteMessageByAccount(mToken, mAccountId);
                return;
            }

            mDeleteCount = mMsgListAdapter.getSelectedNumber();

            new Thread(new Runnable() {

                @Override
                public void run() {
                    Iterator<Entry<Long, Boolean>> iter = mMsgListAdapter
                            .getItemList().entrySet().iterator();
                    while (iter.hasNext()) {
                        Map.Entry<Long, Boolean> entry = iter.next();
                        if (entry.getValue()) {
                            mPAService.deleteMessage(mToken, entry.getKey());
                        }
                    }
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mBackgroundQueryHandler.dismissProgressDialog();
                            Intent intent = new Intent();
                            intent.putExtra("delete_all", false);
                            setResult(RESULT_OK, intent);
                            finish();
                        }
                    });
                }
            }).start();
        }
    }

    private final Handler mMessageListItemHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {

            switch (msg.what) {
            case PaMessageListItem.ITEM_CLICK:
                MessageData msgItem = (MessageData) msg.obj;
                if (msgItem == null) {
                    Log.e(TAG, "ITEM_CLICK but msgItem is null !");
                    return;
                }
                mMsgListAdapter
                        .changeSelectedState(msgItem.getMessageContent().id);

                mIsSelectedAll = false;
                if (mMsgListAdapter.getSelectedNumber() > 0) {
                    if (mMsgListAdapter.getSelectedNumber() == mMsgListAdapter
                            .getCount()) {
                        mIsSelectedAll = true;
                    }
                }
                updateSelectCount();
                if (mSelectMode != null && !isFinishing()) {
                    mSelectMode.invalidate();
                }
                return;

                // case UPDATE_SELECTED_COUNT:
                // updateSelectCount();
                // break;

                // case PaMessageListAdapter.MSG_LIST_NEED_REFRASH:
                // boolean isClearCache = msg.arg1 ==
                // PaMessageListAdapter.MESSAGE_LIST_REFRASH_WITH_CLEAR_CACHE;
                // mMsgListAdapter.setClearCacheFlag(isClearCache);
                // mMsgListAdapter.notifyDataSetChanged();
                // return;

            default:
                Log.d(TAG, "Unknown message:" + msg.what);
                return;
            }
        }
    };

    private final class BackgroundQueryHandler extends AsyncQueryHandler {

        private int mListCount = 0;
        private ProgressDialog mDialog;

        public BackgroundQueryHandler(ContentResolver contentResolver) {
            super(contentResolver);
        }

        public void setProgressDialog(ProgressDialog dialog) {
            if (mDialog == null) {
                mDialog = dialog;
            }
        }

        public void showProgressDialog() {
            if (mDialog != null) {
                mDialog.show();
            }
        }

        protected void dismissProgressDialog() {
            if (mDialog == null) {
                Log.e(TAG, "mDialog is null");
                return;
            }
            try {
                mDialog.dismiss();
            } catch (IllegalArgumentException e) {
                Log.d(TAG, "ignore IllegalArgumentException:" + e);
            }
            mDialog = null;
        }

        @Override
        protected void onQueryComplete(int token, Object cookie, Cursor cursor) {
            switch (token) {
            case MESSAGE_LIST_QUERY_TOKEN:
                Log.d(TAG, "onQeryComplete is called");
                if (cursor == null) {
                    Log.d(TAG, "onQueryComplete, cursor is null");
                    return;
                }
                long accountId = (Long) cookie;
                if (accountId != mAccountId) {
                    Log.d(TAG,
                            "onQueryComplete: msg history query result is for accountId "
                                    + accountId + ", but has mAccountId "
                                    + mAccountId + " starting a new query");
                    startMsgListQuery();
                    if (cursor != null) {
                        cursor.close();
                    }
                    return;
                }

                if (!isFinishing()) {
                    if (mMsgListAdapter.mIsDeleteMode) {
                        if (cursor.getCount() != 0) {
                            mMsgListAdapter.initListMap(cursor);
                            if (mListCount != cursor.getCount()) {
                                mListCount = cursor.getCount();
                                mMsgListView.setSelection(mListCount);
                                mMsgListView.smoothScrollToPosition(mListCount);
                            }
                            PopupMenu pop = mSelectActionMode.getPopupMenu();
                            if (pop != null
                                    && mOldCursorCount != cursor.getCount()) {
                                Log.d(TAG, "cahnge pop in onQueryComplete");
                                mOldCursorCount = cursor.getCount();
                                Menu popupMenu = pop.getMenu();
                                MenuItem selectAllItem = popupMenu
                                        .findItem(R.id.menu_select_all);
                                MenuItem unSelectAllItem = popupMenu
                                        .findItem(R.id.menu_select_cancel);
                                if (mMsgListAdapter.getSelectedNumber() >= cursor
                                        .getCount()) {
                                    if (selectAllItem != null) {
                                        selectAllItem.setVisible(false);
                                    }
                                    if (unSelectAllItem != null) {
                                        unSelectAllItem.setVisible(true);
                                    }
                                } else {
                                    if (selectAllItem != null) {
                                        selectAllItem.setVisible(true);
                                    }
                                    if (unSelectAllItem != null) {
                                        unSelectAllItem.setVisible(false);
                                    }
                                }
                            }
                        } else {
                            if (cursor != null) {
                                Log.d(TAG,
                                        "onQueryComplete, cursor count is 0.");
                                cursor.close();
                                return;
                            }
                        }
                    }
                    mMsgListAdapter.changeCursor(cursor);
                } else {
                    Log.d(TAG, "activity is finishing");
                    if (cursor != null) {
                        cursor.close();
                        return;
                    }
                }
                if (mShowLastestMsg) {
                    mShowLastestMsg = false;
                    mMsgListView.setSelection(cursor.getCount() - 1);
                }
                if (mSelectMode != null) {
                    Log.d(TAG,
                            "mSelectMode is invalidate after onquerycomplete");
                    mSelectMode.invalidate();
                }
                return;

            default:
                break;
            }
        }
    }

    private final PaMessageListAdapter.OnDataSetChangedListener mDataSetChangedListener =
            new PaMessageListAdapter.OnDataSetChangedListener() {

        @Override
        public void onDataSetChanged(PaMessageListAdapter adapter) {

        }

        @Override
        public void onContentChanged(PaMessageListAdapter adapter) {
            Log.d(TAG,
                    "PaMessageListAdapter.OnDataSetChangedListener.onContentChanged");
            if (mIsDeleting) {
                Log.d(TAG, "onContentChange in deleting state");
                return;
            }
            startMsgListQuery();
            mIsSelectedAll = false;
        }
    };

    private void setSelectAll() {
        if (!mIsSelectedAll) {
            mIsSelectedAll = true;
            markCheckedState(mIsSelectedAll);
            if (mSelectMode != null) {
                mSelectMode.invalidate();
            }
        }
    }

    private void setDeselectAll() {
        if (mMsgListAdapter.getSelectedNumber() > 0) {
            mIsSelectedAll = false;
            markCheckedState(mIsSelectedAll);
            if (mSelectMode != null) {
                mSelectMode.invalidate();
            }
        }
    }

    private void updateSelectCount() {
        int selectNum = getSelectedCount();
        mChatSelect.setText(getResources().getQuantityString(
                R.plurals.message_view_selected_message_count, selectNum,
                selectNum)
                + "     ");
    }

    private class SelectActionMode implements ActionMode.Callback {
        private PopupMenu mPopup = null;

        @Override
        public boolean onCreateActionMode(final ActionMode mode, Menu menu) {
            ViewGroup v = (ViewGroup) LayoutInflater.from(
                    PaMultiDeleteActivity.this).inflate(
                    R.layout.chat_select_action_bar, null);
            mode.setCustomView(v);
            mChatSelect = ((Button) v.findViewById(R.id.bt_chat_select));
            mChatSelect.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v) {
                    if (mPopup == null) {
                        mPopup = new PopupMenu(PaMultiDeleteActivity.this, v);
                        mPopup.getMenuInflater().inflate(R.menu.select_menu,
                                mPopup.getMenu());
                    } else {
                        mPopup.dismiss();
                    }

                    mPopup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {

                        @Override
                        public boolean onMenuItemClick(MenuItem item) {
                            int id = item.getItemId();
                            if (id == R.id.menu_select_all) {
                                setSelectAll();
                            } else if (id == R.id.menu_select_cancel) {
                                setDeselectAll();
                            } else {
                                return true;
                            }
                            return false;
                        }
                    });

                    Menu popupMenu = mPopup.getMenu();
                    MenuItem selectAllItem = popupMenu
                            .findItem(R.id.menu_select_all);
                    MenuItem unSelectAllItem = popupMenu
                            .findItem(R.id.menu_select_cancel);
                    if (mMsgListAdapter != null) {
                        Cursor cursor = mMsgListAdapter.getCursor();
                        if (cursor != null) {
                            if (mMsgListAdapter.getSelectedNumber() >= cursor
                                    .getCount()) {
                                if (selectAllItem != null) {
                                    selectAllItem.setVisible(false);
                                }
                                if (unSelectAllItem != null) {
                                    unSelectAllItem.setVisible(true);
                                }
                            } else {
                                if (selectAllItem != null) {
                                    selectAllItem.setVisible(true);
                                }
                                if (unSelectAllItem != null) {
                                    unSelectAllItem.setVisible(false);
                                }
                            }
                        } else {
                            if (selectAllItem != null) {
                                selectAllItem.setVisible(true);
                            }
                            if (unSelectAllItem != null) {
                                unSelectAllItem.setVisible(false);
                            }
                        }
                    } else if (selectAllItem != null) {
                        selectAllItem.setVisible(true);
                        if (unSelectAllItem != null) {
                            unSelectAllItem.setVisible(false);
                        }
                    }
                    mPopup.show();
                }
            });
            updateSelectCount();
            getMenuInflater().inflate(R.menu.compose_multi_select_menu, menu);

            return true;
        }

        public PopupMenu getPopupMenu() {
            return mPopup;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            MenuItem deleteItem = menu.findItem(R.id.delete);

            int selectNum = getSelectedCount();
            Log.d(TAG, "onPrepareActionMode(): selectNum = " + selectNum);
            if (selectNum > 0) {
                deleteItem.setEnabled(true);
            } else {
                deleteItem.setEnabled(false);
            }
            return true;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            switch (item.getItemId()) {
            case R.id.delete:
                int mSelectedNumber = mMsgListAdapter.getSelectedNumber();
                if (mSelectedNumber >= 0) {
                    MultiDeleteMsgListener mMultiDeleteMsgListener = new MultiDeleteMsgListener();
                    confirmMultiDeleteMsgDialog(mMultiDeleteMsgListener);
                }
                break;
            default:
                break;
            }
            return false;
        }

        @Override
        public void onDestroyActionMode(ActionMode arg0) {
            PaMultiDeleteActivity.this.finish();
        }

    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy is called()");
        if (mMsgListAdapter != null) {
            // mMsgListAdapter.destoryTastStack();
            mMsgListAdapter.clearList();
            mMsgListAdapter.changeCursor(null);
            mMsgListAdapter.setOnDataSetChangedListener(null);
        }

        mPAService.unregisterCallback(mToken);
        mPAService = null;

        super.onDestroy();
    }

    public boolean isFastDoubleClick(String viewName, long slotTime) {
        long time = System.currentTimeMillis();
        long lastClickTime = sClickedViewAndTime.get(viewName) == null ? 0L
                : sClickedViewAndTime.get(viewName);
        Log.d(TAG, "isFastDoubleClick, clicked time = " + time
                + ", lastClickTime = " + lastClickTime);
        long slotT = time - lastClickTime;
        sClickedViewAndTime.put(viewName, time);
        if (0 < slotT && slotT < slotTime) {
            return true;
        }
        return false;
    }

    private void initPAService() {
        Log.d(TAG, "initPAService. state=" + mPASrvState);
        mPAServiceCallback = new PAServiceCallback();
        mPAService = PAService.getInstance();
        if (mPAService != null) {
            mToken = mPAService.registerCallback(mPAServiceCallback, false);
            mPAService.registerAck(mToken);
            changePASrvState(PaComposeActivity.PASRV_STATE_REGISTED);
            Log.d(TAG, "initPAService done. mToken=" + mToken);
        } else {
            changePASrvState(PaComposeActivity.PASRV_STATE_INIT);
            PAService.init(this, new PAService.ServiceConnectNotify() {

                @Override
                public void onServiceConnected() {
                    Log.i(TAG, "onServiceConnectedsss");
                    mPAService = PAService.getInstance();
                    mToken = mPAService.registerCallback(mPAServiceCallback,
                            false);
                    mPAService.registerAck(mToken);
                    changePASrvState(PaComposeActivity.PASRV_STATE_REGISTED);
                    Log.d(TAG, "initPAService done. mToken=" + mToken);
                }
            });
        }
    }

    private boolean isPASrvReady() {
        if (mPASrvState == PaComposeActivity.PASRV_STATE_REGISTED) {
            return true;
        }
        return false;
    }

    private boolean changePASrvState(int state) {
        Log.d(TAG, "changePaSrvState from " + mPASrvState + " to " + state);
        boolean ret = false;
        if (mPASrvState != state) {
            mPASrvState = state;
            ret = true;
        }
        return ret;
    }

    class PAServiceCallback extends SimpleServiceCallback {

        @Override
        public void onServiceConnected() throws RemoteException {
            Log.i(TAG, "onServiceConnected.");
        }

        @Override
        public void onServiceDisconnected(int reason) throws RemoteException {
            Log.i(TAG, "onServiceDisconnected For reason:" + reason);
            if (reason == PAService.INTERNAL_ERROR) {
                finish();
            }
        }

        @Override
        public void onServiceRegistered() throws RemoteException {
            Log.i(TAG, "onServiceRegistered.");
        }

        @Override
        public void onServiceUnregistered() throws RemoteException {
            Log.i(TAG, "onServiceUnregistered");
        }

        @Override
        public void reportDeleteMessageResult(long requestId, int resultCode) {
            Log.i(TAG, "onServiceUnregistered. resultCode=" + resultCode);
            final Intent intent = new Intent();
            boolean isDone = false;
            if (ResultCode.SUCCESS != resultCode) {
                Toast.makeText(PaMultiDeleteActivity.this,
                        R.string.delete_fail, Toast.LENGTH_SHORT).show();
            }
            if (mIsSelectedAll) {
                isDone = true;
                intent.putExtra("delete_all", true);
            } else {
                mDeleteRunningCount++;
                if (mDeleteRunningCount == mDeleteCount) {
                    isDone = true;
                    intent.putExtra("delete_all", false);
                    mDeleteRunningCount = 0;
                    mDeleteCount = 0;
                }
            }

            if (isDone) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mBackgroundQueryHandler.dismissProgressDialog();
                        setResult(RESULT_OK, intent);
                        finish();
                    }
                });
            }
        }

    }

}
