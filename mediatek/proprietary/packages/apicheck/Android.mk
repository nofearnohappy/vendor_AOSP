# Copyright Statement:
#
# This software/firmware and related documentation ("MediaTek Software") are
# protected under relevant copyright laws. The information contained herein
# is confidential and proprietary to MediaTek Inc. and/or its licensors.
# Without the prior written permission of MediaTek inc. and/or its licensors,
# any reproduction, modification, use or disclosure of MediaTek Software,
# and information contained herein, in whole or in part, shall be strictly prohibited.
#
# MediaTek Inc. (C) 2010. All rights reserved.
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

LOCAL_PATH := $(call my-dir)

MTK_PLUGIN_MONITORING_API_FILE := $(TARGET_OUT_COMMON_INTERMEDIATES)/PACKAGING/mediatek_plugin_api.txt

PLUGIN_API_DIR := packages/apps/Contacts/ext
PLUGIN_API_DIR += packages/apps/ContactsCommon/ext
PLUGIN_API_DIR += packages/apps/DeskClock/ext
PLUGIN_API_DIR += packages/apps/Dialer/ext
PLUGIN_API_DIR += packages/apps/Email/ext
PLUGIN_API_DIR += packages/apps/Gallery2/ext
PLUGIN_API_DIR += packages/apps/InCallUI/ext
PLUGIN_API_DIR += packages/apps/Launcher3/ext
PLUGIN_API_DIR += packages/apps/Music/ext
PLUGIN_API_DIR += packages/apps/Settings/ext
PLUGIN_API_DIR += packages/apps/SoundRecorder/ext

PLUGIN_API_DIR += packages/providers/DownloadProvider/ext

PLUGIN_API_DIR += packages/services/Mms/ext
PLUGIN_API_DIR += packages/services/Telecomm/ext
PLUGIN_API_DIR += packages/services/Telephony/ext

PLUGIN_API_DIR += frameworks/base/packages/SystemUI/ext
PLUGIN_API_DIR += frameworks/base/packages/Keyguard/ext
PLUGIN_API_DIR += frameworks/base/packages/SettingsLib/ext
PLUGIN_API_DIR += frameworks/base/packages/SettingsProvider/ext

PLUGIN_API_DIR += vendor/mediatek/proprietary/packages/apps/Camera/ext
PLUGIN_API_DIR += vendor/mediatek/proprietary/packages/apps/CMASReceiver/ext
PLUGIN_API_DIR += vendor/mediatek/proprietary/packages/apps/FileManager/ext
PLUGIN_API_DIR += vendor/mediatek/proprietary/packages/apps/Browser/ext
PLUGIN_API_DIR += vendor/mediatek/proprietary/packages/apps/Calendar/ext
PLUGIN_API_DIR += vendor/mediatek/proprietary/packages/apps/Mms/ext
PLUGIN_API_DIR += vendor/mediatek/proprietary/packages/apps/QuickSearchBox/ext
PLUGIN_API_DIR += vendor/mediatek/proprietary/packages/apps/RegionalPhoneManager/ext
PLUGIN_API_DIR += vendor/mediatek/proprietary/packages/apps/Stk/ext

# Get Plug-in API source file list
# ============================================================
# all plug-in API are under eg.*/packages/apps/*/ext/src/.*/I*.java
MTK_PLUGIN_API_SRC_FILE := $(wildcard $(shell find $(PLUGIN_API_DIR) | grep "/I\w*.java$$" | grep "/ext/src"))
MTK_PLUGIN_API_SRC_FILE := $(addprefix ../../../../../, $(MTK_PLUGIN_API_SRC_FILE))

# MediaTek internal API table.
# ============================================================
include $(CLEAR_VARS)

LOCAL_SRC_FILES := $(MTK_PLUGIN_API_SRC_FILE)

LOCAL_JAVA_LIBRARIES := mediatek-framework
LOCAL_JAVA_LIBRARIES += mediatek-common
LOCAL_JAVA_LIBRARIES += telephony-common
LOCAL_JAVA_LIBRARIES += voip-common
LOCAL_JAVA_LIBRARIES += ims-common
LOCAL_JAVA_LIBRARIES += okhttp
LOCAL_JAVA_LIBRARIES += guava
LOCAL_JAVA_LIBRARIES += android-support-v13
LOCAL_JAVA_LIBRARIES += android-opt-datetimepicker

APK_LIBS := Camera
APK_LIBS += Contacts
APK_LIBS += com.android.contacts.common
APK_LIBS += CMASReceiver
APK_LIBS += DeskClock
APK_LIBS += Dialer
APK_LIBS += DownloadProvider
APK_LIBS += DownloadProviderUi
APK_LIBS += Email
APK_LIBS += FileManager
APK_LIBS += Gallery2
APK_LIBS += Launcher3
APK_LIBS += MmsService
APK_LIBS += MtkBrowser
APK_LIBS += MtkCalendar
APK_LIBS += MtkMms
APK_LIBS += MtkQuickSearchBox
APK_LIBS += Music
APK_LIBS += RegionalPhoneManager
APK_LIBS += Settings
APK_LIBS += SettingsProvider
APK_LIBS += SoundRecorder
APK_LIBS += Stk1
APK_LIBS += SystemUI
APK_LIBS += Telecom
APK_LIBS += TeleService

JAVA_LIBS += Keyguard
JAVA_LIBS += SettingsLib


# This is set by packages that are linking to other packages that export
# shared libraries, allowing them to make use of the code in the linked apk.
LOCAL_CLASSPATH := \
$(foreach lib, $(APK_LIBS), \
$(call addifexists, $(lib), APPS))

LOCAL_JAVA_LIBRARIES += \
$(foreach lib, $(JAVA_LIBS), \
$(call addifexists, $(lib), JAVA_LIBRARIES))

# Some modules are not in product packages, so only add classes if available.
define addifexists
	$(eval tmp := $(call intermediates-dir-for, $(2), $(1),, COMMON)/classes.jar) \
	$(if $(wildcard $(tmp)),$(tmp),)
endef

LOCAL_MODULE := mediatek-plugin-api-stubs
LOCAL_MODULE_CLASS := JAVA_LIBRARIES

MTK_PLUGIN_API_STUB_LOG_FILE := $(call local-intermediates-dir, COMMON)/mediatek_plugin_api.log

# NOTE: here we add re-direct options in the droiddoc options to avoid error infromation
# being printed, which is useless.
LOCAL_DROIDDOC_OPTIONS:= \
	-api $(MTK_PLUGIN_MONITORING_API_FILE) \
	-nodocs \
	-internal \
	-warning 106 -warning 110 > $(MTK_PLUGIN_API_STUB_LOG_FILE) 2>&1

include $(BUILD_DROIDDOC)

$(MTK_PLUGIN_MONITORING_API_FILE): $(full_target)

# MediaTek internal API document.
# ============================================================
LOCAL_MODULE := mediatek-plugin-sdk

MTK_PLUGIN_API_SDK_LOG_FILE := $(call local-intermediates-dir, COMMON)/mediatek_plugin_api.log

# NOTE: here we add re-direct options in the droiddoc options to avoid error infromation
# being printed, which is useless.
LOCAL_DROIDDOC_OPTIONS := \
	-title "MediaTek Plugin API Document" \
	-offlinemode \
	-hdf android.whichdoc online \
	-internal \
	-warning 106 -warning 110 > $(MTK_PLUGIN_API_SDK_LOG_FILE) 2>&1

include $(BUILD_DROIDDOC)
