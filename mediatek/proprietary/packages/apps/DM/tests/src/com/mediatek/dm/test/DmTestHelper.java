package com.mediatek.dm.test;

import com.mediatek.dm.DmConst;
import com.mediatek.dm.DmConst.TAG;
import com.mediatek.dm.conn.DmDataConnection;
import com.mediatek.dm.ext.MTKFileUtil;
import com.mediatek.dm.ext.MTKPhone;
import com.mediatek.dm.option.Options;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import junit.framework.Assert;
import android.content.Context;
import android.telephony.TelephonyManager;
import android.util.Log;

public class DmTestHelper {

    private static final String TAG = "DmTestHelper";

    public static final String SCOMO_MESSAGE_FILE = "scomo";
    public static final String LAWMO_MESSAGE_FILE = "lawmo";
    public static final String FUMO_MESSAGE_FILE = "fumo";
    public static final String DMACC_MESSAGE_FILE = "dmacc";

    public static void setNetworkConnection(Context context, boolean isEnable) {
        TelephonyManager teleMgr = TelephonyManager.getDefault();
        if (!teleMgr.getDataEnabled()) {
            teleMgr.setDataEnabled(isEnable);
        }
    }

    public static boolean checkNetwork(Context context) {
        boolean result = false;
        Assert.assertTrue("check network should only be used in DM WAP connection.",
                !Options.USE_DIRECT_INTERNET);
        Log.d(TAG, "checkNetwork begin");
        result = DmDataConnection.getInstance(context).startDmDataConnectivity() == MTKPhone.NETWORK_AVAILABLE;
        Log.d(TAG, "checkNetwork result is " + result);

        return result;
    }

    public static Boolean copyFromAsserts(Context context, String fileName, String desPath) {
        Log.d(TAG, "copyFromAsserts begin");
        String desFilePath = desPath + File.separator + fileName;
        Boolean ret = false;
        InputStream in = null;
        FileOutputStream out = null;
        ByteArrayOutputStream swapStream = new ByteArrayOutputStream();
        try {
            File desFile = new File(desFilePath);
            File desDir = new File(desPath);
            if (desFile.exists()) {
                Log.v(TAG, fileName + "exists, delet it+++++");
                desFile.delete();
            }

            if (!desDir.exists()) {
                Log.w(TAG, desPath + "files dir is not exist");
                if (desDir.mkdir()) {
                    MTKFileUtil.openPermission(DmConst.PathName.PATH_IN_DATA);
                } else {
                    Log.e(TAG, "Create files dir error");
                    return ret;
                }
            }

            Log.v(TAG, fileName + "open begin ++++");
            in = context.getAssets().open(fileName);
            Log.v(TAG, fileName + "open finished ++++");
            out = new FileOutputStream(desFile);
            byte[] buff = new byte[512];
            int rc = 0;
            Log.v(TAG, "read begin ++++");
            while ((rc = in.read(buff, 0, 512)) > 0) {
                Log.i(TAG, "in while rc: " + rc);
                swapStream.write(buff, 0, rc);
            }
            out.write(swapStream.toByteArray());
            Log.v(TAG, "read end ++++");
            ret = true;
        } catch (IOException e) {
            Log.e(TAG, e.getMessage());
        } finally {
            try {
                if (in != null) {
                    in.close();
                    in = null;
                }
                if (out != null) {
                    out.close();
                    out = null;
                }
                if (swapStream != null) {
                    swapStream.close();
                    swapStream = null;
                }
            } catch (IOException e) {
                Log.e(TAG, e.getMessage());
            }
        }
        return ret;
    }

    public static byte[] getWapPushMessage(Context context, String fileName) {

        FileInputStream in = null;
        byte[] message = null;
        try {
            InputStream stream = context.getAssets().open(fileName);
            ByteArrayOutputStream swapStream = new ByteArrayOutputStream();
            byte[] buff = new byte[512];
            int rc = 0;
            while ((rc = stream.read(buff, 0, 512)) > 0) {
                swapStream.write(buff, 0, rc);
            }
            message = swapStream.toByteArray();
            swapStream.close();
        } catch (IOException e) {
            Log.e(TAG, e.getMessage());
            e.printStackTrace();
        } catch (NullPointerException e) {
            Log.e(TAG, e.getMessage());
            e.printStackTrace();
        } catch (IndexOutOfBoundsException e) {
            Log.e(TAG, e.getMessage());
            e.printStackTrace();
        } finally {
            try {
                if (in != null) {
                    in.close();
                    in = null;
                }
            } catch (IOException e) {
                Log.e(TAG, e.getMessage());
            }
        }
        return message;
    }

    // TODO: parser get.xml for fumo
    public static String getXml(Context context, String fileName, String tag) {
        String message = null;
        return message;
    }
}
