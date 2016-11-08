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


#
# Copyright (C) 2008 The Android Open Source Project
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

ifeq ($(MTK_DM_APP),yes)

LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := optional

LOCAL_STATIC_JAVA_LIBRARIES := libredbend1 \
          libredbend2 \
          libredbend3 \
          libredbend4 \
          libredbend5 \
          libredbend6 \
          libredbend7 \
          wbxml \

LOCAL_STATIC_JAVA_LIBRARIES += com.mediatek.lbs.em2.utils

LOCAL_SRC_FILES := \
 	$(call all-java-files-under, src) \

LOCAL_PACKAGE_NAME := dm
LOCAL_CERTIFICATE := platform


#Add for EMMA COVERAGE REPORT
LOCAL_EMMA_COVERAGE_FILTER := +com.mediatek.dm.*,--$(LOCAL_PATH)/exclude_methods.emma \
                              -com.mediatek.dm.polling.* \
                              -com.mediatek.dm.session.* \
                              -com.redbend.vdm.* \
                              -com.redbend.vdm.fumo.* \
                              -com.redbend.vdm.scomo.* \
                              -com.redbend.vdm.lawmo.* \
                              @$(LOCAL_PATH)/filter_file.txt 

#End add for EMMA COVERAGE REPORT

LOCAL_PROGUARD_FLAG_FILES :=proguard.cfg
LOCAL_PROGUARD_ENABLED := disabled
LOCAL_PRIVILEGED_MODULE := true

LOCAL_JAVA_LIBRARIES += mediatek-framework \
                        mediatek-common \
                        telephony-common \
                        ims-common

# when both lib exist, we can use this.
#LOCAL_PREBUILT_JNI_LIBS := \
#    lib/$(TARGET_ARCH)/libvdmengine.so \

LOCAL_PREBUILT_JNI_LIBS := \
    lib/arm/libvdmengine.so \
    lib/arm/libvdmfumo.so \
    lib/arm/libvdmlawmo.so \
    lib/arm/libvdmscinv.so \
    lib/arm/libvdmscomo.so

ifeq ($(TARGET_ARCH),arm64)
LOCAL_PREBUILT_JNI_LIBS += \
    lib/arm64/libvdmengine.so \
    lib/arm64/libvdmfumo.so \
    lib/arm64/libvdmlawmo.so \
    lib/arm64/libvdmscinv.so \
    lib/arm64/libvdmscomo.so
endif

LOCAL_MULTILIB := both

include $(BUILD_PACKAGE)

include $(CLEAR_VARS)

LOCAL_PREBUILT_STATIC_JAVA_LIBRARIES := libredbend1:framework/com.redbend.vdm.jar \
          libredbend2:framework/com.redbend.vdm.comm.jar \
          libredbend3:framework/com.redbend.vdm.ddl.jar \
          libredbend4:framework/com.redbend.vdm.fumo.jar \
          libredbend5:framework/com.redbend.vdm.lawmo.jar \
          libredbend6:framework/com.redbend.vdm.scomo.jar  \
          libredbend7:framework/com.redbend.vdm.log.jar

include $(BUILD_MULTI_PREBUILT)

include $(CLEAR_VARS)

include $(call all-makefiles-under,$(LOCAL_PATH))

endif
