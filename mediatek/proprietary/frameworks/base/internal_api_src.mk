# Copyright Statement:
#
# This software/firmware and related documentation ("MediaTek Software") are
# protected under relevant copyright laws. The information contained herein
# is confidential and proprietary to MediaTek Inc. and/or its licensors.
# Without the prior written permission of MediaTek inc. and/or its licensors,
# any reproduction, modification, use or disclosure of MediaTek Software,
# and information contained herein, in whole or in part, shall be strictly prohibited.
#
# MediaTek Inc. (C) 2011. All rights reserved.
#
# BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
# THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
# RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER ON
# AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL WARRANTIES,
# EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF
# MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR NONINFRINGEMENT.
# NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH RESPECT TO THE
# SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY, INCORPORATED IN, OR
# SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES TO LOOK ONLY TO SUCH
# THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO. RECEIVER EXPRESSLY ACKNOWLEDGES
# THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES
# CONTAINED IN MEDIATEK SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK
# SOFTWARE RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
# STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S ENTIRE AND
# CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE RELEASED HEREUNDER WILL BE,
# AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE MEDIATEK SOFTWARE AT ISSUE,
# OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE CHARGE PAID BY RECEIVER TO
# MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
#
# The following software/firmware and/or related documentation ("MediaTek Software")
# have been modified by MediaTek Inc. All revisions are subject to any receiver's
# applicable license agreements with MediaTek Inc.

internal_docs_FRAMEWORK_AIDL_JAVA_DIR := $(call intermediates-dir-for,JAVA_LIBRARIES,framework,,COMMON)/src
internal_docs_TEL_COMMON_AIDL_JAVA_DIR := $(call intermediates-dir-for,JAVA_LIBRARIES,telephony-common,,COMMON)/src
internal_docs_MTK_COMMON_AIDL_JAVA_DIR := $(call intermediates-dir-for,JAVA_LIBRARIES,mediatek-common,,COMMON)/src

INTERNAL_API_SRC_FILES := \
../../../../../frameworks/base/core/java/android/app/admin/DevicePolicyManager.java \
../../../../../frameworks/base/core/java/android/app/AlarmManager.java \
../../../../../frameworks/base/core/java/android/app/DownloadManager.java \
../../../../../frameworks/base/core/java/android/app/IActivityManager.java \
../../../../../frameworks/base/core/java/android/app/Notification.java \
../../../../../frameworks/base/core/java/android/app/StatusBarManager.java \
../../../../../frameworks/base/core/java/android/bluetooth/BluetoothClass.java \
../../../../../frameworks/base/core/java/android/content/Context.java \
../../../../../frameworks/base/core/java/android/content/Intent.java \
../../../../../frameworks/base/core/java/android/database/DatabaseUtils.java \
../../../../../frameworks/base/core/java/android/hardware/Camera.java \
../../../../../frameworks/base/core/java/android/hardware/usb/UsbManager.java \
../../../../../frameworks/base/core/java/android/net/ConnectivityManager.java \
../../../../../frameworks/base/core/java/android/nfc/NfcAdapter.java \
../../../../../frameworks/base/core/java/android/os/BatteryManager.java \
../../../../../frameworks/base/core/java/android/os/Environment.java \
../../../../../frameworks/base/core/java/android/os/MessageMonitorLogger.java \
../../../../../frameworks/base/core/java/android/os/MessageQueue.java \
../../../../../frameworks/base/core/java/android/os/PowerManager.java \
../../../../../frameworks/base/core/java/android/os/Process.java \
../../../../../frameworks/base/core/java/android/provider/CallLog.java \
../../../../../frameworks/base/core/java/android/provider/ContactsContract.java \
../../../../../frameworks/base/core/java/android/provider/Downloads.java \
../../../../../frameworks/base/core/java/android/provider/MediaStore.java \
../../../../../frameworks/base/core/java/android/util/Patterns.java \
../../../../../frameworks/base/core/java/android/webkit/SavePageClient.java \
../../../../../frameworks/base/core/java/android/webkit/WebSettings.java \
../../../../../frameworks/base/core/java/android/webkit/WebView.java \
../../../../../frameworks/base/core/java/android/widget/ListView.java \
../../../../../frameworks/base/core/java/android/widget/Spinner.java \
../../../../../frameworks/base/core/java/com/android/internal/widget/LockPatternUtils.java \
../../../../../frameworks/base/core/java/com/mediatek/dcfDecoder/java/com/mediatek/dcfdecoder/DcfDecoder.java \
../../../../../frameworks/base/core/java/com/mediatek/geocoding/GeoCodingQuery.java \
../../../../../frameworks/base/core/java/com/mediatek/gifdecoder/GifDecoder.java \
../../../../../frameworks/base/core/java/com/mediatek/hdmi/HdmiDef.java \
../../../../../frameworks/base/core/java/com/mediatek/hotknot/HotKnotAdapter.java \
../../../../../frameworks/base/core/java/com/mediatek/search/SearchEngineManager.java \
../../../../../frameworks/base/core/java/com/mediatek/storage/StorageManagerEx.java \
../../../../../frameworks/base/graphics/java/android/graphics/BitmapFactory.java \
../../../../../frameworks/base/keystore/java/android/security/Credentials.java \
../../../../../frameworks/base/media/java/android/media/MediaPlayer.java \
../../../../../frameworks/base/media/java/android/media/MediaRecorder.java \
../../../../../frameworks/base/media/java/android/media/MiniThumbFile.java \
../../../../../frameworks/base/media/java/android/media/RingtoneManager.java \
../../../../../frameworks/base/media/java/android/mtp/MtpServer.java \
../../../../../frameworks/base/media/java/android/mtp/MtpStorage.java \
../../../../../frameworks/base/media/java/com/mediatek/audioprofile/AudioProfileManager.java \
../../../../../frameworks/base/services/core/java/com/android/server/NetworkManagementService.java \
../../../../../frameworks/base/services/core/java/com/mediatek/search/SearchEngineManagerService.java \
../../../../../frameworks/base/telecomm/java/android/telecom/Call.java \
../../../../../frameworks/base/telecomm/java/android/telecom/Connection.java \
../../../../../frameworks/base/telecomm/java/android/telecom/PhoneAccount.java \
../../../../../frameworks/base/telephony/java/android/telephony/PhoneNumberUtils.java \
../../../../../frameworks/base/telephony/java/android/telephony/SignalStrength.java \
../../../../../frameworks/base/telephony/java/com/android/internal/telephony/IccCardConstants.java \
../../../../../frameworks/base/telephony/java/com/android/internal/telephony/PhoneConstants.java \
../../../../../frameworks/base/telephony/java/com/android/internal/telephony/TelephonyIntents.java \
../../../../../frameworks/base/telephony/java/com/mediatek/internal/telephony/BtSimapOperResponse.java \
../../../../../frameworks/base/telephony/java/com/mediatek/internal/telephony/IccSmsStorageStatus.java \
../../../../../frameworks/base/wifi/java/android/net/wifi/HotspotClient.java \
../../../../../frameworks/base/wifi/java/android/net/wifi/p2p/fastconnect/WifiP2pFastConnectInfo.java \
../../../../../frameworks/base/wifi/java/android/net/wifi/p2p/link/WifiP2pLinkInfo.java \
../../../../../frameworks/base/wifi/java/android/net/wifi/p2p/WifiP2pDevice.java \
../../../../../frameworks/base/wifi/java/android/net/wifi/p2p/WifiP2pGroup.java \
../../../../../frameworks/base/wifi/java/android/net/wifi/p2p/WifiP2pManager.java \
../../../../../frameworks/base/wifi/java/android/net/wifi/WifiConfiguration.java \
../../../../../frameworks/base/wifi/java/android/net/wifi/WifiEnterpriseConfig.java \
../../../../../frameworks/base/wifi/java/android/net/wifi/WifiManager.java \
../../../../../frameworks/base/wifi/java/android/net/wifi/WpsInfo.java \
../../../../../frameworks/opt/telephony/src/java/android/provider/Telephony.java \
../../../../../frameworks/opt/telephony/src/java/android/telephony/SmsManager.java \
../../../../../frameworks/opt/telephony/src/java/android/telephony/SmsMemoryStatus.java \
../../../../../frameworks/opt/telephony/src/java/android/telephony/SmsMessage.java \
../../../../../frameworks/opt/telephony/src/java/com/android/internal/telephony/cat/AppInterface.java \
../../../../../frameworks/opt/telephony/src/java/com/android/internal/telephony/cat/bip/BearerDesc.java \
../../../../../frameworks/opt/telephony/src/java/com/android/internal/telephony/cat/bip/BipCmdMessage.java \
../../../../../frameworks/opt/telephony/src/java/com/android/internal/telephony/cat/bip/DefaultBearerDesc.java \
../../../../../frameworks/opt/telephony/src/java/com/android/internal/telephony/cat/CatCmdMessage.java \
../../../../../frameworks/opt/telephony/src/java/com/android/internal/telephony/cat/CatLog.java \
../../../../../frameworks/opt/telephony/src/java/com/android/internal/telephony/cat/CatResponseMessage.java \
../../../../../frameworks/opt/telephony/src/java/com/android/internal/telephony/cat/CatService.java \
../../../../../frameworks/opt/telephony/src/java/com/android/internal/telephony/cat/Input.java \
../../../../../frameworks/opt/telephony/src/java/com/android/internal/telephony/cat/Menu.java \
../../../../../frameworks/opt/telephony/src/java/com/android/internal/telephony/gsm/GSMPhone.java \
../../../../../frameworks/opt/telephony/src/java/com/android/internal/telephony/gsm/UsimPhoneBookManager.java \
../../../../../frameworks/opt/telephony/src/java/com/android/internal/telephony/IccCard.java \
../../../../../frameworks/opt/telephony/src/java/com/android/internal/telephony/IccProvider.java \
../../../../../frameworks/opt/telephony/src/java/com/android/internal/telephony/Phone.java \
../../../../../frameworks/opt/telephony/src/java/com/android/internal/telephony/uicc/UiccController.java \
../../../../../frameworks/opt/telephony/src/java/com/google/android/mms/pdu/PduPersister.java \
../../../../../frameworks/opt/telephony/src/java/com/mediatek/internal/telephony/FemtoCellInfo.java \
../../../../../frameworks/opt/telephony/src/java/com/mediatek/internal/telephony/ModemSwitchHandler.java \
../../../../../frameworks/opt/telephony/src/java/com/mediatek/internal/telephony/NetworkInfoWithAcT.java \
../../../../../frameworks/opt/telephony/src/java/com/mediatek/internal/telephony/NetworkManager.java \
../../../../../frameworks/opt/telephony/src/java/com/mediatek/internal/telephony/RadioManager.java \
../../../../../frameworks/opt/telephony/src/java/com/mediatek/internal/telephony/uicc/AlphaTag.java \
../../../../../frameworks/opt/telephony/src/java/com/mediatek/internal/telephony/uicc/UsimGroup.java \
../../../../../libcore/luni/src/main/java/java/net/Socket.java \
../../../../../vendor/mediatek/proprietary/frameworks/base/bluetooth/java/com/mediatek/bluetooth/BleAlertNotificationProfileService.java \
../../../../../vendor/mediatek/proprietary/frameworks/base/bluetooth/java/com/mediatek/bluetooth/BleDeviceManager.java \
../../../../../vendor/mediatek/proprietary/frameworks/base/bluetooth/java/com/mediatek/bluetooth/BleFindMeProfile.java \
../../../../../vendor/mediatek/proprietary/frameworks/base/bluetooth/java/com/mediatek/bluetooth/BleGattDevice.java \
../../../../../vendor/mediatek/proprietary/frameworks/base/bluetooth/java/com/mediatek/bluetooth/BleGattUuid.java \
../../../../../vendor/mediatek/proprietary/frameworks/base/bluetooth/java/com/mediatek/bluetooth/BleManager.java \
../../../../../vendor/mediatek/proprietary/frameworks/base/bluetooth/java/com/mediatek/bluetooth/BleProfile.java \
../../../../../vendor/mediatek/proprietary/frameworks/base/bluetooth/java/com/mediatek/bluetooth/BleProfileServiceManager.java \
../../../../../vendor/mediatek/proprietary/frameworks/base/bluetooth/java/com/mediatek/bluetooth/BleProximityProfile.java \
../../../../../vendor/mediatek/proprietary/frameworks/base/bluetooth/java/com/mediatek/bluetooth/BleProximityProfileService.java \
../../../../../vendor/mediatek/proprietary/frameworks/base/bluetooth/java/com/mediatek/bluetooth/BluetoothDevicePickerEx.java \
../../../../../vendor/mediatek/proprietary/frameworks/base/bluetooth/java/com/mediatek/bluetooth/BluetoothUuidEx.java \
../../../../../vendor/mediatek/proprietary/frameworks/base/bluetooth/java/com/mediatek/bluetooth/parcel/ParcelBluetoothGattCharacteristic.java \
../../../../../vendor/mediatek/proprietary/frameworks/base/bluetooth/java/com/mediatek/bluetooth/parcel/ParcelBluetoothGattDescriptor.java \
../../../../../vendor/mediatek/proprietary/frameworks/base/bluetooth/java/com/mediatek/bluetooth/parcel/ParcelBluetoothGattService.java \
../../../../../vendor/mediatek/proprietary/frameworks/base/custom/java/com/mediatek/custom/CustomProperties.java \
../../../../../vendor/mediatek/proprietary/frameworks/base/drm/java/com/mediatek/drm/OmaDrmClient.java \
../../../../../vendor/mediatek/proprietary/frameworks/base/drm/java/com/mediatek/drm/OmaDrmStore.java \
../../../../../vendor/mediatek/proprietary/frameworks/base/drm/java/com/mediatek/drm/OmaDrmUiUtils.java \
../../../../../vendor/mediatek/proprietary/frameworks/base/drm/java/com/mediatek/drm/OmaDrmUtils.java \
../../../../../vendor/mediatek/proprietary/frameworks/base/media/camcorder/java/com/mediatek/camcorder/CamcorderProfileEx.java \
../../../../../vendor/mediatek/proprietary/frameworks/base/media/java/com/mediatek/media/MediaRecorderEx.java \
../../../../../vendor/mediatek/proprietary/frameworks/base/media/matrixeffect/java/com/mediatek/matrixeffect/MatrixEffect.java \
../../../../../vendor/mediatek/proprietary/frameworks/base/media/pq/java/com/mediatek/pq/PictureQuality.java \
../../../../../vendor/mediatek/proprietary/frameworks/base/telephony/java/com/mediatek/telephony/TelephonyManagerEx.java \
../../../../../vendor/mediatek/proprietary/frameworks/base/text/java/com/mediatek/text/style/BackgroundImageSpan.java \
../../../../../vendor/mediatek/proprietary/frameworks/base/widget/java/com/mediatek/widget/AccountItemView.java \
../../../../../vendor/mediatek/proprietary/frameworks/base/widget/java/com/mediatek/widget/AccountViewAdapter.java \
../../../../../vendor/mediatek/proprietary/frameworks/base/widget/java/com/mediatek/widget/ImageViewEx.java \
../../../../../vendor/mediatek/proprietary/frameworks/common/src/com/mediatek/common/search/SearchEngine.java \
../../../../../vendor/mediatek/proprietary/frameworks/common/src/com/mediatek/common/sms/IConcatenatedSmsFwkExt.java \
../../../../../vendor/mediatek/proprietary/frameworks/common/src/com/mediatek/common/sms/TimerRecord.java \
../../../../../vendor/mediatek/proprietary/frameworks/common/src/com/mediatek/common/telephony/AlphaTag.java \
../../../../../vendor/mediatek/proprietary/frameworks/common/src/com/mediatek/common/telephony/ITelephonyExt.java \
../../../../../vendor/mediatek/proprietary/frameworks/common/src/com/mediatek/common/telephony/UsimGroup.java \
../../../../../vendor/mediatek/proprietary/frameworks/common/src/com/mediatek/common/voicecommand/VoiceCommandListener.java

# Include additional files to avoid error logs.
INTERNAL_API_SRC_FILES += \
    ../../../../../frameworks/base/core/java/android/content/pm/PackageManager.java

# Specify directory of intermediate source files (e.g. AIDL) here.
INTERNAL_API_ADDITIONAL_SRC_DIR := \
#    $(internal_docs_TEL_COMMON_AIDL_JAVA_DIR)/src/java/com/android/internal/telephony \
#    $(internal_docs_MTK_COMMON_AIDL_JAVA_DIR)/src/com/mediatek/common/dm
