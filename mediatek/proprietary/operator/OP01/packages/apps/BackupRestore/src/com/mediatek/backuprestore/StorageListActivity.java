package com.mediatek.backuprestore;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.mediatek.backuprestore.utils.Constants;
import com.mediatek.backuprestore.utils.MyLogger;
import com.mediatek.backuprestore.utils.SDCardUtils;
import com.mediatek.storage.StorageManagerEx;

import android.app.Activity;
import android.app.ListActivity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.storage.StorageManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.os.storage.StorageVolume;

public class StorageListActivity extends ListActivity {

    private static final String CLASS_TAG = MyLogger.LOG_TAG + "/StorageListActivity";
    private StorageManager mStorageManager = null;
    ArrayList<StorageData> mDirectoryEntries = new ArrayList<StorageData>();
    private File currentDirectory = null;
    StorageAdapter mStorageAdapter;
    public static final String ROOT_PATH = "/storage";
    FileDataTask fileDataTask = null;
    private String mRootPath = "Root Path";
    StorageListSDCardStatusChangedListener mStorageListSDCardStatusChangedListener = new StorageListSDCardStatusChangedListener();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(android.R.layout.list_content);
        initActionBar();
        initAdapter();
        registerSDCardListener();
    }

    public void onDestroy() {
        unRegisterSDCardListener();
        super.onDestroy();
    }

    private void initAdapter() {
        mRootPath = ROOT_PATH;
        currentDirectory = new File(mRootPath);
        mDirectoryEntries.clear();
        initActionBar();
        mStorageManager = (StorageManager) this.getSystemService(Context.STORAGE_SERVICE);
        StorageVolume[] storageVolumeList = mStorageManager.getVolumeList();
        for (StorageVolume volume : storageVolumeList) {
            StorageData storageData = new StorageData();
            if (!StorageManagerEx.isUSBOTG(volume.getPath())) {
                storageData.mPath = volume.getPath();
                storageData.mDescription = volume.getDescription(this);
                if (volume.isRemovable()) {
                    storageData.mFolderIcon = this.getResources().getDrawable(R.drawable.sdcard);
                } else {
                    storageData.mFolderIcon = this.getResources().getDrawable(
                            R.drawable.phone_storage);
                }
                storageData.mPathIcon = this.getResources().getDrawable(R.drawable.ic_menu_forward);
                /*
                 * File initFile = new File(storageData.mPath); if (initFile
                 * !=null && initFile.listFiles() != null) { for(File subFile :
                 * initFile.listFiles()) { if (subFile.isDirectory()) {
                 * storageData.hasFolder = true; break; } else {
                 * storageData.hasFolder = false; } } } else {
                 * storageData.hasFolder = false; }
                 */

                if (isMounted(volume.getPath())) {
                    mDirectoryEntries.add(storageData);
                }

            }
        }
        mStorageAdapter = new StorageAdapter(this);
        mStorageAdapter.setItemData(mDirectoryEntries);
        // getListView().setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
        getListView().setAdapter(mStorageAdapter);
        // getListView().setDividerHeight(0);
    }

    private void initActionBar() {
        this.getActionBar().setTitle(R.string.storage_path);
    }

    public class StorageAdapter extends BaseAdapter {

        private Context mContext = null;
        private List<StorageData> mItems = new ArrayList<StorageData>();
        private LayoutInflater mInflater;

        public StorageAdapter(Context context) {
            mContext = context;
            mInflater = LayoutInflater.from(context);
        }

        public void setItemData(ArrayList<StorageData> directoryEntries) {
            mItems = directoryEntries;
        }

        @Override
        public int getCount() {
            return mItems.size();
        }

        @Override
        public Object getItem(int position) {
            return mItems.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = convertView;
            if (view == null) {
                view = mInflater.inflate(R.layout.storage_list_item, parent, false);
            }
            ImageView folderImage = (ImageView) view.findViewById(R.id.folder_image);
            TextView folderName = (TextView) view.findViewById(R.id.item_text);
            ImageView pathImage = (ImageView) view.findViewById(R.id.path_image);
            RelativeLayout pathSet = (RelativeLayout) view.findViewById(R.id.path_set);

            final StorageData itemStorage = mItems.get(position);
            folderImage.setImageDrawable(itemStorage.mFolderIcon);
            folderName.setText(itemStorage.mDescription);
            pathImage.setImageDrawable(itemStorage.mPathIcon);
            final String selectPath = itemStorage.mPath;
            float alpha = itemStorage.hasFolder ? Constants.ENABLE_ALPHA : Constants.DISABLE_ALPHA;
            pathImage.setAlpha(alpha);
            pathImage.setEnabled(itemStorage.hasFolder);
            // folderName.setAlpha(alpha);

            pathImage.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (fileDataTask == null
                            || (fileDataTask != null && fileDataTask.getStatus().equals(
                                    AsyncTask.Status.FINISHED))) {
                        File selectFile = new File(selectPath);
                        if (selectFile != null) {
                            browseTo(selectFile);
                        }
                    }
                }
            });

            pathSet.setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View v) {
                    Log.d(CLASS_TAG, "onClick : selectPath = " + selectPath);
                    StorageSettingsActivity.setCurrentPath(mContext,
                            SDCardUtils.getStoragePath(selectPath));
                    StorageSettingsActivity.setPathIndexKey(mContext, Constants.CUSTOMIZE_STOTAGE);
                    StorageListActivity.this.finish();
                }
            });

            return view;
        }

    }

    private void browseTo(File selectFile) {
        if (selectFile.isDirectory()) {
            this.currentDirectory = selectFile;
            fileDataTask = new FileDataTask(selectFile, this);
            fileDataTask.execute();
            Log.d(CLASS_TAG, "browseTo : currentDirectory = " + selectFile.getAbsolutePath());
        }

    }

    private void fill(File[] listFiles) {
        if (listFiles != null) {
            mDirectoryEntries.clear();
            for (File currentFile : listFiles) {
                if (currentFile.isDirectory()) {
                    StorageData storageData = new StorageData();
                    storageData.mDescription = currentFile.getName();
                    storageData.mPath = currentFile.getAbsolutePath();
                    storageData.mFolderIcon = this.getResources().getDrawable(
                            R.drawable.ic_storage_folder);
                    storageData.mPathIcon = this.getResources().getDrawable(
                            R.drawable.ic_menu_forward);
                    /*
                     * File[] tempList = currentFile.listFiles();
                     * storageData.hasFolder = false; if (tempList != null) {
                     * for(File hasFile : tempList) { if (hasFile.isDirectory())
                     * { storageData.hasFolder = true; Log.d(CLASS_TAG,
                     * "fill : currentDirectory = "+hasFile.getAbsolutePath());
                     * break; } } }
                     */
                    mDirectoryEntries.add(storageData);
                }
            }
        } else {
            Toast.makeText(this, "the folder path is empty", Toast.LENGTH_SHORT).show();
        }

    }

    @Override
    public void onBackPressed() {
        boolean matched = false;
        mStorageManager = (StorageManager) this.getSystemService(Context.STORAGE_SERVICE);
        StorageVolume[] storageVolumeList = mStorageManager.getVolumeList();
        for (StorageVolume volume : storageVolumeList) {
            if (currentDirectory.getAbsolutePath().matches(volume.getPath())) {
                matched = true;
                break;
            }
        }
        if (currentDirectory != null && !this.isRootPath(currentDirectory.getAbsolutePath())) {
            Log.d(CLASS_TAG,
                    "onBackPressed : currentDirectory = " + currentDirectory.getAbsolutePath()
                            + "matched == " + matched);
            if (matched) {
                initAdapter();
            } else {
                this.browseTo(this.currentDirectory.getParentFile());
            }
        } else {
            Log.d(CLASS_TAG, "onBackPressed : press");
            super.onBackPressed();
        }
    }

    private static class StorageData {
        String mDescription;
        String mPath;
        Drawable mFolderIcon;
        Drawable mPathIcon;
        boolean hasFolder = true;
    }

    public boolean isRootPath(String path) {
        return mRootPath.equals(path);
    }

    public String getRootPath() {
        return mRootPath;
    }

    private void registerSDCardListener() {
        SDCardReceiver.getInstance().registerOnSDCardChangedListener(
                mStorageListSDCardStatusChangedListener);
    }

    private void unRegisterSDCardListener() {
        if (SDCardReceiver.getInstance() != null) {
            SDCardReceiver.getInstance().unRegisterOnSDCardChangedListener(
                    mStorageListSDCardStatusChangedListener);
        }
    }

    protected boolean isMounted(String mountPoint) {
        if (TextUtils.isEmpty(mountPoint)) {
            return false;
        }
        String state = null;

        state = mStorageManager.getVolumeState(mountPoint);
        return Environment.MEDIA_MOUNTED.equals(state);
    }

    class StorageListSDCardStatusChangedListener implements
            SDCardReceiver.OnSDCardStatusChangedListener {

        @Override
        public void onSDCardStatusChanged(boolean mount, String path) {
            if (SDCardUtils.isSupprotSDcard(getApplicationContext())) {
                initAdapter();
            }
        }
    }

    private class FileDataTask extends AsyncTask<Void, Void, Long> {

        File currentFile = null;
        Context mContext = null;

        FileDataTask(File file, Context context) {
            currentFile = file;
            mContext = context;
        }

        @Override
        protected void onPostExecute(Long result) {
            super.onPostExecute(result);
            if (currentFile != null) {
                getActionBar().setTitle(currentFile.getAbsolutePath());
            }
            // mStorageAdapter = new StorageAdapter(mContext);
            mStorageAdapter.setItemData(mDirectoryEntries);
            mStorageAdapter.notifyDataSetChanged();
            // getListView().setAdapter(mStorageAdapter);
        }

        @Override
        protected Long doInBackground(Void... arg0) {
            if (currentFile != null) {
                fill(currentFile.listFiles());
            }
            return null;
        }

    }

}
