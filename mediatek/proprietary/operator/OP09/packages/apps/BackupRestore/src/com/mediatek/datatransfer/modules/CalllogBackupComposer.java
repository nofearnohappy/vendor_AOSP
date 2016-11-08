package com.mediatek.datatransfer.modules;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.CallLog;

import com.mediatek.datatransfer.utils.Constants;
import com.mediatek.datatransfer.utils.ModuleType;
import com.mediatek.datatransfer.utils.MyLogger;
import com.mediatek.datatransfer.utils.Utils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;

/**
 * @author
 *
 */
public class CalllogBackupComposer extends Composer {

    private static final String CLASS_TAG = MyLogger.LOG_TAG + "/CalllogBackupComposer";
    private static final Uri mCalllogUri = CallLog.Calls.CONTENT_URI;
    private Cursor mCallLogs = null;
    private Writer mWriter = null;

    /**
     * @param context.
     */
    public CalllogBackupComposer(Context context) {
        super(context);
    }

    @Override
    public boolean init() {
        // TODO Auto-generated method stub
        boolean result = false;
        mCallLogs = mContext.getContentResolver().query(mCalllogUri, null, null,
                null, "date ASC");
        if (mCallLogs != null) {
            mCallLogs.moveToFirst();
            result = true;
        }
        MyLogger.logD(CLASS_TAG, "init():" + (result ? "OK!!!" : "FAILED!!!") + ",count:"
                + getCount());
        return result;
    }

    @Override
    public int getModuleType() {
        // TODO Auto-generated method stub
        return ModuleType.TYPE_CALLLOG;
    }



    @Override
    public void onStart() {
        // TODO Auto-generated method stub
        super.onStart();
        MyLogger.logD(CLASS_TAG, "onStart():mParentFolderPath:" + mParentFolderPath);
        if (getCount() > 0) {
            File path = new File(mParentFolderPath + File.separator
                    + Constants.ModulePath.FOLDER_CALLLOG);
            if (!path.exists()) {
                path.mkdirs();
            }

            File file = new File(path.getAbsolutePath() + File.separator
                    + Constants.ModulePath.NAME_CALLLOG);
            if (!file.exists()) {
                try {
                    file.createNewFile();
                } catch (IOException e) {
                    MyLogger.logE(CLASS_TAG, "onStart():file:" + file.getAbsolutePath());
                    MyLogger.logE(CLASS_TAG, "onStart():create file failed");
                }
            }

            try {
                mWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file)));
            } catch (IOException e) {
                MyLogger.logE(CLASS_TAG, "new BufferedWriter failed");
            }
        }
    }

    /**
     * Describe <code>onEnd</code> method here.
     *
     */
    public final void onEnd() {
        super.onEnd();
        try {
            MyLogger.logD(CLASS_TAG, "CalllogBackupComposer onEnd");
            if (mWriter != null) {
                MyLogger.logE(CLASS_TAG, "mWriter.close()");
                mWriter.close();
            }
        } catch (IOException e) {
            MyLogger.logE(CLASS_TAG, "mWriter.close() failed");
        }

        if (mCallLogs != null) {
            mCallLogs.close();
            mCallLogs = null;
        }
    }

    @Override
    public int getCount() {
        // TODO Auto-generated method stub
        if (mCallLogs != null && !mCallLogs.isClosed()) {
            return mCallLogs.getCount();
        }
        return -1;
    }

    @Override
    public boolean isAfterLast() {
        // TODO Auto-generated method stub
        boolean result = true;
        if (mCallLogs != null && !mCallLogs.isAfterLast()) {
            result = false;
        }
        return result;
    }

    @Override
    protected boolean implementComposeOneEntity() {
        // TODO Auto-generated method stub
        MyLogger.logD(CLASS_TAG, "implementComposeOneEntity() GO!");
        boolean result = false;
        if (mCallLogs != null && !mCallLogs.isAfterLast()) {
            CallLogsData callLogsData = new CallLogsData();
            callLogsData.id = mCallLogs.getInt(mCallLogs.getColumnIndex("_id"));
            callLogsData.new_Type = mCallLogs.getInt(mCallLogs.getColumnIndex(CallLog.Calls.NEW));
            callLogsData.type = mCallLogs.getInt(mCallLogs.getColumnIndex(CallLog.Calls.TYPE));
            callLogsData.name = mCallLogs.getString(mCallLogs
                    .getColumnIndex(CallLog.Calls.CACHED_NAME));
            ;
            callLogsData.date = mCallLogs.getLong(mCallLogs.getColumnIndex(CallLog.Calls.DATE));
            callLogsData.number = mCallLogs.getString(mCallLogs.getColumnIndex(
                    CallLog.Calls.NUMBER));
            callLogsData.duration = mCallLogs.getLong(mCallLogs
                    .getColumnIndex(CallLog.Calls.DURATION));
            callLogsData.number_type = mCallLogs.getInt(mCallLogs
                    .getColumnIndex(CallLog.Calls.CACHED_NUMBER_TYPE));
            callLogsData.simid = Utils.simId2Slot(
                    mCallLogs.getLong(mCallLogs.getColumnIndex(CallLog.Calls.SUB_ID)), mContext);
            MyLogger.logD(CLASS_TAG, "implementComposeOneEntity()  ==---CONTENT---==  "
                    + callLogsData.toString());
            try {
                mWriter.write(combineVclwithSim(callLogsData));
                result = true;
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } finally {
                mCallLogs.moveToNext();
            }
        }
        return result;
    }

    private String combineVclwithSim(CallLogsData callLogsData) {
        // TODO Auto-generated method stub
        StringBuilder mBuilder = new StringBuilder();
        mBuilder.append(CallLogsData.BEGIN_VCALL);
        mBuilder.append(CallLogsData.VCL_END_OF_LINE);
        mBuilder.append(CallLogsData.SIMID + callLogsData.simid);
        mBuilder.append(CallLogsData.VCL_END_OF_LINE);
//      mBuilder.append(CallLogsData.NEW+callLogsData.new_Type);
//      mBuilder.append(CallLogsData.VCL_END_OF_LINE);
        mBuilder.append(CallLogsData.TYPE + callLogsData.type);
        mBuilder.append(CallLogsData.VCL_END_OF_LINE);
        String dateString = Utils.parseDate(callLogsData.date);
        MyLogger.logD(CLASS_TAG, "startsWith(DATE) = " + dateString);
        mBuilder.append(CallLogsData.DATE + dateString);
        mBuilder.append(CallLogsData.VCL_END_OF_LINE);
        mBuilder.append(CallLogsData.NUMBER + callLogsData.number);
        mBuilder.append(CallLogsData.VCL_END_OF_LINE);
        mBuilder.append(CallLogsData.DURATION + callLogsData.duration);
        mBuilder.append(CallLogsData.VCL_END_OF_LINE);
        mBuilder.append(CallLogsData.END_VCALL);
        mBuilder.append(CallLogsData.VCL_END_OF_LINE);
        return mBuilder.toString();
    }

    /**
     * @param callLogsData
     * @return
     * @deprecated use {@link #combineVclwithSim} instead
     */
    @Deprecated
    private String combineVcl(CallLogsData callLogsData) {
        // TODO Auto-generated method stub
        StringBuilder mBuilder = new StringBuilder();
        mBuilder.append(CallLogsData.BEGIN_VCALL);
        mBuilder.append(CallLogsData.VCL_END_OF_LINE);
        mBuilder.append(CallLogsData.ID + callLogsData.id);
        mBuilder.append(CallLogsData.VCL_END_OF_LINE);
        mBuilder.append(CallLogsData.NEW + callLogsData.new_Type);
        mBuilder.append(CallLogsData.VCL_END_OF_LINE);
        mBuilder.append(CallLogsData.TYPE + callLogsData.type);
        mBuilder.append(CallLogsData.VCL_END_OF_LINE);
        mBuilder.append(CallLogsData.DATE + callLogsData.date);
        mBuilder.append(CallLogsData.VCL_END_OF_LINE);
        mBuilder.append(CallLogsData.NAME + callLogsData.name);
        mBuilder.append(CallLogsData.VCL_END_OF_LINE);
        mBuilder.append(CallLogsData.NUMBER + callLogsData.number);
        mBuilder.append(CallLogsData.VCL_END_OF_LINE);
        mBuilder.append(CallLogsData.DURATION + callLogsData.duration);
        mBuilder.append(CallLogsData.VCL_END_OF_LINE);
        mBuilder.append(CallLogsData.NMUBER_TYPE + callLogsData.number_type);
        mBuilder.append(CallLogsData.VCL_END_OF_LINE);
        mBuilder.append(CallLogsData.END_VCALL);
        mBuilder.append(CallLogsData.VCL_END_OF_LINE);
        return mBuilder.toString();
    }

}
