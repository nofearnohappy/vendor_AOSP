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

LOCAL_SRC_FILES := $(call all-java-files-under, src) \
src/com/android/phone/INetworkQueryService.aidl \
src/com/android/phone/INetworkQueryServiceCallback.aidl

LOCAL_PACKAGE_NAME :=  OP09Plugin
#LOCAL_CERTIFICATE := shared
LOCAL_CERTIFICATE := platform

# link your plug-in interface .jar here

LOCAL_STATIC_JAVA_LIBRARIES := \
    android-support-v13 \
    android-support-v4

LOCAL_JAVA_LIBRARIES += android-common-chips

LOCAL_JAVA_LIBRARIES += mediatek-framework \
                         mediatek-common \
                         telephony-common
LOCAL_JAVA_LIBRARIES += mediatek-telephony-common
LOCAL_JAVA_LIBRARIES += com.mediatek.downloadmanager.ext
LOCAL_JAVA_LIBRARIES += com.mediatek.browser.ext
LOCAL_JAVA_LIBRARIES += com.mediatek.music.ext
LOCAL_JAVA_LIBRARIES += com.mediatek.email.ext
LOCAL_JAVA_LIBRARIES += com.android.emailcommon
LOCAL_JAVA_LIBRARIES += com.mediatek.systemui.ext
LOCAL_JAVA_LIBRARIES += com.mediatek.launcher3.ext
LOCAL_JAVA_LIBRARIES += com.mediatek.settings.ext
#LOCAL_JAVA_LIBRARIES += com.android.phone.shared
LOCAL_JAVA_LIBRARIES += com.mediatek.phone.ext
LOCAL_JAVA_LIBRARIES += com.mediatek.incallui.ext
LOCAL_JAVA_LIBRARIES += com.mediatek.dialer.ext
LOCAL_JAVA_LIBRARIES += com.mediatek.telecom.ext
LOCAL_JAVA_LIBRARIES += com.mediatek.mms.ext
LOCAL_JAVA_LIBRARIES += com.mediatek.mms.service.ext
LOCAL_JAVA_LIBRARIES += com.mediatek.keyguard.ext
LOCAL_JAVA_LIBRARIES += com.mediatek.providers.settings.ext
LOCAL_JAVA_LIBRARIES += org.apache.http.legacy.boot

# Put plugin apk together to specific folder
LOCAL_MODULE_PATH := $(PRODUCT_OUT)/system/plugin

LOCAL_STATIC_JAVA_LIBRARIES += com.android.vcard

LOCAL_APK_LIBRARIES := Contacts
LOCAL_APK_LIBRARIES += MtkMms
LOCAL_APK_LIBRARIES += Settings

LOCAL_PROGUARD_FLAG_FILES := proguard.flags

include $(BUILD_PACKAGE)

# Include plug-in's makefile to automated generate .mpinfo
include vendor/mediatek/proprietary/frameworks/opt/plugin/mplugin.mk

include $(call all-makefiles-under,$(LOCAL_PATH))
