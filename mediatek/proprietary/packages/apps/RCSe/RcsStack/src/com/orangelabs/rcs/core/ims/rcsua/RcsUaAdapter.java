package com.orangelabs.rcs.core.ims.rcsua;


      
import android.content.BroadcastReceiver;                         
import android.content.Context;                                   
import android.content.Intent;                                    
import android.content.IntentFilter;                              
import android.net.LocalSocket;                                   
import android.net.LocalSocketAddress;                            

import java.lang.Thread.State;                                    
import java.util.List;                                            
import java.util.Scanner;                                         
import java.io.InterruptedIOException;                            
import java.io.IOException;                                       
import java.io.OutputStream;                                      
import java.io.DataInputStream;                                   
import java.io.BufferedOutputStream;                              

import javax2.sip.ListeningPoint;

/*
import org.xbill.DNS.ExtendedResolver;
import org.xbill.DNS.NAPTRRecord;
import org.xbill.DNS.Record;
import org.xbill.DNS.ResolverConfig;
import org.xbill.DNS.SRVRecord;
import org.xbill.DNS.Type;
*/

import android.os.Looper;
import android.os.Messenger;
import android.os.RemoteException;
import android.os.SystemProperties;                               
//import com.android.internal.telephony.TelephonyIntents;           
//import com.android.internal.telephony.gsm.GSMPhone;               
import android.os.Handler;                                        
import android.os.Message;                                        
           
import com.orangelabs.rcs.core.ims.network.sip.FeatureTags;
import com.orangelabs.rcs.utils.logger.Logger;
import com.orangelabs.rcs.core.ims.protocol.sip.SipException;
import com.orangelabs.rcs.provider.settings.RcsSettings;

//import com.mediatek.xlog.Xlog;                                    

public class RcsUaAdapter {
	
	
		private static int MSG_ID_FOR_RCS_PROXY_TEST = 1;
		private static int MSG_ID_FOR_RCS_PROXY_TEST_RSP = 2;
	
		final static public int RCS_PROXY_EVENT_TO_RCSPROXY_START_CODE = 3;
		final static public int  RCS_PROXY_EVENT_REQ_REG_INFO                 = RCS_PROXY_EVENT_TO_RCSPROXY_START_CODE + 1; // registartion ino required
		final static public int  RCS_PROXY_EVENT_RES_REG_INFO                 = RCS_PROXY_EVENT_TO_RCSPROXY_START_CODE + 2; //reg info response
		final static public int  RCS_PROXY_EVENT_RCS_REGISTER                 = RCS_PROXY_EVENT_TO_RCSPROXY_START_CODE + 3; //reg info response
		final static public int  RCS_PROXY_EVENT_RCS_DEREGISTER                 = RCS_PROXY_EVENT_TO_RCSPROXY_START_CODE + 4; //reg info response
		final static public int  RCS_PROXY_EVENT_REQ_SIP_Send                 = RCS_PROXY_EVENT_TO_RCSPROXY_START_CODE + 5; //sip request sent
		final static public int  RCS_PROXY_EVENT_RSP_SIP_Send                 = RCS_PROXY_EVENT_TO_RCSPROXY_START_CODE + 6; //SIp request response
		final static public int  RCS_PROXY_EVENT_RSP_RCS_REGISTER                 = RCS_PROXY_EVENT_TO_RCSPROXY_START_CODE + 7; //reg info response
		
		//IMS specpfic infrmation
		   private static String IMSProxyAddr = "";
		   private static int IMSProxyPort = 0;
		   private static String SIPDefaultProtocolForVoLTE = "";
		   private static String mLocalIPAddr = ""; 
		    
		   //intent 
		   
		   public static final String VOLTE_SERVICE_NOTIFY_INTENT = "COM.MEDIATEK.RCS.IMS.VOLTE_SERVICE_NOTIFICATION";
	//RCS UA event
	 public static class RcsUaEvent {
	        public static final int MAX_DATA_LENGTH = 70960;

	        private int request_id;
	        private int data_len;
	        private int read_offset;
	        private byte data[];
	        private int event_max_data_len = MAX_DATA_LENGTH;

	        public RcsUaEvent(int rid) {
	            request_id = rid;
	            data = new byte[event_max_data_len];
	            data_len = 0;
	            read_offset = 0;
	        }
	        
	        public RcsUaEvent(int rid, int length) {
	            request_id = rid;
	            event_max_data_len = length;
	            data = new byte[event_max_data_len];
	            data_len = 0;
	            read_offset = 0;
	        }

	        public int putInt(int value) {
	            if (data_len > event_max_data_len - 4) {
	                return -1;
	            }

	            synchronized(this) {
	                for (int i = 0 ; i<4 ; ++i) {
	                    data[data_len] = (byte) ((value >> (8*i)) & 0xFF);
	                    data_len++;
	                }
	            }
	            return 0;
	        }

	        public int putShort(int value) {
	            if (data_len > event_max_data_len - 2) {
	                return -1;
	            }

	            synchronized(this) {
	                for (int i = 0 ; i<2 ; ++i) {
	                    data[data_len] = (byte) ((value >> (8*i)) & 0xFF);
	                    data_len++;
	                }
	            }

	            return 0;
	        }

	        public int putByte(int value) {
	            if (data_len > event_max_data_len - 1) {
	                return -1;
	            }

	            synchronized(this) {
	                data[data_len] = (byte) (value & 0xFF);
	                data_len++;
	            }

	            return 0;
	        }

	        public int putString(String str, int len) {
	            if (data_len > event_max_data_len - len) {
	                return -1;
	            }

	            synchronized(this) {
	                byte s[] = str.getBytes();
	                if (len < str.length()) {
	                    System.arraycopy(s, 0, data, data_len, len);
	                    data_len += len;
	                } else {
	                    int remain = len - str.length();
	                    System.arraycopy(s, 0, data, data_len, str.length());
	                    data_len += str.length();
	                    for (int i=0 ; i<remain ; i++) {
	                        data[data_len] = 0;
	                        data_len++;
	                    }
	                }
	            }

	            return 0;
	        }

	        public int putBytes(byte [] value) {
	            int len = value.length;

	            if (len > event_max_data_len) {
	                return -1;
	            }

	            synchronized(this) {
	                System.arraycopy(value, 0, data, data_len, len);
	                data_len += len;
	            }

	            return 0;
	        }

	        public byte [] getData() {
	            return data;
	        }

	        public int getDataLen() {
	            return data_len;
	        }

	        public int getRequestID() {
	            return request_id;
	        }

	        public int getInt() {
	            int ret = 0;
	            synchronized(this) {
	                ret =  ((data[read_offset+3]&0xff)<<24 | (data[read_offset+2]&0xff)<<16 | (data[read_offset+1]&0xff)<< 8 | (data[read_offset]&0xff));
	                read_offset += 4;
	            }
	            return ret;
	        }

	        public int getShort() {
	            int ret = 0;
	            synchronized(this) {
	                ret =  ((data[read_offset+1]&0xff)<< 8 | (data[read_offset]&0xff));
	                read_offset += 2;
	            }
	            return ret;
	        }

	        // Notice: getByte is to get int8 type from VA, not get one byte.
	        public int getByte() {
	            int ret = 0;
	            synchronized(this) {
	                ret = (data[read_offset]&0xff);
	                read_offset += 1;
	            }
	            return ret;
	        }
	        
	        public byte[] getBytes(int length) {
	            if(length > data_len - read_offset) {
	                return null;
	            }
	            
	            byte[] ret = new byte[length];
	                        
	            synchronized(this) {
	                for (int i = 0 ; i < length ; i++) {
	                    ret[i] = data[read_offset];
	                    read_offset++;
	                }
	                return ret;
	            }            
	        }

	        public String getString(int len) {
	            byte buf [] = new byte[len];

	            synchronized(this) {
	                System.arraycopy(data, read_offset, buf, 0, len);
	                read_offset += len;
	            }

	            return (new String(buf)).trim();
	        }
	    }

	 
	 public class RcsUaSocketIO extends Thread {
	        private byte buf[];

	        private int mTyp = -1;
	        private int mId  = -1;
	        private String mSocketName = null;
	        private LocalSocket mSocket = null;
	        private OutputStream mOut = null;
	        private DataInputStream mDin = null;

	        public RcsUaSocketIO(String socket_name) {
	            mSocketName = socket_name;
	            // TODO: buffer size confirm
	            buf = new byte[8];
	            //Xlog.d(TAG, "VaSocketIO(): Enter");
	        }

	        public void run() {
	        	
	        //	Logger.d(TAG, "RCS socket reading thread started");
	            //Xlog.d(TAG, "VaSocketIO(): Run");
	            while(true) {
	                if(misRCSUAAdapterEnabled){
	                    try {
	                        if(mDin != null) {
	                            // read the Event from mIO
	                            RcsUaEvent event = readEvent();

	                            // TODO: need to confirm if event is null or not
	                            if(event != null) {
	                            	
	                            	//put event in queue
	                            	mEventQueue.addEvent(RCSProxyEventQueue.INCOMING_EVENT_MSG, event);
	                                //Message msg = new Message();
	                                //msg.obj = event;
	                                //mRCSUAEventDispatcher.sendMessage(msg);
	                            }
	                        }
	                    } catch (InterruptedIOException e) {
	                        disconnectSocket();
							//mPhone.setTRM(2,null);
	                        e.printStackTrace();
	                        //Xlog.d(TAG, "VaSocketIO(): InterruptedIOException");
	                    } catch (Exception e) {
	                        disconnectSocket();
	                        e.printStackTrace();				
	                        //Xlog.d(TAG, "VaSocketIO(): Exception");
	                    }
	                }
	            } 
	        }

	        public boolean connectSocket() {
	            //Xlog.d(TAG, "connectSocket() Enter");
	        //	Logger.d("RcsUaSocketIO", "connectSocket");
	            if (mSocket != null) {
	                mSocket = null; // reset to null, create the new one
	            }

	            try {
	                mSocket = new LocalSocket();
	                
	            //    Logger.d("RcsUaSocketIO : try to conect with socket - ", mSocketName);
	                LocalSocketAddress addr = new LocalSocketAddress(mSocketName, LocalSocketAddress.Namespace.RESERVED);

	                mSocket.connect(addr);

	                mOut = new BufferedOutputStream(mSocket.getOutputStream(), 4096);
	                mDin = new DataInputStream(mSocket.getInputStream());

	                int sendBufferSize = 0;
	                sendBufferSize = mSocket.getSendBufferSize();
	                mSocket.setSendBufferSize(512);
	                sendBufferSize = mSocket.getSendBufferSize();
	            } catch (IOException e) {
	           // 	Logger.d("RcsUaSocketIO", "connectSocket fail");
	                e.printStackTrace();
	                disconnectSocket();
	                return false;
	            }
	        //    Logger.d("RcsUaSocketIO", "connectSocket success");
	            return true;
	        }

	        public void disconnectSocket() {
	            //Xlog.d(TAG, "disconnectSocket() Enter, mOut="+mOut);
	            try {
	                if (mSocket != null) mSocket.close();
	                if (mOut != null) mOut.close();
	                if (mDin != null) mDin.close();
	            } catch (IOException e) {
	                e.printStackTrace();
	            } finally {
	                mSocket = null;
	                mOut = null;
	                mDin = null;
	            }
	        }

	        private void writeBytes(byte [] value, int len) throws IOException {
	            mOut.write(value, 0, len);
	        }

	        private void writeInt(int value) throws IOException {
	            for (int i = 0 ; i < 4 ; ++i) {
	                mOut.write((value >> (8*i)) & 0xff);
	            }
	        }

	        public int writeEvent(RcsUaEvent event) {
	           // Xlog.d(TAG, "writeEvent Enter");
	            int ret = -1;
	            try {
	                synchronized(this) {
	                    if (mOut != null) {
	                        dumpEvent(event);
							
	                        writeInt(event.getRequestID());
	                        writeInt(event.getDataLen());
	                        writeBytes(event.getData(), event.getDataLen());
	                        
	                        //String eventdata = new String(event.getData());
	                        //Logger.d("RcsUaAdapter", "write data : "+eventdata);
	                        
	                        mOut.flush();
	                        ret = 0;
	                    }
	                    else {
	                        //Xlog.d(TAG, "mOut is null, socket is not setup");
	                    }
	                }
	            } catch (IOException e) {
	                return -1;
	            }

	            return ret;
	        }

	        private int readInt() throws IOException {
	            mDin.readFully(buf, 0, 4);
	            return ((buf[3])<<24 | (buf[2]&0xff)<<16 | (buf[1]&0xff)<< 8 | (buf[0]&0xff));
	        }

	        private void readFully(byte b[], int off, int len) throws IOException {
	            mDin.readFully(b, off, len);
	        }

	        private RcsUaEvent readEvent() throws IOException {
	            //Xlog.d(TAG, "readEvent Enter");
	            int request_id;
	            int data_len;
	            byte buf [];
	            RcsUaEvent event;

	            request_id = readInt();
	            data_len   = readInt();
	            
	            buf = new byte[data_len];
	            readFully(buf, 0, data_len);

	            event = new RcsUaEvent(request_id);
	            event.putBytes(buf);

	            dumpEvent(event);
	            return event;
	        }

	        private void dumpEvent(RcsUaEvent event) {
	        //	Logger.d("RcsUaAdapter", "dumpEvent: reqiest_id:" + event.getRequestID() + "data_len:" + event.getDataLen() + ",event:" + event.getData());
	        }

	    }

	 
	 //socket name decided on rcs_ua_proxy
	    private static final String SOCKET_NAME = "rcs_ua_proxy";


	    private static final String TAG = "RcsUaAdapter";
	    private Context mContext;
	    private RcsUaSocketIO mIO;
	    private RCSProxyEventQueue mEventQueue;
	    private static RcsUaEventDispatcher mRCSUAEventDispatcher;

	    private static RcsUaAdapter mInstance;
	    private static boolean misRCSUAAdapterEnabled = false;
	    private static boolean misRCSUAAdapterInit = false;

	    
	    private static boolean misSingleRegistrationSupported =  false;
	    private static boolean misRegistered = false;
	    private Messenger mMessanger ;
	    public static RcsUaAdapter getInstance() {
	        return mInstance;
	    }
	    
	    public static synchronized void createInstance(Context context){
	    	if(mInstance == null){
	    		mInstance = new  RcsUaAdapter(context);
	    	}
	    }
	    

	    private  RcsUaAdapter(Context context) {
			
	        mContext = context;

	        if (mInstance == null){
	            mInstance = this;
	        }
			
	    //    Logger.d(TAG, "RcsUaAdapter constructor");
	        
	    	//mHandler = new Handler();
        	mIO = new RcsUaSocketIO(SOCKET_NAME);
        	
        
        	mEventQueue = new RCSProxyEventQueue(mInstance);
        	
        	 //mMessanger = new Messenger(mRCSUAEventDispatcher);
	     //   Logger.d(TAG, "RcsUaAdapter before Looper");     
	        
	    	  
	       
	        Thread t = new Thread() {

				@Override
				public void run() {
					Looper.prepare();
       	            mRCSUAEventDispatcher = new RcsUaEventDispatcher(mContext);
       	            mHandler = new Handler();
       	            Looper.loop();
				}
	        	
			};   	
			t.start();
		
       	            //mMessanger = new Messenger(mRCSUAEventDispatcher);
       	     //       Logger.d(TAG, "RcsUaAdapter after Looper.loop");     
                 
       	           // mIO.start();
	       // Xlog.d(TAG, "ImsAdapter(): ImsAdapter Enter");
	        // new the mIO object to communicate with the RCS ua 
	         
	        
	        //TODO : REGISTER RECEIIVER FOR INTENT FROM THE DIALER FOR VOLTE SERVICE START
	        
	     
	        
	        
	        /*
	        IntentFilter filter = new IntentFilter();
	        filter.addAction(TelephonyIntents.ACTION_CLEAR_DATA_BEARER_FINISHED);
	        mContext.registerReceiver(this, filter);
			*/
	        
	        //
	    }

	  
	    private Handler mHandler ;
	    private EnableRCSUARunnable mEnableRCSUARunnable;
	  
	    private class EnableRCSUARunnable implements Runnable {

	        public void run() {
	           while(true){
	            if (mIO.connectSocket() == true) {
	            	
	         //   	Logger.d(TAG,"socket connected successfully");
	               // Xlog.d(TAG, "EnableImsRunnable(): connectSocket success");
	    			
	                // start the receive thread
	                
	                // start domain event dispatcher to recieve broadcast
	            	mRCSUAEventDispatcher.enableRequest();
	    			
	            	
	            	
	                misRCSUAAdapterEnabled = true;
	                misRCSUAAdapterInit = false;
	                
	                //send test request 
	                sendRegistrationInfoRequest();
	                
	                //sendRegisterRequest();
	                
	                break;
	                
	            } else {
	            	
	           // 	Logger.d(TAG,"socket connected failure");
	                
	            
	            	//Xlog.d(TAG, "EnableImsRunnable(): connectSocket error");
	                // restart Va process, and reconnect again
	                //stopVaProcess();
	                //enableImsAdapter2ndStage();
	            }
	           }
	        }
	    };
   
	    
	    /*
	    public void enableImsAdapter (){
	       // Xlog.d(TAG, "enableImsAdapter: misImsAdapterEnabled=" + misImsAdapterEnabled);
	        
	        if(!misRCSUAAdapterEnabled && !misRCSUAAdapterInit) {
	            misRCSUAAdapterInit = true;
	            // disconnect Socket first
	            mIO.disconnectSocket();

	            // stop Va process first, to ensure it correct.
	           // stopVaProcess();
	            //mPhone.clearDataBearer();
	        }
	    }
	    */
	    
	    public void disableRCSUAAdapter (boolean isNormalDisable){

	       // Xlog.d(TAG, "disableImsAdapter(): misImsAdapterEnabled=" + misImsAdapterEnabled + ", isNormalDisable="+ isNormalDisable);
	        
	        if(misRCSUAAdapterEnabled) {
	        	  misRCSUAAdapterEnabled = false;

	            if (mEnableRCSUARunnable != null) {
	                mHandler.removeCallbacks(mEnableRCSUARunnable);
	                mEnableRCSUARunnable = null;
	            }

	            //stop the mio thread
	            if(mIO!=null){	
	            	mIO.interrupt();
	            }
	            
	            mRCSUAEventDispatcher.disableRequest();

	            mEventQueue.stopEventQueuePolling();
	            
	            // TODO: wait time out
	            mIO.disconnectSocket();
	            stopRCSProxyProcess();
	        }
	    }
      
	    // for AP side UT, set event and call ImsAdapter.sendTestEvent(event)
	    public void sendTestEvent(RcsUaEvent event){
	        // Sample Code:
	        // new the event object for Test Event
	        // VaEvent event = new VaEvent(MSG_ID_IMSA_IMCB_TEST_A);                            
	        // event.putInt(2);                
	        // event.putInt(3);        
	        mRCSUAEventDispatcher.dispatchCallback(event);        	
	    }

	    public void enableRCSProxyAdapter(){
	       // Xlog.d(TAG, "enableImsAdapter2ndStage()Enter");
	    //	Logger.d(TAG, "enableRCSProxyAdapter");
	    	
	    	//start the RCS ua proxy process
	       // SystemProperties.set("ril.volte.stack.rcsuaproxy", "1");
	        SystemProperties.set("ctl.start", "volte_rcs_proxy");
	        //SystemProperties.set("ril.volte.ua", "1");
	        //SystemProperties.set("ril.volte.imcb", "1");
	        //Xlog.d(TAG, "enableImsAdapter2ndStage(): Va process started!");

	        if (mEnableRCSUARunnable != null) {
	            mHandler.removeCallbacks(mEnableRCSUARunnable);
	            mEnableRCSUARunnable = null;
	        }

	        if(mIO != null){
	        	mIO.start();	
	        }
	        
	        mEnableRCSUARunnable = new EnableRCSUARunnable();
	        

	    //    Logger.d(TAG, "mEnableRCSUARunnable starts in 3 sec");
	        mHandler.postDelayed(mEnableRCSUARunnable, 4000);
	        
	        mEventQueue.startEventQueuePolling();
	    }
	    
	    private void stopRCSProxyProcess (){
	        
	    	//SystemProperties.set("ril.volte.stack.rcsuaproxy", "0");
	    	SystemProperties.set("ctl.stop", "volte_rcs_proxy");
	    	misSingleRegistrationSupported = false;
	    	misRegistered = false;
	    	
	        //SystemProperties.set("ril.volte.ua", "0");
	        //SystemProperties.set("ril.volte.imcb", "0");
	    }
   
	

	    //get the SIP event Dispatcher
	    public RcsUaEventDispatcher.RCSEventDispatcher getSIPEventDispatcher(){
	    	return mRCSUAEventDispatcher.getSipEventDispatcher();
	    }
	    
	    //get the SIP event Dispatcher
	    public RcsUaEventDispatcher.RCSEventDispatcher getRegistrationEventDispatcher(){
	    	return mRCSUAEventDispatcher.getRegistrationEventHandler();
	    }
	    
	    public boolean isSingleRegistrationSupported(){
	    	return misSingleRegistrationSupported;
	    }
	    
	    public boolean isRegistered(){
	    //	Logger.d(TAG,"isRegistered : " + misRegistered);
	    	return misRegistered;
	    }
	    public void setRegistrationStatus(boolean status){
	      misRegistered = status;	
	   //   Logger.d(TAG,"setRegistrationStatus : " + misRegistered);
	      
	      if(misRegistered){
	    	  
	      }
	      else{
	    	  //stop the volte process 
	    	  disableRCSUAAdapter(true);
	      }
	      
	    }
	    
	    //query the current registration state and information
	    void queryRegistrationState(){
	    	
	    	
	    }
	    
	    public void sendTestRequest(){
	    	
	    	/*
	    //	Logger.d(TAG,"send test request MSG_ID_FOR_RCS_PROXY_TEST");
	    	 RcsUaEvent event = new RcsUaAdapter.RcsUaEvent(MSG_ID_FOR_RCS_PROXY_TEST);

	         //to do : save the txn in a map

	         //TXN ID
	          event.putByte(MSG_ID_FOR_RCS_PROXY_TEST);
	         
	         //msg length
	         event.putByte(0);
	         
	         //write event
	         mIO.writeEvent(event);*/
	    	
	    	sendRegistrationInfoRequest();
	    }
	   
	    
	    //write event to socket
	   void writeEvent(RcsUaEvent event){
		   if(event!=null){
			   mEventQueue.addEvent(RCSProxyEventQueue.OUTGOING_EVENT_MSG, event);
		   }
	   }
	   
	   
	    //Send request to RCS_proxy to gte the current info about the registration
	    void sendRegistrationInfoRequest(){
	    //	Logger.d(TAG,"sendRegistrationInfoRequest");
	    	 RcsUaEvent event = new RcsUaAdapter.RcsUaEvent(RCS_PROXY_EVENT_REQ_REG_INFO);

	         //to do : save the txn in a map

	         //TXN ID
	          event.putByte(MSG_ID_FOR_RCS_PROXY_TEST);
	         
	         //msg length
	         event.putByte(0);
	         
	         //write event
	         mIO.writeEvent(event);

	    }
	    
	    private void sendRegisterRequest(){
	   // Logger.d(TAG,"sendRegisterRequest with Feature tag :  " + getRCSFeatureTag());
	        String rcsCapabilityFeatureTags = getRCSFeatureTag();
	        int length = rcsCapabilityFeatureTags.length();
	      	RcsUaEvent event = new RcsUaAdapter.RcsUaEvent(RCS_PROXY_EVENT_RCS_REGISTER);

	        //put capability
	        event.putString(rcsCapabilityFeatureTags, length-1);
	         //msg length
	        //event.putByte(0);
	        
	      	 //event.
	        //write event
	        mIO.writeEvent(event);

	       }
	    
	    public void handleRegistrationInfo2(RcsUaEvent event){
	    //	Logger.d(TAG,"handleRegistrationInfo2");
	    
	    }
	    //handle the registration info send by rcs_proxy and save it based on state
	    public void handleRegistrationInfo(RcsUaEvent event){
	    	
	    	/* REGISTRATION STATES: 
	    	 *     
	VoLTE_Event_Reg_State_Registered        = 1,
    VoLTE_Event_Reg_State_Unregistered      = 2,
    VoLTE_Event_Reg_State_Registering       = 3,
    VoLTE_Event_Reg_State_Deregistering     = 4,
    VoLTE_Event_Reg_State_Disconnected      = 5,
    VoLTE_Event_Reg_State_Authenticating    = 6,
    VoLTE_Event_Reg_State_OOS               = 7,
	    	 * 
	    	 * */
	    	
	    	
	    	
	    	//INT32 id;           /* /< the account id */
	    	//INT32 state;        /* /< refer to ::VoLTE_Event_Reg_State_e */
	    	//INT32 cause;        /* /< refer to ::VoLTE_Event_Reg_Cause_e */
	    	//UINT32 conn_info;   /* /< the connection information for the others UA to create the connection */

	        /* account information */
	        // INT8 local_address[VOLTE_MAX_ADDRESS_LENGTH];       /* /< local ip address */
	        // INT32 local_port;                                   /* /< local port number */
	        // INT32 protocol_type;                                /* /< refer to ::VoLTE_Stack_Protocol_Type_e */
	        // INT32 protocol_version;                             /* /< refer to ::VoLTE_Stack_Protocol_Version_e */
	        // INT8 public_uid[VOLTE_MAX_REG_UID_LENGTH];          /* /< the public user identity */
	        // INT8 private_uid[VOLTE_MAX_REG_UID_LENGTH];         /* /< the private user identity */
	        // INT8 home_uri[VOLTE_MAX_URI_LENGTH];                /* /< the domain name of the home network */
	        // INT8 pcscf_address[VOLTE_MAX_ADDRESS_LIST_LENGTH];  /* /< the current used PCSCF ip address */
	        // INT32 pcscf_port;                                   /* /< the current used PCSCF port number */
	        // INT8 imei[VOLTE_MAX_IMEI_LENGTH];                   /* /< the IMEI */
	        // INT8 associated_uri[VOLTE_MAX_ASSOCIATED_URI];      /* /< the list of the associated URI */
	        // INT8 pub_gruu[VOLTE_MAX_GRUU_LENGTH];               /* /< the public gruu */
	        // INT8 temp_gruu[VOLTE_MAX_GRUU_LENGTH];              /* /< the temp gruu */
	        // INT8 service_route[VOLTE_MAX_URI_LENGTH];           /* /< the service route */
	        // INT8 path[VOLTE_MAX_URI_LENGTH];                    /* /< the path */
	        // INT32 net_type;                                     /* /< refer to ::VoLTE_Event_Network_Type_e */
	        // INT32 emergency_type;                               /* /< refer to ::VoLTE_Event_Emergency_Type_e */
	        // INT8 reg_timestamp[VOLTE_MAX_TIMESTAMP_LENGTH];
	        
	    
	    	
	    	/*
	    	INT32 id;           
	        INT32 state;        

	       
	        INT8 local_address[VOLTE_MAX_ADDRESS_LENGTH];       
	        INT32 local_port;                                   
	        INT32 protocol_type;                               
	        INT8 public_uid[VOLTE_MAX_REG_UID_LENGTH];          
	    	INT8 pcscf_address[VOLTE_MAX_ADDRESS_LIST_LENGTH];  
	    	INT32 pcscf_port;                                  
	        */
	    	
	   // 	Logger.d(TAG,"handleRegistrationInfo");
	    	
	    	
	    	int length = event.getDataLen();
	    	int id = event.getInt() ;
	    	int state = 0;//event.getInt() ;
			int cause = 0;//event.getInt();
	    	String localaddress = "";
	    	int local_port = 0;
	    	int protocol_type = 0;
	    	int protocol_version = 0;
	    	String public_uid = "";
	    	String private_uid = "";
	    	String home_uri = "";
	    	String pcscf_address = "";
	    	int pcscf_port = 0;
	    	String imei = "";
	    	String associated_uri = "";
	    	String pub_gruu = "";
	    	String temp_gruu = "";
	    	String service_route = "";
	    	String path = "";
	    	int net_type = 0;
	    	int emergency_type = 0;
	    	String reg_timestamp = "";
	    	
	    	
	    	
	    			state = event.getInt();
	    			
	    			
	    			//cause = event.getShort();
	    			localaddress = event.getString(64);
	    			
	    			local_port = event.getInt();
	    			protocol_type =  event.getInt();
	    			//protocol_version =  event.getInt();
	    			public_uid = event.getString(64);
	    			//private_uid = event.getString(64);
	    			//home_uri = event.getString(128);
	    			pcscf_address = event.getString(256);
	    			
	    			pcscf_port = event.getShort();
	    			
	    			
	    			/*imei = event.getString(16);
	    			associated_uri = event.getString(512);
	    			pub_gruu = event.getString(128);
	    			temp_gruu = event.getString(128);
	    			service_route = event.getString(128);
	    			path = event.getString(128);
	    			net_type = event.getInt();
	    			emergency_type = event.getInt();
	    			reg_timestamp = event.getString(256);*/
	    			pcscf_address = "192.168.43.223";
	    			pcscf_port = 5060;
	    			protocol_version = 2;
	    			localaddress  = "192.168.43.1";
	    			protocol_version =  1;
	    			/*
	    			 * 
	    	//get the state
	    	int length = event.getDataLen();
	    	byte[]  bid = event.getBytes(4);
	    	int id = bid[0];
	    	
	    	byte[]  bState = event.getBytes(4);
	    	int state = bState[0];
	    	//int state = bState[0];

	    	byte[]  bcause = event.getBytes(4);
	    	int cause = bcause[0];
	    	*/
	    	
	    /*	Logger.d(TAG,"data length : "+ length +", "+" reg info : "+ id + " :  state : " + state + " , cause :"+cause + " local_address : "+ localaddress
	    			+" local_port : " +local_port + " , public_uid : " +public_uid + " , private_uid : " + private_uid + " , pcscf_address : "+pcscf_address
	    			+"pcscf_port : "+pcscf_port+", pub_gruu : "+pub_gruu+" , service_route : "+service_route 
	    			); */
	    	
	    	
	    	String voltePCSCFaddress = getDNSIp();
	    	
	    	
	    	boolean voltePCSCFaddresesequasl = true;
	    	if(voltePCSCFaddresesequasl){
	    	
	    		misSingleRegistrationSupported = true;
	    	
	    		 IMSProxyAddr = pcscf_address;
	  		     IMSProxyPort = pcscf_port;
	  		     mLocalIPAddr  =  localaddress;
	  		     
	  		     switch(protocol_version){
	  		     case 1:
	  		    	SIPDefaultProtocolForVoLTE = "UDP";
	  		    	 break;
	  		     case 2:
	  		    	SIPDefaultProtocolForVoLTE = "TCP";
	  		    	 break;
	  		      default:
	  		    	SIPDefaultProtocolForVoLTE = "UDP";
	  		     }
	  		     
	    		
	    		
	    //		Logger.d(TAG,"send VOLTE_SERVICE_NOTIFY_INTENT to connection manager");
	    		Intent intent = new Intent(RcsUaAdapter.VOLTE_SERVICE_NOTIFY_INTENT);
	    		mContext.sendBroadcast(intent); 
	    	}else{
	    		misSingleRegistrationSupported = false;
	    	//	Logger.d(TAG,"VOLTE PCSPF address mismatch");
	    	}
	    	
	    	
	    	/*
			THE response contains like this:
			
	    	 RCS[0] 
	    	local address  = 192.168.43.1 ,
	    	local port = 5060,
	    	protocol_type = 2 ,
	    	protocol_version = 1,
	    	public_uid = sip:404119102654903@ims.mnc011.mcc404.3gppnetwork.org,
	    	private_uid = 404119102654903@ims.mnc011.mcc404.3gppnetwork.org ,
	    	home_uri = ims.mnc011.mcc404.3gppnetwork.org ,
	    	pcscf_address = 192.168.43.223 ,
	    	pcscf_port =5060 , 
	    	associated_uri = 864855010002295 ,
	    	pub_gruu = sip:+SIPP@192.168.43.1,tel:+123456789 ,  
	    	temp_gruu = "sip:callee@example.com;gr=urn:uuid:f81d4fae-7dec-11d0-a765-00a0c91e6bf6" ,
	    	service_route = "sip:tgruu.7hs==jd7vnzga5w7fajsc7-ajd6fabz0f8g5@example.com;gr" , 
	    	path = <sip:orig@10.185.16.6:30244;lr;Dpt=7624_246;ca=c2lwOis4NjI4Njg2NTkwODBAMTAuMTg1LjE2LjE2OjI2NjY1> ,
	    	net_type  = -1204917360 , 
	    	emergency_type = 7  ,
	    	reg_timestamp = (null)
	    	*/
	    	
	       	//Logger.d("event_data is :  " +ls);
	       	
	    	//save the data in RCSUA adapater
	    	
	    	
	 
	    	//check if the registraation state is registered or not 
	    	
	    	//in case of registartion success 
	    	
	    	//if volte IP and Configuration IP (PCSCF address) are same 
	    	
//	    	misSingleRegistration = true;
	    	
	    	//send intent to ims connection manager to reconnect with volte
	    	// Send service up intent
			//Intent intent = new Intent(RcsUaAdapter.VOLTE_SERVICE_NOTIFY_INTENT);
//			getApplicationContext().sendBroadcast(intent);

	    	
	    }
	    
	    
	    protected void sendMsgToDispatcher(RcsUaEvent event){
	    	Message msg = new Message();
            msg.obj = event;
            mRCSUAEventDispatcher.sendMessage(msg);
	    }
	    
	    
	    protected void sendMsgToRCSUAProxy(RcsUaEvent event){
	    	// send the event to va
             mIO.writeEvent(event);
	    }
	    
	    protected String getRCSFeatureTag(){
	    	String data = "";
	    	//data = FeatureTags.FEATURE_RCSE;
	    	data = "urn%3Aurn-7%3A3gpp-application.ims.iari.gsma-is";
	    	return data;
	    }
	    
	    //notify imsconnection event of change event
	    public void notifyIMSConnectionManagerConnectEvent(){
	    
	    }
	    
	    
	    public String[] getImsProxyAddrForVoLTE(){
	    	//String data[] = RcsSettings.getInstance().getAllImsProxyAddrForMobile();
	    	//String data[] = {"192.168.43.223"}; 
	    	String data[] = {IMSProxyAddr};
	    	//data[0] =   IMSProxyAddr;
		    return data;	
	    }
	    
	    public int[] getImsProxyPortForVoLTE(){
	    	//int data[] = RcsSettings.getInstance().getAllImsProxyPortForMobile();
	    	int data[] = {IMSProxyPort};
	    	//data[0] =   5060;//IMSProxyPort;
		    return data;	
	    }
	    
	    public String getLocalIPAddress(){
	      return mLocalIPAddr;	
	    }
	    
	    public String getSIPDefaultProtocolForVoLTE(){
	    	//String data = RcsSettings.getInstance().getSipDefaultProtocolForMobile();	
	    	String data = SIPDefaultProtocolForVoLTE;
	    	//data =   SIPDefaultProtocolForVoLTE;
		    return data;
	    }
	    
	    
	    private String getDNSIp(){
	    	return "192.168.43.223";
	    }
}
