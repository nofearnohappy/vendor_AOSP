package com.mediatek.datatransfer.modules;

import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.provider.CallLog;

import com.mediatek.datatransfer.utils.BackupFilePreview;
import com.mediatek.datatransfer.utils.Constants.ModulePath;
import com.mediatek.datatransfer.utils.ModuleType;
import com.mediatek.datatransfer.utils.MyLogger;
import com.mediatek.datatransfer.utils.Utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

/**
 * @author
 *
 */
public class CalllogRestoreComposer extends Composer {

    private static final String CLASS_TAG = MyLogger.LOG_TAG + "/CalllogRestoreComposer";
    private static final Uri mCalllogUri = CallLog.Calls.CONTENT_URI;
    private ArrayList<CallLogsData> mCalllogDatas;
    private int mIndex;
    boolean mOtherPhone = false;

    /**
     * @param context.
     */
    public CalllogRestoreComposer(Context context) {
        super(context);
        // TODO Auto-generated constructor stub
    }

    @Override
    public int getModuleType() {
        // TODO Auto-generated method stub
        return ModuleType.TYPE_CALLLOG;
    }

    @Override
    public int getCount() {
        // TODO Auto-generated method stub
        int count = 0;
        if (mCalllogDatas != null) {
            count = mCalllogDatas.size();
        }
        MyLogger.logD(CLASS_TAG, "[getCount] = " + count);
        return count;
    }
    @Override
    public void onEnd() {
        super.onEnd();
        if (mCalllogDatas != null) {
            mCalllogDatas.clear();
        }
        MyLogger.logD(CLASS_TAG, "onEnd()");
    }
    @Override
    public boolean isAfterLast() {
        // TODO Auto-generated method stub
        boolean result = true;
        if (mCalllogDatas != null) {
            result = (mIndex >= mCalllogDatas.size()) ? true : false;
        }

        MyLogger.logD(CLASS_TAG, "isAfterLast():" + result);
        return result;
    }

    @Override
    public boolean init() {
        // TODO Auto-generated method stub
        MyLogger.logD(CLASS_TAG, "[init]");
        try {
            mCalllogDatas = getCallLogEntry();
            mOtherPhone = isOtherPhone();
            MyLogger.logD(CLASS_TAG, "[init]====otherPhone?===" + mOtherPhone);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            MyLogger.logE(CLASS_TAG, "[init]====>> FAILED!");
            return false;
        }
        return true;
    }

    private boolean isOtherPhone() {
        // TODO Auto-generated method stub
        File file = new File(mParentFolderPath);
        BackupFilePreview preview = new BackupFilePreview(file);
        MyLogger.logD(CLASS_TAG, "[isOtherPhone]====otherPhone?===" +
                preview.isOtherDeviceBackup());
        return preview.isOtherDeviceBackup();
    }

    private ArrayList<CallLogsData> getCallLogEntry() {
        // TODO Auto-generated method stub
        ArrayList<CallLogsData> calllogsList = new ArrayList<CallLogsData>();
        try {
            File file = new File(mParentFolderPath + File.separator + ModulePath.FOLDER_CALLLOG
                    + File.separator + ModulePath.NAME_CALLLOG);
            BufferedReader buffreader = new BufferedReader(new InputStreamReader(
                    new FileInputStream(file)));
            String line = null;
            CallLogsData calllogData = null;
            while ((line = buffreader.readLine()) != null) {
                if (line.startsWith(CallLogsData.BEGIN_VCALL)) {
                    calllogData = new CallLogsData();
//                  MyLogger.logD(CLASS_TAG,"startsWith(BEGIN_VCALL)");
                }
                if (line.startsWith(CallLogsData.ID)) {
                    calllogData.id = Integer.parseInt(getColonString(line));
//                  MyLogger.logD(CLASS_TAG,"startsWith(ID) = " +calllogData.id);
                }

                if (line.startsWith(CallLogsData.SIMID)) {
                    calllogData.simid = Utils.slot2SimId(Integer.parseInt(getColonString(line)),
                            mContext);
                }
                if (line.startsWith(CallLogsData.NEW)) {
                    calllogData.new_Type = Integer.parseInt(getColonString(line));
//                  MyLogger.logD(CLASS_TAG,"startsWith(NEW) = " +calllogData.new_Type);
                }
                if (line.startsWith(CallLogsData.TYPE)) {
                    calllogData.type = Integer.parseInt(getColonString(line));
//                  MyLogger.logD(CLASS_TAG,"startsWith(TYPE) = " +calllogData.type);
                }
                if (line.startsWith(CallLogsData.DATE)) {
                    String time = getColonString(line);
//                  Date date = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss").parse(time);
//                  calllogData.date = date.getTime();

                    calllogData.date = Utils.unParseDate(time);

                    MyLogger.logD(CLASS_TAG, "startsWith(DATE) = " + calllogData.date);
                }
                if (line.startsWith(CallLogsData.NAME)) {
                    calllogData.name = getColonString(line);
//                  MyLogger.logD(CLASS_TAG,"startsWith(NAME) = " +calllogData.name);
                }
                if (line.startsWith(CallLogsData.NUMBER)) {
                    calllogData.number = getColonString(line);
//                  MyLogger.logD(CLASS_TAG,"startsWith(NUMBER) = "+calllogData.number);
                }
                if (line.startsWith(CallLogsData.DURATION)) {
                    calllogData.duration = Long.parseLong(getColonString(line));
//                  MyLogger.logD(CLASS_TAG,"startsWith(DURATION) = "+calllogData.duration);
                }
                if (line.startsWith(CallLogsData.NMUBER_TYPE)) {
                    calllogData.number_type = Integer.parseInt(getColonString(line));
//                  MyLogger.logD(CLASS_TAG,"startsWith(NMUBER_TYPE) = "+calllogData.number_type);
                }
                if (line.startsWith(CallLogsData.END_VCALL)) {
//                  MyLogger.logD(CLASS_TAG,calllogData.toString());
                    calllogsList.add(calllogData);
                    MyLogger.logD(CLASS_TAG, "startsWith(END_VCALL)");
                }
            }
            buffreader.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            MyLogger.logE(CLASS_TAG, "init failed");
        }
        return calllogsList;
    }

    private String getColonString(String str) {
        if (str.contains(":")) {
            return str.substring(str.indexOf(":") + 1);
        }
        return str;
    }
    @Override
    protected boolean implementComposeOneEntity() {
        // TODO Auto-generated method stub
        boolean result = false;
        CallLogsData calllogFileEntry = mCalllogDatas.get(mIndex++);
        ContentValues values = fromCallLogsData(calllogFileEntry);
        if (values == null) {
            MyLogger.logD(CLASS_TAG, "parsePdu():values=null");
        } else {
            try {
                mContext.getContentResolver().insert(mCalllogUri, values);
                result = true;
             } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                result = false;
            }
        }
        return result;
    }

    private ContentValues fromCallLogsData(CallLogsData calllogFileEntry) {
        // TODO Auto-generated method stub
        ContentValues values = new ContentValues();
//      if(!otherPhone){
//          values.put(Phone._ID,calllogFileEntry.id);
//      }
        values.put(CallLog.Calls.NEW, calllogFileEntry.new_Type);
        values.put("simid", calllogFileEntry.simid);
        values.put(CallLog.Calls.TYPE, calllogFileEntry.type);
//      values.put(CallLog.Calls.CACHED_NAME, calllogFileEntry.name);
        values.put(CallLog.Calls.DATE, calllogFileEntry.date);
        values.put(CallLog.Calls.NUMBER, calllogFileEntry.number);
        values.put(CallLog.Calls.DURATION, calllogFileEntry.duration);
//      values.put(CallLog.Calls.CACHED_NUMBER_TYPE, calllogFileEntry.number_type);
        return values;
    }

}
