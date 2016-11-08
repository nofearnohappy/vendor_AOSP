package com.mediatek.browser.plugin;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import com.mediatek.browser.ext.DefaultBrowserUrlExt;
import com.mediatek.common.PluginImpl;

@PluginImpl(interfaceName="com.mediatek.browser.ext.IBrowserUrlExt")
public class Op09BrowserUrlExt extends DefaultBrowserUrlExt {

    private static final String TAG = "Op09BrowserUrlExt";

    private Context mContext;

    public Op09BrowserUrlExt(Context context) {
        super();
        mContext = context;
    }

    public boolean redirectCustomerUrl(String url) {
        Log.i("@M_" + TAG, "Enter: " + "redirectCustomerUrl" + " --OP09 implement");
        if (url.startsWith("estore:")) {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            if (mContext.getPackageManager().resolveActivity(intent, 0) == null) {
                Log.w("@M_" + TAG, "redirectCustomerUrl," + " --Renew intent");
                intent = new Intent(Intent.ACTION_VIEW,
                    Uri.parse("http://3g.189store.com/general"));
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            }
            try {
                mContext.startActivity(intent);
                return true;
            } catch (ActivityNotFoundException e) {
                Log.e("@M_" + TAG, "redirectCustomerUrl," + " --ActivityNotFound");
            }
            return false;
        }
        return false;
    }
}
