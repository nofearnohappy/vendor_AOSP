package com.mediatek.rcse.ims.rcsua;


import java.net.InetAddress;

import com.mediatek.rcse.ims.rcsua.RcsUaAdapter.RcsUaEvent;
import com.mediatek.rcse.ims.rcsua.RcsUaAdapter.RcsUaSocketIO;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import android.os.AsyncResult;
import android.os.Handler;
import android.os.Message;
import android.os.SystemProperties;
import android.util.Log;
import com.mediatek.rcse.api.Logger;

public class RcsProxySipHandler implements RcsUaEventDispatcher.RCSEventDispatcher {

	private static final String TAG = "[RcsProxySipDispatcher]";
	
	    private Context mContext;
	    //private RcsUaSocketIO mSocket;
	    private static RcsUaAdapter rcsuaAdapt;
	  
	 RcsSIPEventListener mSIPEvtListener = null ;

     public static abstract class RcsSIPEventListener{   
	    	public abstract void notifySIPMessage(byte[] sipMsgResponse, InetAddress address,int port);
	    }
	 
	public RcsProxySipHandler(Context context) {
        mContext = context;
       // mSocket = IO;
      

        //log("SimDispatcher()");

        if(rcsuaAdapt == null) {
            //log("ImsAdapter.getInstance");
            rcsuaAdapt = RcsUaAdapter.getInstance();
        }

    }

    public void enableRequest(){
        //log("enableRequest()");
    }

    public void disableRequest(){
        //log("disableRequest()");
    }

    public void EventCallback(RcsUaEvent event){

        try {
            int request_id;
            int len;
            String data;
            int transaction_id;
            int type;

            request_id = event.getRequestID();
            //log("reqeust_id = " + request_id);

            switch (request_id) {
                case 1://MSG_ID_GET_SIP_RSP:
                    int randLen = 0;
                    randLen = event.getByte();
                    byte[] sipMsgResponse = event.getBytes(randLen);
                    
                    //InetAddress inetaddress = new InetAddress();
                   // mSIPEvtListener.notifySIPMessage(sipMsgResponse, inetaddress,0);
                    break;
                case RcsUaAdapter.RCS_PROXY_EVENT_RSP_SIP_Send:
                	 Logger.d(TAG, "EventCallback : event RCS_PROXY_EVENT_RES_SIP_Send");
                	//mSIPEvtListener.notifySIPMessage(sipMsgResponse, rcsuaAdapt.getImsProxyAddrForVoLTE(), rcsuaAdapt.getImsProxyPortForVoLTE());
                	break;
                default:
                    //log( "Unknown request: " + request_id);
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void addRCSSipEventListener(RcsSIPEventListener listener){
    	 Logger.d(TAG, "addRCSSipEventListener");
    	mSIPEvtListener = listener;
    }
    
    public boolean sendSipMsg(byte[] sipMsgBuffer){	
    	
    	 Logger.d(TAG, "sendSipMsg");
    	 RcsUaEvent event = new RcsUaAdapter.RcsUaEvent(RcsUaAdapter.RCS_PROXY_EVENT_REQ_SIP_Send);

         //sip msg buffer
    	 event.putBytes(sipMsgBuffer);
      
        //write event
        rcsuaAdapt.writeEvent(event);
    	 return true;
    }
}
