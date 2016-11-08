package com.mediatek.flp.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import com.android.location.provider.GeofenceProvider;
import android.hardware.location.GeofenceHardware;

public class GeofenceService extends Service {
 public final static String TAG = "FlpEM2.GeofenceService";
 private static GeofenceHardware sHardware;

 private GeofenceProvider mProvider = new GeofenceProvider() {

  @Override
  public void onGeofenceHardwareChange(GeofenceHardware hardware) {
   log("onGeofenceHardwareChange hardware=[" + hardware + "]");
   sHardware = hardware;
  }

 };

 public static boolean isHardwareReady() {
  return (sHardware != null);
 }

 public static GeofenceHardware hardware() {
  return sHardware;
 }

 @Override
 public IBinder onBind(Intent intent) {
  log("onBind");
  return mProvider.getBinder();
 }

 public static void log(Object msg) {
  Log.d(TAG, "" + msg);
 }

}
