package com.mediatek.rcse.ims.rcsua;

import android.content.Context;

import com.mediatek.rcse.api.Logger;
import com.mediatek.rcse.ims.rcsua.RcsUaAdapter;
import com.mediatek.rcse.ims.rcsua.RcsUaAdapter.RcsUaSocketIO;
import com.mediatek.rcse.ims.rcsua.RcsUaAdapter.RcsUaEvent;

//import static com.android.internal.telephony.VaConstants.*;
import java.util.ArrayList;

import android.os.Handler;
import android.os.Message;
import android.os.Looper;

public class RcsUaEventDispatcher extends Handler{

	
	private Context mContext;
    //private RcsUaSocketIO mSocket;
    private ArrayList<RCSEventDispatcher> mRCSEventDispatcher = new ArrayList<RCSEventDispatcher>();
    private static final String TAG = "RcsUaEventDispatcher";
    
    
    /**event dispacther*/
    /* Event Dispatcher */
    private RcsProxySipHandler mRCSProxySiphandler;
    private RcsProxyRegistrationHandler mRCSRegistrationHandler;
   
    
    public RcsUaEventDispatcher (Context context) {
        mContext = context;
        
        createDispatcher();
    }
    

    public interface RCSEventDispatcher {
        void EventCallback(RcsUaEvent event);
        void enableRequest();
        void disableRequest();
    }
    

    
    void enableRequest(){
        for (RCSEventDispatcher dispatcher : mRCSEventDispatcher) {
            dispatcher.enableRequest();
        }
    }

    void disableRequest(){
        for (RCSEventDispatcher dispatcher : mRCSEventDispatcher) {
            dispatcher.disableRequest();
        }
    }
    
    private void createDispatcher(){
        
    	Logger.d(TAG, "Initialize the handlers ");
    	
    	//add the RCS Registartion handler
    	mRCSRegistrationHandler = new RcsProxyRegistrationHandler(mContext);
       	mRCSEventDispatcher.add(mRCSRegistrationHandler);
 
       	
    	//add the RCS proxy handler as 
    	mRCSProxySiphandler = new RcsProxySipHandler(mContext);
        //mTimerDispatcher = new TimerDispatcher(mContext, mSocket);

    	mRCSEventDispatcher.add(mRCSProxySiphandler);
        //mVaEventDispatcher.add(mTimerDispatcher);
    }
    
    
    //HANDLE MESSAGES FROM RCS PROXY
    @Override
    public void handleMessage(Message msg) {
    	
        dispatchCallback((RcsUaEvent)msg.obj);
        
    }
    
    public RCSEventDispatcher getSipEventDispatcher(){
    	return mRCSProxySiphandler;
    }
    
    public RCSEventDispatcher getRegistrationEventHandler(){
    	return mRCSRegistrationHandler;
    }
    
    /* dispatch Callback */
    void dispatchCallback(RcsUaEvent event){
        switch (event.getRequestID()) {
            case 2:
            	//RESponse received from rcs_proxy;
            	Logger.d(TAG, "response to test event received successfully");
                break;
            case RcsUaAdapter.RCS_PROXY_EVENT_RES_REG_INFO:
            	RcsUaAdapter.getInstance().handleRegistrationInfo(event);
            	break;
            case RcsUaAdapter.RCS_PROXY_EVENT_RSP_RCS_REGISTER:
            	mRCSRegistrationHandler.EventCallback(event);
            	break;
            case RcsUaAdapter.RCS_PROXY_EVENT_RSP_SIP_Send:
            	mRCSProxySiphandler.EventCallback(event);
            	break;
        }
    }
    
}
