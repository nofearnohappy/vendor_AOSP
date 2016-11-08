package com.mediatek.gallery3d.plugin;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.android.gallery3d.app.AbstractGalleryActivity;
import com.android.gallery3d.data.DataManager;
import com.android.gallery3d.data.MediaItem;
import com.android.gallery3d.data.Path;
import com.android.gallery3d.ui.ActionModeHandler;
import com.android.gallery3d.ui.SelectionManager;
import com.android.gallery3d.ui.SlotView;

import com.mediatek.common.PluginImpl;
import com.mediatek.gallery3d.ext.DefaultGalleryPickerExt;
import com.mediatek.galleryfeature.platform.PlatformHelper;
import com.mediatek.galleryframework.base.MediaData;
import com.mediatek.op01.plugin.R;

import java.util.ArrayList;

/**
 * OP01 plugin implementation of gallery picker feature.
 */
@PluginImpl(interfaceName = "com.mediatek.gallery3d.ext.IGalleryPickerExt")
public class Op01GalleryPickerExt extends DefaultGalleryPickerExt {
    private static final String TAG = "Gallery2/Op01GalleryPickerExt";

    public static final String EXTRA_PICKER_URIS = "com.mediatek.gallery3d.extra.PICKER_URIS";
    public static final String EXTRA_RCS_PICKER  = "com.mediatek.gallery3d.extra.RCS_PICKER";
    public static final String EXTRA_MAX_SELECTION_NUM =
                                             "com.mediatek.gallery3d.extra.MAX_SELECTION_NUM";

    public static final int DEFAULT_SELECTION_NUM = 1;
    public static final int MENU_ITEM_BASE_ID = 0xF0000000;
    public static final int MENU_ITEM_OK_ID   = MENU_ITEM_BASE_ID + 1;

    private AbstractGalleryActivity mActivity;
    private SelectionManager mSelectionManager;
    private Bundle mData;
    private boolean mRcsPicker = false;
    private Menu mMenu;
    private int mMaxSelectionNum = DEFAULT_SELECTION_NUM;
    private Context mContext;

    /**
     * @hide
     *
     * @param context context instance
     */
    public Op01GalleryPickerExt(Context context) {
        super();
        mContext = context;
    }

    @Override
    public ActionModeHandler onCreate(AbstractGalleryActivity activity, Bundle data,
            ActionModeHandler actionMode, SelectionManager selectMgr) {
        mActivity = activity;
        mData = data;
        mRcsPicker = data.getBoolean(EXTRA_RCS_PICKER, false);
        mMaxSelectionNum = data.getInt(EXTRA_MAX_SELECTION_NUM, DEFAULT_SELECTION_NUM);
        mSelectionManager = selectMgr;
        if (mRcsPicker) {
            // use dummy ActionModeHandler to ignore action mode callback
            actionMode = new Op01ActionModeHandler(mActivity, mSelectionManager);
        }
        Log.d(TAG, "onCreate mRcsPicker: " + mRcsPicker
              + ", mMaxSelectionNum: " + mMaxSelectionNum);
        return actionMode;
    }

    @Override
    public void onResume(SelectionManager selectMgr) {
        Log.d(TAG, "onResume mRcsPicker: " + mRcsPicker);
        if (mRcsPicker) {
            // need to set the selection manager again
            // if back from Container page, selection manager will wrong
            mSelectionManager = selectMgr;
            mSelectionManager.setAutoLeaveSelectionMode(false);
            mSelectionManager.enterSelectionMode();
            updateMenu();
        }
    }

    @Override
    public void onPause() {
        Log.d(TAG, "onPause mRcsPicker: " + mRcsPicker);
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy mRcsPicker: " + mRcsPicker);
    }

    @Override
    public void onCreateActionBar(Menu menu) {
        Log.d(TAG, "onCreateActionBar mRcsPicker: " + mRcsPicker + ", menu: " + menu);
        if (mRcsPicker) {
            mMenu = menu;
            menu.add(0, MENU_ITEM_OK_ID, 0, mContext.getString(R.string.select_ok));
            updateMenu();
        }
    }

    @Override
    public boolean onSingleTapUp(SlotView slotView, MediaItem item) {
        if (mRcsPicker) {
            if (item == null) {
                return false; // Item not ready yet, ignore the click
            }

            // Enter container page if single picker
            MediaData md = item.getMediaData();
            if ((mMaxSelectionNum == DEFAULT_SELECTION_NUM)
                && (md.mediaType == MediaData.MediaType.CONTAINER
                && md.subType == MediaData.SubType.CONSHOT)) {
                PlatformHelper.enterContainerPage(mActivity, md, true, mData);
                return true;
            }

            if (mSelectionManager.getSelectedCount() < mMaxSelectionNum ||
                mSelectionManager.isItemSelected(item.getPath())) {
                mSelectionManager.toggle(item.getPath());
                // refresh slotview
                slotView.invalidate();
            } else {
                Toast.makeText(mActivity,
                    mContext.getString(R.string.reach_max_select_num_hint, mMaxSelectionNum),
                    Toast.LENGTH_LONG).show();
            }

            Log.d(TAG, "onSingleTapUp path: " + item.getPath() + ", selected: " +
                       mSelectionManager.getSelected(true));

            // updatemenu state
            updateMenu();

            return true;
        }
        return false;
    }

    @Override
    public boolean onItemSelected(MenuItem item) {
        if (mRcsPicker && item.getItemId() == MENU_ITEM_OK_ID) {
            ArrayList<Path> selectPaths = mSelectionManager.getSelected(false);
            ArrayList<Uri> selectedUris = new ArrayList<Uri>();
            DataManager manager = mActivity.getDataManager();
            for (Path path : selectPaths) {
                if (manager.getMediaObject(path) != null) {
                    selectedUris.add(manager.getMediaObject(path).getContentUri());
                }
            }

            Log.d(TAG, "onItemSelected, size: " + selectedUris.size() + ", uri: " + selectedUris);

            if (mMaxSelectionNum == DEFAULT_SELECTION_NUM) {
                // Single picker
                Intent intent = new Intent(null, selectedUris.get(0))
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                mActivity.setResult(Activity.RESULT_OK, intent);
            } else {
                // Multi picker
                Intent intent = new Intent().addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                intent.putExtra(Intent.EXTRA_STREAM, selectedUris);
                mActivity.setResult(Activity.RESULT_OK, intent);
            }
            mActivity.finish();
            return true;
        }
        return false;
    }

    private void updateMenu() {
        if (mMenu == null) {
            Log.d(TAG, "mMenu not initialized");
            return;
        }
        MenuItem item = mMenu.findItem(MENU_ITEM_OK_ID);
        if (item != null) {
            item.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
            item.setEnabled(mSelectionManager.getSelectedCount() > 0 ? true : false);
        } else {
            Log.d(TAG, "Can't find OK menu item");
        }

        // hide cancel menu
        item = mMenu.findItem(com.android.gallery3d.R.id.action_cancel);
        if (item != null) {
            item.setVisible(false);
        }
    }

    /**
     * @hide
     */
    private class Op01ActionModeHandler extends ActionModeHandler {
        private static final String TAG = "Gallery2/Op01ActionModeHandler";

        public Op01ActionModeHandler(
                AbstractGalleryActivity activity, SelectionManager selectionManager) {
            super(activity, selectionManager);
        }

        @Override
        public void startActionMode() {
        }

        @Override
        public void finishActionMode() {
        }

        @Override
        public void setTitle(String title) {
        }

        @Override
        public void updateSelectionMenu() {
        }

        @Override
        public void updateSupportedOperation() {
        }

        @Override
        public void resume() {
        }

        @Override
        public void pause() {
        }

        @Override
        public void destroy() {
        }
    }
}
