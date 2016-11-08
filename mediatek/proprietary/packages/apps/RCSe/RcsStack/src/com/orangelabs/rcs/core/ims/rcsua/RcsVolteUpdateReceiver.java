package com.orangelabs.rcs.core.ims.rcsua;


import com.orangelabs.rcs.utils.logger.Logger;

import android.content.BroadcastReceiver;                         
import android.content.Context;                                   
import android.content.Intent;   
import android.os.SystemProperties;

public class RcsVolteUpdateReceiver extends BroadcastReceiver {
	
	
	
	public void onReceive(Context context, Intent intent) {
        final String action = intent.getAction();
        
       // Logger.d("RcsVolteUpdateReceiver", "set notification");
       // SystemProperties.set("ril.volte.stack.rcsuaproxy", "1");
        
       if(action.equals("mediatek.volte.notification")){
        	//enable the RCS service and create socket
        	String data = intent.getStringExtra("status");
        	
        	if(data.equals("true")){
        		RcsUaAdapter.createInstance(context);
            	RcsUaAdapter.getInstance().enableRCSProxyAdapter();
        	}
        	else if(data.equals("false")){
        		RcsUaAdapter.createInstance(context);
            	RcsUaAdapter.getInstance().disableRCSUAAdapter(true);
        	}
      }
 else if(action.equals("com.android.ims.IMS_SERVICE_UP")){
	 RcsUaAdapter.createInstance(context);
 	RcsUaAdapter.getInstance().enableRCSProxyAdapter();
        }
        else if(action.equals("com.android.ims.IMS_SERVICE_DOWN")){
        	RcsUaAdapter.createInstance(context);
        	RcsUaAdapter.getInstance().disableRCSUAAdapter(true);
        }
        	
        else  if(action.equals("mediatek.volte.test.request.registrationinfo")){
        	
        	RcsUaAdapter.createInstance(context);
        	RcsUaAdapter.getInstance().sendTestRequest();
        }
    }
}
