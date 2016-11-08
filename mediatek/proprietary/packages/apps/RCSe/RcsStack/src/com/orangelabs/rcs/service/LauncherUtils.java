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

package com.orangelabs.rcs.service;

import java.util.Date;
import java.util.List;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import com.android.internal.telephony.TelephonyIntents;
import android.net.ConnectivityManager;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.preference.PreferenceManager;
import android.telephony.TelephonyManager;
import android.widget.Toast;
import android.telephony.SubscriptionManager;

import com.android.internal.telephony.TelephonyProperties;
import com.android.internal.telephony.IccCardConstants;
//import com.mediatek.common.featureoption.FeatureOption;
import com.orangelabs.rcs.plugin.apn.RcseOnlyApnUtils;
import com.orangelabs.rcs.addressbook.AccountChangedReceiver;
import com.orangelabs.rcs.addressbook.AuthenticationService;
import com.orangelabs.rcs.platform.AndroidFactory;
import com.orangelabs.rcs.platform.registry.AndroidRegistryFactory;
import com.orangelabs.rcs.provider.eab.ContactsBackup;
import com.orangelabs.rcs.provider.eab.ContactsBackupHelper;
import com.orangelabs.rcs.provider.eab.ContactsManager;
import com.orangelabs.rcs.provider.eab.RichAddressBookProvider;
import com.orangelabs.rcs.provider.ipcall.IPCallProvider;
import com.orangelabs.rcs.provider.messaging.ChatProvider;
import com.orangelabs.rcs.provider.messaging.FileTransferProvider;
import com.orangelabs.rcs.provider.settings.RcsSettings;
import com.orangelabs.rcs.provider.sharing.ImageSharingProvider;
import com.orangelabs.rcs.provider.sharing.VideoSharingProvider;
import com.orangelabs.rcs.provisioning.https.HttpsProvisioningService;
import com.orangelabs.rcs.utils.logger.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;

/**
 * Launcher utility functions
 *
 * @author hlxn7157
 */
public class LauncherUtils {
    /**
     * Logger
     */
    private static Logger logger = Logger.getLogger(LauncherUtils.class.getName());
    
    /**
     * Data SIM change listener
     */
    private static BroadcastReceiver defaultSimStateChangeListener = null;
/**
     * Last user account used
     */
    public static final String REGISTRY_LAST_USER_ACCOUNT = "LastUserAccount";

/** M: if no vodafone sim card, disable service @{ */
    private static final int[] OP01_NUMBERICS = {
            46000, 46001, 46002, 46007
    };

 /**
     * M: Debug mode flag.@{
     */
    //public static boolean sIsDebug = true;
    public static final String DEBUG_FORCEUSE_ONLYAPN_ACTION = "com.mediatek.rcse.service.ENABLE_ONLYAPN";
    public static final boolean DEBUG_ENABLE_ONLY_APN_FEATURE = false;

    /**
     * @}
     */
 /**
     * Key for storing the latest positive provisioning version
     */
    private static final String REGISTRY_PROVISIONING_VERSION = "ProvisioningVersion";
     
    public static final String CORE_CONFIGURATION_STATUS = "status";
     
    
 private static final String REGISTRY_CLIENT_VENDOR= "clientsvendor";
    private static final String REGISTRY_CLIENT_VERSION = "clientsversion";
    
    /**
     * Key for storing the latest positive provisioning validity
     */
    private static final String REGISTRY_PROVISIONING_VALIDITY = "ProvisioningValidity";
    
    /**
     * Key for storing the expiration date of the provisioning
     */
    private static final String REGISTRY_PROVISIONING_EXPIRATION = "ProvisioningExpiration";

    
    /**
     * secondary device mode is on
     */
    private final static boolean isSecondaryDevice = false;
    
    private static boolean WAITING_FOR_TELEPHONY = true;
    
    /**
     *  Subscription ID
     */
    private static Long mSubID = null;
    
    /**
	 * Launch the RCS service
	 * 
	 * @param context
	 *            application context
	 * @param boot
	 *            Boot flag
	 * @param user
	 *            restart is required by user
	 */
/**	public static void launchRcsService(Context context, boolean boot, boolean user) {
		// Instantiate the settings manager
		RcsSettings.createInstance(context);

		// Set the logger properties
		Logger.activationFlag = RcsSettings.getInstance().isTraceActivated();
		Logger.traceLevel = RcsSettings.getInstance().getTraceLevel();

		if (RcsSettings.getInstance().isServiceActivated()) {
			StartService.LaunchRcsStartService(context, boot, user);
		}
	}
     */
    /**
     * Launch the RCS service
     *
     * @param context application context
     * @param boot indicates if RCS is launched from the device boot
     */
    public static void launchRcsService(final Context context, boolean boot,boolean user) {
        
        // Instantiate the settings manager
        RcsSettings.createInstance(context);

        
        if (logger.isActivated()) {
            logger.debug("Launch RCS service ");
        }
        /*TelephonyManager telephoneMgr = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        int simState = telephoneMgr.getSimState();
        String sim = Integer.toString(simState);*/
        
        
     
        TelephonyManager telMgr = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        int simState = telMgr.getSimState();
     
        if (logger.isActivated()) {
            logger.debug("Launch RCS service WAITING_FOR_TELEPHONY= "+WAITING_FOR_TELEPHONY);
        }
        if(WAITING_FOR_TELEPHONY) {
            AsyncTask.execute(new Runnable(){
                public void run() {
                    registerSimModuleStateListener(context);
                }
            });
            
            if (logger.isActivated()) {
                logger.info("launchRcsService()-SIM state is not ready");
                logger.info("launchRcsService()-Register Receiver for SIM State");
            }
            return;
        } else {
            unregisterSimModuleStateListener(context);       
        }

        // Set the logger properties
		Logger.activationFlag = RcsSettings.getInstance().isTraceActivated();
		Logger.traceLevel = RcsSettings.getInstance().getTraceLevel();

		if (RcsSettings.getInstance().isServiceActivated() && RcsSettings.getInstance().isServiceGotPermission()) {
			if (logger.isActivated()) {
	            logger.debug("Launch RCS service (boot=" + boot + ")");
	        }
         
         /**
         * M: Added to send broadcast whether the device has SIM card. @{
         */
        if (context == null) {
            if (logger.isActivated()) {
                logger.info("launchRcsService()-context is null");
            }
            return;
        } else {
            if (!checkSimCard() && !LauncherUtils.getDebugMode(context) && (!isSecondaryDevice())) {
                if (logger.isActivated()) {
                    logger.info("launchRcsService()-checkSimCard return false");
                    logger.info("launchRcsService()-send no SIM card broadcast");
                }
                Intent intent = new Intent();
                intent.setAction(StartService.CONFIGURATION_STATUS);
                intent.putExtra(CORE_CONFIGURATION_STATUS, false);
                context.sendBroadcast(intent);

                if (!LauncherUtils.getDebugMode(context)) {
                    if (logger.isActivated()) {
                        logger.error("launchRcsService()-current release version");
                    }
                    return;
                } else {
                    if (logger.isActivated()) {
                        logger.error("launchRcsService()-current debug version");
                    }
                }
            } 
                else if (supportOP01()) {
                if(!checkOP01SimCard(context)&& RcsSettings.getInstance().getAutoConfigMode()==1) {
                    if (logger.isActivated()) {
                        logger.info("launchRcsService()-OP01 Load and Network not supported");
                        logger.info("launchRcsService()-No service up broadcast");
                    }
                    Intent intent = new Intent();
                    intent.setAction(StartService.CONFIGURATION_STATUS);
                    intent.putExtra(CORE_CONFIGURATION_STATUS, false);
                    context.sendBroadcast(intent);
                    return;
                }
                   
            }
            //if its secondary device
            else if (isSecondaryDevice()){
            	if (logger.isActivated()) {
                    logger.info("launchRcsService()-isSecondaryDevice True");
                    logger.info("launchRcsService()-Secondary device mode activated");
                }
            	 Intent intent = new Intent();
                 intent.setAction(StartService.CONFIGURATION_STATUS);
                 intent.putExtra(CORE_CONFIGURATION_STATUS, true);
                 context.sendStickyBroadcast(intent);
            	
            }
            else {
                if (logger.isActivated()) {
                    logger.info("launchRcsService()-checkSimCard return true");
                    logger.info("launchRcsService()-send has SIM card broadcast");
                }
                Intent intent = new Intent();
                intent.setAction(StartService.CONFIGURATION_STATUS);
                intent.putExtra(CORE_CONFIGURATION_STATUS, true);
                context.sendStickyBroadcast(intent);
            }
            /**
             * @}
             */
            
            StartService.LaunchRcsStartService(context, boot, user);
            /*
            Intent intent = new Intent(StartService.SERVICE_NAME);
	        intent.putExtra("boot", boot);
	        context.startService(intent);
	        */
		 }
		}
		else{
			 if (logger.isActivated()) {
                 logger.error("launchRcsService()-service not activated");
             }
		}
    }    
    
    public static boolean checkSimCard() {
        if (logger.isActivated()) {
            logger.debug("checkSimCard() entry");
        }
        return true;
    }

    private static boolean simCardMatched(String numberic) {
        if (logger.isActivated()) {
            logger.debug("simCardMatched() entry, numberic: " + numberic);
        }
        if ("-1".equals(numberic)) {
            return false;
        } else {
            return true;
		}
    }    
    

   private static boolean checkOP01SimCard(Context mContext) {
        if (logger.isActivated()) {
            logger.debug("checkOP01SimCard entry");
        }
        boolean hasCMCCSimCard = false;
        int subId=0;
       
       
        subId=(int)SubscriptionManager.getDefaultDataSubId();
        //temp patch for CR ALPS01963900
        
      //  subId = 1; //subid 1 is for SIM SLOT 1
        TelephonyManager tm = (TelephonyManager) mContext.getSystemService(
                Context.TELEPHONY_SERVICE);
        if (logger.isActivated()) {
            logger.debug("checkOP01SimCard entry: Sim SubID "+ subId);
        }
        
        if (logger.isActivated()) {
            logger.debug("checkOP01SimCard entry: operator MNC+MCC" +tm.getSimOperator(subId));
        }
        if (isOP01SimCard(tm.getSimOperator(subId))) {
            return true;
        }
        else 
            return false;

        
    }

    private static boolean isOP01SimCard(String numberic) {
        if (logger.isActivated()) {
            logger.debug("isOP01SimCard entry, numberic: " + numberic);
        }
        if (!numberic.isEmpty()) {
        boolean result = binarySearch(OP01_NUMBERICS, Integer.valueOf(numberic));
        if (logger.isActivated()) {
            logger.debug("isOP01SimCard exit, result: " + result);
        }
        return result;
    }
        return false;
    }

    private static boolean binarySearch(int[] list, int value) {
        int low = 0;
        int mid = 0;
        int high = list.length - 1;
        while (low <= high) {
            mid = (high + low) / 2;
            if (list[mid] == value) {
                return true;
            }
            if (list[mid] < value) {
                low = mid + 1;
            } else {
                high = mid - 1;
		}
    }    
        return false;
    }

    /** @} */
    
    /**
     * Launch the RCS core service
     *
     * @param context Application context
     */
    public static void launchRcsCoreService(final Context context) {
        if (logger.isActivated()) {
            logger.debug("Launch core service");
        }
        if (RcsSettings.getInstance().isServiceActivated()) {
            /**
             * M: Added to achieve the RCS-e only APN feature.@{
             */
        	//@tct-stack wuquan add IsProvisioningExpirationValid() to check the expiration is valid before start rcse coreo service
        	if (RcsSettings.getInstance().isUserProfileConfigured() /*&& IsProvisioningExpirationValid(context)*//*&& isValidVersion()*/) {
        	        Thread t = new Thread(){
            	        public void run(){
            	            Intent intent = new Intent(context, RcsCoreService.class);
                    context.startService(intent);
            	        }
        	        };
        	        t.start();
                
	        } else {
		        if (logger.isActivated()) {
		            logger.debug("RCS service not configured");
		        }
	        }
            /**
             * @}
             */
        } else {
	        if (logger.isActivated()) {
	            logger.debug("RCS service is disabled");
	        }        	
        }
    }

    private static boolean isValidVersion() {
		// TODO Auto-generated method stub
    	if(Integer.parseInt(RcsSettings.getInstance().getProvisioningVersion())>0)
    		return true;
		return false;
	}

    /**
     * Force launch the RCS core service
     *
     * @param context Application context
     */
    // TODO: not used.
    public static void forceLaunchRcsCoreService(final Context context) {
        if (logger.isActivated()) {
            logger.debug("Force launch core service");
        }
    	if (RcsSettings.getInstance().isUserProfileConfigured()) {
            RcsSettings.getInstance().setServiceActivationState(true);
            Thread t = new Thread(){
                public void run(){
                    context.startService(new Intent(context, RcsCoreService.class)); 
                }
            };
            t.start();
                   
        } else {
            if (logger.isActivated()) {
                logger.debug("RCS service not configured");
            }
        }
    }

    /**
     * Stop the RCS service
     *
     * @param context Application context
     */
    public static void stopRcsService(Context context) {
        if (logger.isActivated()) {
            logger.debug("Stop RCS service");
        }
        context.stopService(new Intent(context, StartService.class));
        context.stopService(new Intent(context, HttpsProvisioningService.class));
        context.stopService(new Intent(context, RcsCoreService.class));
    }
    /**
     * Stop the RCS core service (but keep provisioning)
     *
     * @param context Application context
     */
    public static void stopRcsCoreService( Context context) {
        if (logger.isActivated()) {
            logger.debug("Stop RCS core service");
        }
        context.stopService(new Intent(context, StartService.class));
       // context.stopService(new Intent(context, HttpsProvisioningService.class));
        context.stopService(new Intent(context, RcsCoreService.class));
    }

    /**
     * Reset RCS config
     *
     * @param context Application context
     */
    public static void resetRcsConfig(Context context) {
        if (logger.isActivated()) {
            logger.debug("Reset RCS config");
        }

        // Stop the Core service
        context.stopService(new Intent(context, RcsCoreService.class));

        // Reset user profile
        RcsSettings.createInstance(context);
        RcsSettings.getInstance().resetUserProfile();

        // Clean the RCS database
        ContactsManager.createInstance(context);
        ContactsManager.getInstance().deleteRCSEntries();

        // Remove the RCS account 
        AuthenticationService.removeRcsAccount(context, null);
        
        // Ensure that factory is set up properly to avoid NullPointerException in AccountChangedReceiver.setAccountResetByEndUser
        //AndroidFactory.setApplicationContext(context);
        AccountChangedReceiver.setAccountResetByEndUser(false);

        // Clean terms status
        RcsSettings.getInstance().setProvisioningTermsAccepted(false);
    }

    /**
     * Get the last user account
     *
     * @param context Application context
     * @return last user account
     */
    public static String getLastUserAccount(Context context) {
        SharedPreferences preferences = context.getSharedPreferences(AndroidRegistryFactory.RCS_PREFS_NAME_ACC, Activity.MODE_PRIVATE);
        if (logger.isActivated()) {
            logger.debug("Last User Account from preference:- "+ preferences.getString(REGISTRY_LAST_USER_ACCOUNT, null));
        }
        return preferences.getString(REGISTRY_LAST_USER_ACCOUNT, null);
    }

    /**
     * Set the last user account
     *
     * @param context Application context
     * @param value last user account
     */
    public static void setLastUserAccount(Context context, String value) {
        SharedPreferences preferences = context.getSharedPreferences(AndroidRegistryFactory.RCS_PREFS_NAME_ACC, Activity.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(REGISTRY_LAST_USER_ACCOUNT, value);
        editor.commit();
        if (logger.isActivated()) {
            logger.debug("Setting Last User Account on preference:- "+ preferences.getString(REGISTRY_LAST_USER_ACCOUNT, null));
        }
    }

    /**
     * Get current user account
     *
     * @param context Application context
     * @return current user account
     */
    public static String getCurrentUserAccount(Context context) {
        TelephonyManager mgr = (TelephonyManager)context.getSystemService(Context.TELEPHONY_SERVICE);
        int subId=0;
        subId=(int)SubscriptionManager.getDefaultDataSubId();
        String currentUserAccount = mgr.getSubscriberId(subId);
        mgr = null;
        return currentUserAccount;
    }

    /** M: Reset all RCS-e config @{ */
/**
     * Reset all RCS-e config
     * 
     * @param context application context
*/
    public static void resetAllRcsConfig(Context context) {
        if (logger.isActivated()) {
            logger.debug("Reset all RCS-e config");
        }
        // Clean the RCS user profile
        RcsSettings.getInstance().removeAllUsersProfile();
        // Clean the RCS databases
        ContactsManager.createInstance(context);
        ContactsManager.getInstance().deleteRCSEntries();
		
        // Remove the RCS account
        AuthenticationService.removeRcsAccount(context, null);
        
        resetBackupAccounts(context);
	}
		
    //Delete account from shared preference
    private static void resetBackupAccounts(Context context) {
        if (logger.isActivated()) {
            logger.debug("resetBackupAccount");
	}
        SharedPreferences preferences = context.getSharedPreferences(
                AndroidRegistryFactory.RCS_PREFS_NAME, Activity.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(StartService.REGISTRY_LAST_FIRST_USER_ACCOUNT, "");
        editor.putString(StartService.REGISTRY_LAST_SECOND_USER_ACCOUNT, "");
        editor.putString(StartService.REGISTRY_LAST_THIRD_USER_ACCOUNT, "");
        editor.commit();
    }
    /** @} */

    /** 
     * M: Added for checking whether the configuration is validity @{ 
     */
    public static long isProvisionValidity(){
        long provisionValidify = RcsSettings.getInstance().getProvisionValidity();
        long provisionTime = RcsSettings.getInstance().getProvisionTime();
        long currentTime = System.currentTimeMillis();
        
        long expirationDate = RcsSettings.getInstance().getProvisioningExpirationDate();

        long diff = expirationDate - currentTime;
	
//		 Boolean debugState = false ;
		 if(logger.isActivated()){
            logger.debug("isProvisionValidity(), provisionValidify = "
                    + provisionValidify + ", expirationDate = " + expirationDate
                    + ", currentTime = " + currentTime + ", diff = " + diff);
        }
        if (diff < 0) {
            return diff;
        }
        
        return provisionValidify - diff;
    }
/**
	 * Get the latest positive provisioning version
	 * 
	 * @param context
	 *            Application context
	 * @return the latest positive provisioning version
	 */
	public static String getProvisioningVersion(Context context) {
			 SharedPreferences preferences = context.getSharedPreferences(AndroidRegistryFactory.RCS_PREFS_NAME, Activity.MODE_PRIVATE);
		return preferences.getString(REGISTRY_PROVISIONING_VERSION, "0");
		 }
		 
	/**
	 * Save the latest positive provisioning version in shared preferences
	 * 
	 * @param context
	 *            Application context
	 * @param value
	 *            the latest positive provisioning version
	 */
	public static void saveProvisioningVersion(Context context, String value) {
		try {
			int vers = Integer.parseInt(value);
			if (vers > 0) {
				SharedPreferences preferences = context.getSharedPreferences(AndroidRegistryFactory.RCS_PREFS_NAME, Activity.MODE_PRIVATE);
				SharedPreferences.Editor editor = preferences.edit();
				editor.putString(REGISTRY_PROVISIONING_VERSION, value);
				editor.commit();
			}
		} catch (NumberFormatException e) {
		}
	}
		     
	
	public static String getClient(Context context) {
		String defaultString = "";
		if(!logger.isActivated()){
			return defaultString;
		}
		
		SharedPreferences preferences = context.getSharedPreferences(AndroidRegistryFactory.RCS_DEBUG_PREFS_NAME, Activity.MODE_PRIVATE);
		return preferences.getString(REGISTRY_CLIENT_VENDOR, "WITS");
		 }
	//	 return debugState;
		 
		 
	public static String getClientVersion(Context context) {
		String defaultString = "";
		if(!logger.isActivated()){
			return defaultString;
	}

		SharedPreferences preferences = context.getSharedPreferences(AndroidRegistryFactory.RCS_DEBUG_PREFS_NAME, Activity.MODE_PRIVATE);
		return preferences.getString(REGISTRY_CLIENT_VERSION,  "RCSAndrd-1.4");
	}
public static void saveClient(Context context, String value) {
		
		
		if(!logger.isActivated()){
			return;
		}
		
		String clientsVendor = "";
		String clientVersion = "";
		try {
			
			   if(value.equals("MTI")){
				   clientsVendor = "";
				   clientVersion ="";
			   }
			   else if (value.equals("WIT")){
				   clientsVendor = "WITS";
				   clientVersion = "RCSAndrd-1.4";
			   }
			   
				SharedPreferences preferences = context.getSharedPreferences(AndroidRegistryFactory.RCS_DEBUG_PREFS_NAME, Activity.MODE_PRIVATE);
				SharedPreferences.Editor editor = preferences.edit();
				editor.putString(REGISTRY_CLIENT_VENDOR, clientsVendor);
				editor.putString(REGISTRY_CLIENT_VERSION, clientVersion);
				editor.commit();
			
		} catch (NumberFormatException e) {
		}
	}

		
	
	/**
	 * Get the expiration date of the provisioning
	 * 
	 * @param context
	 *            Application context
	 * @return the expiration date
	 */
	public static Date getProvisioningExpirationDate(Context context) {
			SharedPreferences preferences = context.getSharedPreferences(AndroidRegistryFactory.RCS_PREFS_NAME, Activity.MODE_PRIVATE);
		Long expiration = preferences.getLong(REGISTRY_PROVISIONING_EXPIRATION, 0L);
		if (expiration > 0L) {
			return new Date(expiration);
		}   		
		return null;
	}

	 
	public static Long getProvisioningExpirationTime(Context context) {
		SharedPreferences preferences = context.getSharedPreferences(AndroidRegistryFactory.RCS_PREFS_NAME, Activity.MODE_PRIVATE);
		Long expiration = preferences.getLong(REGISTRY_PROVISIONING_EXPIRATION, 0L);
		return expiration;	
        }
	/**
	 * Get the expiration date of the provisioning
	 * 
	 * @param context
	 *            Application context
	 * @return the expiration date in seconds
	 */
	public static Long getProvisioningValidity(Context context) {
		SharedPreferences preferences = context.getSharedPreferences(AndroidRegistryFactory.RCS_PREFS_NAME, Activity.MODE_PRIVATE);
		Long validity = preferences.getLong(REGISTRY_PROVISIONING_VALIDITY, 24*3600L);
		if (validity > 0L) {
			return validity;
        }
		return null;
        }
	/**
	 * Save the provisioning validity in shared preferences
	 * 
	 * @param context
	 * @param validity
	 *            validity of the provisioning expressed in seconds
	 */
	public static void saveProvisioningValidity(Context context, long validity) {			
		if (validity > 0L) {
			// Calculate next expiration date in msec
			long next = System.currentTimeMillis() + validity * 1000L;
			SharedPreferences preferences = context.getSharedPreferences(AndroidRegistryFactory.RCS_PREFS_NAME, Activity.MODE_PRIVATE);
			SharedPreferences.Editor editor = preferences.edit();
			editor.putLong(REGISTRY_PROVISIONING_VALIDITY, validity);
			editor.putLong(REGISTRY_PROVISIONING_EXPIRATION, next);
			editor.commit();
            }
            }
	
public static void setDebugMode(Context context, boolean value ){
		
		if(logger.isActivated()){
			SharedPreferences preferences = context.getSharedPreferences(AndroidRegistryFactory.RCS_PREFS_NAME, Activity.MODE_PRIVATE);
			SharedPreferences.Editor editor = preferences.edit();
			editor.putBoolean("DEBUG", value);
			editor.commit();
		}   		
	}

	public static boolean getDebugMode(Context context){
	
		 Boolean debugState = false ;
        if (logger.isActivated()) {
			 SharedPreferences preferences = context.getSharedPreferences(AndroidRegistryFactory.RCS_PREFS_NAME, Activity.MODE_PRIVATE);
			 debugState = preferences.getBoolean("DEBUG",false);     
        }
		 return debugState;
		 
        }

	public static boolean isSecondaryDevice(){
	    return isSecondaryDevice;	
            }

        public static String getConfigurationType(Context context) {
		String defaultString = "";
		if(!logger.isActivated()){
			return defaultString;
            }

		SharedPreferences sharedPrefs = PreferenceManager
        .getDefaultSharedPreferences(context);
		
		String configurationType= sharedPrefs.getString("prefConfig", "Live");
		return configurationType;
            }
	
	
	public static boolean isLoggerActivated(){
		boolean isLogger= false;
		isLogger= logger.isActivated();
		return isLogger;
		
        }

    /**@ */
 /**M
 * added to save messages on user demand account
 */  
     /** 
      * backup account messages
      * 
      *
      */
    public static void backupMessage(Context context, String path) {
    	
    	 
    	String account= getCurrentUserAccount(context);
    	RichAddressBookProvider.backupAddressBookDatabase(account,path);
    	IPCallProvider.backupIPCallDatabase(account,path);
    	FileTransferProvider.backupFileTransferDatabase(account,path);
    	ChatProvider.backupChatDatabase(account,path);
    	ImageSharingProvider.backupImageSharingDatabase(account,path);
    	VideoSharingProvider.backupVideoSharingDatabase(account,path);
    	/*List<String> contactList = ContactsManager.getInstance().getAllContacts();
    	if(logger.isActivated()){
            logger.debug("Contacts Backup before inserting size " +contactList.size()
                   );
    	}*/
    //	ContactsBackupHelper.getInstance().insertContacts(contactList);
//	ContactsBackupHelper.deleteOldDb();
		//if(contactList!=null)
			if(ContactsBackupHelper.getInstance()==null)
				ContactsBackupHelper.createInstance(context);
			ContactsBackupHelper.getInstance().onContactsBackup(context);
		//	LauncherUtils.uploadFile(context);
    	if(logger.isActivated()){
            logger.debug("Contacts Backup with path " +path
                   );
        }
    	ContactsBackup.backupContactsDatabase(account,path);
	    // TODO Auto-generated method stub
	
            }
    
    public static void restoreMessages(Context context, String path)
    {
    	String account= getCurrentUserAccount(context);
    	if(logger.isActivated()){
            logger.debug("path for restore " +path
                   );
    	}
    	boolean mAddressbook=RichAddressBookProvider.restoreAddressBookDatabase(account, path);
    	boolean mIPCall=IPCallProvider.restoreIPCallDatabase(account, path);
    	boolean mFileTransfer=FileTransferProvider.restoreFileTransferDatabase(account, path);
    	boolean mChat=ChatProvider.restoreChatDatabase(account, path);
    	boolean mImageSharing=ImageSharingProvider.restoreImageSharingDatabase(account, path);
    	boolean mVideoSharing=VideoSharingProvider.restoreVideoSharingDatabase(account, path);
    	boolean mContactBackup=ContactsBackup.restoreContactsDatabase(account,path);
    	if(logger.isActivated()){
            logger.debug("inserting contact to native " 
                   );
    	}
    	if(ContactsBackupHelper.getInstance()==null)
			ContactsBackupHelper.createInstance(context);
    	ContactsBackupHelper.getInstance().insertContactToNative(context);
    	
    	String fileRestored="";
    	if(mAddressbook){fileRestored += "AddressBook";}
    	if(mIPCall){fileRestored += ", IPCall";}
    	if(mChat){fileRestored += ", mChat";}
    	if(mImageSharing){fileRestored += ", ImageSharing";}
    	if(mVideoSharing){fileRestored += ", VideoSharing";}
    	if(mFileTransfer){fileRestored += ", FileTransfer";}
    	if(fileRestored=="")
    	{
    		fileRestored="File not Restored:- Database file not found";
    	}
    	else {
    		fileRestored +=" Databases restored";
    	}
    	Toast.makeText(context, fileRestored, 
    			   Toast.LENGTH_LONG).show();
    }

    public static void backupMessage(Context context) {
    		
   	 
    	String account= getCurrentUserAccount(context);
    	RichAddressBookProvider.backupAddressBookDatabase(account);
    	IPCallProvider.backupIPCallDatabase(account);
    	FileTransferProvider.backupFileTransferDatabase(account);
    	ChatProvider.backupChatDatabase(account);
    	ImageSharingProvider.backupImageSharingDatabase(account);
    	VideoSharingProvider.backupVideoSharingDatabase(account);
    	/*List<String> contactList = ContactsManager.getInstance().getAllContacts();
    	if(logger.isActivated()){
            logger.debug("Contacts Backup before inserting size " +contactList.size()
                   );
    	}*/
    //	ContactsBackupHelper.getInstance().insertContacts(contactList);
//	ContactsBackupHelper.deleteOldDb();
		//if(contactList!=null)
			if(ContactsBackupHelper.getInstance()==null)
				ContactsBackupHelper.createInstance(context);
			ContactsBackupHelper.getInstance().onContactsBackup(context);
		//	LauncherUtils.uploadFile(context);
    	if(logger.isActivated()){
            logger.debug("Contacts Backup without path"
                   );
        }
    	ContactsBackup.backupContactsDatabase(account);
	    // TODO Auto-generated method stub
	
            }
    
   public static void restoreMessages(Context context)
    {
   	String account= getCurrentUserAccount(context);
    	RichAddressBookProvider.restoreAddressBookDatabase(account);
    	IPCallProvider.restoreIPCallDatabase(account);
    	FileTransferProvider.restoreFileTransferDatabase(account);
    	ChatProvider.restoreChatDatabase(account);
    	ImageSharingProvider.restoreImageSharingDatabase(account);
    	VideoSharingProvider.restoreVideoSharingDatabase(account);
    }

    
    public static void uploadFile(Context context){
    	String account= getCurrentUserAccount(context);
    	ContactsBackup.backupContactsDatabase(account);
    	try {
    	    // Set your file path here
    	    FileInputStream fstrm = new FileInputStream(Environment.getDataDirectory() + "/data/databases/" +  "contacts" +"_"+ account +".db");

    	    // Set your server page url (and the file title/description)
    	    UploadContacts uc = new UploadContacts("http://www.myurl.com/fileup.aspx", "my file title","my file description");

    	    uc.Send_Now(fstrm);

    	  } catch (FileNotFoundException e) {
    	    // Error: File not found
        }
    }
    
    //@tct-stack wuquan add perso control
      //@tct-stack wuquan add IsProvisioningExpirationValid() to check the expiration is valid before start rcse coreo service start
        public static boolean IsProvisioningExpirationValid(Context context) {
            boolean result = false;
            Date expiration = LauncherUtils.getProvisioningExpirationDate(context);
            if (expiration != null) {
                Date now = new Date();
                if (expiration.after(now)) {
                    if (logger.isActivated())
                        logger.debug("Configuration validity has not expired");
                   result = true;
                }
            }
            return result;
        }
      //@tct-stack wuquan add IsProvisioningExpirationValid() to check the expiration is valid before start rcse coreo service end
    
        public static boolean isCMCCOperator() {
            // TODO Auto-generated method stub
            return false;
        }

        public static boolean supportOP01(Context context) {

            String optr = SystemProperties.get("ro.operator.optr");
            if (logger.isActivated())
                logger.debug("Operator Supported" + optr);
            if (optr!=null && optr.equalsIgnoreCase("op01")){
                TelephonyManager tm = (TelephonyManager) context.getSystemService(
                        Context.TELEPHONY_SERVICE);
                RcsSettings.getInstance().setNetworkOperator("CMCC");    
                return true;
            }
            return false;
        }

        public static boolean supportOP01() {

            String optr = SystemProperties.get("ro.operator.optr");
            if (logger.isActivated())
                logger.debug("Operator Supported" + optr);
            if (optr!=null && optr.equalsIgnoreCase("op01")){ 
                return true;
            }
            return false;
        }


        public static Long getsubId() {
            if (logger.isActivated()) {
                logger.debug("checkOP01SimCard entry: Sim SubID "+ mSubID);
            }
            // TODO Auto-generated method stub
            return mSubID;
        }
    
        
        /**
         * Register the broadcast receiver for SIM state 
         */
        protected static void registerSimModuleStateListener(Context context) {
         
            // Check if network state listener is already registered
            if (defaultSimStateChangeListener != null) {
                if (logger.isActivated()) {
                    logger.debug(" Default data Sim change listener already registered");
                }
                return;
            }

            if (logger.isActivated()) {
                logger.debug("Registering default data SIM change listener");
            }

            // Instantiate the network state listener
            defaultSimStateChangeListener = new BroadcastReceiver() {
                @Override
                public void onReceive(final Context context, final Intent intent) {
                    String stateExtra=intent.getStringExtra(IccCardConstants.INTENT_KEY_ICC_STATE);
                    if (IccCardConstants.INTENT_VALUE_ICC_LOADED.equals(stateExtra)) {
                      //this.simState=IccCard.State.ABSENT;
                        Thread t = new Thread() {
                            public void run() {
                                if (logger.isActivated()) {
                                    logger.debug("Default data SIM change listener - Received broadcast: "
                                            + intent.toString());
                                }
                                WAITING_FOR_TELEPHONY=false;
                                launchRcsService(context, true, false);
                               // AndroidFactory.getApplicationContext().connectionEvent(ConnectivityManager.CONNECTIVITY_ACTION);
                            }
                        };
                        t.start();
                    }
                   // IccCardContants.INTENT_KEY_ICC_STATE = IccCardConstants.INTENT_VALUE_ICC_LOADED

                }
            };

            // Register network state listener
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(TelephonyIntents.ACTION_SIM_STATE_CHANGED);
            context.getApplicationContext().registerReceiver(defaultSimStateChangeListener, intentFilter);
        }

        /**
         * Unregister the broadcast receiver for network state
         */
        public static void unregisterSimModuleStateListener(Context context) {
            if (defaultSimStateChangeListener != null) {
                if (logger.isActivated()) {
                    logger.debug("Unregistering default data SIM change listener");
                }

                try {
                    WAITING_FOR_TELEPHONY=true;
                    context.getApplicationContext().unregisterReceiver(defaultSimStateChangeListener);
                } catch (IllegalArgumentException e) {
                    // Nothing to do
                }
                defaultSimStateChangeListener = null;
            }
        }
/**@ */
}
