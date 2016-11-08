/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010 France Telecom S.A.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package com.cmcc.ccs;

import android.content.Context;
import android.util.Log;

import java.util.List;

/**
 * Abstract RCS service
 * 
 * @author Jean-Marc AUFFRET
 */
public abstract class RCSService {
    /**
     * Action to broadcast when RCS service is up.
     */

    private static final String TAG = "DAPT-RCSService";

	/**
	 * Application context
	 */
	protected Context mContext;

	/**
	 * Service listener
	 */
	protected RCSServiceListener mServiceListener;

	/**
	 * Service version
	 */
	protected Integer version = null;

	/**
	 * Constructor
	 * 
	 * @param ctx Application context
	 * @param listener Service listener
	 */
	public RCSService(Context ctx, RCSServiceListener listener) {
		Log.d(TAG, "RCSService() constructor " + ctx +" listener = "+ listener);
        mContext = ctx;
	}

	/**
	 * Connects to the API
	 */
	public abstract void connect();

	/**
	 * Disconnects from the API
	 */
	public abstract void disconnect();

	/**
	 * Returns true if the service is connected, else returns false
	 * 
	 * @return Returns true if connected else returns false
	 */
	public boolean isServiceConnected() {
		return false;
	}

		/**
	 * Returns true if the service is registered to the platform, else returns
	 * false
	 * 
	 * @return Returns true if registered else returns false
	 */
	public boolean isServiceRegistered() {
		Log.d(TAG, "isServiceRegistered() entry ");
		return false;
	}


    public String getRegisterCapability() {
        return null;
    }


	/**
	 * Registers a listener on service registration events
	 * 
	 * @param listener Service registration listener
	 */
	public void addServiceRegistrationListener(RCSServiceRegistrationListener listener)
	                              throws RCSServiceNotAvailableException{
		Log.d(TAG, "addServiceRegistrationListener() entry ");
	}

	/**
	 * Unregisters a listener on service registration events
	 * 
	 * @param listener Service registration listener
	 */
	public void removeServiceRegistrationListener(RCSServiceRegistrationListener listener)
	                    throws RCSServiceNotAvailableException {
		Log.d(TAG, "removeServiceRegistrationListener() entry ");
	}

    public static class Error {

        		/**
		 * Service connection has been lost
		 */
		public final static int CONNECTION_LOST = 0;


		/**
		 * Service has been disabled
		 */
		public final static int SERVICE_DISABLED = 1;


       /**
		 * Internal error
		 */
		public final static int INTERNAL_ERROR = 2;
                
		private Error() {
		}
	}
}
