package com.orangelabs.rcs.service;

import com.orangelabs.rcs.platform.AndroidFactory;
import com.orangelabs.rcs.provider.settings.RcsSettings;
import com.orangelabs.rcs.utils.logger.Logger;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Device boot event receiver: automatically starts the RCS service
 * 
 * @author jexa7410
 */
public class LaunchServiceUser extends BroadcastReceiver {
    private static Logger logger = Logger.getLogger(LaunchServiceUser.class.getSimpleName());
    
    @Override
    public void onReceive(Context context, Intent intent) {
        if (logger.isActivated())
            logger.debug("User Launch Service");
        if(AndroidFactory.getApplicationContext()== null) {
       //     AndroidFactory.setApplicationContext(context);
        }
       // LauncherUtils.stopRcsService(context);
        if(RcsSettings.getInstance()==null) {
            RcsSettings.createInstance(context);
        }
        RcsSettings.getInstance().setServiceActivationState(true);
        LauncherUtils.launchRcsService(context, false, true);
        
    }
}
