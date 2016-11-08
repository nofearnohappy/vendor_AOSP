
package com.mediatek.dialer.plugin;

import java.util.HashMap;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.telephony.TelephonyManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.SubMenu;
import android.view.View;
import android.util.Log;
import android.widget.PopupMenu;

import com.mediatek.dialer.ext.DefaultDialPadExtension;
import com.mediatek.op03.plugin.R;
import com.mediatek.common.PluginImpl;


@PluginImpl(interfaceName = "com.mediatek.dialer.ext.IDialPadExtension")
public class OP03DialPadEmergencyCallMenuExt extends DefaultDialPadExtension implements OnMenuItemClickListener {

    Context mContext = null;
    static final String TAG = "OP03DialPadEmergencyCallMenuExt";

    static final HashMap<String, String> ecc_map = new HashMap<String, String>();

    static {
        ecc_map.put("28310", "101,102,103,104,112,188");
        ecc_map.put("37001", "*411,911,*555");
        ecc_map.put("63907", "112");
        ecc_map.put("64602", "017,112,118,117");
        ecc_map.put("64114", "112");

    //test code
        ecc_map.put("46000", "119,101");
        ecc_map.put("40411", "119,101");  
    }

    public OP03DialPadEmergencyCallMenuExt(Context context) {
        mContext = context;
    }

    public void constructPopupMenu(PopupMenu popupMenu, View anchorView, Menu menu) {
          int SUBMENU_ID = 1000;
        Log.d(TAG, "Inside the Plug-in's constructPopupMenu");
        try {
            String networkOperator = TelephonyManager.getDefault().getNetworkOperator();

            if (networkOperator != null && !networkOperator.isEmpty()) {
                Log.d(TAG, networkOperator);
            } else {
                if (menu.findItem(SUBMENU_ID) != null){
                  menu.removeItem(SUBMENU_ID);
             }
                Log.d(TAG, "NULL or empty Network Operator string");
                return;
            }
            String emergencyNumList = ecc_map.get(networkOperator);
            
            if ((emergencyNumList != null) && (menu.findItem(SUBMENU_ID) == null))
            {
                        MenuItem item = null;
                        
                SubMenu subMenu = menu.addSubMenu(0,SUBMENU_ID,0, mContext.getString(R.string.submenu_emergency_contacts));


                for (String emergencyNum : emergencyNumList.split(",")) {
                     item = subMenu.add(emergencyNum);
                     Log.d(TAG, "Emergency number added:" + emergencyNum);

                     item.setOnMenuItemClickListener(this);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, e.toString());
            }
    }


            @Override
             public boolean onMenuItemClick(MenuItem item) {

        StringBuffer emergencyNumber = new StringBuffer("tel:");
        emergencyNumber.append(item.getTitle());
                            Log.d(TAG, "ECC number clicked" + emergencyNumber);

        try {
            Intent callIntent = new Intent(Intent.ACTION_CALL);
            callIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            callIntent.setData(Uri.parse(emergencyNumber.toString()));

            mContext.startActivity(callIntent);
                                Log.d(TAG, "Call happened on ECC number:" + emergencyNumber);

            } catch (ActivityNotFoundException e) {
                Log.e(TAG, e.toString());
            }

        return true;
    }
}
