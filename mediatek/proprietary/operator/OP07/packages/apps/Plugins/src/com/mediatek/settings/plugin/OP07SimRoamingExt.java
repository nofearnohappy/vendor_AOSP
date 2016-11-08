package com.mediatek.settings.plugin;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.mediatek.common.PluginImpl;
import com.mediatek.settings.ext.DefaultSimRoamingExt;
import com.mediatek.op07.plugin.R;

@PluginImpl(interfaceName="com.mediatek.settings.ext.ISimRoamingExt")
public class OP07SimRoamingExt extends DefaultSimRoamingExt {
    private String TAG = "OP07SimRoamingExt";
    private Context mContext;

    public OP07SimRoamingExt(Context context) {
        mContext = context;
        Log.d("@M_" + TAG, "OP07SimRoamingExt");
    }

    public void showPinToast(boolean enable) {
        if (enable) {
            Toast.makeText(mContext,
                    mContext.getString(R.string.sim_pin_enable),
                    Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(mContext,
                    mContext.getString(R.string.sim_pin_disable),
                    Toast.LENGTH_SHORT).show();
        }
    }
}
