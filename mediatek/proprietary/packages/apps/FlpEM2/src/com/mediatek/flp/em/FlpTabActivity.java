package com.mediatek.flp.em;

import android.app.TabActivity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.TabHost;

@SuppressWarnings("deprecation")
public class FlpTabActivity extends TabActivity {
 TabHost mTabHost;

 @Override
 protected void onCreate(Bundle savedInstanceState) {
  super.onCreate(savedInstanceState);
  setContentView(R.layout.tab);

  mTabHost = getTabHost();

  mTabHost.addTab(mTabHost.newTabSpec("Diagnostic")
    .setIndicator("Diagnostic")
    .setContent(new Intent().setClass(this, FlpDiagnostic.class)));

  mTabHost.addTab(mTabHost.newTabSpec("Status").setIndicator("Status")
    .setContent(new Intent().setClass(this, FlpStatus.class)));

  mTabHost.addTab(mTabHost
    .newTabSpec("Fused")
    .setIndicator("Fused")
    .setContent(new Intent().setClass(this, FlpFusedLocation.class)));

  mTabHost.addTab(mTabHost.newTabSpec("Geofence")
    .setIndicator("Geofence")
    .setContent(new Intent().setClass(this, FlpGeofence.class)));

  mTabHost.addTab(mTabHost.newTabSpec("Map").setIndicator("Map")
    .setContent(new Intent().setClass(this, FlpMap.class)));

  mTabHost.addTab(mTabHost
    .newTabSpec("Heading")
    .setIndicator("Heading")
    .setContent(new Intent().setClass(this, FlpHeading.class)));

  for (int i = 0; i < 6; i++) {
   mTabHost.setCurrentTab(i);
  }

  mTabHost.setCurrentTab(2);
 }

 @Override
 protected void onDestroy() {
  super.onDestroy();
 }

}
