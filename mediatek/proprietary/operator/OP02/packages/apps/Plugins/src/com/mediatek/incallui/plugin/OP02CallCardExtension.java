package com.mediatek.incallui.plugin;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.mediatek.common.PluginImpl;
import com.mediatek.incallui.ext.DefaultCallCardExt;
import com.mediatek.incallui.ext.ICallCardExt;
import com.mediatek.op02.plugin.R;

@PluginImpl(interfaceName="com.mediatek.incallui.ext.ICallCardExt")
public class OP02CallCardExtension extends DefaultCallCardExt {
    private static final String TAG = "OP02CallCardExtension";
    private static final String ID = "id";
    private static final String HD_ICON = "hdAudioIcon";
    private Context mContext;
    public OP02CallCardExtension(Context context) {
        super();
        Log.d(TAG, "OP02CallCardExtension");
        mContext = context;
    }

    /**
      * Interface to modify the hd icon of OP02
      *
      * @param context the incallactivity context
      * @param view the callcard view
      */
    @Override
    public void onViewCreated(Context context, View view) {
        Resources resource = context.getResources();
        String packageName = context.getPackageName();

        View hdIcon =
                view.findViewById(
                        resource.getIdentifier(HD_ICON, ID, packageName));
        if (hdIcon != null) {
            ((ImageView) hdIcon).setImageDrawable(
                    mContext.getResources().getDrawable(R.drawable.ic_hd_audio));
        }
    }

}