package com.mediatek.mediatekdm;

import android.os.FileUtils;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;

public class CollectSetPermissionControl {
    public static final String PATH_PERMISSION_DIR = DmApplication.getInstance().getFilesDir()
            .getPath();
    public static final String PATH_PERMISSION_FILE = PATH_PERMISSION_DIR + File.separator
            + "permissionNotify.txt";

    private static CollectSetPermissionControl sCollectSetPermissionControl = null;

    public static CollectSetPermissionControl getInstance() {
        if (sCollectSetPermissionControl == null) {
            sCollectSetPermissionControl = new CollectSetPermissionControl();
        }
        return sCollectSetPermissionControl;
    }

    public void isPermFileReady() {
        try {
            File perFilePath = new File(PATH_PERMISSION_FILE);
            File dir = new File(PATH_PERMISSION_DIR);
            if (!perFilePath.exists()) {
                Log.i(DmConst.TAG.COLLECT_SET_PERM, "!fblackFilePath.exists()");
                if (!dir.exists()) {
                    Log.d(DmConst.TAG.COLLECT_SET_PERM, "there is no /files dir in dm folder");
                    if (dir.mkdir()) {
                        // chmod for recovery access
                        FileUtils.setPermissions(dir, FileUtils.S_IRWXU | FileUtils.S_IRWXG
                                | FileUtils.S_IXOTH, -1, -1);
                    } else {
                        throw new Error("Failed to create folder in data folder.");
                    }
                }
                HashMap<String, Boolean> saveMap = new HashMap<String, Boolean>();
                saveMap.put(DmConst.ExtraKey.IS_NEED_AGREE, true);
                saveMap.put(DmConst.ExtraKey.IS_NEED_NOTIFY, true);
                FileOutputStream outStream = new FileOutputStream(PATH_PERMISSION_FILE);
                ObjectOutputStream objectOutputStream = new ObjectOutputStream(outStream);
                objectOutputStream.writeObject(saveMap);
                outStream.close();
                objectOutputStream.close();
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void writeKeyValueToFile(boolean isNeedNotify, boolean isNeedAgree) {
        try {
            HashMap<String, Boolean> map = new HashMap<String, Boolean>();
            map.put(DmConst.ExtraKey.IS_NEED_NOTIFY, isNeedNotify);
            map.put(DmConst.ExtraKey.IS_NEED_AGREE, isNeedAgree);
            FileOutputStream outStream = new FileOutputStream(PATH_PERMISSION_FILE);
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(outStream);
            objectOutputStream.writeObject(map);
            objectOutputStream.flush();
            outStream.flush();
            outStream.getFD().sync();
            objectOutputStream.close();
            outStream.close();
            Log.i(DmConst.TAG.COLLECT_SET_PERM, "outStream.flush");
            Log.i(DmConst.TAG.COLLECT_SET_PERM, "writeObjectToFile successful");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void resetKeyValue() {
        try {
            HashMap<String, Boolean> saveMap = new HashMap<String, Boolean>();
            saveMap.put(DmConst.ExtraKey.IS_NEED_AGREE, true);
            saveMap.put(DmConst.ExtraKey.IS_NEED_NOTIFY, true);
            FileOutputStream outStream = new FileOutputStream(PATH_PERMISSION_FILE);
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(outStream);
            objectOutputStream.writeObject(saveMap);
            outStream.close();
            objectOutputStream.close();
            Log.i(DmConst.TAG.COLLECT_SET_PERM, "writeObjectToFile successful");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean readKeyValueFromFile(String key) {
        boolean value = false;
        HashMap<String, Boolean> map = readObjectFromfile();
        value = map.get(key).booleanValue();
        Log.i(DmConst.TAG.COLLECT_SET_PERM, "Read " + key + ", value is " + value);
        return value;
    }

    private HashMap<String, Boolean> readObjectFromfile() {
        isPermFileReady();
        HashMap<String, Boolean> map = new HashMap<String, Boolean>();
        try {
            FileInputStream freader;
            freader = new FileInputStream(PATH_PERMISSION_FILE);
            ObjectInputStream objectInputStream = new ObjectInputStream(freader);

            map = (HashMap<String, Boolean>) objectInputStream.readObject();
            objectInputStream.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return map;
    }
}
