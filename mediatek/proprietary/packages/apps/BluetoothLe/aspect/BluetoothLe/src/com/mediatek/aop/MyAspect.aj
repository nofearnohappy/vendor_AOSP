package com.mediatek.aop;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.HashMap;

import org.aspectj.lang.Signature;
import com.mediatek.bluetoothle.bleservice.BleDeviceManagerService;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattWrapper;
import android.bluetooth.IBluetoothGatt;
import android.content.Context;
import android.os.Handler;
import android.util.Log;

public privileged aspect MyAspect {
    
    private final static String TAG = "Aspect-Trace";
    
    ///Simulated BT stack handler
    public Handler mBtHostHandler;
    
    public HashMap<BluetoothDevice, BluetoothGattCallback> mDeviceCallbackMap = 
            new HashMap<BluetoothDevice, BluetoothGattCallback>();
    
    public HashMap<BluetoothDevice, BluetoothGatt> mDeviceGattMap = 
            new HashMap<BluetoothDevice, BluetoothGatt>();
    
    public HashMap<BluetoothDevice, BluetoothGattWrapper> mDeviceMockGattMap = 
            new HashMap<BluetoothDevice, BluetoothGattWrapper>();
    
    /*
     * The pointcut that intercepts invocations to BluetoothDevice.connectGatt(..)
     */
    protected pointcut BluetoothDevicePointCut(): 
        call(public android.bluetooth.BluetoothGatt connectGatt(..)) 
        && within(com.mediatek.bluetoothle.bleservice.BleDeviceManagerService) 
        && target(android.bluetooth.BluetoothDevice);
    
    Object around() : BluetoothDevicePointCut(){
        Signature sig = thisJoinPointStaticPart.getSignature();
        Object[] args = thisJoinPoint.getArgs();
        Class[] argClass = new Class[args.length];
        BleDeviceManagerService thisObj = (BleDeviceManagerService)thisJoinPoint.getThis();
        BluetoothDevice device = (BluetoothDevice)thisJoinPoint.getTarget();
        final BluetoothGattCallback callback = (BluetoothGattCallback)args[2];
        
        log("->Call [" + sig.getName() + "]");
        log("  This Class:" + thisObj.getClass()+ ", Target Class:" + thisJoinPoint.getTarget().getClass());
        
        try {
            //Return a GATT client instance
            Constructor ctr = BluetoothGatt.class.getDeclaredConstructor(
                    Context.class, 
                    IBluetoothGatt.class, 
                    BluetoothDevice.class,
                    int.class);
            
            if(!ctr.isAccessible()) {
                log("BluetoothGatt constructor is not accessible");
                ctr.setAccessible(true);
            }

            final BluetoothGatt gatt = (BluetoothGatt)ctr.newInstance(thisObj, null, device, BluetoothDevice.TRANSPORT_AUTO);
            
            //Save device callback
            mDeviceGattMap.put(device, gatt);
            mDeviceCallbackMap.put(device, callback);
            
            log("->Handler:" + mBtHostHandler);
            //BT stack notifies STATE_CONNECTED
            mBtHostHandler.postDelayed(new Runnable() {
                
                public void run() {
                    log("->Callback: onConnectionStateChange");
                    callback.
                    onConnectionStateChange(gatt, BluetoothGatt.GATT_SUCCESS, BluetoothGatt.STATE_CONNECTED);
                }
                
            }, 1500);
            
            return gatt;
            
        } catch (Exception e) {
            log("Exception:" + e);
            
            throw new RuntimeException(e);
            
        }
        
        //return proceed();// Invoke the current joint-point method
    }

    /*
     * The pointcut that intercepts invocations to BluetoothGatt
     * Direct the calls to Mocked object
     */
    protected pointcut BluetoothGattPointCut(): 
        call(public * *(..)) 
        && within(com.mediatek.bluetoothle.bleservice.BleDeviceManagerService) 
        && target(android.bluetooth.BluetoothGatt);

        
    Object around() : BluetoothGattPointCut(){
        Signature sig = thisJoinPointStaticPart.getSignature();
        Object[] args = thisJoinPoint.getArgs();
        Class[] argClass = new Class[args.length];
        
        Object thisObj = thisJoinPoint.getThis();//Could be BleDeviceManagerService or its inner class
        BluetoothGatt gatt = (BluetoothGatt)thisJoinPoint.getTarget();
        
        log("->Call [" + sig.getName() + "]");
        log("  This Class:" + thisObj.getClass()+ ", Target Class:" + thisJoinPoint.getTarget().getClass());
        
        //return proceed();// Invoke the current joint-point method

        log("Call Mocked Wrapper");

        // Get the Class of all parameters
        for (int i = 0; i < args.length; i++) {
            argClass[i] = args[i].getClass();
        }
        // Invoke wrapper's corresponding method
        try {
            Method m = BluetoothGattWrapper.class.getDeclaredMethod(sig.getName(), argClass);
            
            //Get correct GattWrapper instance
            BluetoothGattWrapper gattWrapper = mDeviceMockGattMap.get(gatt.getDevice());
            
            return m.invoke(gattWrapper, args);
            
        } catch (Exception e) {
            log("Exception:" + e);
        }

        return null;

    }
    
    protected void log(String msg) {
        Log.i(TAG, msg);
    }

}
