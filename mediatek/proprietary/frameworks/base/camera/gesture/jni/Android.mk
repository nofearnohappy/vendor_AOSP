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

# This makefile supplies the rules for building a library of JNI code for
# use by our example of how to bundle a shared library with an APK.

LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := samples

# This is the target being built.
LOCAL_MODULE:= libjni_gesture

LOCAL_MULTILIB := 32

#-----------------------------------------------------------
LOCAL_C_INCLUDES += \
    $(MTK_PATH_SOURCE)/frameworks/av/include/ \
    $(TOP)/$(MTK_PATH_SOURCE)/frameworks/av/services/mmsdk/include \
    $(TOP)/$(MTK_PATH_SOURCE)/hardware/include/ \
    $(TOP)/$(MTK_PATH_SOURCE)/hardware/mtkcam/legacy/include/mtkcam \
    $(TOP)/$(MTK_PATH_SOURCE)/hardware/gralloc_extra/include \
    $(TOP)/system/media/camera/include/ \

ifeq ($(strip $(TARGET_BOARD_PLATFORM)), mt6797)
#swo
LOCAL_C_INCLUDES += $(MTK_PATH_SOURCE)/hardware/mtkcam/common/include
else
endif
#-----------------------------------------------------------
LOCAL_SHARED_LIBRARIES += libmmsdkservice
LOCAL_SHARED_LIBRARIES += libbinder


# All of the source files that we will compile.
LOCAL_SRC_FILES:= \
    gesture_jni.cpp \

# All of the shared libraries we link against.
LOCAL_SHARED_LIBRARIES += \
    libandroid_runtime \
    libcutils \
    libutils \

# No static libraries.
LOCAL_STATIC_LIBRARIES :=


# No special compiler flags.
LOCAL_CFLAGS +=

include $(BUILD_SHARED_LIBRARY)
