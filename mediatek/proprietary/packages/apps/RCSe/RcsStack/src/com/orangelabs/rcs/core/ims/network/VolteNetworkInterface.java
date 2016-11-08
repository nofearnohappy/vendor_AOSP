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

package com.orangelabs.rcs.core.ims.network;

import android.net.ConnectivityManager;

import com.orangelabs.rcs.core.ims.rcsua.RcsProxyRegistrationHandler;
import com.orangelabs.rcs.core.ims.rcsua.RcsUaAdapter;
import com.orangelabs.rcs.core.CoreException;
import com.orangelabs.rcs.core.access.VolteNetworkAccess;
import com.orangelabs.rcs.core.access.WifiNetworkAccess;
import com.orangelabs.rcs.core.ims.ImsModule;
import com.orangelabs.rcs.core.ims.network.sip.SipManager;
import com.orangelabs.rcs.provider.settings.RcsSettings;
import com.orangelabs.rcs.provider.settings.RcsSettingsData;
import com.orangelabs.rcs.utils.logger.Logger;

/**
 * Volte Network interface for Single Registration
 *
 * @author 
 */
public class VolteNetworkInterface extends ImsNetworkInterface {
    /**
     * The logger
     */
    private Logger logger = Logger.getLogger(this.getClass().getName());

    private boolean isRegistered = false;
    
    /**
     * SIP manager
     */
    private SipManager sip;
    
    /**
     * Constructor
     *
     * @param imsModule IMS module
     * @throws CoreException
     */
    public VolteNetworkInterface(ImsModule imsModule) throws CoreException {
    	super(imsModule, ConnectivityManager.TYPE_MOBILE,
    			new VolteNetworkAccess(),
    			RcsUaAdapter.getInstance().getImsProxyAddrForVoLTE(),
    			RcsUaAdapter.getInstance().getImsProxyPortForVoLTE(),
    			RcsUaAdapter.getInstance().getSIPDefaultProtocolForVoLTE(),
    			RcsSettingsData.VOLTE_AUTHENT);
       
    	
    	/**
         * M: add for MSRPoTLS 
         */
        if(RcsSettings.getInstance().isSecureMsrpOverMobile()){
			logger.info("MobileNetworkInterface initSecureTlsMsrp0");
			initSecureTlsMsrp(true);
        }
		else if(RcsSettings.getInstance().getSipDefaultProtocolForMobile() == "TLS"){
			logger.info("MobileNetworkInterface initSecureTlsMsrp1");
			initSecureTlsMsrp(true);
		}
		else{
			logger.info("MobileNetworkInterface initSecureTlsMsrp2");
			initSecureTlsMsrp(false);
			}
        /**
         * @}
         */
        
        // Instantiates the SIP manager
        sip = new SipManager(this);
        
        
        
    	if (logger.isActivated()) {
    		logger.info("RCS VOLTE network interface has been loaded");
    	}
    }
    
    
    
    /**
     * Is registered
     *
     * @return Return True if the terminal is registered, else return False
     */
    public boolean isRegistered() {
        return isRegistered;
    }
    
    
    //send register request to the volte_rcs_proxy
    public boolean register(int pcscaddress){
    
    	 isRegistered = volteRegister(pcscaddress);
    	 return isRegistered;
    }
    
    /*
     * Unregister from the VOLTE IMS
     */
    public void unregister() {
		if (logger.isActivated()) {
			logger.debug("Unregister from IMS");
		}
		isRegistered = false;
		
    }
}
