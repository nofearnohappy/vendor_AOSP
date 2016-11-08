package com.orangelabs.rcs.core.ims.rcsua;


import org.xbill.DNS.MRRecord;

import com.orangelabs.rcs.core.ims.rcsua.RcsUaAdapter.RcsUaEvent;
import com.orangelabs.rcs.core.ims.rcsua.RcsUaAdapter.RcsUaSocketIO;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncResult;
import android.os.Handler;
import android.os.Message;
import android.os.SystemProperties;
import android.util.Log;
import com.orangelabs.rcs.utils.logger.Logger;

public class RcsProxyRegistrationHandler implements RcsUaEventDispatcher.RCSEventDispatcher {

	private static final String TAG = "RcsProxyRegistrationHandler";
	
	    private Context mContext;
	    //private RcsUaSocketIO mSocket;
	    private static RcsUaAdapter rcsuaAdapt;
	    private static boolean isRegistrationSuccessful = false;
	    private static boolean isRegistrationRequestSent = false;
	    
	    private static boolean isDeRegistrationRequestSent = false;
	    
	    /**
		 * Wait user answer for session invitation
		 */
	    private static Object waitRegisterResponse = new Object();

		
		
	public RcsProxyRegistrationHandler(Context context) {
        mContext = context;
      

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
                case RcsUaAdapter.RCS_PROXY_EVENT_RSP_RCS_REGISTER:
                	
                //	 Logger.d(TAG, "EventCallback : event RCS_PROXY_EVENT_RSP_RCS_REGISTER");
                    int randLen = 0;
                    int autnLen = 0;
                    int sessionId = 0;
                    String strRand = "";
                    String strAuth = "";
                   // transaction_id = event.getByte();
                    //randLen = event.getByte();
                   // autnLen = event.getByte();
                    // isIsimPrefer: 0 for USIM prefer, 1 for ISIM prefer
                   
                    
                    //read the content and based on that set the value                     
                    synchronized (waitRegisterResponse) {
                    	
                    	//set the value based on registration state 
                    	isRegistrationSuccessful = true ;
                    	
                    	// Logger.d(TAG, "setRegistrationStatus : " +isRegistrationSuccessful);
                    	//set the registration status 
                    	rcsuaAdapt.setRegistrationStatus(isRegistrationSuccessful);
                    	
                    	//resove the semaphore..
                        waitRegisterResponse.notify();	
					}
                    
                    
                    break;
  
                case RcsUaAdapter.RCS_PROXY_EVENT_RCS_DEREGISTER:
                //	Logger.d(TAG, "Deregistered successfully");
                	//set the value based on registration state 
                	isDeRegistrationRequestSent = true ;
                	isRegistrationSuccessful = false;
                	isRegistrationRequestSent = false;
                	
                	//set the registration status 
                	rcsuaAdapt.setRegistrationStatus(isRegistrationSuccessful);
                	break;
                	
                case 2://MSG_ID_REQUEST_QUERY_SIM_STATUS:
                	
                    //VaEvent responseEvent = new ImsAdapter.VaEvent(MSG_ID_RESPONSE_QUERY_SIM_STATUS);
                	RcsUaEvent responseEvent = new  RcsUaAdapter.RcsUaEvent(1);
                	transaction_id = event.getByte();
                    //log("transaction_id: " + transaction_id);

                    //transaction_id
                    responseEvent.putByte(transaction_id);

                   // log("mSimState: " + mSimState);
                    //USIM Type
                    /*
                    if (mSimState.equals(IccCardConstants.INTENT_VALUE_ICC_LOADED)) {
                        responseEvent.putByte(1);
                    } else {
                        responseEvent.putByte(0);
                    }

                    //ISIM Type
                    if (mIsimState.equals(IccCardConstants.INTENT_VALUE_ICC_LOADED)) {
                        responseEvent.putByte(2);
                    } else {
                        responseEvent.putByte(3);
                    }
                    */

                    //Session ID
                    responseEvent.putByte(0);

                    //Pad
                    responseEvent.putByte(0);

                    // send the event to va
                    rcsuaAdapt.writeEvent(responseEvent);

                    break;
                default:
                    //log( "Unknown request: " + request_id);
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    	
    
    public boolean register(){
       
    	boolean regStatus = false;

    	try{
    	synchronized (waitRegisterResponse) {
    		
    		
    		isRegistrationRequestSent = true;
    		
    	//	 Logger.d(TAG, "register : send request");
    		//send register request
    		sendRegisterRequest();
    		
    		//wait for 3 sec
			waitRegisterResponse.wait(3000);
    		
		//	Logger.d(TAG, "register : after register request response ; registration status : " +isRegistrationSuccessful);
			regStatus = isRegistrationSuccessful;
    		
		}
    	}catch(InterruptedException e){
    		regStatus = false;
    	}
    	
    	
    	return regStatus;
    }

    public boolean unregister(){
    	boolean status = false;
    	
    	try{
        	synchronized (waitRegisterResponse) {
        	
        		isDeRegistrationRequestSent = true;
        		
        	//	Logger.d(TAG, "deregister : send request");
        		//send register request
        		sendDeRegisterRequest();
        		
        		//wait for 3 sec
    			waitRegisterResponse.wait(3000);
        		
    		//	Logger.d(TAG, "register : after deregister request response ; deregistration status : " +isDeRegistrationRequestSent);
    			
    			status = isRegistrationSuccessful;
        		
    		 }
        	}catch(InterruptedException e){
        		status = false;
        	}
        	
    	
    	return status;
    }
    
    private void sendRegisterRequest(){
    	
   //  Logger.d(TAG, "sendRegisterRequest");
     String rcsCapabilityFeatureTags = RcsUaAdapter.getInstance().getRCSFeatureTag();
     int length = rcsCapabilityFeatureTags.length();
   	 RcsUaEvent event = new RcsUaAdapter.RcsUaEvent(rcsuaAdapt.RCS_PROXY_EVENT_RCS_REGISTER);

     //put capability
     event.putString(rcsCapabilityFeatureTags, length);
      //msg length
     //event.putByte(0);
     
   	 //event.
     //write event
     rcsuaAdapt.writeEvent(event);

    }
    
    
    private void sendDeRegisterRequest(){
    	
   	// Logger.d(TAG, "sendRegisterRequest");
    String rcsCapabilityFeatureTags = RcsUaAdapter.getInstance().getRCSFeatureTag();
    int length = rcsCapabilityFeatureTags.length();
  	RcsUaEvent event = new RcsUaAdapter.RcsUaEvent(rcsuaAdapt.RCS_PROXY_EVENT_RCS_DEREGISTER);

    //put capability
    event.putString(rcsCapabilityFeatureTags, length);
     //msg length
    //event.putByte(0);
    
  	 //event.
    //write event
    rcsuaAdapt.writeEvent(event);

   }
}
