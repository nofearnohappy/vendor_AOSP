package com.mediatek.mediatekdm.fumo;

import android.content.Context;
import android.util.Log;

import com.mediatek.mediatekdm.DmConst.TAG;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

public class StateValues {
    public static final String DD_FIELD0 = "DD0_SIZE";
    public static final String DD_FIELD1 = "DD1_OBJECT_URI";
    public static final String DD_FIELD2 = "DD2_TYPE";
    public static final String DD_FIELD3 = "DD3_NAME";
    public static final String DD_FIELD4 = "DD4_VERSION";
    public static final String DD_FIELD5 = "DD5_VENDOR";
    public static final String DD_FIELD6 = "DD6_DESCRIPTION";
    public static final String DD_FIELD7 = "DD7_INSTALL_NOTIFY_URI";
    public static final String DD_FIELD8 = "DD8_NEXT_URL";
    public static final String DD_FIELD9 = "DD9_INFO_URL";
    public static final String DD_FIELD10 = "DD10_ICON_URI";
    public static final String DD_FIELD11 = "DD11_INSTALL_PARAM";
    public static final String DD_SIZE = "DD_PACK_SIZE";

    public static final String ST_STATE = "ST_STATE";
    public static final String ST_DOWNLOADED_SIZE = "ST_DOWNLOADED_SIZE";

    private final Context mContext;
    private final String mFileName;
    private Properties mProps;

    public StateValues(Context context, String fileName) {
        mContext = context;
        mFileName = fileName;
        mProps = new Properties();
    }

    public void put(String key, String value) {
        if (value != null) {
            mProps.setProperty(key, value);
        }
    }

    public String get(String key) {
        String value = mProps.getProperty(key);
        return value != null ? value : "";
    }

    public synchronized void load() {
        FileInputStream fis = null;
        try {
            fis = mContext.openFileInput(mFileName);
            mProps.load(fis);
        } catch (FileNotFoundException e) {
            Log.w(TAG.COMMON, "++dm_values not exist yet.");
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public synchronized void commit() {
        FileOutputStream fos = null;
        try {
            fos = mContext.openFileOutput(mFileName, Context.MODE_PRIVATE);
            mProps.store(fos, null);

            // force sync to disk
            fos.flush();
            fos.getFD().sync();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
