package com.mediatek.mms.plugin;

import android.content.Intent;
import android.os.Bundle;
import android.content.Context;
import android.util.Log;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.widget.TextView;
import android.view.Gravity;
import android.widget.ImageView;

import com.mediatek.mms.ext.DefaultOpConversationListExt;
import com.mediatek.op03.plugin.R;


public class Op03ConversationListExt extends DefaultOpConversationListExt {

    private static final String TAG = "Op03ConversationListExt";
    public Op03ConversationListExt(Context context)    {
           super(context);
          Log.i(TAG, "constructor\n");
    }
    public Intent  onSmsPromoBannerViewClick(Intent intent) {
        Log.i(TAG, "onSmsPromoBannerViewClick\n");
        Intent mint = new Intent("android.provider.Telephony.ACTION_CHANGE_DEFAULT");
        mint.setPackage("com.android.settings");
        Bundle bundle = new Bundle();
        bundle.putString("package", "com.android.mms");
        mint.putExtras(bundle);
        return mint;
    }


    public void initSmsPromoBanner(ImageView imageView, TextView smsPromoBannerTitle,
            TextView smsPromoBannermessage, ApplicationInfo appInfo, PackageManager pm)
    {
        Log.i(TAG, "initSmsPromoBanner\n");
        imageView.setImageDrawable(null);
        smsPromoBannerTitle.setGravity(Gravity.CENTER);
        smsPromoBannermessage.setGravity(Gravity.CENTER);
        smsPromoBannermessage.setText(getResources().getString(R.string.banner_sms_promo_message1));
        smsPromoBannerTitle.setText(
        getResources().getString(R.string.banner_sms_promo_title_application1,
                                   appInfo.loadLabel(pm)));
    }
}


