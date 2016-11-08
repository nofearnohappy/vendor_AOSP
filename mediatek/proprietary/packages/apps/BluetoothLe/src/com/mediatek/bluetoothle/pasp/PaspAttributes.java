package com.mediatek.bluetoothle.pasp;

import java.util.HashMap;
import java.util.UUID;

public class PaspAttributes {
    private static HashMap<String, String> sAttributes = new HashMap<String, String>();

    private static HashMap<String, String> sDefaultUuidNameMapping = sAttributes;
    // / uuid of service
    public static final UUID PHONE_ALERT_STATUS_SERVICE_UUID = UUID
            .fromString("0000180E-0000-1000-8000-00805f9b34fb");
    // / uuid of characteristics
    public static final UUID ALERT_STATE_CHAR_UUID = UUID
            .fromString("00002A3F-0000-1000-8000-00805f9b34fb");
    public static final UUID RINGER_SETTING_CHAR_UUID = UUID
            .fromString("00002A41-0000-1000-8000-00805f9b34fb");
    public static final UUID RINGER_CONTROL_POINT_CHAR_UUID = UUID
            .fromString("00002A40-0000-1000-8000-00805f9b34fb");
    // / uuid of descriptors
    public static final UUID ALERT_CHANGE_DES_UUID = UUID
            .fromString("00002902-0000-1000-8000-00805f9b34fb");
    public static final UUID RINGER_CHANGE_DES_UUID = UUID
            .fromString("00002902-0000-1000-8000-00805f9b34fb");
    // / uuid for gatt well-known services
    public static final UUID GENERIC_ACCESS_PROFILE_SERVICE_UUID = UUID
            .fromString("00001800-0000-1000-8000-00805f9b34fb");
    public static final UUID GENERIC_ATTRIBTE_PROFILE_SERVICE_UUID = UUID
            .fromString("00001801-0000-1000-8000-00805f9b34fb");
    public static final UUID IMMEDIATE_ALERT_SERVICE_UUID = UUID
            .fromString("00001802-0000-1000-8000-00805f9b34fb");
    public static final UUID LINK_LOSS_SERVICE_UUID = UUID
            .fromString("00001803-0000-1000-8000-00805f9b34fb");
    public static final UUID TX_POWER_SERVICE_UUID = UUID
            .fromString("00001804-0000-1000-8000-00805f9b34fb");
    public static final UUID DEVICE_INFORMATION_SERVICE = UUID
            .fromString("0000180a-0000-1000-8000-00805f9b34fb");
    public static final UUID HEART_RATE_SERVICE = UUID
            .fromString("0000180d-0000-1000-8000-00805f9b34fb");
    public static final UUID BATTERY_SERVICE_UUID = UUID
            .fromString("0000180f-0000-1000-8000-00805f9b34fb");

    // / uuid for gatt well-known characteristics
    public static final UUID DEVICE_NAME_CHAR_UUID = UUID
            .fromString("00002a00-0000-1000-8000-00805f9b34fb");
    public static final UUID APPEARANCE_CHAR_UUID = UUID
            .fromString("00002a01-0000-1000-8000-00805f9b34fb");
    public static final UUID PERIPHERIAL_CONNECTION_PARAMETERS_CHAR_UUID = UUID
            .fromString("00002a04-0000-1000-8000-00805f9b34fb");
    public static final UUID SERVICE_CHANGED_CHAR_UUID = UUID
            .fromString("00002a05-0000-1000-8000-00805f9b34fb");
    public static final UUID ALERT_LEVEL_CHAR_UUID = UUID
            .fromString("00002a06-0000-1000-8000-00805f9b34fb");
    public static final UUID TX_POWER_LEVEL_CHAR_UUID = UUID
            .fromString("00002a07-0000-1000-8000-00805f9b34fb");
    public static final UUID BATTERY_LEVEL_CHAR_UUID = UUID
            .fromString("00002a19-0000-1000-8000-00805f9b34fb");
    public static final UUID MANUFACTURER_NAME_CHAR_UUID = UUID
            .fromString("00002a24-0000-1000-8000-00805f9b34fb");
    public static final UUID MODEL_NUMBER_CHAR_UUID = UUID
            .fromString("00002a26-0000-1000-8000-00805f9b34fb");
    public static final UUID FIRMWARE_REVISION_CHAR_UUID = UUID
            .fromString("00002a28-0000-1000-8000-00805f9b34fb");
    public static final UUID SOFTWARE_REVISION_CHAR_UUID = UUID
            .fromString("00002a29-0000-1000-8000-00805f9b34fb");
    public static final UUID HEART_RATE_MEASUREMENT_UUID = UUID
            .fromString("00002a37-0000-1000-8000-00805f9b34fb");

    // / uuid for gatt well-known descriptor
    public static final UUID CLIENT_CHARACTERISTIC_CONFIG_DES_UUID = UUID
            .fromString("00002902-0000-1000-8000-00805f9b34fb");
    // / Standard Assigned Number
    // / Services
    static {
        // / Services for testing
        sAttributes.put(PHONE_ALERT_STATUS_SERVICE_UUID.toString(),
                "PHONE ALERT STATUS SERVICE");

        // / Characteristics for testing
        sAttributes.put(ALERT_STATE_CHAR_UUID.toString(), "Alert Status");
        sAttributes.put(RINGER_SETTING_CHAR_UUID.toString(), "Ringer Setting");
        sAttributes.put(RINGER_CONTROL_POINT_CHAR_UUID.toString(),
                "Ringer Control Point");
        // / Descriptors for testing
        sAttributes.put(ALERT_CHANGE_DES_UUID.toString(),
                "Alert Change Notify Configuration");
        sAttributes.put(RINGER_CHANGE_DES_UUID.toString(),
                "Ringer Change Notify Configuration");

        sAttributes.put(GENERIC_ACCESS_PROFILE_SERVICE_UUID.toString(),
                "GENERIC ACCESS");
        sAttributes.put(GENERIC_ATTRIBTE_PROFILE_SERVICE_UUID.toString(),
                "GENERIC ATTRIBUTE");
        sAttributes.put(IMMEDIATE_ALERT_SERVICE_UUID.toString(),
                "IMMEDIATE ALERT");
        sAttributes.put(LINK_LOSS_SERVICE_UUID.toString(), "LINK LOSS");
        sAttributes.put(TX_POWER_SERVICE_UUID.toString(), "TX POWER");
        sAttributes.put(DEVICE_INFORMATION_SERVICE.toString(),
                "DEVICE INFORMATION");
        sAttributes.put(HEART_RATE_SERVICE.toString(), "HEART RATE");
        sAttributes.put(BATTERY_SERVICE_UUID.toString(), "BATTERY");
        // / Characteristics
        sAttributes.put(DEVICE_NAME_CHAR_UUID.toString(), "DEVICE NAME");
        sAttributes.put(APPEARANCE_CHAR_UUID.toString(), "APPEARANCE");
        sAttributes.put(SERVICE_CHANGED_CHAR_UUID.toString(), "SERVICE CHANGED");
        sAttributes.put(PERIPHERIAL_CONNECTION_PARAMETERS_CHAR_UUID.toString(),
                "PERIPHERIAL PARAMETERS");
        sAttributes.put(ALERT_LEVEL_CHAR_UUID.toString(), "ALERT LEVEL");
        sAttributes.put(TX_POWER_LEVEL_CHAR_UUID.toString(), "TX POWER LEVEL");
        sAttributes.put(BATTERY_LEVEL_CHAR_UUID.toString(), "BATTERY LEVEL");
        sAttributes.put(MANUFACTURER_NAME_CHAR_UUID.toString(),
                "MANUFACTURER NAME");
        sAttributes.put(MODEL_NUMBER_CHAR_UUID.toString(), "MODEL NUMBER");
        sAttributes.put(FIRMWARE_REVISION_CHAR_UUID.toString(),
                "FIRMWARE REVISION");
        sAttributes.put(SOFTWARE_REVISION_CHAR_UUID.toString(),
                "SOFTWARE REVISION");
        sAttributes.put(HEART_RATE_MEASUREMENT_UUID.toString(),
                "HEART RATE MEASUREMENT");
        // / Descriptors
        sAttributes.put(CLIENT_CHARACTERISTIC_CONFIG_DES_UUID.toString(),
                "CLIENT CONFIGURATION");

    }

    public static String lookup(String uuid, String defaultName) {
        String name = sAttributes.get(uuid);
        return name == null ? defaultName : name;
    }

    public static void setUuidNameMapping(
            HashMap<String, String> mUuidNameMapping) {
        sAttributes = mUuidNameMapping;
    }

    public static void setDefaultUuidNameMapping() {
        sAttributes = sDefaultUuidNameMapping;
    }
}
