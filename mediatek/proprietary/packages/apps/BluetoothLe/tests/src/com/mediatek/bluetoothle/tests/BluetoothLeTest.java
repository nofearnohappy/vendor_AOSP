package com.mediatek.bluetoothle.tests;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothGattWrapper;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.database.MatrixCursor;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.ParcelUuid;
import android.os.RemoteException;
import android.test.ServiceTestCase;
import android.test.mock.MockContentResolver;
import android.util.Log;

import com.mediatek.aop.MyAspect;
import com.mediatek.bluetooth.BleGattDevice;
import com.mediatek.bluetooth.BleGattUuid;
import com.mediatek.bluetooth.BleProfile;
import com.mediatek.bluetooth.IBleDeviceManager;
import com.mediatek.bluetooth.IBleDeviceManagerCallback;
import com.mediatek.bluetooth.parcel.ParcelBluetoothGattCharacteristic;
import com.mediatek.bluetooth.parcel.ParcelBluetoothGattService;
import com.mediatek.bluetoothle.bleservice.BleApp;
import com.mediatek.bluetoothle.bleservice.BleDeviceManagerService;
import com.mediatek.bluetoothle.provider.BLEConstants;
import com.mediatek.bluetoothle.util.ContextWithMockContentResolver;
import com.mediatek.bluetoothle.util.DeviceMap;
import com.mediatek.bluetoothle.util.HashMapMockContentProvider;

import org.aspectj.lang.Aspects;
import org.mockito.ArgumentMatcher;
import org.mockito.Matchers;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class BluetoothLeTest extends ServiceTestCase<BleDeviceManagerService> {

    private static final String TAG = "BluetoothLeTest";

    private static final int TIME_SHORT = 1500;
    private static final int TIME_LONG = 3000;

    private static final String DEVICE_1 = "00:43:A8:23:10:F0";
    private static final String DEVICE_2 = "01:43:A8:23:10:F0";
    private static final String DEVICE_3 = "02:43:A8:23:10:F0";
    private static final String DEVICE_4 = "03:43:A8:23:10:F0";

    // Simulated BT stack
    private final HandlerThread mBtHostThread = new HandlerThread("BT Host");
    private Handler mBtHostHandler;

    // UT Aspect
    MyAspect mUtAspect = Aspects.aspectOf(MyAspect.class);

    BluetoothManager mBtManager;

    // Service AIDL interface
    IBleDeviceManager mDeviceManager;

    // Map for storing mocked AIDL Callback
    DeviceMap mDeviceMap = DeviceMap.getInstance();

    BleDeviceManagerService mDeviceManagerService;

    public BluetoothLeTest(final Class<BleDeviceManagerService> serviceClass) {
        super(serviceClass);

        Log.d(TAG, "BluetoothLeTest init");
    }

    public BluetoothLeTest() {
        super(BleDeviceManagerService.class);
        Log.d(TAG, "BluetoothLeTest init2");

        // Prepare a Thread to execute remote command in sequence
        mBtHostThread.start();
        mBtHostHandler = new Handler(mBtHostThread.getLooper());
        mUtAspect.mBtHostHandler = mBtHostHandler;
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        Log.d(TAG, "setUp");

        // Workaround1: Space for Mock classes generation
        System.setProperty("dexmaker.dexcache", this.getContext().getCacheDir().getPath());

        // Workaround2: for sharedUserId
        Thread.currentThread().setContextClassLoader(getClass().getClassLoader());

        mBtManager =
                (BluetoothManager) this.getContext().getSystemService(Context.BLUETOOTH_SERVICE);

        //Init Device Map
        mDeviceMap.clear();
    }

    @Override
    protected void tearDown() throws Exception {
        Log.d(TAG, "tearDown");
        super.tearDown();
    }

    /**
     * Test Configuration: 2 device, 1 client
     * Test Scenario: Connect 1
     *
     * @throws RemoteException
     */
    public void testCase101() throws RemoteException {
        final BluetoothDevice[] deviceList = convertToBluetoothDevices(new String[] { DEVICE_1, DEVICE_2 });
        final int clientNum = 1;
        final int targetClientIdx = 0;
        final int targetDeviceIdx = 0;

        /**
         * Start & bind BleDeviceManagerService
         */
        this.initService();

        /**
         * Generate mocked classes & record behavior
         */
        initBluetoothGattMocks(deviceList);
        initDeviceClientCbMocks(deviceList, clientNum);

        /**
         * Get target device & client Id
         */
        final BluetoothDevice targetDevice = deviceList[targetDeviceIdx];
        final List<Integer> clientIdList = this.mDeviceMap.getDeviceClients(targetDevice);
        final int targetClient = clientIdList.get(targetClientIdx);

        /**
         * Test Scenario
         */

        mDeviceManager.connectDevice(targetClient, targetDevice);

        // Wait for connected callback
        waitForResponse(TIME_LONG);

        /**
         * Verification
         */
        for (final BluetoothDevice device : deviceList) {
            final BluetoothGattWrapper mockGatt =
                    mUtAspect.mDeviceMockGattMap.get(device);

            final List<IBleDeviceManagerCallback.Stub> mockCbList =
                    this.mDeviceMap.getDeviceCallbacks(device);

            // Only callback to target device
            if (device.equals(targetDevice)) {
                Mockito.verify(mockGatt, Mockito.atLeastOnce()).getDevice();

                for (final IBleDeviceManagerCallback.Stub mockCb : mockCbList) {

                    Mockito.verify(mockCb, Mockito.times(1)).onConnectionStateChange(
                            device.getAddress(), BleGattDevice.GATT_SUCCESS,
                            BleGattDevice.STATE_CONNECTED);
                }
            } else {
                Mockito.verify(mockGatt, Mockito.never()).getDevice();

                for (final IBleDeviceManagerCallback.Stub mockCb : mockCbList) {

                    Mockito.verify(mockCb, Mockito.never()).onConnectionStateChange(
                            Matchers.anyString(), Matchers.anyInt(), Matchers.anyInt());
                }
            }
        }

    }

    /**
     * Test Configuration: 2 device, 4 clients
     * Test Scenario: Connect 1
     *
     * @throws RemoteException
     */
    public void testCase102() throws RemoteException {
        final BluetoothDevice[] deviceList = convertToBluetoothDevices(new String[] { DEVICE_1, DEVICE_2 });
        final int clientNum = 4;
        final int targetClientIdx = 0;
        final int targetDeviceIdx = 0;

        /**
         * Start & bind BleDeviceManagerService
         */
        this.initService();

        /**
         * Generate mocked classes & record behavior
         */
        initBluetoothGattMocks(deviceList);
        initDeviceClientCbMocks(deviceList, clientNum);

        /**
         * Get target device & client Id
         */
        final BluetoothDevice targetDevice = deviceList[targetDeviceIdx];
        final List<Integer> clientIdList = this.mDeviceMap.getDeviceClients(targetDevice);
        final int targetClient = clientIdList.get(targetClientIdx);

        /**
         * Test Scenario
         */

        mDeviceManager.connectDevice(targetClient, targetDevice);

        // Wait for connected callback
        waitForResponse(TIME_LONG);

        /**
         * Verification
         */
        for (final BluetoothDevice device : deviceList) {
            final BluetoothGattWrapper mockGatt =
                    mUtAspect.mDeviceMockGattMap.get(device);

            final List<IBleDeviceManagerCallback.Stub> mockCbList =
                    this.mDeviceMap.getDeviceCallbacks(device);

            // Only callback to target device
            if (device.equals(targetDevice)) {
                Mockito.verify(mockGatt, Mockito.atLeastOnce()).getDevice();

                for (final IBleDeviceManagerCallback.Stub mockCb : mockCbList) {

                    Mockito.verify(mockCb, Mockito.times(1)).onConnectionStateChange(
                            device.getAddress(), BleGattDevice.GATT_SUCCESS,
                            BleGattDevice.STATE_CONNECTED);
                }
            } else {
                Mockito.verify(mockGatt, Mockito.never()).getDevice();

                for (final IBleDeviceManagerCallback.Stub mockCb : mockCbList) {

                    Mockito.verify(mockCb, Mockito.never()).onConnectionStateChange(
                            Matchers.anyString(), Matchers.anyInt(), Matchers.anyInt());
                }
            }

        }

    }

    /**
     * Test Configuration: 2 device, 4 clients
     * Test Scenario: Connect 1, Connect 2
     *
     * @throws RemoteException
     */
    public void testCase103() throws RemoteException {
        final BluetoothDevice[] deviceList = convertToBluetoothDevices(new String[] { DEVICE_1, DEVICE_2 });
        final int clientNum = 4;
        final int targetClientIdx = 0;

        /**
         * Start & bind BleDeviceManagerService
         */
        this.initService();

        /**
         * Generate mocked classes & record behavior
         */
        initBluetoothGattMocks(deviceList);
        initDeviceClientCbMocks(deviceList, clientNum);

        /**
         * Test Scenario
         */

        for (final BluetoothDevice device : deviceList) {
            // Registered clientID list
            final List<Integer> clientIdList = this.mDeviceMap.getDeviceClients(device);
            // Set target device & client
            final int targetClient = clientIdList.get(targetClientIdx);

            mDeviceManager.connectDevice(targetClient, device);
        }

        // Wait for connected callback
        waitForResponse(TIME_LONG);

        /**
         * Verification
         */
        for (final BluetoothDevice device : deviceList) {
            final BluetoothGattWrapper mockGatt =
                    mUtAspect.mDeviceMockGattMap.get(device);

            final List<IBleDeviceManagerCallback.Stub> mockCbList =
                    this.mDeviceMap.getDeviceCallbacks(device);

            Mockito.verify(mockGatt, Mockito.atLeastOnce()).getDevice();

            for (final IBleDeviceManagerCallback.Stub mockCb : mockCbList) {

                Mockito.verify(mockCb, Mockito.times(1)).onConnectionStateChange(
                        device.getAddress(), BleGattDevice.GATT_SUCCESS,
                        BleGattDevice.STATE_CONNECTED);
            }
        }

    }

    /**
     * Test Configuration: 1 device, 2 clients
     * Test Scenario: Connect, Disconnect
     *
     * @throws RemoteException
     */
    public void testCase104() throws RemoteException {
        final BluetoothDevice[] deviceList = convertToBluetoothDevices(new String[] { DEVICE_1 });
        final int clientNum = 2;
        final int targetClientIdx = 0;

        /**
         * Start & bind BleDeviceManagerService
         */
        this.initService();

        /**
         * Generate mocked classes & record behavior
         */
        initBluetoothGattMocks(deviceList);
        initDeviceClientCbMocks(deviceList, clientNum);

        for (final BluetoothDevice device : deviceList) {
            final BluetoothGattWrapper mockGatt =
                    mUtAspect.mDeviceMockGattMap.get(device);

            Mockito.doAnswer(new Answer<Boolean>() {
                @Override
                public Boolean answer(final InvocationOnMock invocation) {

                    mBtHostHandler.post(new Runnable() {

                        @Override
                        public void run() {
                            Log.e(TAG, "onConnectionStateChange");
                            final BluetoothGatt gatt = mUtAspect.mDeviceGattMap.get(device);
                            final BluetoothGattCallback cb =
                                    mUtAspect.mDeviceCallbackMap.get(device);
                            cb.onConnectionStateChange(gatt, BluetoothGatt.GATT_SUCCESS,
                                    BluetoothProfile.STATE_DISCONNECTED);
                        }

                    });

                    return Boolean.TRUE;
                }
            }).when(mockGatt).disconnect();
        }

        /**
         * Test Scenario
         */

        for (final BluetoothDevice device : deviceList) {
            // Registered clientID list
            final List<Integer> clientIdList = this.mDeviceMap.getDeviceClients(device);
            // Set target device & client
            final int targetClient = clientIdList.get(targetClientIdx);

            mDeviceManager.connectDevice(targetClient, device);

            mDeviceManager.disconnectDevice(targetClient, device);
        }

        // Wait for connected callback
        waitForResponse(TIME_LONG);

        /**
         * Verification
         */

        for (final BluetoothDevice device : deviceList) {
            final BluetoothGattWrapper mockGatt =
                    mUtAspect.mDeviceMockGattMap.get(device);

            final List<IBleDeviceManagerCallback.Stub> mockCbList =
                    this.mDeviceMap.getDeviceCallbacks(device);

            Mockito.verify(mockGatt, Mockito.atLeastOnce()).getDevice();

            for (final IBleDeviceManagerCallback.Stub mockCb : mockCbList) {
                Mockito.verify(mockCb, Mockito.times(1)).onConnectionStateChange(
                        device.getAddress(), BleGattDevice.GATT_SUCCESS,
                        BleGattDevice.STATE_CONNECTED);

                Mockito.verify(mockCb, Mockito.times(1)).onConnectionStateChange(
                        device.getAddress(), BleGattDevice.GATT_SUCCESS,
                        BleGattDevice.STATE_DISCONNECTING);

                Mockito.verify(mockCb, Mockito.times(1)).onConnectionStateChange(
                        device.getAddress(), BleGattDevice.GATT_SUCCESS,
                        BleGattDevice.STATE_DISCONNECTED);
            }
        }
    }

    /**
     * Test Configuration: 1 device, 2 clients
     * Test Scenario: Connect, Discover GATT services
     *
     * @throws RemoteException
     */
    public void testCase201() throws RemoteException {
        final BluetoothDevice[] deviceList = convertToBluetoothDevices(new String[] { DEVICE_1 });
        final int clientNum = 2;
        final int targetClientIdx = 0;

        /**
         * Start & bind BleDeviceManagerService
         */
        this.initService();

        /**
         * Generate mocked classes & record behavior
         */
        initBluetoothGattMocks(deviceList);
        initDeviceClientCbMocks(deviceList, clientNum);

        for (final BluetoothDevice device : deviceList) {
            final BluetoothGattWrapper mockGatt =
                    mUtAspect.mDeviceMockGattMap.get(device);

            Mockito.when(mockGatt.discoverServices()).then(new Answer<Boolean>() {
                @Override
                public Boolean answer(final InvocationOnMock invocation) {

                    mBtHostHandler.post(new Runnable() {

                        @Override
                        public void run() {
                            Log.e(TAG, "onServicesDiscovered");
                            final BluetoothGatt gatt = mUtAspect.mDeviceGattMap.get(device);
                            final BluetoothGattCallback cb =
                                    mUtAspect.mDeviceCallbackMap.get(device);
                            cb.onServicesDiscovered(gatt, BluetoothGatt.GATT_SUCCESS);
                        }

                    });

                    return Boolean.TRUE;
                }
            });

            Mockito.when(mockGatt.getServices()).thenReturn(null);
        }

        /**
         * Test Scenario
         */

        for (final BluetoothDevice device : deviceList) {
            // Registered clientID list
            final List<Integer> clientIdList = this.mDeviceMap.getDeviceClients(device);
            // Set target device & client
            final int targetClient = clientIdList.get(targetClientIdx);

            // Connect device
            mDeviceManager.connectDevice(targetClient, device);
        }

        // Wait for callback
        waitForResponse(TIME_LONG);

        for (final BluetoothDevice device : deviceList) {
            // Registered clientID list
            final List<Integer> clientIdList = this.mDeviceMap.getDeviceClients(device);
            // Set target device & client
            final int targetClient = clientIdList.get(targetClientIdx);

            // Discover services
            mDeviceManager.discoverServices(targetClient, device);
        }

        // Wait for callback
        waitForResponse(TIME_SHORT);

        /**
         * Verification
         */
        for (final BluetoothDevice device : deviceList) {
            final BluetoothGattWrapper mockGatt =
                    mUtAspect.mDeviceMockGattMap.get(device);

            final List<IBleDeviceManagerCallback.Stub> mockCbList =
                    this.mDeviceMap.getDeviceCallbacks(device);

            Mockito.verify(mockGatt, Mockito.atLeastOnce()).getDevice();

            // Verify connection callback
            for (final IBleDeviceManagerCallback.Stub mockCb : mockCbList) {
                Mockito.verify(mockCb, Mockito.times(1)).onConnectionStateChange(
                        device.getAddress(), BleGattDevice.GATT_SUCCESS,
                        BleGattDevice.STATE_CONNECTED);
            }

            // Verify service discovery callback
            for (final IBleDeviceManagerCallback.Stub mockCb : mockCbList) {
                Mockito.verify(mockCb, Mockito.times(1)).onServicesChanged(
                        device.getAddress(), BleGattDevice.GATT_SUCCESS);
            }
        }
    }

    /**
     * Test Configuration: 1 device, 4 clients
     * Test Scenario: Connect, ReadRssi
     *
     * @throws RemoteException
     */
    public void testCase301() throws RemoteException {
        final BluetoothDevice[] deviceList = convertToBluetoothDevices(new String[] { DEVICE_1 });
        final int clientNum = 4;
        final int targetClientIdx = 0;

        /**
         * Start & bind BleDeviceManagerService
         */
        this.initService();

        /**
         * Generate mocked classes & record behavior
         */
        initBluetoothGattMocks(deviceList);
        initDeviceClientCbMocks(deviceList, clientNum);

        final int rssiResult = -50;

        for (final BluetoothDevice device : deviceList) {
            final BluetoothGattWrapper mockGatt =
                    mUtAspect.mDeviceMockGattMap.get(device);

            Mockito.when(mockGatt.getServices()).thenReturn(null);

            Mockito.when(mockGatt.readRemoteRssi()).thenReturn(true);

            Mockito.when(mockGatt.readRemoteRssi()).then(new Answer<Boolean>() {
                @Override
                public Boolean answer(final InvocationOnMock invocation) {

                    mBtHostHandler.post(new Runnable() {

                        @Override
                        public void run() {
                            Log.e(TAG, "onReadRemoteRssi");
                            final BluetoothGatt gatt = mUtAspect.mDeviceGattMap.get(device);
                            final BluetoothGattCallback cb =
                                    mUtAspect.mDeviceCallbackMap.get(device);
                            cb.onReadRemoteRssi(gatt, rssiResult, BluetoothGatt.GATT_SUCCESS);
                        }

                    });

                    return Boolean.TRUE;
                }
            });
        }

        /**
         * Test Scenario
         */

        for (final BluetoothDevice device : deviceList) {
            // Registered clientID list
            final List<Integer> clientIdList = this.mDeviceMap.getDeviceClients(device);
            // Set target device & client
            final int targetClient = clientIdList.get(targetClientIdx);

            // Connect device
            mDeviceManager.connectDevice(targetClient, device);
        }

        // Wait for connected callback
        waitForResponse(TIME_LONG);

        for (final BluetoothDevice device : deviceList) {
            // Registered clientID list
            final List<Integer> clientIdList = this.mDeviceMap.getDeviceClients(device);
            // Set target device & client
            final int targetClient = clientIdList.get(targetClientIdx);

            // Read RSSI
            mDeviceManager.readRemoteRssi(
                    targetClient, BleProfile.PXP, device);
        }

        // Wait for connected callback
        waitForResponse(TIME_LONG);

        /**
         * Verification
         */
        for (final BluetoothDevice device : deviceList) {
            final BluetoothGattWrapper mockGatt =
                    mUtAspect.mDeviceMockGattMap.get(device);

            final List<IBleDeviceManagerCallback.Stub> mockCbList =
                    this.mDeviceMap.getDeviceCallbacks(device);

            // Registered clientID list
            final List<Integer> clientIdList = this.mDeviceMap.getDeviceClients(device);
            // Set target device & client
            final int targetClient = clientIdList.get(targetClientIdx);


            Mockito.verify(mockGatt, Mockito.atLeastOnce()).getDevice();

            for (final IBleDeviceManagerCallback.Stub mockCb : mockCbList) {

                Mockito.verify(mockCb, Mockito.times(1)).onConnectionStateChange(
                        device.getAddress(), BleGattDevice.GATT_SUCCESS,
                        BleGattDevice.STATE_CONNECTED);
            }

            for (final int clientID : clientIdList) {
                final IBleDeviceManagerCallback.Stub mockCb =
                        this.mDeviceMap.getDeviceClientCallback(device, clientID);

                if (clientID == targetClient) {
                    Mockito.verify(mockCb, Mockito.times(1)).onReadRemoteRssi(device.getAddress(),
                            BleProfile.PXP, rssiResult, BleGattDevice.GATT_SUCCESS);
                } else {
                    Mockito.verify(mockCb, Mockito.never()).onReadRemoteRssi(Matchers.anyString(),
                            Matchers.anyInt(), Matchers.anyInt(), Matchers.anyInt());
                }
            }
        }
    }

    /**
     * Test Configuration: 1 device, 4 clients
     * Test Scenario: Connect, ReadRssi (Profile 1), ReadRssi (Profile 2)
     *
     * @throws RemoteException
     */
    public void testCase302() throws RemoteException {
        final BluetoothDevice[] deviceList = convertToBluetoothDevices(new String[] { DEVICE_1 });
        final int clientNum = 4;
        final int targetClientIdx = 0;

        /**
         * Start & bind BleDeviceManagerService
         */
        this.initService();

        /**
         * Generate mocked classes & record behavior
         */
        initBluetoothGattMocks(deviceList);
        initDeviceClientCbMocks(deviceList, clientNum);

        final int[] rssiResult = { -50, -90 };

        for (final BluetoothDevice device : deviceList) {
            final BluetoothGattWrapper mockGatt =
                    mUtAspect.mDeviceMockGattMap.get(device);

            Mockito.when(mockGatt.getServices()).thenReturn(null);

            Mockito.when(mockGatt.readRemoteRssi()).then(new Answer<Boolean>() {

                int count = 0;

                @Override
                public Boolean answer(final InvocationOnMock invocation) {

                    final int rssi = rssiResult[count];

                    mBtHostHandler.post(new Runnable() {

                        @Override
                        public void run() {
                            Log.e(TAG, "onReadRemoteRssi");
                            final BluetoothGatt gatt = mUtAspect.mDeviceGattMap.get(device);
                            final BluetoothGattCallback cb =
                                    mUtAspect.mDeviceCallbackMap.get(device);
                            cb.onReadRemoteRssi(gatt, rssi, BluetoothGatt.GATT_SUCCESS);
                        }

                    });

                    count = (++count) % 2;

                    return Boolean.TRUE;
                }
            });
        }

        /**
         * Test Scenario
         */

        for (final BluetoothDevice device : deviceList) {
            // Registered clientID list
            final List<Integer> clientIdList = this.mDeviceMap.getDeviceClients(device);
            // Set target device & client
            final int targetClient = clientIdList.get(targetClientIdx);

            // Connect device
            mDeviceManager.connectDevice(targetClient, device);
        }

        // Wait for connected callback
        waitForResponse(TIME_LONG);

        for (final BluetoothDevice device : deviceList) {
            // Registered clientID list
            final List<Integer> clientIdList = this.mDeviceMap.getDeviceClients(device);
            // Set target device & client
            final int targetClient = clientIdList.get(targetClientIdx);

            // Read RSSI
            mDeviceManager.readRemoteRssi(
                    targetClient, BleProfile.PXP, device);

            mDeviceManager.readRemoteRssi(
                    targetClient, BleProfile.FMP, device);
        }

        // Wait for connected callback
        waitForResponse(TIME_LONG);

        /**
         * Verification
         */

        for (final BluetoothDevice device : deviceList) {
            final BluetoothGattWrapper mockGatt =
                    mUtAspect.mDeviceMockGattMap.get(device);

            final List<IBleDeviceManagerCallback.Stub> mockCbList =
                    this.mDeviceMap.getDeviceCallbacks(device);

            // Registered clientID list
            final List<Integer> clientIdList = this.mDeviceMap.getDeviceClients(device);
            // Set target device & client
            final int targetClient = clientIdList.get(targetClientIdx);

            Mockito.verify(mockGatt, Mockito.atLeastOnce()).getDevice();

            for (final IBleDeviceManagerCallback.Stub mockCb : mockCbList) {
                Mockito.verify(mockCb, Mockito.times(1)).onConnectionStateChange(
                        device.getAddress(),
                        BleGattDevice.GATT_SUCCESS,
                        BleGattDevice.STATE_CONNECTED);
            }

            for (final int clientID : clientIdList) {
                final IBleDeviceManagerCallback.Stub mockCb =
                        this.mDeviceMap.getDeviceClientCallback(device, clientID);

                if (clientID == targetClient) {
                    Mockito.verify(mockCb, Mockito.atLeastOnce()).onReadRemoteRssi(
                            device.getAddress(), BleProfile.PXP,
                            rssiResult[0], BleGattDevice.GATT_SUCCESS);

                    Mockito.verify(mockCb, Mockito.atLeastOnce()).onReadRemoteRssi(
                            device.getAddress(), BleProfile.FMP,
                            rssiResult[1], BleGattDevice.GATT_SUCCESS);

                } else {
                    Mockito.verify(mockCb, Mockito.never()).onReadRemoteRssi(Matchers.anyString(),
                            Matchers.anyInt(), Matchers.anyInt(), Matchers.anyInt());
                }
            }
        }

    }

    /**
     * Test Configuration: 1 device, 2 clients/device
     * Test Scenario: Connect, Discover Services, Write Char
     *
     * @throws RemoteException
     */
    public void testCase303() throws RemoteException {
        final BluetoothDevice[] deviceList = convertToBluetoothDevices(new String[] { DEVICE_1 });
        final int clientNum = 2;
        final int targetClientIdx = 0;

        /**
         * Start & bind BleDeviceManagerService
         */
        this.initService();

        /**
         * Generate mocked classes & record behavior
         */
        initBluetoothGattMocks(deviceList);
        initDeviceClientCbMocks(deviceList, clientNum);

        for (final BluetoothDevice device : deviceList) {
            final BluetoothGattWrapper mockGatt =
                    mUtAspect.mDeviceMockGattMap.get(device);

            Mockito.when(mockGatt.discoverServices()).then(new Answer<Boolean>() {
                @Override
                public Boolean answer(final InvocationOnMock invocation) {

                    mBtHostHandler.post(new Runnable() {

                        @Override
                        public void run() {
                            Log.e(TAG, "onServicesDiscovered");
                            final BluetoothGatt gatt = mUtAspect.mDeviceGattMap.get(device);
                            final BluetoothGattCallback cb =
                                    mUtAspect.mDeviceCallbackMap.get(device);
                            cb.onServicesDiscovered(gatt, BluetoothGatt.GATT_SUCCESS);
                        }

                    });

                    return Boolean.TRUE;
                }
            });

            Mockito.when(mockGatt.getServices()).thenReturn(
                    createServicesSample1(device, new UUID[][] { {
                            BleGattUuid.Service.IMMEDIATE_ALERT, BleGattUuid.Char.ALERT_LEVEL } }));

            Mockito.when(
                    mockGatt.writeCharacteristic(
                            Matchers.<BluetoothGattCharacteristic> anyObject())
                            )
                    .then(new Answer<Boolean>() {

                        @Override
                        public Boolean answer(final InvocationOnMock invocation) {

                            final BluetoothGattCharacteristic gattChar =
                                    (BluetoothGattCharacteristic) invocation.getArguments()[0];

                            mBtHostHandler.post(new Runnable() {

                                @Override
                                public void run() {

                                    Log.e(TAG, "onCharacteristicWrite");
                                    final BluetoothGatt gatt =
                                            mUtAspect.mDeviceGattMap.get(device);
                                    final BluetoothGattCallback cb =
                                            mUtAspect.mDeviceCallbackMap.get(device);
                                    cb.onCharacteristicWrite(
                                            gatt, gattChar, BluetoothGatt.GATT_SUCCESS);
                                }

                            });

                            return Boolean.TRUE;
                        }

                    });
        }


        /**
         * Test Scenario
         */

        for (final BluetoothDevice device : deviceList) {
            // Registered clientID list
            final List<Integer> clientIdList = this.mDeviceMap.getDeviceClients(device);
            // Set target device & client
            final int targetClient = clientIdList.get(targetClientIdx);

            // Connect device
            mDeviceManager.connectDevice(targetClient, device);
        }

        // Wait for callback
        waitForResponse(TIME_LONG);

        for (final BluetoothDevice device : deviceList) {
            // Registered clientID list
            final List<Integer> clientIdList = this.mDeviceMap.getDeviceClients(device);
            // Set target device & client
            final int targetClient = clientIdList.get(targetClientIdx);

            // Discover services
            mDeviceManager.discoverServices(targetClient, device);
        }

        // Wait for callback
        waitForResponse(TIME_SHORT);

        for (final BluetoothDevice device : deviceList) {
            // Registered clientID list
            final List<Integer> clientIdList = this.mDeviceMap.getDeviceClients(device);
            // Set target device & client
            final int targetClient = clientIdList.get(targetClientIdx);

            // Write Characteristic
            final ParcelBluetoothGattService parcelGattService =
                    mDeviceManager.getService(device, new ParcelUuid(
                            BleGattUuid.Service.IMMEDIATE_ALERT));
            assertNotNull(parcelGattService);

            final BluetoothGattService gattService = parcelGattService.unpack();

            final BluetoothGattCharacteristic alertLevel =
                    gattService.getCharacteristic(BleGattUuid.Char.ALERT_LEVEL);
            assertNotNull(alertLevel);

            alertLevel.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
            alertLevel.setValue(1, BluetoothGattCharacteristic.FORMAT_UINT8, 0);

            mDeviceManager.writeCharacteristic(
                    targetClient, BleProfile.FMP, device,
                    ParcelBluetoothGattCharacteristic.from(alertLevel));
        }

        // Wait for callback
        waitForResponse(TIME_SHORT);

        /**
         * Verification
         */

        for (final BluetoothDevice device : deviceList) {
            final BluetoothGattWrapper mockGatt =
                    mUtAspect.mDeviceMockGattMap.get(device);

            final List<IBleDeviceManagerCallback.Stub> mockCbList =
                    this.mDeviceMap.getDeviceCallbacks(device);

            // Registered clientID list
            final List<Integer> clientIdList = this.mDeviceMap.getDeviceClients(device);
            // Set target device & client
            final int targetClient = clientIdList.get(targetClientIdx);

            Mockito.verify(mockGatt, Mockito.atLeastOnce()).getDevice();

            for (final IBleDeviceManagerCallback.Stub mockCb : mockCbList) {
                Mockito.verify(mockCb, Mockito.times(1)).onConnectionStateChange(
                        device.getAddress(), BleGattDevice.GATT_SUCCESS,
                        BleGattDevice.STATE_CONNECTED);
            }

            for (final IBleDeviceManagerCallback.Stub mockCb : mockCbList) {
                Mockito.verify(mockCb, Mockito.times(1)).onServicesChanged(device.getAddress(),
                        BleGattDevice.GATT_SUCCESS);
            }

            for (final int clientID : clientIdList) {
                final IBleDeviceManagerCallback.Stub mockCb =
                        this.mDeviceMap.getDeviceClientCallback(device, clientID);

                if (clientID == targetClient) {
                    Mockito.verify(mockCb, Mockito.times(1)).onCharacteristicWrite(
                            Matchers.eq(device.getAddress()),
                            Matchers.eq(BleProfile.FMP),
                            Matchers.argThat(
                                    new ArgumentMatcher<ParcelBluetoothGattCharacteristic>() {
                                @Override
                                public boolean matches(final Object item) {
                                    final ParcelBluetoothGattCharacteristic parcelChar =
                                            (ParcelBluetoothGattCharacteristic) item;

                                    final boolean result = (parcelChar != null)
                                                    && (parcelChar.getUuid().getUuid().
                                                            equals(BleGattUuid.Char.ALERT_LEVEL))
                                                    && (parcelChar.getInstanceId()
                                                            == 1)
                                                    && (parcelChar.getWriteType()
                                                            == BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);

                                    Log.e(TAG,
                                            "ParcelBluetoothGattCharacteristic matches: " + result);

                                    return result;
                                }
                            }),
                            Matchers.eq(BluetoothGatt.GATT_SUCCESS)
                            );
                } else {
                    Mockito.verify(mockCb, Mockito.never()).onReadRemoteRssi(Matchers.anyString(),
                            Matchers.anyInt(), Matchers.anyInt(), Matchers.anyInt());
                }
            }
        }
    }

    /**
     * Test Configuration: 1 device, 2 clients/device
     * Test Scenario: Service Start, Auto Connect (No device)
     *
     * @throws RemoteException
     */
    public void testCase401() throws RemoteException {
        final BluetoothDevice[] deviceList = convertToBluetoothDevices(new String[] { DEVICE_3 });
        final int targetDeviceIdx = 0;

        // Set target device & client
        final BluetoothDevice targetDevice = deviceList[targetDeviceIdx];

        /**
         * Generate mocked classes & record behavior
         */
        initBluetoothGattMocks(deviceList);

        final BluetoothGattWrapper mockGatt =
                mUtAspect.mDeviceMockGattMap.get(targetDevice);

        Mockito.when(mockGatt.discoverServices()).then(new Answer<Boolean>() {
            @Override
            public Boolean answer(final InvocationOnMock invocation) {

                mBtHostHandler.post(new Runnable() {

                    @Override
                    public void run() {
                        Log.e(TAG, "onServicesDiscovered");
                        final BluetoothGatt gatt = mUtAspect.mDeviceGattMap.get(targetDevice);
                        final BluetoothGattCallback cb =
                                mUtAspect.mDeviceCallbackMap.get(targetDevice);
                        cb.onServicesDiscovered(gatt, BluetoothGatt.GATT_SUCCESS);
                    }

                });

                return Boolean.TRUE;
            }
        });

        // Mock content provider
        final String[] examleProjection =
                { BLEConstants.COLUMN_BT_ADDRESS, BLEConstants.CLIENT_TABLE.DEVICE_AUTO_CONNECT };
        final MatrixCursor matrixCursor = new MatrixCursor(examleProjection);

        // Create a stub content provider and add the matrix cursor
        // as the expected result of the query
        final HashMapMockContentProvider mockProvider = new HashMapMockContentProvider();
        mockProvider.addQueryResult(BLEConstants.TABLE_CLIENT_URI, matrixCursor);

        // Create a mock resolver and add the content provider.
        final MockContentResolver mockResolver = new MockContentResolver();
        mockResolver.addProvider(BLEConstants.AUTORITY, mockProvider);

        // Add the mock resolver to the mock context
        final ContextWithMockContentResolver mockContext =
                new ContextWithMockContentResolver(this.getSystemContext());
        mockContext.setContentResolver(mockResolver);

        this.setContext(mockContext);

        /**
         * Test Scenario:
         * Start & bind BleDeviceManagerService
         */
        this.initService();

        /**
         * Verification onConnectionStateChange will invoke getDevice();
         */

        waitForResponse(TIME_LONG);

        // Should not be called
        Mockito.verify(mockGatt, Mockito.never()).getDevice();
    }

    /**
     * Test Configuration: 1 device, 2 clients/device
     * Test Scenario: Service Start, Auto Connect (1 device)
     *
     * @throws RemoteException
     */
    public void testCase402() throws RemoteException {
        final BluetoothDevice[] deviceList = convertToBluetoothDevices(new String[] { DEVICE_3 });
        final int targetDeviceIdx = 0;

        // Set target device & client
        final BluetoothDevice targetDevice = deviceList[targetDeviceIdx];

        /**
         * Generate mocked classes & record behavior
         */
        initBluetoothGattMocks(deviceList);

        final BluetoothGattWrapper mockGatt =
                mUtAspect.mDeviceMockGattMap.get(targetDevice);

        Mockito.when(mockGatt.discoverServices()).then(new Answer<Boolean>() {
            @Override
            public Boolean answer(final InvocationOnMock invocation) {

                mBtHostHandler.post(new Runnable() {

                    @Override
                    public void run() {
                        Log.e(TAG, "onServicesDiscovered");
                        final BluetoothGatt gatt = mUtAspect.mDeviceGattMap.get(targetDevice);
                        final BluetoothGattCallback cb =
                                mUtAspect.mDeviceCallbackMap.get(targetDevice);
                        cb.onServicesDiscovered(gatt, BluetoothGatt.GATT_SUCCESS);
                    }

                });

                return Boolean.TRUE;
            }
        });

        // Mock content provider
        final Object[] exampleData = { targetDevice, 1 };
        final String[] examleProjection =
                { BLEConstants.COLUMN_BT_ADDRESS, BLEConstants.CLIENT_TABLE.DEVICE_AUTO_CONNECT };
        final MatrixCursor matrixCursor = new MatrixCursor(examleProjection);
        matrixCursor.addRow(exampleData);

        // Create a stub content provider and add the matrix cursor
        // as the expected result of the query
        final HashMapMockContentProvider mockProvider = new HashMapMockContentProvider();
        mockProvider.addQueryResult(BLEConstants.TABLE_CLIENT_URI, matrixCursor);

        // Create a mock resolver and add the content provider.
        final MockContentResolver mockResolver = new MockContentResolver();
        mockResolver.addProvider(BLEConstants.AUTORITY, mockProvider);

        // Add the mock resolver to the mock context
        final ContextWithMockContentResolver mockContext =
                new ContextWithMockContentResolver(this.getContext());
        mockContext.setContentResolver(mockResolver);

        this.setContext(mockContext);

        /**
         * Test Scenario:
         * Start & bind BleDeviceManagerService
         */
        this.initService();

        /**
         * Verification onConnectionStateChange will invoke getDevice();
         */
        // There will be 2 times of callback (1 for connection, 1 for service discovery)
        Mockito.verify(mockGatt, Mockito.timeout(TIME_LONG).times(2)).getDevice();

        Mockito.verify(mockGatt, Mockito.timeout(TIME_LONG)).discoverServices();

    }

    private BluetoothDevice[] convertToBluetoothDevices(final String[] addrList) {
        if (addrList == null) {
            return null;
        }

        final BluetoothDevice[] deviceList = new BluetoothDevice[addrList.length];

        for (int i = 0; i < addrList.length; i++) {
            deviceList[i] = mBtManager.getAdapter().getRemoteDevice(addrList[i]);
        }

        return deviceList;
    }

    private void initBluetoothGattMocks(final BluetoothDevice[] devices) {
        for (final BluetoothDevice device : devices) {
            final BluetoothGattWrapper mockGatt = Mockito.mock(BluetoothGattWrapper.class);
            mUtAspect.mDeviceMockGattMap.put(device, mockGatt);

            Mockito.when(mockGatt.getDevice()).thenReturn(device);
        }
    }

    private void initDeviceClientCbMocks(final BluetoothDevice[] devices, final int numOfClients)
            throws RemoteException {
        // Generate Mocked Callback for each client
        for (final BluetoothDevice device : devices) {
            // Client : Callback Map
            final Map<Integer, IBleDeviceManagerCallback.Stub> mockCbMap =
                    new HashMap<Integer, IBleDeviceManagerCallback.Stub>();

            for (int i = 0; i < numOfClients; i++) {
                Log.d(TAG, "Generate mocked IBleDeviceManagerCallback.Stub, " + i);

                // Generate mocked client callback
                final IBleDeviceManagerCallback.Stub mockedCb =
                        Mockito.mock(IBleDeviceManagerCallback.Stub.class);
                Mockito.when(mockedCb.asBinder()).thenReturn(mockedCb);

                // Register client callback
                final int clientID =
                        mDeviceManager.registerClient(
                                new ParcelUuid(UUID.randomUUID()), device, mockedCb);
                mockCbMap.put(clientID, mockedCb);
            }

            this.mDeviceMap.addDeviceCallback(device, mockCbMap);
        }
    }

    private void initService() {

        // Auto Start BleDeviceManagerService
        final Intent newIntent = new Intent(this.mContext, BleDeviceManagerService.class);
        newIntent.putExtra(BleApp.EXTRA_ACTION, BleApp.ACTION_SERVICE_STATE_CHANGED);
        newIntent.putExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.STATE_ON);

        this.startService(newIntent);
        Log.d(TAG, "setUp: service started");

        final Intent intent = new Intent(IBleDeviceManager.class.getName());

        final IBinder service = this.bindService(intent);

        Log.d(TAG, "setUp: service bound");

        mDeviceManager = IBleDeviceManager.Stub.asInterface(service);

    }

    private List<BluetoothGattService> createServicesSample1(final BluetoothDevice device,
            final UUID[][] uuidTable) {

        final List<BluetoothGattService> result = new ArrayList<BluetoothGattService>();

        for (final UUID[] serviceEntry : uuidTable) {
            final UUID serviceUuid = serviceEntry[0];

            final ParcelBluetoothGattService parcelService =
                    new ParcelBluetoothGattService(device, new ParcelUuid(serviceUuid), 0,
                            BluetoothGattService.SERVICE_TYPE_PRIMARY);

            for (int i = 1; i < serviceEntry.length; i++) {
                final UUID charUuid = serviceEntry[i];

                final ParcelBluetoothGattCharacteristic parcelChar =
                        new ParcelBluetoothGattCharacteristic(null, new ParcelUuid(charUuid), 1, 0,
                                0);

                parcelService.addCharacteristic(parcelChar);

            }

            result.add(parcelService.unpack());
        }

        return result;
    }

    private void waitForResponse(final int msec) {

        // Wait for connected callback
        try {
            Thread.sleep(msec);
        } catch (final InterruptedException e) {
            Log.e(TAG, "" + e);
        }

    }
}
