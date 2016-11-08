package com.mediatek.flp.util;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.widget.Toast;

public class MyToast {

 private Toast mToast;
 private MyHandler mHandler;

 @SuppressLint("ShowToast")
 public MyToast(Context context) {
  this(Toast.makeText(context.getApplicationContext(), "default text",
    Toast.LENGTH_SHORT));
 }

 public MyToast(Toast toast) {
  mToast = toast;
  mHandler = new MyHandler(toast);
 }

 public void setLong() {
  mToast.setDuration(Toast.LENGTH_LONG);
 }

 public void setShort() {
  mToast.setDuration(Toast.LENGTH_SHORT);
 }

 public void show(Object message) {
  mHandler.removeMessages(0);
  Message msg = Message.obtain();
  msg.what = 0;
  msg.obj = message;
  mHandler.sendMessage(msg);
 }

 private static class MyHandler extends Handler {
  private Toast mToast;

  public MyHandler(Toast toast) {
   mToast = toast;
  }

  @Override
  public void handleMessage(Message msg) {
   mToast.setText("" + msg.obj);
   mToast.show();
  }
 }
}
