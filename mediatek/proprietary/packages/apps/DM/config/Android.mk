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

LOCAL_PATH := $(call my-dir)

	
include $(CLEAR_VARS)

ifeq ($(MTK_DM_APP), yes)

LOCAL_MODULE := reminder.xml
LOCAL_MODULE_TAGS := optional
LOCAL_MODULE_CLASS := ETC
LOCAL_MODULE_PATH := $(TARGET_OUT_ETC)/dm
LOCAL_SRC_FILES := $(LOCAL_MODULE)
LOCAL_CERTIFICATE := platform

include $(BUILD_PREBUILT)

include $(CLEAR_VARS)

LOCAL_MODULE := tree.xml
LOCAL_MODULE_TAGS := optional
LOCAL_MODULE_CLASS := ETC
LOCAL_MODULE_PATH := $(TARGET_OUT_ETC)/dm
LOCAL_SRC_FILES := $(LOCAL_MODULE)
LOCAL_CERTIFICATE := platform
include $(BUILD_PREBUILT)

include $(CLEAR_VARS)

LOCAL_MODULE := DmApnInfo.xml
LOCAL_MODULE_TAGS := optional
LOCAL_MODULE_CLASS := ETC
LOCAL_MODULE_PATH := $(TARGET_OUT_ETC)/dm
LOCAL_SRC_FILES := $(LOCAL_MODULE)
LOCAL_CERTIFICATE := platform

include $(BUILD_PREBUILT)

include $(CLEAR_VARS)
LOCAL_MODULE := vdmconfig.xml
LOCAL_MODULE_TAGS := optional
LOCAL_MODULE_CLASS := ETC
LOCAL_MODULE_PATH := $(TARGET_OUT_ETC)/dm
LOCAL_SRC_FILES := $(LOCAL_MODULE)
LOCAL_CERTIFICATE := platform

include $(BUILD_PREBUILT)

endif



