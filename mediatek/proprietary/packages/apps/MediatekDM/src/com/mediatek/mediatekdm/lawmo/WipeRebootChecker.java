package com.mediatek.mediatekdm.lawmo;

import android.content.Context;
import android.content.Intent;
import android.os.RemoteException;
import android.util.Log;

import com.android.internal.os.storage.ExternalStorageFormatter;
import com.mediatek.mediatekdm.DmConst;
import com.mediatek.mediatekdm.DmConst.TAG;
import com.mediatek.mediatekdm.PlatformManager;

import java.io.File;

public class WipeRebootChecker extends com.mediatek.mediatekdm.KickoffActor {

    public WipeRebootChecker(Context context) {
        super(context);
    }

    @Override
    public void run() {
        File file = new File(PlatformManager.getInstance().getPathInData(mContext,
                DmConst.Path.WIPE_FILE));
        if (file.exists()) {
            Intent eraseIntent = new Intent(ExternalStorageFormatter.FORMAT_AND_FACTORY_RESET);
            eraseIntent.setComponent(ExternalStorageFormatter.COMPONENT_NAME);
            mContext.startService(eraseIntent);
            Log.i(TAG.LAWMO, "Retry wipe");
        } else if (isWipeReboot()) {
            // TODO: check for wipe flag file. if it still exists, than wipe failed.
            Log.i(TAG.LAWMO, "Stub for async wipe");
        }
    }

    private boolean isWipeReboot() {
        Log.i(TAG.LAWMO, "isWipeReboot");
        boolean ret = false;
        try {
            ret = PlatformManager.getInstance().isWipeSet();
        } catch (RemoteException e) {
            Log.e(TAG.LAWMO, e.getMessage());
        }
        return ret;
    }
}
