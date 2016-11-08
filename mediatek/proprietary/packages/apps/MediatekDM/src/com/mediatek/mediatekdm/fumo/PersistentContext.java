package com.mediatek.mediatekdm.fumo;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.mediatek.mediatekdm.DmApplication;
import com.mediatek.mediatekdm.DmConst.TAG;
import com.mediatek.mediatekdm.mdm.DownloadDescriptor;

public class PersistentContext {
    // use 2 files to make recoverable from power lost.
    private final StateValues mValues;
    private final StateValues mValuesBackup;

    /* Use lazy-loading instance in place of double checked lock */
    private static class LazyHolder {
        private static final PersistentContext INSTANCE = new PersistentContext();
    }

    private PersistentContext() {
        Context context = DmApplication.getInstance();
        mValues = new StateValues(context, "dm_values");
        mValues.load();
        mValuesBackup = new StateValues(context, "dm_values_bak");
        mValuesBackup.load();
        int state = getFumoState();
        if (state == DmFumoState.NEW_VERSION_FOUND || state == DmFumoState.DOWNLOADING) {
            Log.d(TAG.FUMO,
                    "Reset FUMO state to DOWNLOAD_PAUSED for NEW_VERSION_FOUND and DOWNLOADING.");
            setFumoState(DmFumoState.DOWNLOAD_PAUSED);
        }
        Log.d(TAG.FUMO, "Initial FUMO state is " + state);
    }

    public static PersistentContext getInstance() {
        return LazyHolder.INSTANCE;
    }

    public long getDownloadedSize() {
        long dlSize = 0L;

        mValues.load();
        String value = mValues.get(StateValues.ST_DOWNLOADED_SIZE);
        if (!TextUtils.isEmpty(value)) {
            dlSize = Long.valueOf(value);
        } else {
            mValuesBackup.load();
            value = mValuesBackup.get(StateValues.ST_DOWNLOADED_SIZE);
            if (!TextUtils.isEmpty(value)) {
                dlSize = Long.valueOf(value);
            }
        }

        Log.d(TAG.COMMON, "[persistent]:DOWNLOADED_SIZE->get=" + dlSize);
        return dlSize;
    }

    public void setDownloadedSize(long size) {
        String dlSize = Long.toString(size);
        Log.d(TAG.COMMON, "[persistent]:DOWNLOADED_SIZE->set=" + dlSize);

        mValues.put(StateValues.ST_DOWNLOADED_SIZE, dlSize);
        mValues.commit();

        mValuesBackup.put(StateValues.ST_DOWNLOADED_SIZE, dlSize);
        mValuesBackup.commit();
    }

    public long getTotalSize() {
        long size = 0L;

        mValues.load();
        String value = mValues.get(StateValues.DD_SIZE);
        if (!TextUtils.isEmpty(value)) {
            size = Long.valueOf(value);
        } else {
            mValuesBackup.load();
            value = mValuesBackup.get(StateValues.DD_SIZE);
            if (!TextUtils.isEmpty(value)) {
                size = Long.valueOf(value);
            }
        }
        Log.d(TAG.COMMON, "[persistent]:DD_SIZE->get=" + size);
        return size;
    }

    /**
     * Default value is MSG_NETWORKERROR.
     */
    public int getFumoState() {
        // default session state value for DM, will check network in first step.
        int state = DmFumoState.IDLE;

        mValues.load();
        String value = mValues.get(StateValues.ST_STATE);
        if (!TextUtils.isEmpty(value)) {
            state = Integer.valueOf(value);
        } else {
            mValuesBackup.load();
            value = mValuesBackup.get(StateValues.ST_STATE);
            if (!TextUtils.isEmpty(value)) {
                state = Integer.valueOf(value);
            } else {
                Log.w(TAG.COMMON, "[persistent]:DL_STATE->DEFAULT(network)");
            }
        }
        Log.d(TAG.COMMON, "[persistent]:DL_STATE->get=" + state);
        return state;
    }

    // Store status to persistent storage.
    public void setFumoState(int status) {
        String state = Integer.toString(status);
        Log.d(TAG.COMMON, "[persistent]:DL_STATE->set=" + state);

        mValues.put(StateValues.ST_STATE, state);
        mValues.commit();

        mValuesBackup.put(StateValues.ST_STATE, state);
        mValuesBackup.commit();
    }

    public DownloadDescriptor getDownloadDescriptor() {
        DownloadDescriptor dd = new DownloadDescriptor();

        mValues.load();
        dd.field[0] = mValues.get(StateValues.DD_FIELD0);
        dd.field[1] = mValues.get(StateValues.DD_FIELD1);
        dd.field[2] = mValues.get(StateValues.DD_FIELD2);
        dd.field[3] = mValues.get(StateValues.DD_FIELD3);
        dd.field[4] = mValues.get(StateValues.DD_FIELD4);
        dd.field[5] = mValues.get(StateValues.DD_FIELD5);
        dd.field[6] = mValues.get(StateValues.DD_FIELD6);
        dd.field[7] = mValues.get(StateValues.DD_FIELD7);
        dd.field[8] = mValues.get(StateValues.DD_FIELD8);
        dd.field[9] = mValues.get(StateValues.DD_FIELD9);
        dd.field[10] = mValues.get(StateValues.DD_FIELD10);
        dd.field[11] = mValues.get(StateValues.DD_FIELD11);

        String value = mValues.get(StateValues.DD_SIZE);
        if (!TextUtils.isEmpty(value)) {
            dd.size = Long.valueOf(value);
        } else {
            mValuesBackup.load();
            dd.field[0] = mValuesBackup.get(StateValues.DD_FIELD0);
            dd.field[1] = mValuesBackup.get(StateValues.DD_FIELD1);
            dd.field[2] = mValuesBackup.get(StateValues.DD_FIELD2);
            dd.field[3] = mValuesBackup.get(StateValues.DD_FIELD3);
            dd.field[4] = mValuesBackup.get(StateValues.DD_FIELD4);
            dd.field[5] = mValuesBackup.get(StateValues.DD_FIELD5);
            dd.field[6] = mValuesBackup.get(StateValues.DD_FIELD6);
            dd.field[7] = mValuesBackup.get(StateValues.DD_FIELD7);
            dd.field[8] = mValuesBackup.get(StateValues.DD_FIELD8);
            dd.field[9] = mValuesBackup.get(StateValues.DD_FIELD9);
            dd.field[10] = mValuesBackup.get(StateValues.DD_FIELD10);
            dd.field[11] = mValuesBackup.get(StateValues.DD_FIELD11);

            value = mValuesBackup.get(StateValues.DD_SIZE);
            if (!TextUtils.isEmpty(value)) {
                dd.size = Long.valueOf(value);
            }
        }

        return dd;
    }

    public void setDownloadDescriptor(DownloadDescriptor dd) {
        if (dd == null) {
            throw new RuntimeException("You can't save an empty DD.");
        }
        mValues.put(StateValues.DD_FIELD0, dd.field[0]);
        mValues.put(StateValues.DD_FIELD1, dd.field[1]);
        mValues.put(StateValues.DD_FIELD2, dd.field[2]);
        mValues.put(StateValues.DD_FIELD3, dd.field[3]);
        mValues.put(StateValues.DD_FIELD4, dd.field[4]);
        mValues.put(StateValues.DD_FIELD5, dd.field[5]);
        mValues.put(StateValues.DD_FIELD6, dd.field[6]);
        mValues.put(StateValues.DD_FIELD7, dd.field[7]);
        mValues.put(StateValues.DD_FIELD8, dd.field[8]);
        mValues.put(StateValues.DD_FIELD9, dd.field[9]);
        mValues.put(StateValues.DD_FIELD10, dd.field[10]);
        mValues.put(StateValues.DD_FIELD11, dd.field[11]);
        mValues.put(StateValues.DD_SIZE, Long.toString(dd.size));

        mValues.commit();

        mValuesBackup.put(StateValues.DD_FIELD0, dd.field[0]);
        mValuesBackup.put(StateValues.DD_FIELD1, dd.field[1]);
        mValuesBackup.put(StateValues.DD_FIELD2, dd.field[2]);
        mValuesBackup.put(StateValues.DD_FIELD3, dd.field[3]);
        mValuesBackup.put(StateValues.DD_FIELD4, dd.field[4]);
        mValuesBackup.put(StateValues.DD_FIELD5, dd.field[5]);
        mValuesBackup.put(StateValues.DD_FIELD6, dd.field[6]);
        mValuesBackup.put(StateValues.DD_FIELD7, dd.field[7]);
        mValuesBackup.put(StateValues.DD_FIELD8, dd.field[8]);
        mValuesBackup.put(StateValues.DD_FIELD9, dd.field[9]);
        mValuesBackup.put(StateValues.DD_FIELD10, dd.field[10]);
        mValuesBackup.put(StateValues.DD_FIELD11, dd.field[11]);
        mValuesBackup.put(StateValues.DD_SIZE, Long.toString(dd.size));

        mValuesBackup.commit();
        Log.d(TAG.COMMON, "[persistent]: dd saved.");
    }

    public void deleteDeltaPackage() {
        Log.d(TAG.COMMON, "[persistent]: delete package.");
        Context context = DmApplication.getInstance();
        context.deleteFile(FumoComponent.FUMO_FILE_NAME);
        context.deleteFile(FumoComponent.FUMO_RESUME_FILE_NAME);

        String state = Integer.toString(DmFumoState.IDLE);
        mValues.put(StateValues.ST_DOWNLOADED_SIZE, "");
        mValues.put(StateValues.ST_STATE, state);
        mValues.commit();

        mValuesBackup.put(StateValues.ST_DOWNLOADED_SIZE, "");
        mValues.put(StateValues.ST_STATE, state);
        mValuesBackup.commit();
    }

    public DownloadInfo getDownloadInfo() {
        DownloadInfo info = new DownloadInfo();

        mValues.load();
        info.url = mValues.get(StateValues.DD_FIELD1);
        info.version = mValues.get(StateValues.DD_FIELD4);

        if (TextUtils.isEmpty(info.url)) {
            mValuesBackup.load();
            info.url = mValuesBackup.get(StateValues.DD_FIELD1);
            info.version = mValuesBackup.get(StateValues.DD_FIELD4);
        }

        return info;
    }

}
