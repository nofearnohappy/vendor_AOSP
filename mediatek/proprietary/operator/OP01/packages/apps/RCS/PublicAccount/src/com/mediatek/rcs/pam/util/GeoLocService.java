package com.mediatek.rcs.pam.util;

import android.content.Context;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

public final class GeoLocService {

    public static final String GET_LOC_ASYNC = "PA/GeoLocAsync";

    private static final String TAG = "PA/GeoLocService";
    private Context mContext;
    private LocationManager mLocationManager;
    private LocationListener mLocationListener;
    private String mProvider;
    private MyHandler mMyHandler;
    private static final int MSG_ID_GET_LOC_TIMEOUT = 0;
    private static final int GET_LOC_TIMEOUT_MSEC_1ST = 60 * 1000;
    private static final int GET_LOC_TIMEOUT_MSEC_2ND = 15 * 1000;


    private callback mCallerCallback;
    public interface callback {
        void queryGeoLocResult(boolean ret, final Location mLocation);
    }

    public GeoLocService(Context cntx) {
        mContext = cntx;
        mLocationManager = (LocationManager) mContext.getSystemService(Context.LOCATION_SERVICE);
        mLocationListener = new MyLocationListener();
        mMyHandler = new MyHandler(Looper.getMainLooper());
    }

    public boolean isEnable() {
        return mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
               mLocationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }

    public String queryCurrentGeoLocation(GeoLocService.callback cb) {
        mCallerCallback = cb;
        mProvider = null;
        /*
        Criteria criteria = new Criteria();
        //criteria.setCostAllowed(false);
        criteria.setAccuracy(Criteria.ACCURACY_FINE);
        mProvider = mLocationManager.getBestProvider(criteria, true);
        Log.i(TAG, "getBestProvider: " + providerName);

        if (providerName != null) {
            mMyHandler.removeMessages(MSG_ID_GET_LOC_TIMEOUT);
            mMyHandler.sendEmptyMessageDelayed(MSG_ID_GET_LOC_TIMEOUT, GET_LOC_TIMEOUT_MSEC);
            mLocationManager.requestLocationUpdates(mProvider, 1000, 0, mLocationListener);
            return GET_LOC_ASYNC;
        }
        */

        if (mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) &&
            mLocationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            mProvider = LocationManager.GPS_PROVIDER;
        } else if (mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            mProvider = LocationManager.GPS_PROVIDER;
        } else if (mLocationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            mProvider = LocationManager.NETWORK_PROVIDER;
        }

        Log.i(TAG, "getProvider: " + mProvider);
        if (mProvider != null) {
            mMyHandler.removeMessages(MSG_ID_GET_LOC_TIMEOUT);
            mMyHandler.sendEmptyMessageDelayed(MSG_ID_GET_LOC_TIMEOUT, GET_LOC_TIMEOUT_MSEC_1ST);
            mLocationManager.requestLocationUpdates(mProvider, 1000, 0, mLocationListener);
            return GET_LOC_ASYNC;
        }
        return null;
    }

    public void removeCallback() {
        Log.d(TAG, "removeCallback, mProvider=" + mProvider);
        cancelQueryGeoLocation();
    }

    private void cancelQueryGeoLocation() {
        Log.d(TAG, "cancelQueryGeoLocation, mProvider=" + mProvider);
        mLocationManager.removeUpdates(mLocationListener);
        mMyHandler.removeMessages(MSG_ID_GET_LOC_TIMEOUT);
        mCallerCallback = null;
    }

    private class MyLocationListener implements LocationListener {
        @Override
        public void onLocationChanged(final Location loc) {
            if (loc != null) {
                double latitude = loc.getLatitude();
                double longitude = loc.getLongitude();
                Log.i(TAG, "onLocationChanged. latitude:" + latitude + ",longtitude:" + longitude);
                mCallerCallback.queryGeoLocResult(true, loc);
            } else {
                Log.i(TAG, "onLocationChanged. location is null");
                mCallerCallback.queryGeoLocResult(false, null);
            }

            cancelQueryGeoLocation();
        }
        @Override
        public void onProviderDisabled(final String s) {
            Log.i(TAG, "onProviderDisabled, " + s);
        }
        @Override
        public void onProviderEnabled(final String s) {
            Log.i(TAG, "onProviderEnabled, " + s);
        }
        @Override
        public void onStatusChanged(final String s, final int i, final Bundle b) {
            Log.i(TAG, "onStatusChanged, " + s + ",i=" + i);
        }
    }


    private class MyHandler extends Handler {
        private MyHandler(Looper looper) {
            super(looper);
        }
        @Override
        public final void handleMessage(Message msg) {
           switch (msg.what) {
               case MSG_ID_GET_LOC_TIMEOUT :
                   Log.i(TAG, "handleMessage, MSG_ID_GET_LOC_TIMEOUT, mProvider=" + mProvider);
                   if (!(mProvider.equals(LocationManager.NETWORK_PROVIDER)) &&
                           mLocationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                       // try again with NLP
                       sendEmptyMessageDelayed(MSG_ID_GET_LOC_TIMEOUT, GET_LOC_TIMEOUT_MSEC_2ND);
                       mProvider = LocationManager.NETWORK_PROVIDER;
                       mLocationManager.removeUpdates(mLocationListener);
                       mLocationManager.requestLocationUpdates(mProvider, 1000, 0,
                               mLocationListener);
                   } else {
                       mCallerCallback.queryGeoLocResult(false, null);
                       cancelQueryGeoLocation();
                   }
                   break;

               default :
                   break;
            }
        }
    }
}
