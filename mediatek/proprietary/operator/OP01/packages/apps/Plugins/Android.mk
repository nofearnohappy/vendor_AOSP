# Copyright Statement:
#
# This software/firmware and related documentation ("MediaTek Software") are
# protected under relevant copyright laws. The information contained herein
# is confidential and proprietary to MediaTek Inc. and/or its licensors.
# Without the prior written permission of MediaTek inc. and/or its licensors,
# any reproduction, modification, use or disclosure of MediaTek Software,
# and information contained herein, in whole or in part, shall be strictly prohibited.

# MediaTek Inc. (C) 2012. All rights reserved.
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

LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

define all-java-files-under-no-FM
$(patsubst ./%,%, \
  $(shell cd $(LOCAL_PATH) ; \
          find -L $(1) -name "*.java" -and -not -name ".*" -and -not -name "Op01FavoriteExt.java") \
 )
endef

LOCAL_JACK_ENABLED := disabled
LOCAL_MODULE_TAGS := optional
#LOCAL_SRC_FILES := $(call all-subdir-java-files)

ifeq ($(MTK_FM_SUPPORT), yes)
ifeq ($(MTK_FM_RX_SUPPORT), yes)
LOCAL_SRC_FILES := $(call all-java-files-under, src)
else
LOCAL_SRC_FILES := $(call all-java-files-under-no-FM, src)
endif
else
LOCAL_SRC_FILES := $(call all-java-files-under-no-FM, src)
endif

LOCAL_SRC_FILES += $(call all-java-files-under, ../../../../common/packages/apps/Plugins/src/com/mediatek/gallery3d/plugin)
LOCAL_SRC_FILES += $(call all-java-files-under, ../../../../common/packages/apps/Plugins/src/com/mediatek/calendar/plugin)
# temp unbuild mms plugin files
#MMS_PLUGIN_SRC_FILES := $(call all-java-files-under, src/com/mediatek/mms/plugin/)
#LOCAL_SRC_FILES := $(filter-out $(MMS_PLUGIN_SRC_FILES), $(LOCAL_SRC_FILES))

# temp unbuild systemui plugin files
# SYSTEMUI_PLUGIN_SRC_FILES := $(call all-java-files-under, src/com/mediatek/systemui/plugin/)
# LOCAL_SRC_FILES := $(filter-out $(SYSTEMUI_PLUGIN_SRC_FILES), $(LOCAL_SRC_FILES))

# temp unbuild launcher3 plugin files
LAUNCHER3_PLUGIN_SRC_FILES := $(call all-java-files-under, src/com/mediatek/launcher3/plugin/)
LOCAL_SRC_FILES := $(filter-out $(LAUNCHER3_PLUGIN_SRC_FILES), $(LOCAL_SRC_FILES))

LOCAL_RESOURCE_DIR += $(LOCAL_PATH)/res vendor/mediatek/proprietary/operator/common/packages/apps/Plugins/res
LOCAL_AAPT_FLAGS := --auto-add-overlay --extra-packages com.mediatek.common.plugin

LOCAL_JAVA_LIBRARIES += mediatek-framework telephony-common
LOCAL_STATIC_JAVA_LIBRARIES := com.android.services.telephony.common
LOCAL_STATIC_JAVA_LIBRARIES := com.mediatek.lbs.em2.utils
#LOCAL_STATIC_JAVA_LIBRARIES += android-support-v13
LOCAL_PACKAGE_NAME :=  OP01Plugin
LOCAL_CERTIFICATE := platform
LOCAL_PRIVILEGED_MODULE := true
# link your plug-in interface .jar here
LOCAL_JAVA_LIBRARIES += com.mediatek.camera.ext
LOCAL_JAVA_LIBRARIES += com.mediatek.mms.ext
LOCAL_JAVA_LIBRARIES += com.mediatek.mms.callback
LOCAL_JAVA_LIBRARIES += com.mediatek.mms.service.ext
LOCAL_JAVA_LIBRARIES += com.mediatek.qsb.ext
LOCAL_JAVA_LIBRARIES += org.apache.http.legacy.boot
LOCAL_APK_LIBRARIES += Contacts
LOCAL_APK_LIBRARIES += Gallery2
LOCAL_APK_LIBRARIES += SoundRecorder
LOCAL_APK_LIBRARIES += Settings
LOCAL_APK_LIBRARIES += TeleService
LOCAL_APK_LIBRARIES += Telecom
LOCAL_APK_LIBRARIES += MtkMms
LOCAL_JAVA_LIBRARIES += com.mediatek.dialer.ext
LOCAL_JAVA_LIBRARIES += com.mediatek.phone.ext
LOCAL_JAVA_LIBRARIES += com.mediatek.systemui.ext
LOCAL_JAVA_LIBRARIES += com.mediatek.keyguard.ext
LOCAL_JAVA_LIBRARIES += com.mediatek.browser.ext
LOCAL_JAVA_LIBRARIES += com.mediatek.downloadmanager.ext
LOCAL_JAVA_LIBRARIES += mediatek-framework telephony-common
LOCAL_JAVA_LIBRARIES += com.mediatek.music.ext
LOCAL_JAVA_LIBRARIES += com.mediatek.deskclock.ext
#LOCAL_JAVA_LIBRARIES += com.mediatek.launcher3.ext
ifeq ($(MTK_FM_SUPPORT), yes)
ifeq ($(MTK_FM_RX_SUPPORT), yes)
LOCAL_APK_LIBRARIES += FMRadio
endif
endif

LOCAL_JAVA_LIBRARIES += com.mediatek.providers.settings.ext
LOCAL_JAVA_LIBRARIES += com.mediatek.soundrecorder.ext

LOCAL_JAVA_LIBRARIES += com.mediatek.calendar.ext
LOCAL_APK_LIBRARIES += MtkCalendar

# Put plugin apk together to specific folder
LOCAL_MODULE_PATH := $(PRODUCT_OUT)/system/plugin

LOCAL_PROGUARD_FLAG_FILES := proguard.flags
include $(BUILD_PACKAGE)

# Include plug-in's makefile to automated generate .mpinfo
include vendor/mediatek/proprietary/frameworks/opt/plugin/mplugin.mk

# This finds and builds the test apk as well, so a single make does both.
include $(call all-makefiles-under,$(LOCAL_PATH))
