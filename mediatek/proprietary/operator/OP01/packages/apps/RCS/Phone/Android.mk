# Copyright Statement:
#
# This software/firmware and related documentation ("MediaTek Software") are
# protected under relevant copyright laws. The information contained herein
# is confidential and proprietary to MediaTek Inc. and/or its licensors.
# Without the prior written permission of MediaTek inc. and/or its licensors,
# any reproduction, modification, use or disclosure of MediaTek Software,
# and information contained herein, in whole or in part, shall be strictly prohibited.

# MediaTek Inc. (C) 2014. All rights reserved.
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
LOCAL_SRC_FILES := $(call all-java-files-under, src) \
                   $(call all-Iaidl-files-under, src)

LOCAL_RESOURCE_DIR += $(LOCAL_PATH)/res

# link your plug-in interface .jar here
LOCAL_JAVA_LIBRARIES += mediatek-framework
LOCAL_JAVA_LIBRARIES += telephony-common
LOCAL_JAVA_LIBRARIES += ims-common
LOCAL_JAVA_LIBRARIES += com.mediatek.incallui.ext
LOCAL_APK_LIBRARIES := Dialer

#Add libkh.so to apk for sso sdk
LOCAL_MULTILIB := 32
LOCAL_JNI_SHARED_LIBRARIES := libkh
LOCAL_JNI_SHARED_LIBRARIES := libcmcc_haze
LOCAL_JNI_SHARED_LIBRARIES := libcmcc_rusteze

LOCAL_STATIC_JAVA_LIBRARIES := \
    com.mediatek.services.rcs.phone \
    com.android.vcard \
    CaiYinRCSSDK \
    cmcc-enc \
    cmcc-sso \
    cmcc-sso-sdk \
    javabase64 \
    pinyin4j \
    xstream

LOCAL_APK_LIBRARIES += Contacts
LOCAL_PACKAGE_NAME :=  RCSPhone
LOCAL_CERTIFICATE := platform
LOCAL_PRIVILEGED_MODULE := true

# Put plugin apk together to specific folder
LOCAL_MODULE_PATH := $(PRODUCT_OUT)/system/plugin

#LOCAL_PROGUARD_FLAG_FILES := proguard.flags
LOCAL_PROGUARD_ENABLED := disabled
include $(BUILD_PACKAGE)

# Include plug-in's makefile to automated generate .mpinfo
include vendor/mediatek/proprietary/frameworks/opt/plugin/mplugin.mk

include $(CLEAR_VARS)
LOCAL_PREBUILT_STATIC_JAVA_LIBRARIES := CaiYinRCSSDK:../libs/CaiYinRCSSDK_1.0.1.jar \
    cmcc-enc:../libs/cmcc-enc.jar \
    cmcc-sso:../libs/cmcc-sso.jar \
    cmcc-sso-sdk:../libs/cmcc-sso-sdk.jar \
    javabase64:../libs/javabase64-1.3.1.jar \
    pinyin4j:../libs/pinyin4j-2.5.0.jar \
    xstream:../libs/xstream-1.4.7.jar

include $(BUILD_MULTI_PREBUILT)

# This finds and builds the test apk as well, so a single make does both.
include $(call all-makefiles-under,$(LOCAL_PATH))
