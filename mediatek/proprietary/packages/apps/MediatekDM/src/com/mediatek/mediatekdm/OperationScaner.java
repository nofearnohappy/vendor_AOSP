package com.mediatek.mediatekdm;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.mediatek.mediatekdm.DmOperationManager.IOperationScannerHandler;

class OperationScaner extends KickoffActor {

    public OperationScaner(Context context) {
        super(context);
    }

    public void run() {
        // Check for pending operations
        DmOperationManager.getInstance().scanPendingOperations(new IOperationScannerHandler() {
            @Override
            public void notify(boolean pendingOperationFound) {
                Intent intent = new Intent(DmConst.IntentAction.DM_PENDING_OPERATION_SCAN_RESULT);
                intent.setClass(mContext, DmService.class);
                Bundle bundle = new Bundle();
                bundle.putBoolean("found", pendingOperationFound);
                intent.putExtras(bundle);
                mContext.startService(intent);
            }
        });
    }
}
