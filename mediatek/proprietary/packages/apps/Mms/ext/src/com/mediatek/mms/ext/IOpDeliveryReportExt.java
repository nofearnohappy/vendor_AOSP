package com.mediatek.mms.ext;

import android.content.Intent;
import android.database.Cursor;

public interface IOpDeliveryReportExt {

    /**
     * @internal
     */
    Cursor getSmsReportItems(Intent intent, String[] projection, String selection);

}
