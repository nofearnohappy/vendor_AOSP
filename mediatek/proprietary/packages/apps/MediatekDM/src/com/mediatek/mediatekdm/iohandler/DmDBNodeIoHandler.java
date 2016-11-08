package com.mediatek.mediatekdm.iohandler;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import com.mediatek.mediatekdm.DmConfig;
import com.mediatek.mediatekdm.DmConst.TAG;
import com.mediatek.mediatekdm.PlatformManager;
import com.mediatek.mediatekdm.mdm.MdmException;
import com.mediatek.mediatekdm.mdm.NodeIoHandler;

import java.util.HashMap;
import java.util.Map;

public abstract class DmDBNodeIoHandler implements NodeIoHandler, ICacheable {

    protected Context mContext = null;
    protected Uri mUri = null;
    protected String mMccMnc = null;
    protected String mRecordToWrite = null;
    protected String mRecordToRead = null;

    protected Map<String, String> mMap = new HashMap<String, String>();

    @Override
    public String getKey() {
        return mUri.getPath();
    }

    public int read(int arg0, byte[] arg1) throws MdmException {
        String recordToRead = null;
        String uriPath = mUri.getPath();
        Log.i(TAG.NODEIOHANDLER, "mUri: " + uriPath);
        Log.i(TAG.NODEIOHANDLER, "arg0: " + arg0);

        recordToRead = new String();
        for (int i = 0; i < getItem().length; i++) {
            if (mUri.getPath().contains(getItem()[i])) {
                if ((String) mMap.get(getItem()[i]) != null) {
                    if (specificHandlingForRead(getItem()[i]) != null) {
                        recordToRead += specificHandlingForRead(getItem()[i]);
                        Log.d(TAG.NODEIOHANDLER, "Special read result = " + recordToRead);
                    }
                    if (recordToRead.length() == 0) {
                        String sqlString = buildSqlString(getMccMnc());
                        Cursor cur = null;

                        Log.d(TAG.NODEIOHANDLER, "Normal read, sqlString = " + sqlString);

                        cur = mContext.getContentResolver().query(getTableToBeQueryed(),
                                getProjection(), sqlString, null, null);
                        if (cur != null && cur.moveToFirst()) {
                            int col = cur.getColumnIndex((String) mMap.get(getItem()[i]));
                            recordToRead = cur.getString(col);
                        }
                        if (cur != null) {
                            cur.close();
                        }
                    }
                } else {
                    recordToRead += getContentValue()[i];
                }
                break;
            }
        }
        Log.v(TAG.NODEIOHANDLER, "mRecordToRead = " + recordToRead);
        if (TextUtils.isEmpty(recordToRead)) {
            return 0;
        } else {
            byte[] temp = recordToRead.getBytes();
            if (arg1 == null) {
                return temp.length;
            }
            int numberRead = 0;
            for (; numberRead < arg1.length - arg0; numberRead++) {
                if (numberRead < temp.length) {
                    arg1[numberRead] = temp[arg0 + numberRead];
                } else {
                    break;
                }
            }
            if (numberRead < arg1.length - arg0) {
                recordToRead = null;
            } else if (numberRead < temp.length) {
                recordToRead = recordToRead.substring(arg1.length - arg0);
            }
            return numberRead;
        }
    }

    public void write(int arg0, byte[] arg1, int arg2) throws MdmException {

        Log.i(TAG.NODEIOHANDLER, "mUri: " + mUri.getPath());
        Log.i(TAG.NODEIOHANDLER, "arg1: " + new String(arg1));
        Log.i(TAG.NODEIOHANDLER, "arg0: " + arg0);
        Log.i(TAG.NODEIOHANDLER, "arg2: " + arg2);

        // if (mRecordToWrite == null) {
        // mRecordToWrite = new String();
        // }
        mRecordToWrite = new String(arg1);
        if (mRecordToWrite.length() == arg2) {
            // Modify: added************start
            String uriPath = mUri.getPath();
            String uriPathName = "";
            if (uriPath != null) {
                int indexOfSlash = uriPath.lastIndexOf("/");
                if (indexOfSlash == -1) {
                    Log.e(TAG.NODEIOHANDLER, "index of / of mUri is null");
                    return;
                }
                uriPathName = uriPath.substring(indexOfSlash + 1);
            } else {
                Log.e(TAG.NODEIOHANDLER, "mUri.getPath is null!");
                return;
            }

            // Modify: added************end

            for (int i = 0; i < getItem().length; i++) {
                if (uriPathName.equals(getItem()[i])) {
                    ContentValues values = new ContentValues();
                    if ((String) mMap.get(getItem()[i]) != null) {
                        if (!specificHandlingForWrite(mRecordToWrite, values, getItem()[i])) {
                            values.put((String) mMap.get(getItem()[i]), mRecordToWrite);
                        }
                    } else {
                        mRecordToWrite = null;
                        break;
                    }
                    if (mContext.getContentResolver().update(getTableToBeQueryed(), values,
                            buildSqlString(getMccMnc()), null) == 0) {
                        if (getInsertUri() == null) {
                            setInsertUri(mContext.getContentResolver().insert(
                                    getTableToBeQueryed(), values));
                        } else {
                            if (mContext != null) {
                                mContext.getContentResolver().update(getInsertUri(), values, null,
                                        null);
                            }
                        }
                    }
                    mRecordToWrite = null;
                    break; // modify: added
                }
            }
        }
    }

    protected boolean specificHandlingForWrite(String str, ContentValues cv, String item) {
        return false;
    }

    protected String specificHandlingForRead(String item) {
        return null;
    }

    protected void setInsertUri(Uri uri) {
    };

    protected Uri getInsertUri() {
        return null;
    };

    protected String buildSqlString(String mccMnc) {
        return null;
    };

    protected abstract String[] getItem();

    protected abstract String[] getProjection();

    protected abstract Uri getTableToBeQueryed();

    protected abstract String[] getContentValue();

    protected final String getMccMnc() {
        if (DmConfig.getInstance().useSmsReg()) {
            if (mMccMnc != null) {
                Log.d(TAG.NODEIOHANDLER, "mMccMnc is " + mMccMnc + ", lengh is " + mMccMnc.length());
            }

            long subId = PlatformManager.getInstance().getRegisteredSubId();
            if (subId != -1) {
                mMccMnc = PlatformManager.getInstance().getSubOperator(subId);
                Log.d(TAG.NODEIOHANDLER, "getSubOperator(" + subId + "): " + mMccMnc);
            } else {
                Log.e(TAG.NODEIOHANDLER, "No sim card registered!");
                /* should not reach here, dummy string */
                return "22222";
            }

            Log.d(TAG.NODEIOHANDLER, "mMccMnc is " + mMccMnc);
            return mMccMnc;
        } else {
            return null;
        }
    }
}
