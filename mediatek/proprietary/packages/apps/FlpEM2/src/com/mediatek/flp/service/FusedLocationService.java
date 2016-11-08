package com.mediatek.flp.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import com.android.location.provider.FusedLocationHardware;
import com.android.location.provider.FusedProvider;

public class FusedLocationService extends Service {
 public final static String TAG = "FlpEM2.FusedLocationService";
 private static FusedLocationHardware sHardware;

 private FusedProvider mFusedProvider = new FusedProvider() {
  @Override
  public void setFusedLocationHardware(FusedLocationHardware hardware) {
   log("setFusedLocationHardware hardware=[" + hardware + "]");
   sHardware = hardware;
  }
 };

 public static boolean isHardwareReady() {
  return (sHardware != null);
 }

 public static FusedLocationHardware hardware() {
  return sHardware;
 }

 @Override
 public IBinder onBind(Intent intent) {
  log("onBind");
  return mFusedProvider.getBinder();
 }

 public static void log(Object msg) {
  Log.d(TAG, "" + msg);
 }

}
