package com.mediatek.bluetoothle.tests;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.content.res.Resources;
import android.test.InstrumentationTestCase;
import android.util.Log;

import com.mediatek.bluetoothle.BleProfileServerObjectPool;
import com.mediatek.bluetoothle.BleProfileServicesFactory;
import com.mediatek.bluetoothle.IBleProfileServer;
import com.mediatek.bluetoothle.TestUtil;

import java.util.UUID;

/**
 * Test Case For BleProfileServiceObjectPool
 */
public class BleProfileServiceObjectPoolTest extends InstrumentationTestCase {
    private static final String TAG = "BleProfileServiceObjectPoolTest";
    private Resources mRes;
    private BleProfileServerObjectPool mPool;

    @Override
    protected void setUp() throws Exception {
        mRes = getInstrumentation().getContext().getResources();
        mPool = BleProfileServerObjectPool.getInstance();
        mPool.init(getInstrumentation().getContext());
    }

    @Override
    protected void tearDown() throws Exception {
        mRes = null;
        mPool = null;
        TestUtil.clearServiceCache(BleProfileServicesFactory.getInstance());
    }

    /**
     * Test a profile with only one service without included service
     */
    public void testCase01() {
        IBleProfileServer profileServer = null;

        profileServer = getProfileServer(0);
        assertTrue(1 == profileServer.getServices().size());
        verifyTestService1(profileServer.getService(getUuidFromService1()));
        releaseProfileServer(profileServer);
    }

    /**
     * Test a profile with only one service with included services
     */
    public void testCase02() {
        IBleProfileServer profileServer = null;

        profileServer = getProfileServer(1);
        assertTrue(2 == profileServer.getServices().size());
        assertTrue(1 == profileServer.getService(getUuidFromService3()).getIncludedServices()
                .size());
        verifyTestService1(profileServer.getService(getUuidFromService1()));
        verifyTestService3(profileServer.getService(getUuidFromService3()));
        releaseProfileServer(profileServer);
    }

    /**
     * Test a profile with multiple services without included service
     */
    public void testCase03() {
        IBleProfileServer profileServer = null;

        profileServer = getProfileServer(2);
        assertTrue(3 == profileServer.getServices().size());
        assertTrue(1 == profileServer.getService(getUuidFromService3()).getIncludedServices()
                .size());
        verifyTestService1(profileServer.getService(getUuidFromService1()));
        verifyTestService2(profileServer.getService(getUuidFromService2()));
        verifyTestService3(profileServer.getService(getUuidFromService3()));
        releaseProfileServer(profileServer);

    }

    /**
     * Test illegal case for forward reference included service
     */
    public void testCase04() {
        String msg = null;

        try {
            IBleProfileServer profileServer = null;
            profileServer = getProfileServer(3);
            profileServer.getServices();
        } catch (final RuntimeException e) {
            /// it's a specal case for catching RuntimeException
            msg = e.getMessage();
        }

        assertNotNull(msg);
        Log.v(TAG, "testCase04: msg=" + msg);
        assertTrue("Illegal Forward Reference Included Service".equals(msg));
    }

    /*
     * public void testCase99(){ String msg = null; try{ IBleProfileServer
     * profileServer = null; profileServer = getProfileServer(4);
     * profileServer.getServices(); }catch(RuntimeException e){ msg =
     * e.getMessage(); } assertNotNull(msg); Log.v(TAG, "testCase05: msg=" +
     * msg); assertTrue(msg.startsWith("checkServiceNoDuplication:")); }
     */
    private void verifyTestService1(final BluetoothGattService service) {
        final BluetoothGattCharacteristic characteristic1 = service.getCharacteristics().get(0);
        final BluetoothGattCharacteristic characteristic2 = service.getCharacteristics().get(1);
        final BluetoothGattDescriptor descriptor1 = characteristic1.getDescriptors().get(0);
        final BluetoothGattDescriptor descriptor2 = characteristic1.getDescriptors().get(1);
        final BluetoothGattDescriptor descriptor3 = characteristic2.getDescriptors().get(0);
        // check service
        assertTrue(getUuidFromService1().equals(service.getUuid()));
        assertTrue(BluetoothGattService.SERVICE_TYPE_PRIMARY == service.getType());

        // check characteristic
        assertTrue(characteristic1.getService() == service);
        assertTrue(getUuidFromCharacteristic11().equals(characteristic1.getUuid()));
        assertTrue(characteristic1.getProperties() == getAllCharacteristicProperty());
        assertTrue(characteristic1.getPermissions() == getAllCharacteristicPermission());

        assertTrue(characteristic2.getService() == service);
        assertTrue(getUuidFromCharacteristic12().equals(characteristic2.getUuid()));
        assertTrue(characteristic2.getProperties() == 0);
        assertTrue(characteristic2.getPermissions() == 0);

        // check descriptor
        assertTrue(descriptor1.getCharacteristic() == characteristic1);
        assertTrue(getUuidFromDescriptor11().equals(descriptor1.getUuid()));
        assertTrue(descriptor1.getPermissions() == getAllDescriptorPermission());

        assertTrue(descriptor2.getCharacteristic() == characteristic1);
        assertTrue(getUuidFromDescriptor12().equals(descriptor2.getUuid()));
        assertTrue(descriptor2.getPermissions() == 0);

        assertTrue(descriptor3.getCharacteristic() == characteristic2);
        assertTrue(getUuidFromDescriptor13().equals(descriptor3.getUuid()));
        Log.v(TAG,
                "verifyTestService1: descriptor3.getPermissions()=" + descriptor3.getPermissions());
        assertTrue(descriptor3.getPermissions() == getPartialDescritporPermission());
    }

    private void verifyTestService2(final BluetoothGattService service) {
        final BluetoothGattCharacteristic characteristic1 = service.getCharacteristics().get(0);
        final BluetoothGattCharacteristic characteristic2 = service.getCharacteristics().get(1);
        final BluetoothGattDescriptor descriptor1 = characteristic1.getDescriptors().get(0);
        final BluetoothGattDescriptor descriptor2 = characteristic1.getDescriptors().get(1);
        final BluetoothGattDescriptor descriptor3 = characteristic2.getDescriptors().get(0);

        // check service
        assertTrue(getUuidFromService2().equals(service.getUuid()));
        assertTrue(BluetoothGattService.SERVICE_TYPE_PRIMARY == service.getType());

        // check characteristic
        assertTrue(characteristic1.getService() == service);
        assertTrue(getUuidFromCharacteristic21().equals(characteristic1.getUuid()));
        assertTrue(characteristic1.getProperties() == getAllCharacteristicProperty());
        assertTrue(characteristic1.getPermissions() == getAllCharacteristicPermission());

        assertTrue(characteristic2.getService() == service);
        assertTrue(getUuidFromCharacteristic22().equals(characteristic2.getUuid()));
        assertTrue(characteristic2.getProperties() == 0);
        assertTrue(characteristic2.getPermissions() == 0);

        // check descriptor
        assertTrue(descriptor1.getCharacteristic() == characteristic1);
        assertTrue(getUuidFromDescriptor21().equals(descriptor1.getUuid()));
        assertTrue(descriptor1.getPermissions() == getAllDescriptorPermission());

        assertTrue(descriptor2.getCharacteristic() == characteristic1);
        assertTrue(getUuidFromDescriptor22().equals(descriptor2.getUuid()));
        assertTrue(descriptor2.getPermissions() == 0);

        assertTrue(descriptor3.getCharacteristic() == characteristic2);
        assertTrue(getUuidFromDescriptor23().equals(descriptor3.getUuid()));
        assertTrue(descriptor3.getPermissions() == getPartialDescritporPermission());
    }

    private void verifyTestService3(final BluetoothGattService service) {
        final BluetoothGattService includedService = service.getIncludedServices().get(0);
        final BluetoothGattCharacteristic characteristic1 = service.getCharacteristics().get(0);
        final BluetoothGattCharacteristic characteristic2 = service.getCharacteristics().get(1);
        final BluetoothGattDescriptor descriptor1 = characteristic1.getDescriptors().get(0);
        final BluetoothGattDescriptor descriptor2 = characteristic1.getDescriptors().get(1);
        final BluetoothGattDescriptor descriptor3 = characteristic2.getDescriptors().get(0);

        // check service
        assertTrue(getUuidFromService3().equals(service.getUuid()));
        assertTrue(BluetoothGattService.SERVICE_TYPE_PRIMARY == service.getType());

        // check included service
        assertTrue(getUuidFromService1().equals(includedService.getUuid()));
        assertTrue(BluetoothGattService.SERVICE_TYPE_PRIMARY == includedService.getType());

        // check characteristic
        assertTrue(characteristic1.getService() == service);
        assertTrue(getUuidFromCharacteristic31().equals(characteristic1.getUuid()));
        assertTrue(characteristic1.getProperties() == getAllCharacteristicProperty());
        assertTrue(characteristic1.getPermissions() == getAllCharacteristicPermission());

        assertTrue(characteristic2.getService() == service);
        assertTrue(getUuidFromCharacteristic32().equals(characteristic2.getUuid()));
        assertTrue(characteristic2.getProperties() == 0);
        assertTrue(characteristic2.getPermissions() == 0);

        // check descriptor
        assertTrue(descriptor1.getCharacteristic() == characteristic1);
        assertTrue(getUuidFromDescriptor31().equals(descriptor1.getUuid()));
        assertTrue(descriptor1.getPermissions() == getAllDescriptorPermission());

        assertTrue(descriptor2.getCharacteristic() == characteristic1);
        assertTrue(getUuidFromDescriptor32().equals(descriptor2.getUuid()));
        assertTrue(descriptor2.getPermissions() == 0);

        assertTrue(descriptor3.getCharacteristic() == characteristic2);
        assertTrue(getUuidFromDescriptor33().equals(descriptor3.getUuid()));
        assertTrue(descriptor3.getPermissions() == getPartialDescritporPermission());
    }

    private IBleProfileServer getProfileServer(final int profile) {
        long start, end;
        IBleProfileServer profileServer = null;

        start = System.nanoTime();
        profileServer = mPool.acquire(profile);
        end = System.nanoTime();

        Log.v(TAG, "getProfileServer: cost=" + (end - start));

        return profileServer;
    }

    private void releaseProfileServer(final IBleProfileServer object) {
        mPool.release(object);
    }

    private UUID getUuidFromService1() {
        return UUID.fromString(mRes.getStringArray(R.array.service_uuid1)[0]);
    }

    private UUID getUuidFromCharacteristic11() {
        return UUID.fromString(mRes.getStringArray(R.array.characteristic_uuid1)[0]);

    }

    private UUID getUuidFromCharacteristic12() {
        return UUID.fromString(mRes.getStringArray(R.array.characteristic_uuid1)[1]);
    }

    private UUID getUuidFromDescriptor11() {
        return UUID.fromString(mRes.getStringArray(R.array.descriptor_uuid1)[0]);
    }

    private UUID getUuidFromDescriptor12() {
        return UUID.fromString(mRes.getStringArray(R.array.descriptor_uuid1)[1]);
    }

    private UUID getUuidFromDescriptor13() {
        return UUID.fromString(mRes.getStringArray(R.array.descriptor_uuid1)[2]);
    }

    private UUID getUuidFromService2() {
        return UUID.fromString(mRes.getStringArray(R.array.service_uuid2)[0]);
    }

    private UUID getUuidFromCharacteristic21() {
        return UUID.fromString(mRes.getStringArray(R.array.characteristic_uuid2)[0]);

    }

    private UUID getUuidFromCharacteristic22() {
        return UUID.fromString(mRes.getStringArray(R.array.characteristic_uuid2)[1]);
    }

    private UUID getUuidFromDescriptor21() {
        return UUID.fromString(mRes.getStringArray(R.array.descriptor_uuid2)[0]);
    }

    private UUID getUuidFromDescriptor22() {
        return UUID.fromString(mRes.getStringArray(R.array.descriptor_uuid2)[1]);
    }

    private UUID getUuidFromDescriptor23() {
        return UUID.fromString(mRes.getStringArray(R.array.descriptor_uuid2)[2]);
    }

    private UUID getUuidFromService3() {
        return UUID.fromString(mRes.getStringArray(R.array.service_uuid3)[0]);
    }

    private UUID getUuidFromCharacteristic31() {
        return UUID.fromString(mRes.getStringArray(R.array.characteristic_uuid3)[0]);

    }

    private UUID getUuidFromCharacteristic32() {
        return UUID.fromString(mRes.getStringArray(R.array.characteristic_uuid3)[1]);
    }

    private UUID getUuidFromDescriptor31() {
        return UUID.fromString(mRes.getStringArray(R.array.descriptor_uuid3)[0]);
    }

    private UUID getUuidFromDescriptor32() {
        return UUID.fromString(mRes.getStringArray(R.array.descriptor_uuid3)[1]);
    }

    private UUID getUuidFromDescriptor33() {
        return UUID.fromString(mRes.getStringArray(R.array.descriptor_uuid3)[2]);
    }

    private int getAllCharacteristicProperty() {
        return BluetoothGattCharacteristic.PROPERTY_BROADCAST |
                BluetoothGattCharacteristic.PROPERTY_EXTENDED_PROPS |
                BluetoothGattCharacteristic.PROPERTY_INDICATE |
                BluetoothGattCharacteristic.PROPERTY_NOTIFY |
                BluetoothGattCharacteristic.PROPERTY_READ |
                BluetoothGattCharacteristic.PROPERTY_SIGNED_WRITE |
                BluetoothGattCharacteristic.PROPERTY_WRITE |
                BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE;
    }

    private int getAllCharacteristicPermission() {
        return BluetoothGattCharacteristic.PERMISSION_READ |
                BluetoothGattCharacteristic.PERMISSION_READ_ENCRYPTED |
                BluetoothGattCharacteristic.PERMISSION_READ_ENCRYPTED_MITM |
                BluetoothGattCharacteristic.PERMISSION_WRITE |
                BluetoothGattCharacteristic.PERMISSION_WRITE_ENCRYPTED |
                BluetoothGattCharacteristic.PERMISSION_WRITE_ENCRYPTED_MITM |
                BluetoothGattCharacteristic.PERMISSION_WRITE_SIGNED |
                BluetoothGattCharacteristic.PERMISSION_WRITE_SIGNED_MITM;
    }

    private int getAllDescriptorPermission() {
        return BluetoothGattDescriptor.PERMISSION_READ |
                BluetoothGattDescriptor.PERMISSION_READ_ENCRYPTED |
                BluetoothGattDescriptor.PERMISSION_READ_ENCRYPTED_MITM |
                BluetoothGattDescriptor.PERMISSION_WRITE |
                BluetoothGattDescriptor.PERMISSION_WRITE_ENCRYPTED |
                BluetoothGattDescriptor.PERMISSION_WRITE_ENCRYPTED_MITM |
                BluetoothGattDescriptor.PERMISSION_WRITE_SIGNED |
                BluetoothGattDescriptor.PERMISSION_WRITE_SIGNED_MITM;
    }

    private int getPartialDescritporPermission() {
        return BluetoothGattDescriptor.PERMISSION_READ |
                BluetoothGattDescriptor.PERMISSION_READ_ENCRYPTED |
                BluetoothGattDescriptor.PERMISSION_READ_ENCRYPTED_MITM |
                BluetoothGattDescriptor.PERMISSION_WRITE;
    }
}
