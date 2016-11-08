package com.mediatek.dialer.plugin.speeddial;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.os.SystemProperties;
import android.text.Editable;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.View;
import android.widget.EditText;

import com.mediatek.common.PluginImpl;
import com.mediatek.dialer.ext.DefaultDialPadExtension;
import com.mediatek.dialer.ext.IDialPadExtension;
import com.mediatek.op01.plugin.R;

import java.util.List;

@PluginImpl(interfaceName="com.mediatek.dialer.ext.IDialPadExtension")
public class OP01DialPadExtension extends DefaultDialPadExtension implements View.OnLongClickListener{

    private static final String TAG = "OP01DialPadExtension";

    private Activity mHostActivity;
    private String mHostPackage;
    private Resources mHostResources;
    private EditText mEditText;
    
    private Context mContext;
    /**
     * for op01
     * @param durationView the duration text
     */

    public OP01DialPadExtension (Context context) {
        super();
        mContext = context;
    }

    @Override
    public void buildOptionsMenu(final Activity activity, Menu menu){
        int index = menu.size();
        MenuItem speedDialMenu = menu.add(Menu.NONE, index, 0, mContext.getText(R.string.call_speed_dial));
        speedDialMenu.setOnMenuItemClickListener(new OnMenuItemClickListener() {
            public boolean onMenuItemClick(MenuItem item) {
                Log.d(TAG, "SpeedDial onMenuItemClick");
                SpeedDialController.getInstance().enterSpeedDial(activity);
                return true;
            }
        });
    }

    @Override
    public void onViewCreated(Activity activity, View view) {
        Log.d(TAG, "onViewCreated.");
        mHostActivity = activity;
    
        mHostPackage = activity.getPackageName();
        mHostResources = activity.getResources();

        View two = (View) view.findViewById(mHostResources.getIdentifier("two",
                                "id", mHostPackage));
        two.setOnLongClickListener(this);
        
        View three = (View) view.findViewById(mHostResources.getIdentifier("three",
                                "id", mHostPackage));
        three.setOnLongClickListener(this);
        
        View four = (View) view.findViewById(mHostResources.getIdentifier("four",
                                "id", mHostPackage));
        four.setOnLongClickListener(this);
        
        View five = (View) view.findViewById(mHostResources.getIdentifier("five",
                                "id", mHostPackage));
        five.setOnLongClickListener(this);
        
        View six = (View) view.findViewById(mHostResources.getIdentifier("six",
                                "id", mHostPackage));
        six.setOnLongClickListener(this);
        
        View seven = (View) view.findViewById(mHostResources.getIdentifier("seven",
                                "id", mHostPackage));
        seven.setOnLongClickListener(this);
        
        View eight = (View) view.findViewById(mHostResources.getIdentifier("eight",
                                "id", mHostPackage));
        eight.setOnLongClickListener(this);
        
        View nine = (View) view.findViewById(mHostResources.getIdentifier("nine",
                                "id", mHostPackage));
        nine.setOnLongClickListener(this);

        mEditText = (EditText) view.findViewById(mHostResources.getIdentifier("digits",
                                "id", mHostPackage));
    }

    @Override
    public boolean onLongClick(View view) {
        int id = view.getId();

        int key = 0;
        if (id == mHostResources.getIdentifier("two","id", mHostPackage)) {
            key = 2;
        }
        else if (id == mHostResources.getIdentifier("three","id", mHostPackage)) {
            key = 3;
        }
        else if (id == mHostResources.getIdentifier("four","id", mHostPackage)) {
            key = 4;
        }
        else if (id == mHostResources.getIdentifier("five","id", mHostPackage)) {
            key = 5;
        }
        else if (id == mHostResources.getIdentifier("six","id", mHostPackage)) {
            key = 6;
        }
        else if (id == mHostResources.getIdentifier("seven","id", mHostPackage)) {
            key = 7;
        }
        else if (id == mHostResources.getIdentifier("eight","id", mHostPackage)) {
            key = 8;
        }
        else if (id == mHostResources.getIdentifier("nine","id", mHostPackage)) {
            key = 9;
        }

        if (key > 0 && key < 10 && mEditText.getText().length() <= 1) {
            SpeedDialController.getInstance().handleKeyLongProcess(mHostActivity, mContext, key);
            mEditText.getText().clear();
            return true;
        }
        return false;
    }

    @Override
    public List<String> getSingleIMEI(List<String> list) {
        Log.d(TAG, "getSingleIMEI");
        if (isSigleImeiEnabled()) {
            if (list.size() > 1) {
                for (int i = list.size() - 1; i < list.size(); i++) {
                    list.remove(i);
                }
            }
        }
        return list;
    }

    private boolean isSigleImeiEnabled() {
        return SystemProperties.get("ro.mtk_single_imei").equals("1");
    }
}

