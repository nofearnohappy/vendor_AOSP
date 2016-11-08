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

LOCAL_MODULE_TAGS := optional

#LOCAL_SRC_FILES := $(call all-subdir-java-files)
LOCAL_SRC_FILES := $(call all-java-files-under, src)\
			$(call all-java-files-under, ../../../../common/packages/apps/Plugins/src/com/mediatek/contacts/aas/plugin)
LOCAL_SRC_FILES += \
#src/com/mediatek/settings/plugin/IOP03SettingsInterface.aidl \
#
LOCAL_PACKAGE_NAME :=  OP03Plugin
LOCAL_CERTIFICATE := shared
LOCAL_APK_LIBRARIES := Contacts
LOCAL_APK_LIBRARIES += Dialer
LOCAL_APK_LIBRARIES += Email
LOCAL_APK_LIBRARIES += MtkMms

# link your plug-in interface .jar here 
LOCAL_JAVA_LIBRARIES += com.mediatek.mms.ext
LOCAL_JAVA_LIBRARIES += com.mediatek.mms.callback
LOCAL_JAVA_LIBRARIES += com.mediatek.browser.ext
LOCAL_JAVA_LIBRARIES += com.mediatek.providers.settings.ext
LOCAL_JAVA_LIBRARIES += com.mediatek.settings.ext
LOCAL_JAVA_LIBRARIES += com.mediatek.phone.ext
#LOCAL_JAVA_LIBRARIES += com.mediatek.contacts.ext
LOCAL_JAVA_LIBRARIES += com.mediatek.dialer.ext
LOCAL_JAVA_LIBRARIES += com.mediatek.incallui.ext
LOCAL_JAVA_LIBRARIES += com.mediatek.systemui.ext
LOCAL_JAVA_LIBRARIES += com.mediatek.settings.ext 
LOCAL_JAVA_LIBRARIES += mediatek-common
LOCAL_JAVA_LIBRARIES += mediatek-framework
LOCAL_JAVA_LIBRARIES += services
LOCAL_JAVA_LIBRARIES += mediatek-common telephony-common
LOCAL_JAVA_LIBRARIES += com.mediatek.email.ext
LOCAL_JAVA_LIBRARIES += com.android.emailcommon

# Put plugin apk together to specific folder
# Specify install path for MTK CIP solution
ifeq ($(strip $(MTK_CIP_SUPPORT)),yes)
LOCAL_MODULE_PATH := $(TARGET_CUSTOM_OUT)/plugin
else
LOCAL_MODULE_PATH := $(PRODUCT_OUT)/system/plugin
endif

LOCAL_PROGUARD_ENABLED := disabled
LOCAL_JACK_ENABLED := disabled

include $(BUILD_PACKAGE)

# Include plug-in's makefile to automated generate .mpinfo
include vendor/mediatek/proprietary/frameworks/opt/plugin/mplugin.mk

include $(call all-makefiles-under,$(LOCAL_PATH))
