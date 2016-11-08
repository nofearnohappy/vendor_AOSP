/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein is
 * confidential and proprietary to MediaTek Inc. and/or its licensors. Without
 * the prior written permission of MediaTek inc. and/or its licensors, any
 * reproduction, modification, use or disclosure of MediaTek Software, and
 * information contained herein, in whole or in part, shall be strictly
 * prohibited.
 *
 * MediaTek Inc. (C) 2015. All rights reserved.
 *
 * BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 * THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
 * RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER
 * ON AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL
 * WARRANTIES, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR
 * NONINFRINGEMENT. NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH
 * RESPECT TO THE SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY,
 * INCORPORATED IN, OR SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES
 * TO LOOK ONLY TO SUCH THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO.
 * RECEIVER EXPRESSLY ACKNOWLEDGES THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO
 * OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES CONTAINED IN MEDIATEK
 * SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK SOFTWARE
 * RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
 * STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S
 * ENTIRE AND CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE
 * RELEASED HEREUNDER WILL BE, AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE
 * MEDIATEK SOFTWARE AT ISSUE, OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE
 * CHARGE PAID BY RECEIVER TO MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 * The following software/firmware and/or related documentation ("MediaTek
 * Software") have been modified by MediaTek Inc. All revisions are subject to
 * any receiver's applicable license agreements with MediaTek Inc.
 */

package com.mediatek.bluetoothgatt;

import com.mediatek.bluetoothgatt.characteristic.FormatUtils;

import java.util.UUID;

/**
 * Provide GATT service and GATT characteristic UUID.
 */
public class GattUuid {
    /**
     * Bluetooth GATT Service UUID.
     */

    //Alert Notification Service
    public static final UUID SRVC_ANS =
            UUID.fromString(FormatUtils.uuid16ToUuid128("1811"));

    // Battery Service
    public static final UUID SRVC_BAS =
            UUID.fromString(FormatUtils.uuid16ToUuid128("180F"));

    // Blood Pressure
    public static final UUID SRVC_BLS =
            UUID.fromString(FormatUtils.uuid16ToUuid128("1810"));

    // Body Composition
    public static final UUID SRVC_BCS =
            UUID.fromString(FormatUtils.uuid16ToUuid128("181B"));

    // Bond Management
    public static final UUID SRVC_BMS =
            UUID.fromString(FormatUtils.uuid16ToUuid128("181E"));

    // Continuous Glucose Monitoring
    public static final UUID SRVC_CGMS =
            UUID.fromString(FormatUtils.uuid16ToUuid128("181F"));

    // Current Time Service
    public static final UUID SRVC_CTS =
            UUID.fromString(FormatUtils.uuid16ToUuid128("1805"));

    // Cycling Power
    public static final UUID SRVC_CPS =
            UUID.fromString(FormatUtils.uuid16ToUuid128("1818"));

    // Cycling Speed and Cadence
    public static final UUID SRVC_CSCS =
            UUID.fromString(FormatUtils.uuid16ToUuid128("1816"));

    // Device Information
    public static final UUID SRVC_DIS =
            UUID.fromString(FormatUtils.uuid16ToUuid128("180A"));

    // Environmental Sensing
    public static final UUID SRVC_ESS =
            UUID.fromString(FormatUtils.uuid16ToUuid128("181A"));

    // Generic Access
    public static final UUID SRVC_GAS =
            UUID.fromString(FormatUtils.uuid16ToUuid128("1800"));

    // Generic Attribute
    public static final UUID SRVC_GATTS =
            UUID.fromString(FormatUtils.uuid16ToUuid128("1801"));

    // Glucose
    public static final UUID SRVC_GLS =
            UUID.fromString(FormatUtils.uuid16ToUuid128("1808"));

    // Health Thermometer
    public static final UUID SRVC_HTS =
            UUID.fromString(FormatUtils.uuid16ToUuid128("1809"));

    // Heart Rate
    public static final UUID SRVC_HRS =
            UUID.fromString(FormatUtils.uuid16ToUuid128("180D"));

    // Human Interface Device
    public static final UUID SRVC_HIDS =
            UUID.fromString(FormatUtils.uuid16ToUuid128("1812"));

    // Immediate Alert
    public static final UUID SRVC_IAS =
            UUID.fromString(FormatUtils.uuid16ToUuid128("1802"));

    // Indoor Positioning
    public static final UUID SRVC_IPS =
            UUID.fromString(FormatUtils.uuid16ToUuid128("1821"));

    // Internet Protocol Support
    public static final UUID SRVC_IPSS =
            UUID.fromString(FormatUtils.uuid16ToUuid128("1820"));

    // Link Loss
    public static final UUID SRVC_LLS =
            UUID.fromString(FormatUtils.uuid16ToUuid128("1803"));

    // Location and Navigation
    public static final UUID SRVC_LNS =
            UUID.fromString(FormatUtils.uuid16ToUuid128("1819"));

    // Next DST Change Service
    public static final UUID SRVC_NDCS =
            UUID.fromString(FormatUtils.uuid16ToUuid128("1807"));

    // Phone Alert Status Service
    public static final UUID SRVC_PASS =
            UUID.fromString(FormatUtils.uuid16ToUuid128("180E"));

    // Reference Time Update Service
    public static final UUID SRVC_RTUS =
            UUID.fromString(FormatUtils.uuid16ToUuid128("1806"));

    // Running Speed and Cadence
    public static final UUID SRVC_RSCS =
            UUID.fromString(FormatUtils.uuid16ToUuid128("1814"));

    // Scan Parameters
    public static final UUID SRVC_SCPS =
            UUID.fromString(FormatUtils.uuid16ToUuid128("1813"));

    // Tx Power
    public static final UUID SRVC_TPS =
            UUID.fromString(FormatUtils.uuid16ToUuid128("1804"));

    // User Data
    public static final UUID SRVC_UDS =
            UUID.fromString(FormatUtils.uuid16ToUuid128("181C"));

    // Weight Scale
    public static final UUID SRVC_WSS =
            UUID.fromString(FormatUtils.uuid16ToUuid128("181D"));

    /**
     * Bluetooth GATT Characteristic UUID.
     */

    //Aerobic Heart Rate Lower Limit
    public static final UUID CHAR_AEROBIC_HEART_RATE_LOWER_LIMIT =
            UUID.fromString(FormatUtils.uuid16ToUuid128("2A7E"));

    //Aerobic Heart Rate Upper Limit
    public static final UUID CHAR_AEROBIC_HEART_RATE_UPPER_LIMIT =
            UUID.fromString(FormatUtils.uuid16ToUuid128("2A84"));

    //Aerobic Threshold
    public static final UUID CHAR_AEROBIC_THRESHOLD =
            UUID.fromString(FormatUtils.uuid16ToUuid128("2A7F"));

    //Age
    public static final UUID CHAR_AGE =
            UUID.fromString(FormatUtils.uuid16ToUuid128("2A80"));

    //Alert Category ID
    public static final UUID CHAR_ALERT_CATEGORY_ID =
            UUID.fromString(FormatUtils.uuid16ToUuid128("2A43"));

    //Alert Category ID Bit Mask
    public static final UUID CHAR_ALERT_CATEGORY_ID_BIT_MASK =
            UUID.fromString(FormatUtils.uuid16ToUuid128("2A42"));

    //Alert Level
    public static final UUID CHAR_ALERT_LEVEL =
            UUID.fromString(FormatUtils.uuid16ToUuid128("2A06"));

    //Alert Notification Control Point
    public static final UUID CHAR_ALERT_NOTIFICATION_CONTROL_POINT =
            UUID.fromString(FormatUtils.uuid16ToUuid128("2A44"));

    //Alert Status
    public static final UUID CHAR_ALERT_STATUS =
            UUID.fromString(FormatUtils.uuid16ToUuid128("2A3F"));

    //Altitude
    public static final UUID CHAR_ALTITUDE =
            UUID.fromString(FormatUtils.uuid16ToUuid128("2AB3"));

    //Anaerobic Heart Rate Lower Limit
    public static final UUID CHAR_ANAEROBIC_HEART_RATE_LOWER_LIMIT =
            UUID.fromString(FormatUtils.uuid16ToUuid128("2A81"));

    //Anaerobic Heart Rate Upper Limit
    public static final UUID CHAR_ANAEROBIC_HEART_RATE_UPPER_LIMIT =
            UUID.fromString(FormatUtils.uuid16ToUuid128("2A82"));

    //Anaerobic Threshold
    public static final UUID CHAR_ANAEROBIC_THRESHOLD =
            UUID.fromString(FormatUtils.uuid16ToUuid128("2A83"));

    //Apparent Wind Direction?
    public static final UUID CHAR_APPARENT_WIND_DIRECTION =
            UUID.fromString(FormatUtils.uuid16ToUuid128("2A73"));

    //Apparent Wind Speed
    public static final UUID CHAR_APPARENT_WIND_SPEED =
            UUID.fromString(FormatUtils.uuid16ToUuid128("2A72"));

    //Appearance
    public static final UUID CHAR_APPEARANCE =
            UUID.fromString(FormatUtils.uuid16ToUuid128("2A01"));

    //Barometric Pressure Trend
    public static final UUID CHAR_BAROMETRIC_PRESSURE_TREND =
            UUID.fromString(FormatUtils.uuid16ToUuid128("2AA3"));

    //Battery Level
    public static final UUID CHAR_BATTERY_LEVEL =
            UUID.fromString(FormatUtils.uuid16ToUuid128("2A19"));

    //Blood Pressure Feature
    public static final UUID CHAR_BLOOD_PRESSURE_FEATURE =
            UUID.fromString(FormatUtils.uuid16ToUuid128("2A49"));

    //Blood Pressure Measurement
    public static final UUID CHAR_BLOOD_PRESSURE_MEASUREMENT =
            UUID.fromString(FormatUtils.uuid16ToUuid128("2A35"));

    //Body Composition Feature
    public static final UUID CHAR_BODY_COMPOSITION_FEATURE =
            UUID.fromString(FormatUtils.uuid16ToUuid128("2A9B"));

    //Body Composition Measurement
    public static final UUID CHAR_BODY_COMPOSITION_MEASUREMENT =
            UUID.fromString(FormatUtils.uuid16ToUuid128("2A9C"));

    //Body Sensor Location
    public static final UUID CHAR_BODY_SENSOR_LOCATION =
            UUID.fromString(FormatUtils.uuid16ToUuid128("2A38"));

    //Bond Management Control Point
    public static final UUID CHAR_BOND_MANAGEMENT_CONTROL_POINT =
            UUID.fromString(FormatUtils.uuid16ToUuid128("2AA4"));

    //Bond Management Feature
    public static final UUID CHAR_BOND_MANAGEMENT_FEATURE =
            UUID.fromString(FormatUtils.uuid16ToUuid128("2AA5"));

    //Boot Keyboard Input Report
    public static final UUID CHAR_BOOT_KEYBOARD_INPUT_REPORT =
            UUID.fromString(FormatUtils.uuid16ToUuid128("2A22"));

    //Boot Keyboard Output Report
    public static final UUID CHAR_BOOT_KEYBOARD_OUTPUT_REPORT =
            UUID.fromString(FormatUtils.uuid16ToUuid128("2A32"));

    //Boot Mouse Input Report
    public static final UUID CHAR_BOOT_MOUSE_INPUT_REPORT =
            UUID.fromString(FormatUtils.uuid16ToUuid128("2A33"));

    //Central Address Resolution
    public static final UUID CHAR_CENTRAL_ADDRESS_RESOLUTION_SUPPORT =
            UUID.fromString(FormatUtils.uuid16ToUuid128("2AA6"));

    //CGM Feature
    public static final UUID CHAR_CGM_FEATURE =
            UUID.fromString(FormatUtils.uuid16ToUuid128("2AA8"));

    //CGM Measurement
    public static final UUID CHAR_CGM_MEASUREMENT =
            UUID.fromString(FormatUtils.uuid16ToUuid128("2AA7"));

    //CGM Session Run Time
    public static final UUID CHAR_CGM_SESSION_RUN_TIME =
            UUID.fromString(FormatUtils.uuid16ToUuid128("2AAB"));

    //CGM Session Start Time
    public static final UUID CHAR_CGM_SESSION_START_TIME =
            UUID.fromString(FormatUtils.uuid16ToUuid128("2AAA"));

    //CGM Specific Ops Control Point
    public static final UUID CHAR_CGM_SPECIFIC_OPS_CONTROL_POINT =
            UUID.fromString(FormatUtils.uuid16ToUuid128("2AAC"));

    //CGM Status
    public static final UUID CHAR_CGM_STATUS =
            UUID.fromString(FormatUtils.uuid16ToUuid128("2AA9"));

    //CSC Feature
    public static final UUID CHAR_CSC_FEATURE =
            UUID.fromString(FormatUtils.uuid16ToUuid128("2A5C"));

    //CSC Measurement
    public static final UUID CHAR_CSC_MEASUREMENT =
            UUID.fromString(FormatUtils.uuid16ToUuid128("2A5B"));

    //Current Time
    public static final UUID CHAR_CURRENT_TIME =
            UUID.fromString(FormatUtils.uuid16ToUuid128("2A2B"));

    //Cycling Power Control Point
    public static final UUID CHAR_CYCLING_POWER_CONTROL_POINT =
            UUID.fromString(FormatUtils.uuid16ToUuid128("2A66"));

    //Cycling Power Feature
    public static final UUID CHAR_CYCLING_POWER_FEATURE =
            UUID.fromString(FormatUtils.uuid16ToUuid128("2A65"));

    //Cycling Power Measurement
    public static final UUID CHAR_CYCLING_POWER_MEASUREMENT =
            UUID.fromString(FormatUtils.uuid16ToUuid128("2A63"));

    //Cycling Power Vector
    public static final UUID CHAR_CYCLING_POWER_VECTOR =
            UUID.fromString(FormatUtils.uuid16ToUuid128("2A64"));

    //Database Change Increment
    public static final UUID CHAR_DATABASE_CHANGE_INCREMENT =
            UUID.fromString(FormatUtils.uuid16ToUuid128("2A99"));

    //Date of Birth
    public static final UUID CHAR_DATE_OF_BIRTH =
            UUID.fromString(FormatUtils.uuid16ToUuid128("2A85"));

    //Date of Threshold Assessment
    public static final UUID CHAR_DATE_OF_THRESHOLD_ASSESSMENT =
            UUID.fromString(FormatUtils.uuid16ToUuid128("2A86"));

    //Date Time
    public static final UUID CHAR_DATE_TIME =
            UUID.fromString(FormatUtils.uuid16ToUuid128("2A08"));

    //Day Date Time
    public static final UUID CHAR_DAY_DATE_TIME =
            UUID.fromString(FormatUtils.uuid16ToUuid128("2A0A"));

    //Day of Week
    public static final UUID CHAR_DAY_OF_WEEK =
            UUID.fromString(FormatUtils.uuid16ToUuid128("2A09"));

    //Descriptor Value Changed
    public static final UUID CHAR_DESCRIPTOR_VALUE_CHANGED =
            UUID.fromString(FormatUtils.uuid16ToUuid128("2A7D"));

    //Device Name
    public static final UUID CHAR_DEVICE_NAME =
            UUID.fromString(FormatUtils.uuid16ToUuid128("2A00"));

    //Dew Point
    public static final UUID CHAR_DEW_POINT =
            UUID.fromString(FormatUtils.uuid16ToUuid128("2A7B"));

    //DST Offset
    public static final UUID CHAR_DST_OFFSET =
            UUID.fromString(FormatUtils.uuid16ToUuid128("2A0D"));

    //Elevation
    public static final UUID CHAR_ELEVATION =
            UUID.fromString(FormatUtils.uuid16ToUuid128("2A6C"));

    //Email Address
    public static final UUID CHAR_EMAIL_ADDRESS =
            UUID.fromString(FormatUtils.uuid16ToUuid128("2A87"));

    //Exact Time 256
    public static final UUID CHAR_EXACT_TIME_256 =
            UUID.fromString(FormatUtils.uuid16ToUuid128("2A0C"));

    //Fat Burn Heart Rate Lower Limit
    public static final UUID CHAR_FAT_BURN_HEART_RATE_LOWER_LIMIT =
            UUID.fromString(FormatUtils.uuid16ToUuid128("2A88"));

    //Fat Burn Heart Rate Upper Limit
    public static final UUID CHAR_FAT_BURN_HEART_RATE_UPPER_LIMIT =
            UUID.fromString(FormatUtils.uuid16ToUuid128("2A89"));

    //Firmware Revision String
    public static final UUID CHAR_FIRMWARE_REVISION_STRING =
            UUID.fromString(FormatUtils.uuid16ToUuid128("2A26"));

    //First Name
    public static final UUID CHAR_FIRST_NAME =
            UUID.fromString(FormatUtils.uuid16ToUuid128("2A8A"));

    //Five Zone Heart Rate Limits
    public static final UUID CHAR_FIVE_ZONE_HEART_RATE_LIMITS =
            UUID.fromString(FormatUtils.uuid16ToUuid128("2A8B"));

    //Floor Number
    public static final UUID CHAR_FLOOR_NUMBER =
            UUID.fromString(FormatUtils.uuid16ToUuid128("2AB2"));

    //Gender
    public static final UUID CHAR_GENDER =
            UUID.fromString(FormatUtils.uuid16ToUuid128("2A8C"));

    //Glucose Feature
    public static final UUID CHAR_GLUCOSE_FEATURE =
            UUID.fromString(FormatUtils.uuid16ToUuid128("2A51"));

    //Glucose Measurement
    public static final UUID CHAR_GLUCOSE_MEASUREMENT =
            UUID.fromString(FormatUtils.uuid16ToUuid128("2A18"));

    //Glucose Measurement Context
    public static final UUID CHAR_GLUCOSE_MEASUREMENT_CONTEXT =
            UUID.fromString(FormatUtils.uuid16ToUuid128("2A34"));

    //Gust Factor
    public static final UUID CHAR_GUST_FACTOR =
            UUID.fromString(FormatUtils.uuid16ToUuid128("2A74"));

    //Hardware Revision String
    public static final UUID CHAR_HARDWARE_REVISION_STRING =
            UUID.fromString(FormatUtils.uuid16ToUuid128("2A27"));

    //Heart Rate Control Point
    public static final UUID CHAR_HEART_RATE_CONTROL_POINT =
            UUID.fromString(FormatUtils.uuid16ToUuid128("2A39"));

    //Heart Rate Max
    public static final UUID CHAR_HEART_RATE_MAX =
            UUID.fromString(FormatUtils.uuid16ToUuid128("2A8D"));

    //Heart Rate Measurement
    public static final UUID CHAR_HEART_RATE_MEASUREMENT =
            UUID.fromString(FormatUtils.uuid16ToUuid128("2A37"));

    //Heat Index
    public static final UUID CHAR_HEAT_INDEX =
            UUID.fromString(FormatUtils.uuid16ToUuid128("2A7A"));

    //Height
    public static final UUID CHAR_HEIGHT =
            UUID.fromString(FormatUtils.uuid16ToUuid128("2A8E"));

    //HID Control Point
    public static final UUID CHAR_HID_CONTROL_POINT =
            UUID.fromString(FormatUtils.uuid16ToUuid128("2A4C"));

    //HID Information
    public static final UUID CHAR_HID_INFORMATION =
            UUID.fromString(FormatUtils.uuid16ToUuid128("2A4A"));

    //Hip Circumference
    public static final UUID CHAR_HIP_CIRCUMFERENCE =
            UUID.fromString(FormatUtils.uuid16ToUuid128("2A8F"));

    //Humidity
    public static final UUID CHAR_HUMIDITY =
            UUID.fromString(FormatUtils.uuid16ToUuid128("2A6F"));

    //IEEE 11073-20601 Regulatory Certification Data List
    public static final UUID CHAR_REG_CERT_DATA_LIST =
            UUID.fromString(FormatUtils.uuid16ToUuid128("2A2A"));

    //Indoor Positioning Configuration
    public static final UUID CHAR_INDOOR_POSITIONING_CONFIGURATION =
            UUID.fromString(FormatUtils.uuid16ToUuid128("2AAD"));

    //Intermediate Cuff Pressure
    public static final UUID CHAR_INTERMEDIATE_CUFF_PRESSURE =
            UUID.fromString(FormatUtils.uuid16ToUuid128("2A36"));

    //Intermediate Temperature
    public static final UUID CHAR_INTERMEDIATE_TEMPERATURE =
            UUID.fromString(FormatUtils.uuid16ToUuid128("2A1E"));

    //Irradiance
    public static final UUID CHAR_IRRADIANCE =
            UUID.fromString(FormatUtils.uuid16ToUuid128("2A77"));

    //Language
    public static final UUID CHAR_LANGUAGE =
            UUID.fromString(FormatUtils.uuid16ToUuid128("2AA2"));

    //Last Name
    public static final UUID CHAR_LAST_NAME =
            UUID.fromString(FormatUtils.uuid16ToUuid128("2A90"));

    //Latitude
    public static final UUID CHAR_LATITUDE =
            UUID.fromString(FormatUtils.uuid16ToUuid128("2AAE"));

    //LN Control Point
    public static final UUID CHAR_LN_CONTROL_POINT =
            UUID.fromString(FormatUtils.uuid16ToUuid128("2A6B"));

    //LN Feature
    public static final UUID CHAR_LN_FEATURE =
            UUID.fromString(FormatUtils.uuid16ToUuid128("2A6A"));

    //Local East Coordinate
    public static final UUID CHAR_LOCAL_EAST_COORDINATE =
            UUID.fromString(FormatUtils.uuid16ToUuid128("2AB1"));

    //Local North Coordinate
    public static final UUID CHAR_LOCAL_NORTH_COORDINATE =
            UUID.fromString(FormatUtils.uuid16ToUuid128("2AB0"));

    //Local Time Information
    public static final UUID CHAR_LOCAL_TIME_INFORMATION =
            UUID.fromString(FormatUtils.uuid16ToUuid128("2A0F"));

    //Location and Speed
    public static final UUID CHAR_LOCATION_AND_SPEED =
            UUID.fromString(FormatUtils.uuid16ToUuid128("2A67"));

    //Location Name
    public static final UUID CHAR_LOCATION_NAME =
            UUID.fromString(FormatUtils.uuid16ToUuid128("2AB5"));

    //Longitude
    public static final UUID CHAR_LONGITUDE =
            UUID.fromString(FormatUtils.uuid16ToUuid128("2AAF"));

    //Magnetic Declination
    public static final UUID CHAR_MAGNETIC_DECLINATION =
            UUID.fromString(FormatUtils.uuid16ToUuid128("2A2C"));

    //Magnetic Flux Density - 2D
    public static final UUID CHAR_MAGNETIC_FLUX_DENSITY_2D =
            UUID.fromString(FormatUtils.uuid16ToUuid128("2AA0"));

    //Magnetic Flux Density - 3D
    public static final UUID CHAR_MAGNETIC_FLUX_DENSITY_3D =
            UUID.fromString(FormatUtils.uuid16ToUuid128("2AA1"));

    //Manufacturer Name String
    public static final UUID CHAR_MANUFACTURER_NAME_STRING =
            UUID.fromString(FormatUtils.uuid16ToUuid128("2A29"));

    //Maximum Recommended Heart Rate
    public static final UUID CHAR_MAXIMUM_RECOMMENDED_HEART_RATE =
            UUID.fromString(FormatUtils.uuid16ToUuid128("2A91"));

    //Measurement Interval
    public static final UUID CHAR_MEASUREMENT_INTERVAL =
            UUID.fromString(FormatUtils.uuid16ToUuid128("2A21"));

    //Model Number String
    public static final UUID CHAR_MODEL_NUMBER_STRING =
            UUID.fromString(FormatUtils.uuid16ToUuid128("2A24"));

    //Navigation
    public static final UUID CHAR_NAVIGATION =
            UUID.fromString(FormatUtils.uuid16ToUuid128("2A68"));

    //New Alert
    public static final UUID CHAR_NEW_ALERT =
            UUID.fromString(FormatUtils.uuid16ToUuid128("2A46"));

    //Peripheral Preferred Connection Parameters
    public static final UUID CHAR_PERIPHERAL_PREFERRED_CONNECTION_PARAMETERS =
            UUID.fromString(FormatUtils.uuid16ToUuid128("2A04"));

    //Peripheral Privacy Flag
    public static final UUID CHAR_PERIPHERAL_PRIVACY_FLAG =
            UUID.fromString(FormatUtils.uuid16ToUuid128("2A02"));

    //PnP ID
    public static final UUID CHAR_PNP_ID =
            UUID.fromString(FormatUtils.uuid16ToUuid128("2A50"));

    //Pollen Concentration
    public static final UUID CHAR_POLLEN_CONCENTRATION =
            UUID.fromString(FormatUtils.uuid16ToUuid128("2A75"));

    //Position Quality
    public static final UUID CHAR_POSITION_QUALITY =
            UUID.fromString(FormatUtils.uuid16ToUuid128("2A69"));

    //Pressure
    public static final UUID CHAR_PRESSURE =
            UUID.fromString(FormatUtils.uuid16ToUuid128("2A6D"));

    //Protocol Mode
    public static final UUID CHAR_PROTOCOL_MODE =
            UUID.fromString(FormatUtils.uuid16ToUuid128("2A4E"));

    //Rainfall
    public static final UUID CHAR_RAINFALL =
            UUID.fromString(FormatUtils.uuid16ToUuid128("2A78"));

    //Reconnection Address
    public static final UUID CHAR_RECONNECTION_ADDRESS =
            UUID.fromString(FormatUtils.uuid16ToUuid128("2A03"));

    //Record Access Control Point
    public static final UUID CHAR_RECORD_ACCESS_CONTROL_POINT =
            UUID.fromString(FormatUtils.uuid16ToUuid128("2A52"));

    //Reference Time Information
    public static final UUID CHAR_REFERENCE_TIME_INFORMATION =
            UUID.fromString(FormatUtils.uuid16ToUuid128("2A14"));

    //Report
    public static final UUID CHAR_REPORT =
            UUID.fromString(FormatUtils.uuid16ToUuid128("2A4D"));

    //Report Map
    public static final UUID CHAR_REPORT_MAP =
            UUID.fromString(FormatUtils.uuid16ToUuid128("2A4B"));

    //Resting Heart Rate
    public static final UUID CHAR_RESTING_HEART_RATE =
            UUID.fromString(FormatUtils.uuid16ToUuid128("2A92"));

    //Ringer Control Point
    public static final UUID CHAR_RINGER_CONTROL_POINT =
            UUID.fromString(FormatUtils.uuid16ToUuid128("2A40"));

    //Ringer Setting
    public static final UUID CHAR_RINGER_SETTING =
            UUID.fromString(FormatUtils.uuid16ToUuid128("2A41"));

    //RSC Feature
    public static final UUID CHAR_RSC_FEATURE =
            UUID.fromString(FormatUtils.uuid16ToUuid128("2A54"));

    //RSC Measurement
    public static final UUID CHAR_RSC_MEASUREMENT =
            UUID.fromString(FormatUtils.uuid16ToUuid128("2A53"));

    //SC Control Point
    public static final UUID CHAR_SC_CONTROL_POINT =
            UUID.fromString(FormatUtils.uuid16ToUuid128("2A55"));

    //Scan Interval Window
    public static final UUID CHAR_SCAN_INTERVAL_WINDOW =
            UUID.fromString(FormatUtils.uuid16ToUuid128("2A4F"));

    //Scan Refresh
    public static final UUID CHAR_SCAN_REFRESH =
            UUID.fromString(FormatUtils.uuid16ToUuid128("2A31"));

    //Sensor Location
    public static final UUID CHAR_SENSOR_LOCATION =
            UUID.fromString(FormatUtils.uuid16ToUuid128("2A5D"));

    //Serial Number String
    public static final UUID CHAR_SERIAL_NUMBER_STRING =
            UUID.fromString(FormatUtils.uuid16ToUuid128("2A25"));

    //Service Changed
    public static final UUID CHAR_SERVICE_CHANGED =
            UUID.fromString(FormatUtils.uuid16ToUuid128("2A05"));

    //Software Revision String
    public static final UUID CHAR_SOFTWARE_REVISION_STRING =
            UUID.fromString(FormatUtils.uuid16ToUuid128("2A28"));

    //Sport Type for Aerobic and Anaerobic Thresholds
    public static final UUID CHAR_SPORT_TYPE_FOR_AEROBIC_AND_ANAEROBIC_THRESHOLDS =
            UUID.fromString(FormatUtils.uuid16ToUuid128("2A93"));

    //Supported New Alert Category
    public static final UUID CHAR_SUPPORTED_NEW_ALERT_CATEGORY =
            UUID.fromString(FormatUtils.uuid16ToUuid128("2A47"));

    //Supported Unread Alert Category
    public static final UUID CHAR_SUPPORTED_UNREAD_ALERT_CATEGORY =
            UUID.fromString(FormatUtils.uuid16ToUuid128("2A48"));

    //System ID
    public static final UUID CHAR_SYSTEM_ID =
            UUID.fromString(FormatUtils.uuid16ToUuid128("2A23"));

    //Temperature
    public static final UUID CHAR_TEMPERATURE =
            UUID.fromString(FormatUtils.uuid16ToUuid128("2A6E"));

    //Temperature Measurement
    public static final UUID CHAR_TEMPERATURE_MEASUREMENT =
            UUID.fromString(FormatUtils.uuid16ToUuid128("2A1C"));

    //Temperature Type
    public static final UUID CHAR_TEMPERATURE_TYPE =
            UUID.fromString(FormatUtils.uuid16ToUuid128("2A1D"));

    //Three Zone Heart Rate Limits
    public static final UUID CHAR_THREE_ZONE_HEART_RATE_LIMITS =
            UUID.fromString(FormatUtils.uuid16ToUuid128("2A94"));

    //Time Accuracy
    public static final UUID CHAR_TIME_ACCURACY =
            UUID.fromString(FormatUtils.uuid16ToUuid128("2A12"));

    //Time Source
    public static final UUID CHAR_TIME_SOURCE =
            UUID.fromString(FormatUtils.uuid16ToUuid128("2A13"));

    //Time Update Control Point
    public static final UUID CHAR_TIME_UPDATE_CONTROL_POINT =
            UUID.fromString(FormatUtils.uuid16ToUuid128("2A16"));

    //Time Update State
    public static final UUID CHAR_TIME_UPDATE_STATE =
            UUID.fromString(FormatUtils.uuid16ToUuid128("2A17"));

    //Time with DST
    public static final UUID CHAR_TIME_WITH_DST =
            UUID.fromString(FormatUtils.uuid16ToUuid128("2A11"));

    //Time Zone
    public static final UUID CHAR_TIME_ZONE =
            UUID.fromString(FormatUtils.uuid16ToUuid128("2A0E"));

    //True Wind Direction
    public static final UUID CHAR_TRUE_WIND_DIRECTION =
            UUID.fromString(FormatUtils.uuid16ToUuid128("2A71"));

    //True Wind Speed
    public static final UUID CHAR_TRUE_WIND_SPEED =
            UUID.fromString(FormatUtils.uuid16ToUuid128("2A70"));

    //Two Zone Heart Rate Limit
    public static final UUID CHAR_TWO_ZONE_HEART_RATE_LIMIT =
            UUID.fromString(FormatUtils.uuid16ToUuid128("2A95"));

    //Tx Power Level
    public static final UUID CHAR_TX_POWER_LEVEL =
            UUID.fromString(FormatUtils.uuid16ToUuid128("2A07"));

    //Uncertainty
    public static final UUID CHAR_UNCERTAINTY =
            UUID.fromString(FormatUtils.uuid16ToUuid128("2AB4"));

    //Unread Alert Status
    public static final UUID CHAR_UNREAD_ALERT_STATUS =
            UUID.fromString(FormatUtils.uuid16ToUuid128("2A45"));

    //User Control Point
    public static final UUID CHAR_USER_CONTROL_POINT =
            UUID.fromString(FormatUtils.uuid16ToUuid128("2A9F"));

    //User Index
    public static final UUID CHAR_USER_INDEX =
            UUID.fromString(FormatUtils.uuid16ToUuid128("2A9A"));

    //UV Index
    public static final UUID CHAR_UV_INDEX =
            UUID.fromString(FormatUtils.uuid16ToUuid128("2A76"));

    //VO2 Max
    public static final UUID CHAR_VO2_MAX =
            UUID.fromString(FormatUtils.uuid16ToUuid128("2A96"));

    //Waist Circumference
    public static final UUID CHAR_WAIST_CIRCUMFERENCE =
            UUID.fromString(FormatUtils.uuid16ToUuid128("2A97"));

    //Weight
    public static final UUID CHAR_WEIGHT =
            UUID.fromString(FormatUtils.uuid16ToUuid128("2A98"));

    //Weight Measurement
    public static final UUID CHAR_WEIGHT_MEASUREMENT =
            UUID.fromString(FormatUtils.uuid16ToUuid128("2A9D"));

    //Weight Scale Feature
    public static final UUID CHAR_WEIGHT_SCALE_FEATURE =
            UUID.fromString(FormatUtils.uuid16ToUuid128("2A9E"));

    //Wind Chill
    public static final UUID CHAR_WIND_CHILL =
            UUID.fromString(FormatUtils.uuid16ToUuid128("2A79"));

    /**
     * Bluetooth GATT Descriptor UUID.
     */

    // Characteristic Extended Properties
    public static final UUID DESCR_CHARACTERISTIC_EXTENDED_PROPERTIES =
            UUID.fromString(FormatUtils.uuid16ToUuid128("2900"));

    // Characteristic User Description
    public static final UUID DESCR_CHARACTERISTIC_USER_DESCRIPTION =
            UUID.fromString(FormatUtils.uuid16ToUuid128("2901"));

    // Client Characteristic Configuration
    public static final UUID DESCR_CLIENT_CHARACTERISTIC_CONFIGURATION =
            UUID.fromString(FormatUtils.uuid16ToUuid128("2902"));

    // Server Characteristic Configuration
    public static final UUID DESCR_SERVER_CHARACTERISTIC_CONFIGURATION =
            UUID.fromString(FormatUtils.uuid16ToUuid128("2903"));

    // Characteristic Presentation Format
    public static final UUID DESCR_CHARACTERISTIC_PRESENTATION_FORMAT =
            UUID.fromString(FormatUtils.uuid16ToUuid128("2904"));

    // Characteristic Aggregate Format
    public static final UUID DESCR_CHARACTERISTIC_AGGREGATE_FORMAT =
            UUID.fromString(FormatUtils.uuid16ToUuid128("2905"));

    // Valid Range
    public static final UUID DESCR_VALID_RANGE =
            UUID.fromString(FormatUtils.uuid16ToUuid128("2906"));

    // External Report Reference
    public static final UUID DESCR_EXTERNAL_REPORT_REFERENCE =
            UUID.fromString(FormatUtils.uuid16ToUuid128("2907"));

    // Report Reference
    public static final UUID DESCR_REPORT_REFERENCE =
            UUID.fromString(FormatUtils.uuid16ToUuid128("2908"));

    // Environmental Sensing Configuration
    public static final UUID DESCR_ES_CONFIGURATION =
            UUID.fromString(FormatUtils.uuid16ToUuid128("290B"));

    // Environmental Sensing Measurement
    public static final UUID DESCR_ES_MEASUREMENT =
            UUID.fromString(FormatUtils.uuid16ToUuid128("290C"));

    // Environmental Sensing Trigger Setting
    public static final UUID DESCR_ES_TRIGGER_SETTING =
            UUID.fromString(FormatUtils.uuid16ToUuid128("290D"));
}
