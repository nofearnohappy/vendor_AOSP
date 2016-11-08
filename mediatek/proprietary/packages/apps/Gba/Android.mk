# Copyright (C) 2011 The Android Open Source Project
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

ifeq ($(MTK_IMS_SUPPORT),yes)
ENABLE_GBA_SUPPORT := true
endif

ifeq ($(MTK_RCS_SUPPORT),yes)
ENABLE_GBA_SUPPORT := true
endif

ifeq ($(strip $(MTK_CIP_SUPPORT)),yes)
ENABLE_GBA_SUPPORT := true
endif

ifeq ($(ENABLE_GBA_SUPPORT),true)

LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

LOCAL_SRC_FILES := $(call all-java-files-under, src)

LOCAL_PACKAGE_NAME := Gba
LOCAL_CERTIFICATE := platform

LOCAL_JAVA_LIBRARIES += mediatek-framework
LOCAL_JAVA_LIBRARIES += telephony-common

ifeq ($(strip $(MTK_CIP_SUPPORT)),yes)
LOCAL_MODULE_PATH := $(TARGET_CUSTOM_OUT)/app
endif

include $(BUILD_PACKAGE)

# Use the folloing include to make our test apk.
include $(call all-makefiles-under,$(LOCAL_PATH))

endif