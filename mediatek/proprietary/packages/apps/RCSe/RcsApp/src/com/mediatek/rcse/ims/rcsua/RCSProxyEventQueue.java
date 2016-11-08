package com.mediatek.rcse.ims.rcsua;

import android.content.Context;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;

import com.mediatek.rcse.api.Logger;
import com.mediatek.rcse.ims.rcsua.RcsUaAdapter.RcsUaEvent;

public class RCSProxyEventQueue implements Runnable{

	public static final int INCOMING_EVENT_MSG = 0;
	public static final int OUTGOING_EVENT_MSG = 1;
	
	  private static final String TAG = "RCSProxyEventQueue";
	private class RCSQueueElement{
		RcsUaEvent rcsEvent;
		int direction;
		
	   public RCSQueueElement(RcsUaEvent rcsEvent, int dir){
		    this.rcsEvent = rcsEvent;
			this.direction = dir;
		}
	};
	
	private Object obj = new Object();
	private Queue<RCSQueueElement> eventQueue ;
	Iterator eventQueueIterator;
	private static RCSProxyEventQueue mInstance;
	 private static RcsUaAdapter mRCSUAAdapter;
	
	
	public RCSProxyEventQueue(RcsUaAdapter rcsUAAdapter){
		mRCSUAAdapter = rcsUAAdapter;
		eventQueue = new LinkedList<RCSQueueElement>();
		eventQueueIterator = eventQueue.iterator();
		
	}
	
	/*
	public static synchronized void createInstance(RcsUaAdapter rcsUAAdapter){
	    	if(mInstance == null){
	    		mInstance = new RCSProxyEventQueue(rcsUAAdapter);
	    	}
	    }
	
	public static RCSProxyEventQueue getInstance() {
	        return mInstance;
	 }
	 */
	
	 /**
     * IMS polling thread Id
     */
    private long eventPollingThreadID = -1;
    
    /**
     * IMS polling thread
     */
    private Thread eventPollingThread = null;
    
    
    protected void startEventQueuePolling(){
     
    	if(eventPollingThreadID >=0 ){
    		return;
    	}
    	try {
    		
    		 Logger.d(TAG, "startEventQueuePolling");
    		eventPollingThread = new Thread(this);
    		eventPollingThreadID = eventPollingThread.getId();
    		eventPollingThread.start();
			} catch(Exception e) {
			
			}	
    }
    
    
    protected void stopEventQueuePolling(){
    	if(eventPollingThreadID == -1 ){
    		return;
    	}
    	try {
    		 Logger.d(TAG, "stopEventQueuePolling");
    		eventPollingThreadID = -1;
    		eventPollingThread.interrupt();
    		eventPollingThread = null;
			} catch(Exception e) {
			
			}
    }
    
	public void addEvent(int direction, RcsUaEvent event){
		RCSQueueElement element = new RCSQueueElement(event, direction);
		synchronized (obj) {
			eventQueue.add(element);
		}
	}
	
	
	public void run() {
        while(true){
        	if(eventQueue.size() == 0){
        		continue;
        	}
        	RCSQueueElement event = null;
        	synchronized(obj){
        		event = eventQueue.poll();
        	}
        	
        	 if(event !=null){
        		 //from RCS ua proxy to adapter
        		 if(event.direction == INCOMING_EVENT_MSG){
        			 Logger.d(TAG, "event to dispatcher");
        			 mRCSUAAdapter.sendMsgToDispatcher(event.rcsEvent);
        			 //Message msg = new Message();
                     //msg.obj = event;
                     //mRCSUAEventDispatcher.sendMessage(msg);
        		 }
        		 //from adapter to RCS_uaproxy
        		 else  if(event.direction == OUTGOING_EVENT_MSG){
        			// send the event to va
                     //mSocket.writeEvent(responseEvent);
        			 Logger.d(TAG, "event to RCS_UA_Proxy");
        			 mRCSUAAdapter.sendMsgToRCSUAProxy(event.rcsEvent);
        		 }
        	 }
        }
     }

	
	 
	
}
