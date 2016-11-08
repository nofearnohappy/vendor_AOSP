package com.mediatek.rcs.message.ui;

import android.app.ActionBar;
import android.app.Activity;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.database.Cursor;
import android.media.MediaFile;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.ActionMode;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.webkit.MimeTypeMap;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.graphics.Bitmap;

import com.mediatek.rcs.common.provider.SpamMsgData;
import com.mediatek.rcs.common.provider.SpamMsgUtils;
import com.mediatek.rcs.common.service.PortraitService;
import com.mediatek.rcs.common.service.PortraitService.Portrait;
import com.mediatek.rcs.common.service.PortraitService.UpdateListener;
import com.mediatek.rcs.common.utils.EmojiShop;
import com.mediatek.rcs.common.utils.RCSUtils;
import com.mediatek.rcs.message.R;
import com.mediatek.rcs.message.ui.SpamDataItem.Constants;
import com.mediatek.rcs.message.ui.SpamDataItem.FTData;
import com.mediatek.rcs.message.ui.SpamDataItem.GeolocationData;
import com.mediatek.rcs.message.ui.SpamDataItem.MusicData;
import com.mediatek.rcs.message.ui.SpamDataItem.PictureData;
import com.mediatek.rcs.message.ui.SpamDataItem.VcardData;
import com.mediatek.rcs.message.ui.SpamDataItem.VideoData;
import com.mediatek.rcs.message.utils.RcsMessageUtils;
import com.mediatek.rcs.message.location.GeoLocUtils;
import com.mediatek.rcs.message.location.GeoLocXmlParser;
import com.mediatek.rcs.message.plugin.EmojiImpl;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

/**
 * Show spam item.
 */
public class SpamMsgActivity extends ListActivity {
    private static final String TAG = "com.mediatek.rcsmessage.favspam/SpamMsgActivity";
    private static final String STATE_CHECKED_ITEMS = "checkedItems";
    private CarrymoreActionMode mCarryMoreActionMode;
    private ActionMode mActionMode;
    private boolean mIsInActionMode = false;
    private HashSet<Integer> mCheckedItemIds;
    private static final String STATE_ACTION_MODE = "ActionMode";
    private static final int START_ACTION_MODE_DELAY_TIME = 500;
    private static final Uri SPAM_URI = SpamMsgData.CONTENT_URI;
    private ListView mListView;
    private SpamCursorAdapter mListAdapter = null;
    private Handler mHandler;
    private SpamMsgUtils mSpamMsgUtils;
    private Context mContext;
    protected PortraitService mPortraitService;
    private SpamDdObserver mSpamDdObserver;
    private PortraitUpdateListener mPortraitUpdateListener;


    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        Log.v(TAG, "onCreate");
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.favorites_list_view);
        View emptyView = findViewById(R.id.empty);
        mListView = getListView();
        mListView.setEmptyView(emptyView);
        mContext = this;
        init();
        mPortraitService = PortraitService.getInstance(this.getApplicationContext(),
                R.drawable.ic_default_contact,
                R.drawable.contact_blank_avatar);
        mPortraitUpdateListener = new PortraitUpdateListener();
        mPortraitService.addListener(mPortraitUpdateListener);
        registerForContextMenu(mListView);
        mSpamDdObserver = new SpamDdObserver(mHandler);
        getContentResolver().registerContentObserver(SPAM_URI, true, mSpamDdObserver);

        if (savedInstanceState != null) {
            boolean isActionMode = savedInstanceState.getBoolean(STATE_ACTION_MODE, false);
            if (isActionMode) {
                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        Log.v(TAG, "onCreate startActionMode");
                        mActionMode = SpamMsgActivity.this.startActionMode(mCarryMoreActionMode);
                        mCarryMoreActionMode.restoreState(savedInstanceState);
                    }
                }, START_ACTION_MODE_DELAY_TIME);
            }
        }
    }

    private void init() {
        initListAdapter();
        initHandler();
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        Log.v(TAG, "onCreateContextMenu");
        menu.setHeaderTitle(R.string.operation);
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.spam_menu, menu);
        super.onCreateContextMenu(menu, v, menuInfo);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo menuInfo;
        try {
            menuInfo = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        } catch (ClassCastException e) {
            Log.e(TAG, "bad menuInfo", e);
            return false;
        }

        Cursor cursor = (Cursor) mListView.getItemAtPosition(menuInfo.position);
        int msgId = cursor.getInt(cursor.getColumnIndex(Constants.COLUMN_NAME_ID));
        HashSet<Integer> checkedItemIds = new HashSet<Integer>();
        checkedItemIds.add(msgId);
        Log.v(TAG, "menuInfo.position = " + menuInfo.position);
        Log.v(TAG, "select.getMsgId() = " + msgId);

        switch (item.getItemId()) {
        case R.id.menu_delete:
            deleteCheckItem(checkedItemIds);
            return true;

        case R.id.menu_restore:
            restoreCheckItem(checkedItemIds);
            return true;

        case R.id.menu_more:
            Log.v(TAG, "menu_more is pressed, then will open action mode");
            mCarryMoreActionMode = new CarrymoreActionMode();
            mActionMode = this.startActionMode(mCarryMoreActionMode);
            mListAdapter.notifyDataSetChanged();
            return true;

        default:
            Log.e(TAG, "conetext item select error!");
            return super.onContextItemSelected(item);
        }
    }

    private void initListAdapter() {
        Log.v(TAG, " initListAdapter()");
        Cursor cursor = this.getContentResolver()
                .query(SPAM_URI, null, null, null, "date DESC");
        cursor.moveToFirst();
        mListAdapter = new SpamCursorAdapter(this, cursor, false);
        setListAdapter(mListAdapter);
    }

    private void initHandler() {
        mHandler = new Handler() {
            @Override
            public void handleMessage(final Message msg) {
                switch (msg.what) {
                default:
                    break;
                }
            }
        };
    }

    /**
     * The class is used to execute delete select data from database.
     */
    private class DeleteCheckItemTask extends AsyncTask<ArrayList<Integer>, String, Long> {

        private ProgressDialog mProgressDialog;
        private ArrayList<Integer> mMsgIdList;

        public DeleteCheckItemTask() {
            mProgressDialog = new ProgressDialog(SpamMsgActivity.this);
            mProgressDialog.setCancelable(false);
            mProgressDialog.setIndeterminate(true);
            mProgressDialog.setMessage(getString(R.string.delete_please_wait));
        }

        @Override
        protected void onPostExecute(Long arg0) {
            super.onPostExecute(arg0);
            int delCount = mMsgIdList.size();
            Log.v(TAG, "onPostExecute");
            Activity activity = SpamMsgActivity.this;
            if (activity != null && mProgressDialog != null) {
                mProgressDialog.dismiss();
            }
        }

        @Override
        protected void onPreExecute() {
            Activity activity = SpamMsgActivity.this;
            if (activity != null && mProgressDialog != null) {
                mProgressDialog.show();
            }
        }

        @Override
        protected Long doInBackground(ArrayList<Integer>... params) {
            mMsgIdList = params[0];
            deleteMsg(mMsgIdList);
            return null;
        }
    }

    @Override
    protected void onPause() {
        Log.v(TAG, "onPause()");
        super.onPause();
    }

    @Override
    protected void onResume() {
        Log.v(TAG, "onResume()");
        super.onResume();
        mSpamMsgUtils = SpamMsgUtils.getInstance(this.getApplicationContext());
        if (mNeedDelBurnMsgId != 0) {
            ArrayList<Integer> delBurnMsgId = new ArrayList<Integer>();
            delBurnMsgId.add(mNeedDelBurnMsgId);
            deleteMsg(delBurnMsgId);
            mNeedDelBurnMsgId = 0;
        }
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy()");
        super.onDestroy();
        if (mPortraitService != null && mPortraitUpdateListener != null) {
            mPortraitService.removeListener(mPortraitUpdateListener);
        }
        this.getContentResolver().unregisterContentObserver(mSpamDdObserver);
    }

    @Override
    protected void onStart() {
        Log.v(TAG, "onStart()");
        super.onStart();
        ActionBar actionBar = getActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setTitle(R.string.my_spam);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case android.R.id.home:
            finish();
            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }

    public boolean isInActionMode() {
        return mIsInActionMode;
    }

    @Override
    protected void onStop() {
        Log.v(TAG, "onStop()");
        super.onStop();
    }

    private int mNeedDelBurnMsgId = 0;
    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        Log.v(TAG, "onListItemClick");
        super.onListItemClick(l, v, position, id);
        Cursor cursor = (Cursor) l.getItemAtPosition(position);
        int type = cursor.getInt(cursor.getColumnIndex(Constants.COLUMN_NAME_TYPE));
        String path = cursor.getString(cursor.getColumnIndex(Constants.COLUMN_NAME_BODY));
        // if not downloaded emoticon message, return
        if (type == Constants.Type.TYPE_EMOTICON_MSG) {
            String emId = EmojiShop.getEmIdByXml(path);
            if (!EmojiShop.isLocalEmoticon(emId)) {
                return;
            }
        }

        int msgId = cursor.getInt(cursor.getColumnIndex(Constants.COLUMN_NAME_ID));

        if (mActionMode == null) {
            if (Constants.Type.TYPE_MMS_PUSH == type && path == null) {
                Toast.makeText(SpamMsgActivity.this, R.string.no_open_push,
                        Toast.LENGTH_SHORT).show();
                return;
            }
            Intent intent = getClickIntent(cursor);
            if (intent == null) {
                Log.v(TAG, "Cant open unDownLoad file, show toase");
                Toast.makeText(this, R.string.cant_open_ftNotif,
                        Toast.LENGTH_SHORT).show();
                return;
            }
            try {
                startActivity(intent);
                if (type == Constants.Type.TYPE_IP_BURN_FT_MSG
                       || type == Constants.Type.TYPE_IP_BURN_TEXT_MSG) {
                    mNeedDelBurnMsgId = msgId;
                }
            } catch (ActivityNotFoundException e) {
                Log.d(TAG, "ActivityNotFoundException");
                e.printStackTrace();
                Toast.makeText(this, R.string.cant_open_item,
                        Toast.LENGTH_SHORT).show();
                mNeedDelBurnMsgId = 0;
            }
        } else if (mCarryMoreActionMode != null) {
            Log.v(TAG, "onListItemClick, (mCarryMoreActionMode != null");
            mCarryMoreActionMode.setItemChecked(v, position,
                    !mCheckedItemIds.contains(msgId));
        }
    }

    private void deleteCheckItem(HashSet<Integer> checkedItemIds) {
        ArrayList<Integer> delItemIds = new ArrayList<Integer>();
        for(int id : checkedItemIds) {
            delItemIds.add(id);
        }
        Log.d(TAG, "deleteCheckItem checkedItemIds size = " + checkedItemIds.size());
        Log.d(TAG, "deleteCheckItem delItemIds size = " + delItemIds.size());
        DeleteCheckItemTask deleteTask = new DeleteCheckItemTask();
        deleteTask.execute(delItemIds);
    }

    public void deleteMsg(ArrayList<Integer> msgIdList) {
        Log.v(TAG, "deleteMsg");
        // delete spam
        String selectArg = "_id IN (";
        for (int msgId : msgIdList) {
            selectArg = selectArg + msgId + ",";
        }
        selectArg = selectArg.substring(0, selectArg.length() - 1);
        selectArg = selectArg + ")";
        Log.d(TAG, "selectArg = " + selectArg);
        int count = this.getContentResolver().delete(SPAM_URI, selectArg, null);
        Log.d(TAG, "have delete count = " + count);
    }

    private void restoreCheckItem(HashSet<Integer> checkedItemIds) {
        ArrayList<Integer> restoreItemIds = new ArrayList<Integer>();
        for(int id : checkedItemIds) {
            restoreItemIds.add(id);
        }
        Log.d(TAG, "deleteCheckItem checkedItemIds size = " + checkedItemIds.size());
        Log.d(TAG, "deleteCheckItem delItemIds size = " + restoreItemIds.size());

        Log.d(TAG, "restoreCheckItem restoreItemIds size = " + restoreItemIds.size());
        RestoreCheckItemTask restoreTask = new RestoreCheckItemTask();
        restoreTask.execute(restoreItemIds);
    }

    private void restoreSpamMsg(ArrayList<Integer> msgIdList) {
        Log.e(TAG, "restoreSpamMsg begin");
        if (mSpamMsgUtils == null) {
            Log.e(TAG, "restoreSpamMsg, mSpamMsgUtils == null return");
            return;
        }
        for (Integer msgId : msgIdList) {
            mSpamMsgUtils.restoreMessage(msgId);
        }
    }

    /**
     * When user click "more" menuItem on context menu, will open this action
     * mode. It used to select item that want to operator.
     */
    private class CarrymoreActionMode implements ActionMode.Callback {

        private int mCheckedCount;
        private ActionMode mMode;

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            switch (item.getItemId()) {
            case R.id.select_all:
                setAllItemChecked(true);
                break;

            case R.id.cancel_select:
                setAllItemChecked(false);
                break;

            case R.id.spam_restore:
                if (mCheckedCount == 0) {
                    Toast.makeText(SpamMsgActivity.this, R.string.no_item_selected,
                            Toast.LENGTH_SHORT).show();
                } else {
                    restoreCheckItem(mCheckedItemIds);
                    mMode.finish();
                }
                break;

            case R.id.spam_delete:
                if (mCheckedCount == 0) {
                    Toast.makeText(SpamMsgActivity.this, R.string.no_item_selected,
                            Toast.LENGTH_SHORT).show();
                } else {
                    deleteCheckItem(mCheckedItemIds);
                    mMode.finish();
                }
                break;

            default:
                break;
            }
            return false;
        }

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            Log.v(TAG, "onCreateActionMode");
            mIsInActionMode = true;
            mMode = mode;
            mMode.setTitleOptionalHint(false);
            mListView.setLongClickable(false);
            MenuInflater inflater = getMenuInflater();
            inflater.inflate(R.menu.rst_del_select_menu, menu);
            mCheckedItemIds = new HashSet<Integer>();
            setAllItemChecked(false);
            updateTitle();
            return true;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            mIsInActionMode = false;
            Log.v(TAG, "onDestroyActionMode");
            mCheckedItemIds = null;
            mCheckedCount = 0;
            mActionMode = null;
            mListView.setLongClickable(true);
            mListAdapter.notifyDataSetChanged();
        }

        private void updateTitle() {
            StringBuilder builder = new StringBuilder();
            builder.append(mCheckedCount);
            builder.append(" ");
            builder.append(getString(R.string.selected));
            mMode.setTitle(builder.toString());
        }

        public void setItemChecked(View view, int position, final boolean checked) {
            Log.v(TAG, "setItemChecked position = " + position);
            Log.v(TAG, "setItemChecked checked = " + checked);

            Cursor cursor = (Cursor) mListView.getItemAtPosition(position);
            int msgId = cursor.getInt(cursor.getColumnIndex(Constants.COLUMN_NAME_ID));

            boolean isChecked = mCheckedItemIds.contains(msgId);
            if (isChecked != checked) {
                CheckBox checkbox = (CheckBox) view.findViewById(R.id.checkbox);
                checkbox.setChecked(checked);
                if (checked) {
                    mCheckedItemIds.add(msgId);
                    mCheckedCount++;
                } else {
                    mCheckedItemIds.remove(msgId);
                    mCheckedCount--;
                }
            } else {
                Log.v(TAG, "onCreateActionMode setItemChecked (data.isChecked() == checked)");
            }
            updateTitle();
        }

        private void setAllItemChecked(boolean checked) {
            mCheckedCount = 0;
            mCheckedItemIds.clear();
            ListAdapter adapter = mListView.getAdapter();
            if (adapter == null) {
                Log.e(TAG, "show Check box error, adapter null");
                return;
            }

            if (checked) {
                String[] cols = new String[] { Constants.COLUMN_NAME_ID };
                Cursor cursor = mContext.getContentResolver().query(SPAM_URI, cols, null, null,
                        null);
                cursor.moveToFirst();
                while (!cursor.isAfterLast()) {
                    int msgId = cursor.getInt(cursor.getColumnIndex(Constants.COLUMN_NAME_ID));
                    mCheckedItemIds.add(msgId);
                    mCheckedCount++;
                    cursor.moveToNext();
                }
                cursor.close();
            }
            updateTitle();
            mListAdapter.notifyDataSetChanged();
        }

        public void saveState(final Bundle outState) {
            ArrayList<Integer> list = new ArrayList<Integer>();
            for (Integer item : mCheckedItemIds) {
                list.add(item);
            }
            outState.putIntegerArrayList(STATE_CHECKED_ITEMS, list);
        }

        public void restoreState(Bundle state) {
            mCheckedItemIds.clear();
            mCheckedCount = 0;

            ArrayList<Integer> list = state.getIntegerArrayList(STATE_CHECKED_ITEMS);
            if (list != null && !list.isEmpty()) {
                for (Integer item : list) {
                    mCheckedItemIds.add(item);
                }
            }
            ListAdapter adapter = mListView.getAdapter();
            if (adapter == null) {
                Log.e(TAG, "show Check box error, adapter null");
                return;
            }
            if (adapter.getCount() > 0) {
                confirmSyncCheckedPositons();
            }
        }

        /**
         * after refreshed, must sync witch mCheckedItemIds.
         */
        public void confirmSyncCheckedPositons() {
            ListAdapter adapter = mListView.getAdapter();
            if (adapter == null) {
                Log.e(TAG, "show Check box error, adapter null");
                return;
            }
            int count = adapter.getCount();
            for (int position = 0; position < count; position++) {
                SpamDataItem item = (SpamDataItem) adapter.getItem(position);
                View view = mListView.getChildAt(position);
                CheckBox checkbox = (CheckBox) view.findViewById(R.id.checkbox);

                if (mCheckedItemIds.contains(item.getMsgId())) {
                    mCheckedCount++;
                    checkbox.setChecked(true);
                } else {
                    checkbox.setChecked(false);
                }
            }
            updateTitle();
            mListAdapter.notifyDataSetChanged();
        }

        @Override
        public boolean onPrepareActionMode(ActionMode arg0, Menu arg1) {
            mIsInActionMode = true;
            return false;
        }

    }

    /**
     * Used to restore selected spam messages.
     */
    private class RestoreCheckItemTask extends AsyncTask<ArrayList<Integer>, String, Long> {
        private ProgressDialog mRestoreDialog;
        private ArrayList<Integer> mMsgIdList;

        public RestoreCheckItemTask() {
            Log.d(TAG, "RestoreCheckItemTask");
            mRestoreDialog = new ProgressDialog(SpamMsgActivity.this);
            mRestoreDialog.setCancelable(false);
            mRestoreDialog.setMessage(getString(R.string.restore_please_wait));
            mRestoreDialog.setIndeterminate(true);
        }

        @Override
        protected void onPostExecute(Long arg0) {
            super.onPostExecute(arg0);
            Activity activity = SpamMsgActivity.this;
            if (activity != null && mRestoreDialog != null) {
                mRestoreDialog.dismiss();
            }
            mListAdapter.notifyDataSetChanged();
        }

        @Override
        protected void onPreExecute() {
            Activity activity = SpamMsgActivity.this;
            if (activity != null && mRestoreDialog != null) {
                mRestoreDialog.show();
            }
        }

        @Override
        protected Long doInBackground(ArrayList<Integer>... params) {
            mMsgIdList = params[0];
            restoreSpamMsg(mMsgIdList);
            return null;
        }
    }

    /**
     * @param data
     *            the item will be query.
     * @return the item check status.
     */
    public boolean isItemByChecked(SpamDataItem data) {
        if (mCheckedItemIds != null) {
            if (mCheckedItemIds.contains(data.getMsgId())) {
                Log.i(TAG, "isItemByChecked TURE");
                return true;
            }
        } else {
            Log.e(TAG, "isItemByChecked mCheckedItemIds == null");
        }
        return false;
    }

    private class SpamCursorAdapter extends CursorAdapter {
        private LayoutInflater mLayoutInflater;
        @Override
        public void changeCursor(Cursor cursor) {
            super.changeCursor(cursor);
            Log.d(TAG, "change new cursor");
        }

        public SpamCursorAdapter(Context context, Cursor c, boolean autoRequery) {
            super(context, c, autoRequery);
            mLayoutInflater = LayoutInflater.from(context);
            Log.d(TAG, "SpamCursorAdapter");
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            Log.d(TAG, "bindView");
            TextView content = (TextView) view.findViewById(R.id.content);
            ImageView avatar = (ImageView) view.findViewById(R.id.avatar);
            TextView from = (TextView) view.findViewById(R.id.from);
            TextView date = (TextView) view.findViewById(R.id.date);
            CheckBox checkbox = (CheckBox) view.findViewById(R.id.checkbox);

            int msgId = cursor.getInt(cursor.getColumnIndex(Constants.COLUMN_NAME_ID));
            String address = cursor.getString(cursor.getColumnIndex(Constants.COLUMN_NAME_ADDRESS));
            SpamDataItem data = new SpamDataItem(context, cursor);
            data.initData(cursor, mPortraitService);

            int msgType = cursor.getInt(cursor.getColumnIndex(Constants.COLUMN_NAME_TYPE));
            if (msgType == Constants.Type.TYPE_IP_BURN_FT_MSG
                    || msgType == Constants.Type.TYPE_IP_BURN_TEXT_MSG) {
                content.setBackgroundResource(R.drawable.ic_ipbar_show_mail);
                content.setText(""); //burn msg, only show burn icon,don't show real content
            } else {
                content.setBackgroundDrawable(null); //if normal msg, don't show burn mail icon
                String contentStr = data.getTypeData().getContent();
                CharSequence body = null;
                if (contentStr != null) {
                    if (msgType == Constants.Type.TYPE_EMOTICON_MSG) {
                        body = EmojiShop.parseEmSmsString(contentStr);
                    } else {
                        EmojiImpl emojiImpl = EmojiImpl.getInstance(context);
                        if (emojiImpl.hasAnyEmojiExpression(contentStr)) {
                            body = emojiImpl.getEmojiExpression(contentStr, true);
                        } else {
                            body = contentStr;
                        }
                    }
                }
                content.setText(body);
            }
            view.setTag(address);
            avatar.setImageBitmap(data.getImage());
            from.setText(data.getFrom());
            String time = RcsMessageUtils.formatTimeStampStringExtend(context, data.getDate());
            date.setText(time);

            boolean isInActionMode = isInActionMode();
            Log.i(TAG, "getView  isInActionMode = + " + isInActionMode);
            if (isInActionMode) {
                checkbox.setVisibility(View.VISIBLE);
                if (isItemByChecked(data)) {
                    checkbox.setChecked(true);
                } else {
                    checkbox.setChecked(false);
                }
            } else {
                checkbox.setVisibility(View.INVISIBLE);
            }
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            Log.d(TAG, "newView");
            return mLayoutInflater.inflate(R.layout.spam_view_list_item, parent, false);
        }

    }

    /**
     * if spam database has new data insert, then ui will load data from
     * database again.
     */
    private final class SpamDdObserver extends ContentObserver {

        public SpamDdObserver(Handler handler) {
            super(handler);
        }

        @Override
        public void onChange(boolean selfChange) {
            Log.d(TAG, "SpamDdObserver onChange");
            Cursor cursor = mContext.getContentResolver()
                    .query(SPAM_URI, null, null, null, "date DESC");
            cursor.moveToFirst();
            mListAdapter.changeCursor(cursor);
        }
    }

    /**
     * Implement the portrait updata listener. Adapter will updata the info when
     * the contact name or image is change.
     */
    private class PortraitUpdateListener implements UpdateListener {
        public void onPortraitUpdate(Portrait p, String chatId) {
            Log.d(TAG, "PortraitUpdateListener. onPortraitUpdate");
            String number = p.mNumber;
            Log.d(TAG, "PortraitUpdateListener. onPortraitUpdate number = " + number);
            if (mListView == null) {
                return;
            }

            int count = mListView.getCount();
            Log.d(TAG, "PortraitUpdateListener. onPortraitUpdate count = " + count);
            for (int index = 0; index < count; index++) {
                View view = mListView.getChildAt(index);
                if (view != null) {
                    String address = (String) view.getTag();
                    if (address != null && address.equals(number)) {
                        ImageView avatar = (ImageView) view.findViewById(R.id.avatar);
                        TextView from = (TextView) view.findViewById(R.id.from);
                        avatar.setImageBitmap(mPortraitService.decodeString(p.mImage));
                        from.setText(p.mName);
                        Log.d(TAG, "PortraitUpdateListener. onPortraitUpdate mName  = " +  p.mName);
                    }
                } else {
                    return;
                }
            }
        }

        public void onGroupUpdate(String chatId, Set<String> numberSet) {
            Log.d(TAG, "PortraitUpdateListener. onGroupUpdate");
        }

        // will pass the result to caller
        public void onGroupThumbnailUpdate(String chatId, Bitmap thumbnail) {
            Log.d(TAG, "PortraitUpdateListener. onGroupThumbnailUpdate() ");
        }
    }

    private Intent getClickIntent(Cursor cursor) {
        int type = cursor.getInt(cursor.getColumnIndex(Constants.COLUMN_NAME_TYPE));
        String path = cursor.getString(cursor.getColumnIndex(Constants.COLUMN_NAME_BODY));

        switch (type) {
        case Constants.Type.TYPE_SMS:
            Log.d(TAG, "getClickIntent SMS");
            Intent intent = new Intent();
            intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
            String body = cursor.getString(cursor.getColumnIndex(Constants.COLUMN_NAME_BODY));
            intent.putExtra("path", body);
            intent.putExtra("fav_spam", true);
            intent.putExtra("type", 0);
            intent.setAction("com.mediatek.rcs.message.ui.RcsIpMsgContentShowActivity");
            return intent;

        case Constants.Type.TYPE_EMOTICON_MSG:
            Intent vemoticonIntent = new Intent();
            vemoticonIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
            vemoticonIntent.putExtra("path", cursor.getString(cursor
                    .getColumnIndex(Constants.COLUMN_NAME_BODY)));
            vemoticonIntent.putExtra("fav_spam", true);
            vemoticonIntent.putExtra("type", Constants.Type.TYPE_EMOTICON_MSG);
            vemoticonIntent
                    .setAction("com.mediatek.rcs.message.ui.RcsIpMsgContentShowActivity");
            return vemoticonIntent;

        case Constants.Type.TYPE_IP_BURN_TEXT_MSG:
            Log.d(TAG, "getClickIntent burn text message");
            Intent burnTextintent = new Intent();
            burnTextintent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
            String burnTextBody = cursor.getString(
                                cursor.getColumnIndex(Constants.COLUMN_NAME_BODY));
            burnTextintent.putExtra("path", burnTextBody);
            burnTextintent.putExtra("fav_spam", true);
            burnTextintent.putExtra("type", 0);
            burnTextintent.setAction("com.mediatek.rcs.message.ui.RcsIpMsgContentShowActivity");
            return burnTextintent;

        case Constants.Type.TYPE_IP_TEXT_MSG:
            Log.d(TAG, "getClickIntent TEXT_IPMSG");
            Intent ipTextIntent = new Intent();
            ipTextIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
            String ipBody = cursor.getString(cursor.getColumnIndex(Constants.COLUMN_NAME_BODY));
            ipTextIntent.putExtra("path", ipBody);
            ipTextIntent.putExtra("fav_spam", true);
            ipTextIntent.putExtra("type", 0);
            ipTextIntent.setAction("com.mediatek.rcs.message.ui.RcsIpMsgContentShowActivity");
            return ipTextIntent;

        case Constants.Type.TYPE_MMS_PUSH:
            Log.d(TAG, "getClickIntent mms");
            Intent mmsIntent = new Intent();
            return mmsIntent;

        case Constants.Type.TYPE_IP_BURN_FT_MSG:
        case Constants.Type.TYPE_IP_FT_MSG:
            if (path == null) {
                Log.d(TAG, "getClickIntent path = null, return intent null");
                return null;
            }

            File ftFile = new File(path);
            if (ftFile == null || !ftFile.exists()) {
                Log.d(TAG, "getClickIntent file = null or file not exits, return intent null");
                return null;
            }

            String ct = analysisFileType(path);
            Log.d(TAG, "getClickIntent ct = " + ct);
            if (ct != null) {
                if (ct.equals(Constants.CT_TYPE_VEDIO)) {
                    Log.d(TAG, "getClickIntent new Video");
                    Intent vidioIntent = new Intent();
                    vidioIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    vidioIntent.putExtra("path", path);
                    vidioIntent.putExtra("fav_spam", true);
                    vidioIntent.putExtra("type", 9);
                    vidioIntent.setAction("com.mediatek.rcs.message.ui.RcsIpMsgContentShowActivity");
                    return vidioIntent;
                } else if (ct.equals(Constants.CT_TYPE_AUDIO)) {
                    Log.d(TAG, "getClickIntent audio");
                    Intent audioIntent = new Intent(Intent.ACTION_VIEW);
                    File file = new File(path);
                    Uri audioUri = Uri.fromFile(file);
                    audioIntent.setDataAndType(audioUri, "audio/amr");
                    return audioIntent;
                } else if (ct.equals(Constants.CT_TYPE_IMAGE)) {
                    Log.d(TAG, "getClickIntentnew PictureData()");
                    Intent imageIntent = new Intent();
                    imageIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    imageIntent.putExtra("path", path);
                    imageIntent.putExtra("fav_spam", true);
                    imageIntent.putExtra("type", 4);
                    imageIntent.setAction("com.mediatek.rcs.message.ui.RcsIpMsgContentShowActivity");
                    return imageIntent;
                } else if (ct.equals(Constants.CT_TYPE_VCARD)) {
                    Log.d(TAG, "getClickIntentnew Vcard");
                    Intent vcardIntent = new Intent(Intent.ACTION_VIEW);
                    File file = new File(path);
                    Uri vcardUri = Uri.fromFile(file);
                    vcardIntent.setDataAndType(vcardUri, "text/x-vCard".toLowerCase());
                    vcardIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    return vcardIntent;
                } else if (ct.equals(Constants.CT_TYPE_GEOLOCATION)) {
                    Log.d(TAG, "getClickIntentnew GeolocationData()");
                    GeoLocXmlParser parser = GeoLocUtils.parseGeoLocXml(path);
                    double latitude = parser.getLatitude();
                    double longitude = parser.getLongitude();
                    Log.d(TAG, "parseGeoLocXml:latitude=" + latitude + ",longitude=" + longitude);
                    if (latitude != 0.0 || longitude != 0.0) {
                        Uri geoUri = Uri.parse("geo:" + latitude + "," + longitude);
                        Intent geoIntent = new Intent(Intent.ACTION_VIEW, geoUri);
                        return geoIntent;
                    } else {
                        Toast.makeText(this, this.getString(R.string.geoloc_map_failed),
                                Toast.LENGTH_SHORT).show();
                        return null;
                    }
                } else {
                    return null;
                }
            }

        default:
            Log.e(TAG, "unknown type = " + type);
            return null;
        }
    }

    private String analysisFileType(String filePath) {
        if (filePath != null) {
            String mimeType = MediaFile.getMimeTypeForFile(filePath);
            if (mimeType == null) {
                mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(
                        RCSUtils.getFileExtension(filePath));
            }
            if (mimeType != null) {
                if (mimeType.contains(Constants.FILE_TYPE_IMAGE)) {
                    return Constants.CT_TYPE_IMAGE;
                } else if (mimeType.contains(Constants.FILE_TYPE_AUDIO)
                        || mimeType.contains("application/ogg")) {
                    return Constants.CT_TYPE_AUDIO;
                } else if (mimeType.contains(Constants.FILE_TYPE_VIDEO)) {
                    return Constants.CT_TYPE_VEDIO;
                } else if (filePath.toLowerCase().endsWith(".vcf")) {
                    return Constants.CT_TYPE_VCARD;
                } else if (filePath.toLowerCase().endsWith(".xml")) {
                    return Constants.CT_TYPE_GEOLOCATION;
                } else {
                    Log.d(TAG, "analysisFileType() other type add here!");
                }
            }
        } else {
            Log.w(TAG, "analysisFileType(), file name is null!");
        }
        return null;
    }

}
