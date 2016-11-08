/******************************************************************************
 *  This program is protected under international and U.S. copyright laws as
 *  an unpublished work. This program is confidential and proprietary to the
 *  copyright owners. Reproduction or disclosure, in whole or in part, or the
 *  production of derivative works therefrom without the express permission of
 *  the copyright owners is prohibited.
 *
 *                 Copyright (C) 2014 by Dolby Laboratories,
 *                             All rights reserved.
 ******************************************************************************/

/**
 * @defgroup    ds DsBase
 * @details     Base class of main API for controlling DS.
 *
 *              This API can be used by media activities to control the
 *              global DS audio effect integrated on the device.
 *              The implementation uses inter-process communication to interact
 *              with the DS service running on the device.
 * @{
 */

package com.dolby.api;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.media.audiofx.AudioEffect;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.util.Log;

/*
 * The comment below will appear as the Detailed Description on the class
 * documentation page. The text in @details tag above appears as the Detailed
 * Description on the Modules page.
 */
/**
 * Base class of main API for controlling DS.
 *
 */
class DsBase
{
    private static final String TAG = "DsBase";

    /**
     * The API version number, increase by one when API is updated.
     *
     *  @internal
     */
    private static final int VERSION = 1;

    /**
     * The interface for controlling the DsService.
     *
     */
    protected IDs iDs_ = null;

    /**
     * The listener for the access callbacks.
     *
     */
    protected IDsAccessEvents accessListener_ = null;

    /**
     * Context for bind/unbind DsService.
     *
     */
    protected Context context_ = null;

    /**
     * Information of the client binding to DsService.
     *
     */
    protected DsClientInfo DsClientInfo_ = null;

    /**
     * The handle value of the client.
     *
     */
    protected int clientHandle_ = 0;

    /**
     * The access right which an application is used.
     *
     */
    protected int connectionBridge_ = DsAccess.ACCESS_NONE;

    /**
     * Convert the error code into a runtime exception.
     *
     */
    protected void convertErrorCodeToException (int errorCode)
    {
        if(errorCode >= DsCommon.DS_NO_ERROR)
            return;
        
        Log.d(TAG, "convertErrorCodeToException " + errorCode);
        RuntimeException e;
        switch (errorCode)
        {
            case DsCommon.DS_INVALID_ARGUMENT:
                IllegalArgumentException ex = new IllegalArgumentException("Exception: invalid argument.");
                throw ex;
            case DsCommon.DS_NOT_RUNNING:
                e = new RuntimeException("Exception: DS not running.");
                throw e;
            case DsCommon.DS_CANNOT_LOAD_SETTINGS:
                e = new RuntimeException("Exception: can not load settings.");
                throw e;
            case DsCommon.DS_CANNOT_LOAD_TUNINGS:
                e = new RuntimeException("Exception: can not load tunings.");
                throw e;
            case DsCommon.DS_UNKNOWN_ERROR:
                e = new RuntimeException("Exception: unknown problem.");
                throw e;
            default:
                break;
        }
    }

    /**
     * When any exception is caught, new a RuntimeException with the information and throw it.
     *
     */
    protected void handleException (Exception e, String methodName)
    {
        String msg = "Exception in " + methodName;
        Log.e(TAG, msg + " " + e.toString());
        e.printStackTrace();
        throw new RuntimeException(msg, e);
    }

    /**
     * Store the information of current client.
     *
     */
    protected void setConnectionInfo(int access)
    {
        connectionBridge_ = access;
    }

    /**
     * Binds to the DS Service so that DS API calls can be made on this object.
     * This method must be called before calling other DS methods.
     *
     * @param context The object binds to the DsService.
     * @param listener The object interested in the access events.
     *
     * @return True if you have successfully bound to the service, false otherwise.
     */
    public boolean registerClient(Context context, IDsAccessEvents listener)
    {
        DsLog.log1(TAG, "registerClient");
        int error = DsCommon.DS_UNKNOWN_ERROR;
        
        try
        {
            if ((listener != null) && (context != null))
            {
                context_ = context;
                accessListener_ = listener;
                DsClientInfo_ = new DsClientInfo();
                DsClientInfo_.setPackageName(context_.getPackageName());
                DsClientInfo_.setConnectionBridge(connectionBridge_);
                error = DsCommon.DS_NO_ERROR;
            }
            else
            {
                error = DsCommon.DS_INVALID_ARGUMENT;
            }
        }
        catch(Exception e)
        {
            handleException(e, "registerClient");
        }
        convertErrorCodeToException(error);

        if(context != null)
        {
            Intent bindIntent = new Intent(IDs.class.getName());

            // Establish a couple connections with the service, binding by interface names. This allows other applications to be
            // installed that replace the remote service by implementing the same interface.
            return context.bindService(bindIntent, connection_, Context.BIND_AUTO_CREATE);
        }
        return false;
    }

    /**
     * Unbinds from the DsService.
     * After this call, no DS methods should be called on this object except for registerClient.
     * This method must be called when an application no-longer needs to communicate with the DsService
     * or make any further DS method calls.
     * Service will continue to run in the background.
     * All settings changes done by App will be lost after unregister.
     */
    public void unregisterClient()
    {
        DsLog.log1(TAG, "unregisterClient");

        if(iDs_ != null)
        {
            try
            {
                iDs_.iUnregisterDsAccess(clientHandle_);
                iDs_.iUnregisterDeathHandler(clientHandle_, deathHandler_);
            }
            catch(Exception e)
            {
                handleException(e, "unregisterClient");
            }
            // Detach our existing connection.
            if(context_ != null)
            {
                context_.unbindService(connection_);
                context_ = null;
            }
            iDs_ = null;
        }
        accessListener_ = null;
    }

    /**
     * Get the version of the DS Android integration.
     *
     * @return The version string of the DS Android integration.
     */
    public String getDsVersion()
    {
        String version = "";
        String[] paramString = new String[1];
        int error = 0;
        try
        {
            error = iDs_.iGetDsServiceVersion(paramString);
        }
        catch(Exception e)
        {
            handleException(e, "getDsVersion");
        }
        convertErrorCodeToException(error);
        version = paramString[0];
        //TODO: update with the final version string
        return version;
    }

    /**
     * Get the version of the DS API.
     *
     * @throws DsRuntimeException if there is an internal error.
     * @throws RuntimeException if get version failed.
     *
     * @return The version of the DS API.
     */
    public int getApiVersion() /*throws DsRuntimeException, RuntimeException*/
    {
        return VERSION;
    }

    /**
     * Request a specific access right.
     *
     * @param accessRight The type of the access right.
     *
     * @return DsConstants.DS_NO_ERROR - Success.
     *         or other warnings defined in DsAccess.java.
     */
    public int requestAccessRight(int accessRight)
    {
        int error = 0;
        try
        {
            error = iDs_.iRequestAccessRight(clientHandle_, accessRight);
        }
        catch(Exception e)
        {
            handleException(e, "requestAccessRight");
        }
        return error;
    }

    /**
     * Abandon a specific access right.
     *
     * @param accessRight The type of the access right.
     *
     * @return DsConstants.DS_NO_ERROR - Success.
     *         or other warnings defined in DsConstants.java.
     */
    public int abandonAccessRight(int accessRight)
    {
        int error = 0;
        try
        {
            error = iDs_.iAbandonAccessRight(clientHandle_, accessRight);
        }
        catch(Exception e)
        {
            handleException(e, "abandonAccessRight");
        }
        return error;
    }

    /**
     * Get the access right current client can request.
     *
     * @return The type of the access right current client can request.
     */
    public int getAvailableAccessRight()
    {
        int ret_val = -1;
        // This method should be implemented differently in each derived classes.
        return ret_val;
    }

    /**
     * Query if a specific access right already hold by another clients.
     *
     * @param accessRight The type of the access right.
     *
     * @return The state of a specific access right.
     */
    public int checkAccessRight(int accessRight)
    {
        int ret_val = -1;
        int[] paramInt = new int[1];
        int error = 0;
        try
        {
            error = iDs_.iCheckAccessRight(clientHandle_, accessRight, paramInt);
        }
        catch(Exception e)
        {
            handleException(e, "checkAccessRight");
        }
        convertErrorCodeToException(error);
        ret_val = paramInt[0];
        return ret_val;
    }

    /**
     * The connection return from the service.
     *
     */
    protected ServiceConnection connection_ = new ServiceConnection()
    {
        public void onServiceConnected(ComponentName className, IBinder service)
        {
            DsLog.log1(TAG, "ServiceConnection.onServiceConnected()");

            iDs_ = IDs.Stub.asInterface(service);

            clientHandle_ = this.hashCode();
            try
            {
                iDs_.iRegisterDeathHandler(clientHandle_, deathHandler_);
                iDs_.iRegisterDsAccess(clientHandle_, DsClientInfo_);
            }
            catch(Exception e)
            {
                handleException(e, "iRegisterDsAccess");
            }

            if (accessListener_ != null)
            {
                accessListener_.onClientConnected();
            }

            DsLog.log3(TAG, "CONNECTED: DsService");
        }

        public void onServiceDisconnected(ComponentName className)
        {
            DsLog.log1(TAG, "ServiceConnection.onServiceDisconnected()");

            if (accessListener_ != null)
            {
                accessListener_.onClientDisconnected();
            }

            iDs_ = null;

            DsLog.log3(TAG, "/ServiceConnection.onServiceDisconnected()");
        }
    };

    /**
     * The deathHandler_ to make DsService listen the death of DS client.
     *
     *  @internal
     */
    private IDsDeathHandler deathHandler_ = new IDsDeathHandler.Stub()
    {
        public void onClientDied()
        {
            // not implement
        } 
    };
}

/**
 * @}
 */
