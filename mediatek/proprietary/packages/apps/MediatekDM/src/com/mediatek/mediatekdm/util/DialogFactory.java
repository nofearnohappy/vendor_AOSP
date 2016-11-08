package com.mediatek.mediatekdm.util;

import android.app.AlertDialog;
import android.content.Context;

public final class DialogFactory {
    public static AlertDialog.Builder newAlert(Context context) {
        return new AlertDialog.Builder(context, AlertDialog.THEME_HOLO_DARK).setCancelable(false);
    }
}
