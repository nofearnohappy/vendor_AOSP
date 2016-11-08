package com.mediatek.rcse.ims.rcsua;


import android.content.BroadcastReceiver;                         
import android.content.Context;                                   
import android.content.Intent;   

public class RcsVolteUpdateReceiver extends BroadcastReceiver {
	
	
	
	public void onReceive(Context context, Intent intent) {
        final String action = intent.getAction();
      
        
        if(action.equals("mediatek.volte.notification")){
        	//enable the RCS service and create socket
        	
        	RcsUaAdapter.createInstance(context);
        	RcsUaAdapter.getInstance().enableRCSProxyAdapter();
        }
        else  if(action.equals("mediatek.volte.test.request.registrationinfo")){
        	
        	RcsUaAdapter.createInstance(context);
        	RcsUaAdapter.getInstance().sendTestRequest();
        }
    }
}
