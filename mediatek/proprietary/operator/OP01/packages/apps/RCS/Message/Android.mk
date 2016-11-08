# Copyright Statement:
#
# This software/firmware and related documentation ("MediaTek Software") are
# protected under relevant copyright laws. The information contained herein
# is confidential and proprietary to MediaTek Inc. and/or its licensors.
# Without the prior written permission of MediaTek inc. and/or its licensors,
# any reproduction, modification, use or disclosure of MediaTek Software,
# and information contained herein, in whole or in part, shall be strictly prohibited.

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
#
# The following software/firmware and/or related documentation ("MediaTek Software")
# have been modified by MediaTek Inc. All revisions are subject to any receiver's
# applicable license agreements with MediaTek Inc.


# Copyright 2007-2008 The Android Open Source Project
ifeq ($(strip $(MTK_RCS_SUPPORT)),yes)
LOCAL_PATH := $(call my-dir)
##########################################################################
# Build the RCS message apk: Op01RCSMessagePlugin.apk
##########################################################################

#LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)
chips_dir := ../../../../../../../../../frameworks/ex/chips/res
res_dirs := $(chips_dir) res
$(shell rm -f $(LOCAL_PATH)/chips)

LOCAL_MODULE_TAGS := optional

LOCAL_SRC_FILES := $(call all-java-files-under, src)
LOCAL_SRC_FILES += src/com/mediatek/rcs/message/aidl/IMessageSender.aidl
#LOCAL_SRC_FILES += $(call all-java-files-under, src/org) $(call all-java-files-under, src/com/ibm)

LOCAL_PACKAGE_NAME := Op01RCSMessagePlugin

#LOCAL_CERTIFICATE := platform
LOCAL_JAVA_LIBRARIES += com.mediatek.mms.ext
LOCAL_JAVA_LIBRARIES += mediatek-telephony-common
LOCAL_JAVA_LIBRARIES += mediatek-framework
LOCAL_JAVA_LIBRARIES += telephony-common
LOCAL_JAVA_LIBRARIES += android-support-v4
LOCAL_JAVA_LIBRARIES += ims-common
LOCAL_JAVA_LIBRARIES += com.cmcc.ccs
LOCAL_JAVA_LIBRARIES += rcsmessage-common

LOCAL_AAPT_FLAGS := --auto-add-overlay
LOCAL_AAPT_FLAGS += --extra-packages com.android.mtkex.chips

LOCAL_STATIC_JAVA_LIBRARIES += android-common-chips
LOCAL_STATIC_JAVA_LIBRARIES += com.mediatek.services.rcs.phone
LOCAL_STATIC_JAVA_LIBRARIES += cloudbackup-protocol

LOCAL_APK_LIBRARIES += MtkMms

LOCAL_RESOURCE_DIR := $(addprefix $(LOCAL_PATH)/, $(res_dirs))
# Put plugin apk together to specific folder
LOCAL_MODULE_PATH := $(PRODUCT_OUT)/system/plugin/

LOCAL_PROGUARD_ENABLED := disabled
#LOCAL_PROGUARD_FLAG_FILES := proguard.flags

LOCAL_PRIVILEGED_MODULE := true

include $(BUILD_PACKAGE)

# This finds and builds the test apk as well, so a single make does both.

# Include plug-in's makefile to automated generate .mpinfo
include vendor/mediatek/proprietary/frameworks/opt/plugin/mplugin.mk

include $(CLEAR_VARS)
#LOCAL_PREBUILT_STATIC_JAVA_LIBRARIES := cmccsdk:libs/cmcc-sso-sdk.jar
include $(BUILD_MULTI_PREBUILT)


include $(CLEAR_VARS)
LOCAL_MODULE_TAGS := optional
LOCAL_SRC_FILES := $(call all-java-files-under, protocol_src)
LOCAL_MODULE := cloudbackup-protocol
include $(BUILD_STATIC_JAVA_LIBRARY)

include $(call all-makefiles-under,$(LOCAL_PATH))

endif