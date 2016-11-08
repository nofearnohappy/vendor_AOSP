package com.mediatek.browser.plugin;

import android.content.Context;
import android.util.Log;

import com.mediatek.browser.ext.DefaultBrowserSiteNavigationExt;
import com.mediatek.common.PluginImpl;
import com.mediatek.op01.plugin.R;

@PluginImpl(interfaceName="com.mediatek.browser.ext.IBrowserSiteNavigationExt")
public class Op01BrowserSiteNavigationExt extends DefaultBrowserSiteNavigationExt {

    private static final String TAG = "Op01BrowserSiteNavigationExt";

    private Context mContext;

    public Op01BrowserSiteNavigationExt(Context context) {
        super();
        mContext = context;
    }

    public CharSequence[] getPredefinedWebsites() {
        Log.i("@M_" + TAG, "Enter: " + "getPredefinedWebsites" + " --OP01 implement");
        return mContext.getResources().getTextArray(R.array.predefined_websites_op01);
    }

}
