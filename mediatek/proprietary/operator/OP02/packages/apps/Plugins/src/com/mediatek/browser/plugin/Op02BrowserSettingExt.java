package com.mediatek.browser.plugin;

import android.content.Context;
import android.os.Build;
import android.preference.ListPreference;
import android.util.Log;

import com.mediatek.browser.ext.DefaultBrowserSettingExt;
import com.mediatek.common.PluginImpl;
import com.mediatek.op02.plugin.R;

import java.text.SimpleDateFormat;
import java.util.Date;

@PluginImpl(interfaceName="com.mediatek.browser.ext.IBrowserSettingExt")
public class Op02BrowserSettingExt extends DefaultBrowserSettingExt {

    private static final String TAG = "Op02BrowserSettingExt";

    private Context mContext;

    public Op02BrowserSettingExt(Context context) {
        super();
        mContext = context;
    }

    public void setTextEncodingChoices(ListPreference e) {
        Log.i("@M_" + TAG, "Enter: " + "setTextEncodingChoices" + " --OP02 implement");
        e.setEntries(mContext.getResources().getTextArray(R.array.pref_op02_text_encoding_choices));
        e.setEntryValues(mContext.getResources().getTextArray(R.array.pref_op02_text_encoding_values));
    }

    public String getCustomerHomepage() {
        Log.i("@M_" + TAG, "Enter: " + "getCustomerHomepage" + " --OP02 implement");
        return mContext.getResources().getString(R.string.homepage_for_op02);
    }

    public String getOperatorUA(String defaultUA) {
        Log.i("@M_" + TAG, "Enter: " + "getOperatorUA" + " --OP02 implement");

        Date date = new Date(Build.TIME);
        String strTime = new SimpleDateFormat("MM.dd.yyyy").format(date);
        String model = Build.MODEL + "/V1";

        String op02UA = model + " Linux/3.4.67 Android/" + Build.VERSION.RELEASE
                + " Release/" + strTime + " Browser/AppleWebKit537.36 Profile/MIDP-2.0 Configuration/CLDC-1.1"
                + " Chrome/30.0.0.0 Mobile Safari/537.36 System/Android " + Build.VERSION.RELEASE + ";";
        return op02UA;
    }
}