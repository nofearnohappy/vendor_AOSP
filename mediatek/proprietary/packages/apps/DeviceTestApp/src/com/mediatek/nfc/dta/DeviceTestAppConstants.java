package com.mediatek.nfc.dta;

import java.util.List;
import java.util.Arrays;
import java.util.ArrayList;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import android.util.Log;

public class DeviceTestAppConstants  {

    /// ***  DTA Java Version ***
    public static final String DTA_JAVA_VERSION = "15012001";
    /// **********************

    public static final String DATA_PATTERN_NUMBER = "PATTERN_NUMBER";
    public static final String ACTION_OPERATION_TEST_START = "action.operation.test.start";
    public static final String ACTION_DTA_MAIN_START = "action.dta.main.start";
    public static final String ACTION_DTA_PLATFORM_START = "action.dta.platform.start";
    public static final String ACTION_DTA_OPERATION_START = "action.dta.operation.start";
    public static final String ACTION_DTA_P2P_START = "action.dta.p2p.start";
    public static final String ACTION_DTA_SWP_START = "action.dta.swp.start";
    public static final String ACTION_DTA_LLCP_START = "action.dta.llcp.start";
    public static final String ACTION_DTA_SNEP_START = "action.dta.snep.start";
    public static final String DTA_INSTRUMENT_NAME = "DTA_INSTRUMENT_NAME";
    public static final String DTA_DEST_CONFIG_PATH = "DTA_DEST_CONFIG_PATH";

    public static final int TEST_TYPE_PLATFORM = 0;
    public static final int TEST_TYPE_OPERATION = 1;
    public static final int TEST_TYPE_P2P = 2;
    //--------------------------DTA General intent---------------------------------//
    public static final String PREF_P2P_MANAGER_MODE = "com.mediatek.nfc.p2pmanagermode";
    public static final String ACTION_SET_MODE = "com.mediatek.nfc.dta.ACTION_SET_MODE";
    public static final String EXTRA_P2P_MODE = "com.mediatek.nfc.dta.EXTRA_MODE";
    public static final int MANAGER_MODE_DEFAULT = 0;
    public static final int MANAGER_MODE_DTA = 1;
    public static final String DTA_PREF = "DeviceTestAppPrefs";

    public static final String ACTION_LLCP_EVT = "com.mediatek.nfc.dta.ACTION_LLCP_EVT";
    public static final String LLCP_STATUS = "com.mediatek.nfc.dta.LLCP_STATUS";
    //--------------------------DTA General intent---------------------------------//

    //--------------------------LLCP intent---------------------------------//
    public static final String ACTION_LLCP_SET_PATTERN =
                                                   "com.mediatek.nfc.dta.ACTION_LLCP_SET_PATTERN";
    public static final String LLCP_PATTERN = "com.mediatek.nfc.dta.LLCP_PATTERN";
    //--------------------------LLCP intent---------------------------------//

    //--------------------------SNEP intent---------------------------------//
    public static final String ACTION_SNEP_CLIENT_REQUEST =
                                               "com.mediatek.nfc.dta.ACTION_SNEP_CLIENT_REQUEST";
    public static final String SNEP_CLIENT_SN = "com.mediatek.nfc.dta.SNEP_CLIENT_SN";
    public static final String ACTION_SNEP_CLIENT_RESPONSE =
                                         "public static final String ACTION_SNEP_CLIENT_REQUEST";
    public static final String SNEP_CLIENT_RES_CODE = "com.mediatek.nfc.dta.SNEP_CLIENT_RES_CODE";
    public static final String SNEP_CLIENT_RES_DATA = "com.mediatek.nfc.dta.SNEP_CLIENT_RES_DATA";

    public static final String SNEP_CLIENT_REQ_CODE = "com.mediatek.nfc.dta.SNEP_CLIENT_REQ_CODE";
    public static final String SNEP_CLIENT_REQ_DATA = "com.mediatek.nfc.dta.SNEP_CLIENT_REQ_DATA";


    public static final String ACTION_SNEP_SERVER_REQ =
                                                     "com.mediatek.nfc.dta.ACTION_SNEP_SERVER_REQ";
    public static final String SNEP_REQ_CODE = "com.mediatek.nfc.dta.SNEP_SERVER_REQ_CODE";
    public static final String SNEP_REQ_DATA = "com.mediatek.nfc.dta.SNEP_SERVER_REQ_DATA";
    public static final String SNEP_SERVER_SN = "com.mediatek.nfc.dta.SNEP_SERVER_SN";

    public static final String ACTION_SNEP_SERVER_RES =
                                                     "com.mediatek.nfc.dta.ACTION_SNEP_SERVER_RES";
    public static final String SNEP_RES_CODE = "com.mediatek.nfc.dta.SNEP_SERVER_RES_CODE";
    public static final String SNEP_RES_DATA = "com.mediatek.nfc.dta.SNEP_SERVER_RES_DATA";

    //public static final String ACTION_DTA_TEST = "com.mediatek.nfc.dta.ACTION_DTA_TEST";
    //public static final String EXTRA_TEST_TYPE = "com.mediatek.nfc.dta.EXTRA_TEST_TYPE";
    //public static final String EXTRA_TEST_SCENARIO = "com.mediatek.nfc.dta.EXTRA_TEST_SCENARIO";
    //public static final int SNEP_TEST = 0;
    //--------------------------SNEP intent---------------------------------//

    //config folder
    public static final String DTA_CONFIG_FOLDER_SOURCE = "/system/etc/nfc_conformance/DTA_Config/";
    public static final String DTA_CONFIG_FOLDER_DEST   = "/system/etc/nfc_conformance/DTA_Config/";

    public static byte[] hexToBinary(String s){
        int len = s.length();
        byte[] data = new byte[len/2];
        if( len%2 != 0)
            return null;
        for(int i = 0; i < len; i += 2 ) {
            data[i/2] = (byte)((Character.digit(s.charAt(i), 16) << 4)+
                                                            Character.digit(s.charAt(i+1), 16));
        }
        return data;
    }

    public static String binaryToHex(byte[] buf, int len){
        String ret = "";
        for(int i = 0; i < len; i++){
            ret = ret + "0123456789ABCDEF".charAt((byte)0xf&buf[i]>>4) +
                                                 "0123456789ABCDEF".charAt(buf[i]&(byte)0xf) + " ";
        }
        return ret;
    }

    public static void copy(File src, File dst) throws IOException {
        InputStream in = new FileInputStream(src);
        OutputStream out = new FileOutputStream(dst);

        // Transfer bytes from in to out
        byte[] buf = new byte[1024];
        int len;
        while ((len = in.read(buf)) > 0) {
            out.write(buf, 0, len);
        }
        in.close();
        out.close();
    }

    public static List<String> listFilesForFolder(final File folder) {
        List<String> fileList = new ArrayList<String>();
        if(folder != null & folder.listFiles() !=null){
            for (final File fileEntry : folder.listFiles()) {
                if (fileEntry.isDirectory()) {
                    listFilesForFolder(fileEntry);
                } else {
                    //System.out.println(fileEntry.getName());
                    Log.d("DTA", "" + fileEntry.getName());
                    fileList.add(fileEntry.getName());
                }
            }
        }else{
            Log.d("DTA", "listFileForFolder is null !");
        }
        return fileList;
    }

}
