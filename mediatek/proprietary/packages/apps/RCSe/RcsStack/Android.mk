# Copyright Statement:
#
# This software/firmware and related documentation ("MediaTek Software") are
# protected under relevant copyright laws. The information contained herein
# is confidential and proprietary to MediaTek Inc. and/or its licensors.
# Without the prior written permission of MediaTek inc. and/or its licensors,
# any reproduction, modification, use or disclosure of MediaTek Software,
# and information contained herein, in whole or in part, shall be strictly prohibited.

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

# Software Name : RCS IMS Stack
#
# Copyright (C) 2010 France Telecom S.A.
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

LOCAL_PATH := $(call my-dir)

##########################################################################
# Build the RCS Sdk : joyn-api.jar
##########################################################################

#include $(CLEAR_VARS)
#
## This is the target being built. (Name of Jar)
#LOCAL_MODULE := joyn-api
#
## Only compile source java files in this jar.
#LOCAL_SRC_FILES := \
#    $(call all-java-files-under, src/org)
#
## Add AIDL files (the parcelable must not be added in SRC_FILES, but included in LOCAL_AIDL_INCLUDES)
#LOCAL_SRC_FILES += \
#    src/org/gsma/joyn/IJoynServiceRegistrationListener.aidl\
#    src/org/gsma/joyn/capability/ICapabilitiesListener.aidl\
#    src/org/gsma/joyn/capability/ICapabilityService.aidl\
#    src/org/gsma/joyn/chat/IChat.aidl\
#    src/org/gsma/joyn/chat/IChatListener.aidl\
#    src/org/gsma/joyn/chat/IChatService.aidl\
#    src/org/gsma/joyn/chat/IGroupChatListener.aidl\
#    src/org/gsma/joyn/chat/INewChatListener.aidl\
#    src/org/gsma/joyn/chat/IGroupChat.aidl\
#    src/org/gsma/joyn/chat/ISpamReportListener.aidl\
#    src/org/gsma/joyn/gsh/IGeolocSharingListener.aidl\
#    src/org/gsma/joyn/gsh/INewGeolocSharingListener.aidl\
#    src/org/gsma/joyn/gsh/IGeolocSharing.aidl\
#    src/org/gsma/joyn/gsh/IGeolocSharingService.aidl\
#    src/org/gsma/joyn/ipcall/IIPCall.aidl\
#    src/org/gsma/joyn/ipcall/IIPCallPlayer.aidl\
#    src/org/gsma/joyn/ipcall/IIPCallRenderer.aidl\
#    src/org/gsma/joyn/ipcall/IIPCallListener.aidl\
#    src/org/gsma/joyn/ipcall/IIPCallPlayerListener.aidl\
#    src/org/gsma/joyn/ipcall/IIPCallRendererListener.aidl\
#    src/org/gsma/joyn/ipcall/IIPCallService.aidl\
#    src/org/gsma/joyn/ipcall/INewIPCallListener.aidl\
#    src/org/gsma/joyn/ish/IImageSharing.aidl\
#    src/org/gsma/joyn/ish/IImageSharingListener.aidl\
#    src/org/gsma/joyn/ish/IImageSharingService.aidl\
#    src/org/gsma/joyn/ish/INewImageSharingListener.aidl\
#    src/org/gsma/joyn/vsh/INewVideoSharingListener.aidl\
#    src/org/gsma/joyn/vsh/IVideoSharingListener.aidl\
#    src/org/gsma/joyn/vsh/IVideoPlayer.aidl\
#    src/org/gsma/joyn/vsh/IVideoPlayerListener.aidl\
#    src/org/gsma/joyn/vsh/IVideoRenderer.aidl\
#    src/org/gsma/joyn/vsh/IVideoRendererListener.aidl\
#    src/org/gsma/joyn/vsh/IVideoSharing.aidl\
#    src/org/gsma/joyn/vsh/IVideoSharingService.aidl\
#    src/org/gsma/joyn/session/IMultimediaSession.aidl\
#    src/org/gsma/joyn/session/IMultimediaSessionListener.aidl\
#    src/org/gsma/joyn/session/IMultimediaSessionService.aidl\
#    src/org/gsma/joyn/ft/IFileTransfer.aidl\
#    src/org/gsma/joyn/ft/IFileTransferService.aidl\
#    src/org/gsma/joyn/ft/IFileTransferListener.aidl\
#    src/org/gsma/joyn/ft/INewFileTransferListener.aidl\
#    src/org/gsma/joyn/ft/IFileSpamReportListener.aidl\
#    src/org/gsma/joyn/contacts/IContactsService.aidl\
#    src/org/gsma/joyn/ICoreServiceWrapper.aidl \    
#
#
##Specify install path for MTK CIP solution
#ifeq ($(strip $(MTK_CIP_SUPPORT)),yes)
#LOCAL_MODULE_PATH := $(TARGET_CUSTOM_OUT)/framework
#endif
#
#LOCAL_PROGUARD_ENABLED := disabled
#
## Tell it to build an Jar
#include $(BUILD_JAVA_LIBRARY)
#
##For make files
#include $(CLEAR_VARS)

##########################################################################
# Build the RCS Sdk : RcsStack.apk
##########################################################################

include $(CLEAR_VARS)

# This is the target being built. (Name of APK)
LOCAL_PACKAGE_NAME := RcsStack
LOCAL_MODULE_TAGS := optional



LOCAL_CERTIFICATE := platform


LOCAL_JAVA_LIBRARIES += \
	bouncycastle \
	mediatek-framework\
	ims-common\
	com.mediatek.settings.ext\
	org.apache.http.legacy.boot

LOCAL_JAVA_LIBRARIES += telephony-common

# Only compile source java files in this apk.
LOCAL_SRC_FILES := \
    $(call all-java-files-under, src/com/orangelabs)\
    $(call all-java-files-under, src/gov2)\
    $(call all-java-files-under, src/javax2)\
    $(call all-java-files-under, src/org)

# Add AIDL files (the parcelable must not be added in SRC_FILES, but included in LOCAL_AIDL_INCLUDES)
LOCAL_SRC_FILES += \
    src/com/orangelabs/rcs/plugin/apn/IRcseOnlyApnStatus.aidl\
    src/com/orangelabs/rcs/core/ims/network/INetworkConnectivity.aidl \
    src/com/orangelabs/rcs/core/ims/network/INetworkConnectivityApi.aidl \
    src/com/orangelabs/rcs/service/api/client/terms/ITermsApi.aidl \
    src/org/gsma/joyn/ICoreServiceWrapper.aidl \
    
    

# FRAMEWORKS_BASE_JAVA_SRC_DIRS comes from build/core/pathmap.mk
LOCAL_AIDL_INCLUDES += $(FRAMEWORKS_BASE_JAVA_SRC_DIRS)
LOCAL_AIDL_INCLUDES += \
    org/gsma/joyn/capability/Capabilities.aidl \
    org/gsma/joyn/chat/ChatMessage.aidl \
    org/gsma/joyn/chat/ConferenceUser.aidl\

#Added for JPE begin
LOCAL_JAVASSIST_ENABLED := true
LOCAL_JAVASSIST_OPTIONS := $(LOCAL_PATH)/jpe.config
#Added for JPE end

# Add classes used by reflection
# LOCAL_PROGUARD_FLAG_FILES := proguard.cfg
LOCAL_PROGUARD_ENABLED := disabled

#Specify install path for MTK CIP solution
ifeq ($(strip $(MTK_CIP_SUPPORT)),yes)
LOCAL_MODULE_PATH := $(TARGET_CUSTOM_OUT)/app
endif

# Tell it to build an APK
include $(BUILD_PACKAGE)
include $(CLEAR_VARS)

include $(BUILD_MULTI_PREBUILT)

include $(call all-makefiles-under,$(LOCAL_PATH))
