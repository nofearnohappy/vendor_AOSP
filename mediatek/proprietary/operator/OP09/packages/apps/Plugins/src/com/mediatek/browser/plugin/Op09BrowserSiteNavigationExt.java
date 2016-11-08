package com.mediatek.browser.plugin;

import android.content.Context;
import android.util.Log;

import com.mediatek.browser.ext.DefaultBrowserSiteNavigationExt;
import com.mediatek.common.PluginImpl;
import com.mediatek.op09.plugin.R;

@PluginImpl(interfaceName="com.mediatek.browser.ext.IBrowserSiteNavigationExt")
public class Op09BrowserSiteNavigationExt extends DefaultBrowserSiteNavigationExt {

    private static final String TAG = "Op09BrowserSiteNavigationExt";

    private static final int SITE_NAVIGATION_COUNT = 9;

    private Context mContext;

    public Op09BrowserSiteNavigationExt(Context context) {
        super();
        mContext = context;
    }

    public int getSiteNavigationCount() {
        Log.i("@M_" + TAG, "Enter: " + "getSiteNavigationCount" + " --OP09 implement");
        return SITE_NAVIGATION_COUNT;
    }

    public CharSequence[] getPredefinedWebsites() {
        Log.i("@M_" + TAG, "Enter: " + "getPredefinedWebsites" + " --OP09 implement");
        return mContext.getResources().getTextArray(R.array.predefined_websites_op09);
    }

}
